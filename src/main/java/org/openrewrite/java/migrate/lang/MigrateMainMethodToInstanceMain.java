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
package org.openrewrite.java.migrate.lang;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.search.SemanticallyEqual;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.staticanalysis.VariableReferences;

import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Collections.emptyList;

public class MigrateMainMethodToInstanceMain extends Recipe {
    @Override
    public String getDisplayName() {
        return "Migrate `public static void main(String[] args)` to instance `void main()`";
    }

    @Override
    public String getDescription() {
        return "Migrate `public static void main(String[] args)` method to instance `void main()` method when the `args` parameter is unused, as supported by JEP 512 in Java 25+.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesJavaVersion<>(25), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                J.MethodDeclaration md = super.visitMethodDeclaration(method, ctx);

                // Check if this is a main method: public static void main(String[] args)
                if (!"main".equals(md.getSimpleName()) ||
                        md.getReturnTypeExpression() == null ||
                        md.getReturnTypeExpression().getType() != JavaType.Primitive.Void ||
                        !md.hasModifier(J.Modifier.Type.Public) ||
                        !md.hasModifier(J.Modifier.Type.Static) ||
                        md.getParameters().size() != 1 ||
                        !(md.getParameters().get(0) instanceof J.VariableDeclarations) ||
                        md.getBody() == null) {
                    return md;
                }

                // Check if parameter is String[] type
                J.VariableDeclarations param = (J.VariableDeclarations) md.getParameters().get(0);
                JavaType paramType = param.getType();
                if (!TypeUtils.isOfClassType(paramType, "java.lang.String") || !(paramType instanceof JavaType.Array)) {
                    return md;
                }

                // Remove the parameter if unused
                if (argumentsUnused(param.getVariables().get(0).getName(), md.getBody())) {
                    md = md.withParameters(emptyList());
                }
                return md.withReturnTypeExpression(md.getReturnTypeExpression().withPrefix(md.getModifiers().get(0).getPrefix()))
                        .withModifiers(emptyList());
            }

            private boolean argumentsUnused(J.Identifier variableName, J context) {
                return VariableReferences.findRhsReferences(context, variableName).isEmpty() &&
                        !usedInModifyingUnary(variableName, context);
            }

            private boolean usedInModifyingUnary(J.Identifier variableName, J context) {
                return new JavaIsoVisitor<AtomicBoolean>() {
                    @Override
                    public J.Unary visitUnary(J.Unary unary, AtomicBoolean atomicBoolean) {
                        if (unary.getOperator().isModifying() &&
                                SemanticallyEqual.areEqual(variableName, unary.getExpression())) {
                            atomicBoolean.set(true);
                        }
                        return super.visitUnary(unary, atomicBoolean);
                    }
                }.reduce(context, new AtomicBoolean(false)).get();
            }
        });
    }

}
