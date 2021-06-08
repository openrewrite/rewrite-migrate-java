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
package org.openrewrite.java.migrate.logging;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;

public class MigrateGetLoggingMXBeanToGetPlatformMXBean extends Recipe {
    private static final MethodMatcher MATCHER = new MethodMatcher("java.util.logging.LogManager getLoggingMXBean()");

    @Override
    public String getDisplayName() {
        return "Use `ManagementFactory#getPlatformMXBean(PlatformLoggingMXBean.class)`";
    }

    @Override
    public String getDescription() {
        return "`LogManager#getLoggingMXBean()` was deprecated in Java 9.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new UsesMethod<>(MATCHER);
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MigrateGetLoggingMXBeanToGetPlatformMXBeanVisitor();
    }

    private static class MigrateGetLoggingMXBeanToGetPlatformMXBeanVisitor extends JavaIsoVisitor<ExecutionContext> {
        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation m = method;
            if (MATCHER.matches(m)) {
                m = m.withTemplate(
                        template("ManagementFactory.getPlatformMXBean(PlatformLoggingMXBean.class)")
                                .imports("java.lang.management.ManagementFactory", "java.lang.management.PlatformLoggingMXBean")
                                .build(),
                        m.getCoordinates().replace()
                );
                maybeAddImport("java.lang.management.ManagementFactory");
                maybeAddImport("java.lang.management.PlatformLoggingMXBean");
                maybeRemoveImport("java.util.logging.LogManager");
            }
            return super.visitMethodInvocation(m, ctx);
        }
    }

}
