/*
 * Copyright 2021 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.migrate.lang;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.search.HasJavaVersion;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeTree;

import java.time.Duration;

import static java.lang.String.format;
import static java.util.Objects.*;

@Value
@EqualsAndHashCode(callSuper = false)
public class UseVarKeyword extends Recipe {
    public String getDisplayName() {
        return "Use local variable type-inference (var) where possible";
    }

    @Override
    public String getDescription() {
        return "Local variable type-inference reduce the noise produces by repeating the type definitions in Java 10 or higher.";
    }

    @Override
    public @Nullable Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(1);
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new HasJavaVersion("10", true).getVisitor();
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        // J.VariableDeclarations
        return new JavaVisitor<ExecutionContext>() {
            private final JavaType.Primitive SHORT_TYPE = JavaType.Primitive.Short;
            private final JavaType.Primitive BYTE_TYPE = JavaType.Primitive.Byte;
            private final JavaTemplate template = JavaTemplate.builder(this::getCursor, "var #{} = #{any()}")
                    .javaParser(JavaParser.fromJavaVersion()).build();


            @Override
            public J visitForEachControl(J.ForEachLoop.Control control, ExecutionContext executionContext) {
                // für enhanced for-Loops wäre hier der korrekter punkt.
                // es gelten dieselben Dinge für primitives, null und Generics
                // ob wir am richtigen Ort sind muss nicht geprüft werden; ebenso single und pure
                return super.visitForEachControl(control, executionContext);
            }

            @Override
            public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext executionContext) {
                J.VariableDeclarations vd = (J.VariableDeclarations) super.visitVariableDeclarations(multiVariable, executionContext);

                boolean isOutsideMethod = !determineIfIsInsideMethod(this.getCursor());
                boolean isMethodParameter = determineIfMethodParameter(vd, this.getCursor());
                boolean isOutsideInitializer = !determineIfOutsideInitializer(this.getCursor(), false);
                if ((isOutsideMethod && isOutsideInitializer) || isMethodParameter) return vd;

                TypeTree typeExpression = vd.getTypeExpression();
                boolean isByteVariable = typeExpression instanceof J.Primitive && BYTE_TYPE.equals(typeExpression.getType());
                boolean isShortVariable = typeExpression instanceof J.Primitive && SHORT_TYPE.equals(typeExpression.getType());
                if (isByteVariable || isShortVariable) return vd;

                boolean definesSigleVariable = vd.getVariables().size() == 1;
                boolean isPureAssigment = JavaType.Primitive.Null.equals(vd.getType());
                if (!definesSigleVariable || isPureAssigment) return vd;

                Expression initializer = vd.getVariables().get(0).getInitializer();
                boolean isDeclarationOnly = isNull(initializer);
                if (isDeclarationOnly) return vd;

                initializer = initializer.unwrap();
                boolean isNullAssigment = initializer instanceof J.Literal && isNull(((J.Literal) initializer).getValue());
                boolean alreadyUseVar = typeExpression instanceof J.Identifier && "var".equals(((J.Identifier) typeExpression).getSimpleName());
                boolean isGenericDefinition = typeExpression instanceof J.ParameterizedType ;
                boolean isGenericInitializer = initializer instanceof J.NewClass && ((J.NewClass) initializer).getClazz() instanceof J.ParameterizedType;
                boolean useTernary = initializer instanceof J.Ternary;
                if (alreadyUseVar || isNullAssigment|| isGenericDefinition || isGenericInitializer|| useTernary) return vd;

                return transformToVar(vd);
            }

            private boolean determineIfOutsideInitializer(Cursor cursor, boolean childWasBlock) {
                Object currentStatement = cursor.getValue();

                boolean isClassDeclaration = currentStatement instanceof J.ClassDeclaration;
                boolean classFollowedByBlock = childWasBlock && isClassDeclaration;
                if (classFollowedByBlock) return true;

                Cursor parentStatement = cursor.getParent();
                boolean cannotClimbUpFurther = isNull(parentStatement);
                if (cannotClimbUpFurther) return false;

                boolean isBlock = currentStatement instanceof J.Block;
                return determineIfOutsideInitializer(parentStatement, isBlock);
            }

            private boolean determineIfMethodParameter(@NotNull J.VariableDeclarations vd, @NotNull Cursor cursor) {
                J.MethodDeclaration methodDeclaration = cursor.firstEnclosing(J.MethodDeclaration.class);
                return nonNull(methodDeclaration) && methodDeclaration.getParameters().contains(vd);
            }

            /**
             * Determines if a cursor is contained inside a Method declaration without an intermediate Class declaration
             * @param cursor value to determine
             */
            private boolean determineIfIsInsideMethod(@NotNull Cursor cursor) {
                Object current = cursor.getValue();

                if (Cursor.ROOT_VALUE.equals(current)) return false; // we are at the top, no further climbing needed
                if (current instanceof J.ClassDeclaration)
                    return false; // after a ClassDeclaration we left the scope of search
                if (current instanceof J.MethodDeclaration) return true; // we found the MethodDeclaration

                return determineIfIsInsideMethod(requireNonNull(cursor.getParent())); // climb up
            }

            @NotNull
            private J.VariableDeclarations transformToVar(@NotNull J.VariableDeclarations vd) {
                Expression initializer = vd.getVariables().get(0).getInitializer();
                String simpleName = vd.getVariables().get(0).getSimpleName();

                if (initializer instanceof J.Literal) {
                    initializer = expandWithPrimitivTypeHint(vd, initializer);
                } else if(initializer instanceof J.MethodInvocation && nonNull(((J.MethodInvocation) initializer).getTypeParameters())) {
                    initializer = initializer;
                } else if(initializer instanceof J.NewClass && ((J.NewClass) initializer).getClazz() instanceof J.ParameterizedType) {

                }
                return vd.withTemplate(template, vd.getCoordinates().replace(), simpleName, initializer);
            }

            @NotNull
            private Expression expandWithPrimitivTypeHint(@NotNull J.VariableDeclarations vd, @NotNull Expression initializer) {
                String valueSource = ((J.Literal) initializer).getValueSource();

                if (isNull(valueSource)) return initializer;

                boolean isLongLiteral = JavaType.Primitive.Long.equals(vd.getType());
                boolean inferredAsLong = valueSource.endsWith("l") || valueSource.endsWith("L");
                boolean isFloatLiteral = JavaType.Primitive.Float.equals(vd.getType());
                boolean inferredAsFloat = valueSource.endsWith("f") || valueSource.endsWith("F");
                boolean isDoubleLiteral = JavaType.Primitive.Double.equals(vd.getType());
                boolean inferredAsDouble = valueSource.endsWith("d") || valueSource.endsWith("D") || valueSource.contains(".");

                String typNotation = null;
                if (isLongLiteral && !inferredAsLong) {
                    typNotation = "L";
                } else if (isFloatLiteral && !inferredAsFloat) {
                    typNotation = "F";
                } else if (isDoubleLiteral && !inferredAsDouble) {
                    typNotation = "D";
                }

                if (nonNull(typNotation)) {
                    initializer = ((J.Literal) initializer).withValueSource(format("%s%s", valueSource, typNotation));
                }

                return initializer;
            }
        };
    }
}
