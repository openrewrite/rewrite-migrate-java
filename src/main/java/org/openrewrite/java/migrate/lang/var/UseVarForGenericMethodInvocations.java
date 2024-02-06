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

import org.openrewrite.*;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;

public class UseVarForGenericMethodInvocations extends Recipe {
    @Override
    public String getDisplayName() {
        //language=markdown
        return "Apply `var` to Generic Method Invocations";
    }

    @Override
    public String getDescription() {
        //language=markdown
        return "Apply `var` to variables initialized by invocations of Generic Methods. " +
               "This recipe ignores generic factory methods without parameters, because open rewrite cannot handle them correctly ATM.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                new UsesJavaVersion<>(10),
                new UseVarForGenericMethodInvocations.UseVarForGenericsVisitor());
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

            //now we deal with generics, check for method invocations
            Expression initializer = vd.getVariables().get(0).getInitializer();
            boolean isMethodInvocation = initializer != null && initializer.unwrap() instanceof J.MethodInvocation;
            if (!isMethodInvocation) return vd;

            //if no type paramters are present and no arguments we assume the type is hard to determine a needs manual action
            boolean hasNoTypeParams = ((J.MethodInvocation) initializer).getTypeParameters() == null;
            boolean argumentsEmpty = allArgumentsEmpty((J.MethodInvocation) initializer);
            if (hasNoTypeParams && argumentsEmpty) return vd;

            // mark imports for removal if unused
            if (vd.getType() instanceof JavaType.FullyQualified) maybeRemoveImport((JavaType.FullyQualified) vd.getType());

            return transformToVar(vd, new ArrayList<>(), new ArrayList<>());
        }

        private static boolean allArgumentsEmpty(J.MethodInvocation invocation) {
            for (Expression argument : invocation.getArguments()) {
                if (!(argument instanceof J.Empty)) {
                    return false;
                }
            }
            return true;
        }

        private J.VariableDeclarations transformToVar(J.VariableDeclarations vd, List<JavaType> leftTypes, List<JavaType> rightTypes) {
            Expression initializer = vd.getVariables().get(0).getInitializer();
            String simpleName = vd.getVariables().get(0).getSimpleName();

            // if left is defined but not right, copy types to initializer
            if (rightTypes.isEmpty() && !leftTypes.isEmpty()) {
                // we need to switch type infos from left to right here
                List<Expression> typeArgument = new ArrayList<>();
                for (JavaType t : leftTypes) {
                    typeArgument.add(new J.Identifier(Tree.randomId(), Space.EMPTY, Markers.EMPTY, emptyList(), ((JavaType.Class) t).getClassName(), t, null));
                }
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
