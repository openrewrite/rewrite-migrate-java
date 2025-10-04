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
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeTree;

import java.util.ArrayList;
import java.util.List;

public class UseVarForGenericMethodInvocations extends Recipe {
    @Override
    public String getDisplayName() {
        //language=markdown
        return "Apply `var` to generic method invocations";
    }

    @Override
    public String getDescription() {
        //language=markdown
        return "Apply `var` to variables initialized by invocations of generic methods. " +
                "This recipe ignores generic factory methods without parameters, because open rewrite cannot handle them correctly ATM.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                new UsesJavaVersion<>(10),
                new UseVarForGenericMethodInvocations.UseVarForGenericsVisitor());
    }

    static final class UseVarForGenericsVisitor extends JavaIsoVisitor<ExecutionContext> {
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

            // Now we deal with generics, check for method invocations
            Expression initializer = vd.getVariables().get(0).getInitializer();
            boolean isMethodInvocation = initializer != null && initializer.unwrap() instanceof J.MethodInvocation;
            if (!isMethodInvocation) {
                return vd;
            }

            // If no type parameters and no arguments are present, we assume the type is too hard to determine
            boolean hasNoTypeParams = ((J.MethodInvocation) initializer).getTypeParameters() == null;
            boolean argumentsEmpty = allArgumentsEmpty((J.MethodInvocation) initializer);
            if (hasNoTypeParams && argumentsEmpty) {
                return vd;
            }

            if (vd.getType() instanceof JavaType.FullyQualified) {
                maybeRemoveImport((JavaType.FullyQualified) vd.getType());
            }

            // Make nested generic types explicit before converting to var
            J.VariableDeclarations finalVd = vd;
            return DeclarationCheck.transformToVar(vd, (J.MethodInvocation mi) -> makeNestedGenericsExplicit(mi, finalVd));
        }

        /**
         * Makes nested generic types explicit by replacing diamond operators in constructor calls
         * with explicit type parameters based on the variable declaration type.
         */
        private J.MethodInvocation makeNestedGenericsExplicit(J.MethodInvocation mi, J.VariableDeclarations vd) {
            // Extract type parameters from the variable declaration
            if (!(vd.getTypeExpression() instanceof J.ParameterizedType)) {
                return mi;
            }

            J.ParameterizedType leftType = (J.ParameterizedType) vd.getTypeExpression();
            List<Expression> leftTypeParams = leftType.getTypeParameters();
            if (leftTypeParams == null || leftTypeParams.isEmpty()) {
                return mi;
            }

            // Visit arguments and replace diamond operators with explicit type parameters
            return mi.withArguments(ListUtils.map(mi.getArguments(), arg -> {
                if (arg instanceof J.NewClass) {
                    J.NewClass newClass = (J.NewClass) arg;
                    List<JavaType> rightTypeParams = extractJavaTypes(newClass);
                    // Check if using diamond operator (rightTypeParams is empty)
                    if (rightTypeParams != null && rightTypeParams.isEmpty() && newClass.getClazz() instanceof J.ParameterizedType) {
                        // Copy type parameters from left side to right side
                        J.ParameterizedType rightType = (J.ParameterizedType) newClass.getClazz();
                        return newClass.withClazz(
                                rightType.withTypeParameters(leftTypeParams)
                        );
                    }
                }
                return arg;
            }));
        }

        /**
         * Extract JavaTypes from a NewClass expression's type parameters.
         *
         * @return null if not a parameterized type, or an empty list for diamond operator.
         */
        private @Nullable List<JavaType> extractJavaTypes(J.NewClass newClass) {
            TypeTree clazz = newClass.getClazz();
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
            return null;
        }

        private static boolean allArgumentsEmpty(J.MethodInvocation invocation) {
            for (Expression argument : invocation.getArguments()) {
                if (!(argument instanceof J.Empty)) {
                    return false;
                }
            }
            return true;
        }
    }
}
