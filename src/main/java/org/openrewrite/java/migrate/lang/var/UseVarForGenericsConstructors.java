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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
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
import static org.openrewrite.Tree.randomId;

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

            return transformToVar(vd, leftTypes, rightTypes);
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
         *
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

        private J.VariableDeclarations transformToVar(J.VariableDeclarations vd, List<JavaType> leftTypes, List<JavaType> rightTypes) {
            J.VariableDeclarations.NamedVariable varZero = vd.getVariables().get(0);
            J.NewClass initializer = Objects.requireNonNull((J.NewClass) varZero.getInitializer());

            // If left is defined but right is not, copy types from typeExpression to initializer
            if (rightTypes.isEmpty() && !leftTypes.isEmpty() && vd.getTypeExpression() instanceof J.ParameterizedType && initializer.getClazz() instanceof J.ParameterizedType) {
                J.ParameterizedType typedInitializerClazz = ((J.ParameterizedType) initializer.getClazz())
                        .withTypeParameters(((J.ParameterizedType) vd.getTypeExpression()).getTypeParameters());
                initializer = initializer.withClazz(typedInitializerClazz);
            }

            // Replace actual type by `var` keyword and replace the first variable's name, initializer and type
            J.NewClass finalInitializer = initializer;
            List<J.VariableDeclarations.NamedVariable> variables = ListUtils.mapFirst(vd.getVariables(), it -> {
                JavaType.Variable variableType = it.getVariableType() == null ? null : it.getVariableType().withOwner(null);
                return it
                        .withName(it.getName().withType(finalInitializer.getType()).withFieldType(variableType))
                        .withInitializer(finalInitializer)
                        .withVariableType(variableType);
            });
            J.Identifier typeExpression = new J.Identifier(randomId(), vd.getTypeExpression().getPrefix(),
                    Markers.build(singleton(JavaVarKeyword.build())), emptyList(), "var", initializer.getType(), null);

            return vd.withVariables(variables).withTypeExpression(typeExpression);
        }
    }
}
