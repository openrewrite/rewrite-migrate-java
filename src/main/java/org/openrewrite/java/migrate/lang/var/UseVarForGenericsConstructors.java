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
package org.openrewrite.java.migrate.lang.var;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

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
        private final JavaTemplate template = JavaTemplate.builder("var #{} = #{any()}")
                .javaParser(JavaParser.fromJavaVersion()).build();

        @Override
        public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations vd, ExecutionContext ctx) {
            vd = super.visitVariableDeclarations(vd, ctx);

            boolean isGeneralApplicable = DeclarationCheck.isVarApplicable(this.getCursor(), vd);
            if (!isGeneralApplicable) return vd;

            // recipe specific
            boolean isPrimitive = DeclarationCheck.isPrimitive(vd);
            boolean usesNoGenerics = !DeclarationCheck.useGenerics(vd);
            boolean usesTernary = DeclarationCheck.initializedByTernary(vd);
            if (isPrimitive || usesTernary || usesNoGenerics) return vd;

            //now we deal with generics
            J.VariableDeclarations.NamedVariable variable = vd.getVariables().get(0);
            List<JavaType> leftTypes = extractParameters(variable.getVariableType());
            List<JavaType> rightTypes = extractParameters(variable.getInitializer());
            if (rightTypes == null || (leftTypes.isEmpty() && rightTypes.isEmpty())) return vd;

            // skip generics with type bounds, it's not yet implemented
            boolean genericHasBounds = leftTypes.stream()
                    .filter(t -> t instanceof JavaType.GenericTypeVariable)
                    .map(t -> (JavaType.GenericTypeVariable) t)
                    .map(t -> !t.getBounds().isEmpty())
                    .reduce(false, Boolean::logicalOr);
            if (genericHasBounds) return vd;

            // mark imports for removal if unused
            if (vd.getType() instanceof JavaType.FullyQualified) maybeRemoveImport((JavaType.FullyQualified) vd.getType());

            return transformToVar(vd, leftTypes, rightTypes);
        }

        /**
         * Tries to extract the generic parameters from the expression,
         * if the Initializer is no new class or not of a parameterized type, returns null to signale "no info".
         * if the initializer uses empty diamonds use an empty list to signale no type information
         * @param initializer to extract parameters from
         * @return null or list of type parameters in diamond
         */
        private @Nullable List<JavaType> extractParameters(@Nullable Expression initializer) {
            if (initializer instanceof J.NewClass) {
                TypeTree clazz = ((J.NewClass) initializer).getClazz();
                if (clazz instanceof J.ParameterizedType) {
                    List<Expression> typeParameters = ((J.ParameterizedType) clazz).getTypeParameters();
                    if (typeParameters != null) {
                        return typeParameters
                                .stream()
                                .map(Expression::getType)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());
                    } else {
                        return new ArrayList<>();
                    }
                }
            }
            return null;
        }

        /**
         * Try to extract the parameters from the variables type.
         * @param variable to extract from
         * @return may be empty list of type parameters
         */
        private List<JavaType> extractParameters(@Nullable JavaType.Variable variable) {
            if (variable != null && variable.getType() instanceof JavaType.Parameterized) {
                return ((JavaType.Parameterized) variable.getType()).getTypeParameters();
            } else {
                return new ArrayList<>();
            }
        }

        private J.VariableDeclarations transformToVar(J.VariableDeclarations vd, List<JavaType> leftTypes, List<JavaType> rightTypes) {
            Expression initializer = vd.getVariables().get(0).getInitializer();
            String simpleName = vd.getVariables().get(0).getSimpleName();


            // if left is defined but not right, copy types to initializer
            if(rightTypes.isEmpty() && !leftTypes.isEmpty()) {
                // we need to switch type infos from left to right here
                List<Expression> typeArgument = leftTypes.stream()
                        .map(UseVarForGenericsConstructorsVisitor::typeToExpression)
                        .collect(Collectors.toList());

                J.ParameterizedType typedInitializerClazz = ((J.ParameterizedType) ((J.NewClass) initializer)
                        .getClazz())
                        .withTypeParameters(typeArgument);
                initializer = ((J.NewClass) initializer).withClazz(typedInitializerClazz);
            }

            J.VariableDeclarations result = template.<J.VariableDeclarations>apply(getCursor(), vd.getCoordinates().replace(), simpleName, initializer)
                    .withPrefix(vd.getPrefix());

            // apply modifiers like final
            List<J.Modifier> modifiers = vd.getModifiers();
            boolean hasModifiers = !modifiers.isEmpty();
            if (hasModifiers) {
                result = result.withModifiers(modifiers);
            }

            // apply prefix to type expression
            TypeTree resultingTypeExpression = result.getTypeExpression();
            boolean resultHasTypeExpression = resultingTypeExpression != null;
            if (resultHasTypeExpression) {
                result = result.withTypeExpression(resultingTypeExpression.withPrefix(vd.getTypeExpression().getPrefix()));
            }

            return result;
        }

        /**
         * recursively map a JavaType to an Expression with same semantics
         * @param type to map
         * @return semantically equal Expression
         */
        private static Expression typeToExpression(JavaType type) {
            if (type instanceof JavaType.Class) { // easy just parse to identifier
                String className = ((JavaType.Class) type).getClassName();
                return new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, className, type, null);
            }
            if (type instanceof JavaType.GenericTypeVariable) {
                String variableName = ((JavaType.GenericTypeVariable) type).getName();
                J.Identifier identifier = new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, variableName, type, null);

                List<JavaType> bounds1 = ((JavaType.GenericTypeVariable) type).getBounds();
                if (bounds1.isEmpty()) {
                    return identifier;
                } else {
                    /*
                    List<JRightPadded<TypeTree>> bounds = bounds1.stream()
                            .map(b -> new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, , null, null))
                            .map(JRightPadded::build)
                            .collect(Collectors.toList());

                    return new J.TypeParameter(Tree.randomId(), Space.EMPTY, Markers.EMPTY, new ArrayList<>(), identifier, JContainer.build(bounds));
                     */
                    throw new IllegalStateException("Generic type variables with bound are not supported, yet.");
                }
            }
            if (type instanceof JavaType.Parameterized) { // recursively parse
                List<JRightPadded<Expression>> typeParams = ((JavaType.Parameterized) type).getTypeParameters().stream()
                        .map(UseVarForGenericsConstructorsVisitor::typeToExpression)
                        .map(JRightPadded::build)
                        .collect(Collectors.toList());

                NameTree clazz = new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, ((JavaType.Parameterized) type).getClassName(),null, null);
                return new J.ParameterizedType(Tree.randomId(), Space.EMPTY, Markers.EMPTY, clazz, JContainer.build(typeParams), type);
            }

            throw new IllegalArgumentException(String.format("Unable to parse expression from JavaType %s", type));
        }
    }
}
