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
package org.openrewrite.java.migrate.io;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;

public class ReplaceSystemOutWithIOPrint extends Recipe {

    @Override
    public String getDisplayName() {
        return "Migrate `System.out.print` to Java 25 IO utility class";
    }

    @Override
    public String getDescription() {
        return "Replace `System.out.print()`, `System.out.println()` with `IO.print()` and `IO.println()`. " +
                "Migrates to the new IO utility class introduced in Java 25.";
    }

    private static final MethodMatcher SYSTEM_OUT_PRINT = new MethodMatcher("java.io.PrintStream print*(..)");

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesMethod<>(SYSTEM_OUT_PRINT), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                if (!isSystemOutMethod(m)) {
                    return m;
                }
                String methodName = m.getName().getSimpleName();
                return m.getArguments().isEmpty() ?
                        JavaTemplate.builder("IO.#{}()").build()
                                .apply(getCursor(), m.getCoordinates().replace(), methodName) :
                        JavaTemplate.builder("IO.#{}(#{any()})").build()
                                .apply(getCursor(), m.getCoordinates().replace(), methodName, m.getArguments().get(0));
            }

            private boolean isSystemOutMethod(J.MethodInvocation mi) {
                if (SYSTEM_OUT_PRINT.matches(mi)) {
                    Expression expression = mi.getSelect();
                    if (expression instanceof J.FieldAccess) {
                        return isSystemOut(((J.FieldAccess) expression).getName());
                    }
                    if (expression instanceof J.Identifier) {
                        maybeRemoveImport("java.lang.System.out");
                        return isSystemOut((J.Identifier) expression);
                    }
                }
                return false;
            }

            private boolean isSystemOut(J.Identifier identifier) {
                return "out".equals(identifier.getSimpleName()) &&
                        identifier.getFieldType() != null &&
                        TypeUtils.isAssignableTo("java.lang.System", identifier.getFieldType().getOwner());
            }
        });
    }
}
