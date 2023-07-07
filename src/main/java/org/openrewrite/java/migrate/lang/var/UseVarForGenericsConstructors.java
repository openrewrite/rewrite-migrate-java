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
                new UseVarForGenericsConstructors.UseVarForGenericsVisitor());
    }

    static final class UseVarForGenericsVisitor extends JavaIsoVisitor<ExecutionContext> {
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

            return transformToVar(vd, leftTypes, rightTypes);
        }

        /**
         * Tries to extract the genric parameters from the expression,
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

        private List<JavaType> extractParameters(@Nullable JavaType.Variable variableType) {
            if (variableType != null && variableType.getType() instanceof JavaType.Parameterized) {
                return ((JavaType.Parameterized) variableType.getType()).getTypeParameters();
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
                        .map(t ->
                                new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, ((JavaType.Class)t).getClassName(), t, null))
                        .collect(Collectors.toList());
                J.ParameterizedType typedInitializerClazz = ((J.ParameterizedType) ((J.NewClass) initializer).getClazz()).withTypeParameters(typeArgument);
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
    }
}
