/*
 * Copyright 2024 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.migrate.lang.var;

import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.java.tree.Space.EMPTY;

public class UseVarForGenericsConstructors extends Recipe {
    @Override
    public String getDisplayName() {
        //language=markdown
        return "Apply `var` to Generic Constructors";
    }

    @Override
    public String getDescription() {
        //language=markdown
        return "Apply `var` to generics variables initialized by constructor calls.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                new UsesJavaVersion<>(10),
                new UseVarForGenericsConstructorsVisitor());
    }

    static final class UseVarForGenericsConstructorsVisitor extends JavaIsoVisitor<ExecutionContext> {
        @Override
        public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations vd, ExecutionContext ctx) {
            vd = super.visitVariableDeclarations(vd, ctx);

            boolean isGeneralApplicable = DeclarationCheck.isVarApplicable(this.getCursor(), vd);
            if (!isGeneralApplicable) {
                return vd;
            }

            // Recipe specific
            boolean isPrimitive = DeclarationCheck.isPrimitive(vd);
            boolean usesNoGenerics = !DeclarationCheck.useGenerics(vd);
            boolean usesTernary = DeclarationCheck.initializedByTernary(vd);
            if (isPrimitive || usesTernary || usesNoGenerics) {
                return vd;
            }

            // Now we deal with generics
            J.VariableDeclarations.NamedVariable variable = vd.getVariables().get(0);
            List<JavaType> leftTypes = extractTypeParameters(variable.getVariableType());
            List<JavaType> rightTypes = extractTypeParameters(variable.getInitializer());
            if (rightTypes == null || (leftTypes.isEmpty() && rightTypes.isEmpty())) {
                return vd;
            }

            // Java does not support declaration-site variance (see https://openjdk.org/jeps/300), things like `var x = new ArrayList<? extends Object>()` do not compile.
            // Therefore, skip variable declarations with generic wildcards.
            boolean genericHasBounds = anyTypeHasBounds(leftTypes);
            if (genericHasBounds) {
                return vd;
            }

            // Mark imports for removal if unused
            if (vd.getType() instanceof JavaType.FullyQualified) {
                maybeRemoveImport((JavaType.FullyQualified) vd.getType());
            }

            return transformToVar(vd, leftTypes, rightTypes, ctx);
        }

        private static Boolean anyTypeHasBounds(List<JavaType> leftTypes) {
            for (JavaType type : leftTypes) {
                if (type instanceof JavaType.Parameterized) {
                    return anyTypeHasBounds(((JavaType.Parameterized) type).getTypeParameters());
                }
                if (type instanceof JavaType.GenericTypeVariable) {
                    return !((JavaType.GenericTypeVariable) type).getBounds().isEmpty();
                }
            }
            return false;
        }

        /**
         * Tries to extract the generic parameters from the expression,
         * if the Initializer is no new class or not of a parameterized type, returns null to signal "no info".
         * if the initializer uses empty diamonds, use an empty list to signal no type information
         * @param initializer to extract parameters from
         * @return null or list of type parameters in diamond
         */
        private @Nullable List<JavaType> extractTypeParameters(@Nullable Expression initializer) {
            if (initializer instanceof J.NewClass) {
                TypeTree clazz = ((J.NewClass) initializer).getClazz();
                if (clazz instanceof J.ParameterizedType) {
                    List<Expression> typeParameters = ((J.ParameterizedType) clazz).getTypeParameters();
                    List<JavaType> params = new ArrayList<>();
                    if (typeParameters != null) {
                        for (Expression curType : typeParameters) {
                            JavaType type = curType.getType();
                            if (type != null) {
                                params.add(type);
                            }
                        }
                    }
                    return params;
                }
            }
            return null;
        }

        private List<JavaType> extractTypeParameters(JavaType.@Nullable Variable variable) {
            if (variable != null && variable.getType() instanceof JavaType.Parameterized) {
                return ((JavaType.Parameterized) variable.getType()).getTypeParameters();
            }
            return new ArrayList<>();
        }

        private J.VariableDeclarations transformToVar(J.VariableDeclarations vd, List<JavaType> leftTypes, List<JavaType> rightTypes, ExecutionContext ctx) {
            J.VariableDeclarations.NamedVariable varZero = vd.getVariables().get(0);
            Expression initializer = Objects.requireNonNull(varZero.getInitializer());

            // if left is defined but right is not, copy types to initializer
            if (rightTypes.isEmpty() && !leftTypes.isEmpty()) {
                // we need to switch type infos from left to right here
                List<Expression> typeExpressions = leftTypes.stream().map(UseVarForGenericsConstructorsVisitor::typeToExpression).collect(toList());
                J.ParameterizedType typedInitializerClazz = ((J.ParameterizedType) ((J.NewClass) initializer).getClazz())
                        .withTypeParameters(typeExpressions);
                initializer = ((J.NewClass) initializer).withClazz(typedInitializerClazz);
            }

            // Replace actual type by `var` keyword and replace the first variable's name, initializer and type
            Expression finalInitializer = initializer;
            List<J.VariableDeclarations.NamedVariable> variables = ListUtils.mapFirst(vd.getVariables(), it -> {
                JavaType.Variable variableType = it.getVariableType() == null ? null : it.getVariableType().withOwner(null);
                return it
                        .withName(it.getName().withType(finalInitializer.getType()).withFieldType(variableType))
                        .withInitializer(finalInitializer)
                        .withVariableType(variableType);
            });
            J.Identifier typeExpression = new J.Identifier(randomId(), vd.getTypeExpression().getPrefix(),
                    Markers.build(singleton(JavaVarKeyword.build())), emptyList(), "var", initializer.getType(), null);

            return maybeAutoFormat(vd, vd.withVariables(variables).withTypeExpression(typeExpression), ctx);
        }

        /**
         * recursively map a JavaType to an Expression with the same semantics
         * @param type to map
         * @return semantically equal Expression
         */
        private static Expression typeToExpression(JavaType type) {
            if (type instanceof JavaType.Primitive) {
                JavaType.Primitive primitiveType = JavaType.Primitive.fromKeyword(((JavaType.Primitive) type).getKeyword());
                return new J.Primitive(randomId(), EMPTY, Markers.EMPTY, primitiveType);
            } else if (type instanceof JavaType.Class) {
                String className = ((JavaType.Class) type).getClassName();
                return new J.Identifier(randomId(), EMPTY, Markers.EMPTY, emptyList(), className, type, null);
            } else if (type instanceof JavaType.Array) {
                TypeTree elemType = (TypeTree) typeToExpression(((JavaType.Array) type).getElemType());
                return new J.ArrayType(randomId(), EMPTY, Markers.EMPTY, elemType, null, JLeftPadded.build(EMPTY), type);
            } else if (type instanceof JavaType.GenericTypeVariable) {
                String variableName = ((JavaType.GenericTypeVariable) type).getName();
                J.Identifier identifier = new J.Identifier(randomId(), EMPTY, Markers.EMPTY, emptyList(), variableName, type, null);
                if (((JavaType.GenericTypeVariable) type).getBounds().isEmpty()) {
                    return identifier;
                }
                throw new IllegalStateException("Declaration-site variance type variables are not supported in Java.");
            } else if (type instanceof JavaType.Parameterized) { // recursively parse
                List<JRightPadded<Expression>> typeParamsExpression = ((JavaType.Parameterized) type).getTypeParameters().stream()
                        .map(curType -> JRightPadded.build(typeToExpression(curType)))
                        .collect(toList());
                NameTree clazz = new J.Identifier(randomId(), EMPTY, Markers.EMPTY, emptyList(), ((JavaType.Parameterized) type).getClassName(), null, null);
                return new J.ParameterizedType(randomId(), EMPTY, Markers.EMPTY, clazz, JContainer.build(typeParamsExpression), type);
            }
            throw new IllegalArgumentException(String.format("Unable to parse expression from JavaType %s", type));
        }
    }
}
