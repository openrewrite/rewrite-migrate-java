/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.java.migrate.net;

import lombok.Getter;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.staticanalysis.java.JavaFileChecker;

public class MigrateMulticastSocketGetTTLToGetTimeToLive extends Recipe {
    private static final MethodMatcher GET_TTL = new MethodMatcher("java.net.MulticastSocket getTTL()");

    @Getter
    final String displayName = "Use `java.net.MulticastSocket#getTimeToLive()`";

    @Getter
    final String description = "Use `java.net.MulticastSocket#getTimeToLive()` instead of the deprecated `java.net.MulticastSocket#getTTL()` in Java 1.2 or higher. " +
            "Where the result is used, it is cast back to `byte`, so that the value and the expression type stay the same.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(Preconditions.and(new JavaFileChecker<>(), new UsesMethod<>(GET_TTL)), new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
                if (!GET_TTL.matches(m)) {
                    return m;
                }
                Object parent = getCursor().getParentTreeCursor().getValue();
                // Whether the result is used here depends on the target type, and a cast is not a valid statement
                if (parent instanceof J.Lambda || parent instanceof J.Case) {
                    return m;
                }
                JavaType.Method type = m.getMethodType().withName("getTimeToLive").withReturnType(JavaType.Primitive.Int);
                J.MethodInvocation getTimeToLive = m.withName(m.getName().withSimpleName("getTimeToLive").withType(type)).withMethodType(type);
                if (isStatement(parent)) {
                    return getTimeToLive;
                }
                return JavaTemplate.apply("(byte) #{any(int)}", updateCursor(m), m.getCoordinates().replace(), getTimeToLive);
            }

            private boolean isStatement(Object parent) {
                return parent instanceof J.Block ||
                        parent instanceof J.If ||
                        parent instanceof J.If.Else ||
                        parent instanceof J.WhileLoop ||
                        parent instanceof J.DoWhileLoop ||
                        parent instanceof J.ForLoop ||
                        parent instanceof J.ForLoop.Control ||
                        parent instanceof J.ForEachLoop ||
                        parent instanceof J.Label;
            }
        });
    }
}
