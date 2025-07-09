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

import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

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

            return DeclarationCheck.transformToVar(vd);
            // TODO implement to support cases like `var strs = List.<String>of();`
            /*return DeclarationCheck.<J.MethodInvocation>transformToVar(vd, it -> {
                // if left is defined but not right, copy types to initializer
                if (finalVd.getTypeExpression() instanceof J.ParameterizedType && !((J.ParameterizedType) finalVd.getTypeExpression()).getTypeParameters().isEmpty() && it.getTypeParameters() == null) {
                    return it.withTypeParameters(((J.ParameterizedType) finalVd.getTypeExpression()).getPadding().getTypeParameters());
                }
                return it;
            });*/
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
