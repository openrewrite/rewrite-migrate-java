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
package org.openrewrite.java.migrate.net;

import lombok.SneakyThrows;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.ChangeMethodName;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;

public class MigrateMulticastSocketSetTTLToSetTimeToLive extends Recipe {
    private static final MethodMatcher MATCHER = new MethodMatcher("java.net.MulticastSocket setTTL(byte)");

    @Override
    public String getDisplayName() {
        return "Migrate `java.net.MulticastSocket#setTTL(byte)`";
    }

    @Override
    public String getDescription() {
        return "Migrates the deprecated method `java.net.MulticastSocket#setTTL(byte)` to `java.net.MulticastSocket#setTimeToLive(int)` using " +
                "`Byte.valueOf(byte).intValue()`. The result being `java.net.MulticastSocket#setTimeToLive(Byte.valueOf(byte).intValue())`.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new UsesMethod<>(MATCHER);
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MigrateMulticastSocketSetTTLToSetTimeToLiveVisitor();
    }

    private static class MigrateMulticastSocketSetTTLToSetTimeToLiveVisitor extends JavaIsoVisitor<ExecutionContext> {
        @SneakyThrows
        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation m = method;
            if (MATCHER.matches(m)) {
                m = m.withTemplate(
                        template("Byte.valueOf(#{any(byte)}).intValue()").build(),
                        m.getCoordinates().replaceArguments(),
                        m.getArguments().get(0)
                );
                doAfterVisit(new ChangeMethodName("java.net.MulticastSocket setTTL(byte)", "setTimeToLive"));
            }
            return super.visitMethodInvocation(m, ctx);
        }
    }

}
