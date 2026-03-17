/*
 * Copyright 2025 the original author or authors.
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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.tree.*;

import java.util.List;

/**
 * Replaces explicit type declarations with {@code var} keyword when the initializer
 * is a constructor call with an exactly matching type.
 *
 * <p>This recipe is more conservative than {@link UseVarForObject} and
 * {@link UseVarForGenericsConstructors}. It only transforms when the declared type
 * exactly matches the constructor type, avoiding cases where the declared type is an
 * interface or supertype.
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class UseVarForConstructors extends Recipe {

    String displayName = "Use `var` for constructor call assignments";

    String description = "Replace explicit type declarations with `var` when the variable is initialized with a " +
            "constructor call of exactly the same type. Does not transform when declared type " +
            "differs from constructor type (e.g., interface vs implementation).";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                new UsesJavaVersion<>(10),
                new UseVarForConstructorsVisitor());
    }

    static final class UseVarForConstructorsVisitor extends JavaIsoVisitor<ExecutionContext> {

        @Override
        public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations vd, ExecutionContext ctx) {
            vd = super.visitVariableDeclarations(vd, ctx);

            if (!DeclarationCheck.isVarApplicable(getCursor(), vd)) {
                return vd;
            }

            Expression initializer = vd.getVariables().get(0).getInitializer();
            if (initializer == null) {
                return vd;
            }
            initializer = initializer.unwrap();

            // Only transform constructor calls
            if (!(initializer instanceof J.NewClass)) {
                return vd;
            }

            // Declared type must exactly match constructor type
            if (!TypeUtils.isOfType(vd.getType(), initializer.getType())) {
                return vd;
            }

            if (vd.getType() instanceof JavaType.FullyQualified) {
                maybeRemoveImport((JavaType.FullyQualified) vd.getType());
            }

            J.VariableDeclarations finalVd = vd;
            return DeclarationCheck.<J.NewClass>transformToVar(vd, it -> maybeTransferTypeArguments(finalVd, it));
        }

        private static J.NewClass maybeTransferTypeArguments(J.VariableDeclarations vd, J.NewClass initializer) {
            TypeTree typeExpression = vd.getTypeExpression();

            if (!(typeExpression instanceof J.ParameterizedType)) {
                return initializer;
            }
            J.ParameterizedType paramType = (J.ParameterizedType) typeExpression;

            List<Expression> declaredTypeParams = paramType.getTypeParameters();
            if (declaredTypeParams == null || declaredTypeParams.isEmpty()) {
                return initializer;
            }

            TypeTree constructorClazz = initializer.getClazz();
            if (!(constructorClazz instanceof J.ParameterizedType)) {
                return initializer;
            }
            J.ParameterizedType constructorParamType = (J.ParameterizedType) constructorClazz;

            List<Expression> constructorTypeParams = constructorParamType.getTypeParameters();
            if (constructorTypeParams == null || isDiamondOperator(constructorTypeParams)) {
                J.ParameterizedType newClazz = constructorParamType.withTypeParameters(declaredTypeParams);
                return initializer.withClazz(newClazz);
            }

            return initializer;
        }

        private static boolean isDiamondOperator(List<Expression> typeParams) {
            return typeParams.isEmpty() || typeParams.stream().allMatch(J.Empty.class::isInstance);
        }
    }
}
