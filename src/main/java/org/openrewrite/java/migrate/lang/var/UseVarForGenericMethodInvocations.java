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

import static java.util.Objects.requireNonNull;

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
         * Also adds explicit type parameters to the method invocation itself when needed.
         */
        private J.MethodInvocation makeNestedGenericsExplicit(J.MethodInvocation mi, J.VariableDeclarations vd) {
            // Extract type parameters from the variable declaration
            if (!(vd.getTypeExpression() instanceof J.ParameterizedType)) {
                return mi;
            }

            List<Expression> leftTypeParams = ((J.ParameterizedType) vd.getTypeExpression()).getTypeParameters();
            if (leftTypeParams == null || leftTypeParams.isEmpty()) {
                return mi;
            }

            // Add explicit type parameters when the method is generic and the return type's type parameter matches a type parameter from the declaring class
            if (mi.getTypeParameters() == null && mi.getMethodType() != null && containsGenericTypeVariable(mi.getMethodType().getReturnType())) {
                // Create JRightPadded list from leftTypeParams
                List<JRightPadded<Expression>> typeParamsList = new ArrayList<>();
                for (Expression typeParam : leftTypeParams) {
                    typeParamsList.add(JRightPadded.build(typeParam));
                }
                mi = mi.withTypeParameters(JContainer.build(Space.EMPTY, typeParamsList, Markers.EMPTY));
            }

            // Visit arguments and replace diamond operators with explicit type parameters
            return mi.withArguments(ListUtils.map(mi.getArguments(), arg -> {
                if (arg instanceof J.NewClass) {
                    J.NewClass newClass = (J.NewClass) arg;
                    // Check if using diamond operator (rightTypeParams is empty)
                    if (!hasTypeParams(newClass.getClazz())) {
                        // Copy type parameters from left side to right side
                        J.ParameterizedType rightType = (J.ParameterizedType) newClass.getClazz();
                        return newClass.withClazz(requireNonNull(rightType).withTypeParameters(leftTypeParams));
                    }
                }
                return arg;
            }));
        }

        private boolean containsGenericTypeVariable(JavaType type) {
            if (type instanceof JavaType.GenericTypeVariable) {
                return true;
            }

            if (type instanceof JavaType.Parameterized) {
                for (JavaType typeParam : ((JavaType.Parameterized) type).getTypeParameters()) {
                    if (containsGenericTypeVariable(typeParam)) {
                        return true;
                    }
                }
            }

            return false;
        }

        private static boolean hasTypeParams(@Nullable TypeTree clazz) {
            if (clazz instanceof J.ParameterizedType) {
                List<Expression> typeParameters = ((J.ParameterizedType) clazz).getTypeParameters();
                if (typeParameters != null) {
                    for (Expression curType : typeParameters) {
                        if (curType.getType() != null) {
                            return true;
                        }
                    }
                }
            }
            return false;
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
