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
package org.openrewrite.java.migrate.net;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;

import java.util.Set;

import static java.util.Collections.singleton;

public class MigrateMulticastSocketSetTTLToSetTimeToLive extends Recipe {
    private static final MethodMatcher MATCHER = new MethodMatcher("java.net.MulticastSocket setTTL(byte)");

    @Override
    public String getDisplayName() {
        return "Use `java.net.MulticastSocket#setTimeToLive(int)`";
    }

    @Override
    public String getDescription() {
        return "Use `java.net.MulticastSocket#setTimeToLive(int)` instead of the deprecated `java.net.MulticastSocket#setTTL(byte)` in Java 1.2 or higher.";
    }

    @Override
    public Set<String> getTags() {
        return singleton("deprecated");
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesMethod<>(MATCHER), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = method;
                if (MATCHER.matches(m)) {
                    m = m.withName(m.getName().withSimpleName("setTimeToLive"));
                    m = JavaTemplate.builder("Byte.valueOf(#{any(byte)}).intValue()")
                            .build()
                            .apply(updateCursor(m), m.getCoordinates().replaceArguments(), m.getArguments().get(0));
                }
                return super.visitMethodInvocation(m, ctx);
            }
        });
    }
}
