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

import org.openrewrite.Preconditions;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.ChangeType;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

public class MigrateGetLoggingMXBeanToGetPlatformMXBean extends Recipe {
    private static final MethodMatcher MATCHER = new MethodMatcher("java.util.logging.LogManager getLoggingMXBean()");

    @Override
    public String getDisplayName() {
        return "Use `ManagementFactory#getPlatformMXBean(PlatformLoggingMXBean.class)`";
    }

    @Override
    public String getDescription() {
        return "Use `ManagementFactory#getPlatformMXBean(PlatformLoggingMXBean.class)` instead of the deprecated `LogManager#getLoggingMXBean()` in Java 9 or higher.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(5);
    }

    public Set<String> getTags() {
        return Collections.singleton("deprecated");
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        TreeVisitor<?, ExecutionContext> check = Preconditions.and(
                new UsesJavaVersion<>(9),
                new UsesMethod<>(MATCHER));
        return Preconditions.check(check, new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                cu = (J.CompilationUnit) new ChangeType(
                        "java.util.logging.LoggingMXBean",
                        "java.lang.management.PlatformLoggingMXBean",
                        false
                ).getVisitor().visitNonNull(cu, ctx);
                return super.visitCompilationUnit(cu, ctx);
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                if (MATCHER.matches(m)) {
                    maybeAddImport("java.lang.management.ManagementFactory");
                    maybeAddImport("java.lang.management.PlatformLoggingMXBean");
                    maybeRemoveImport("java.util.logging.LogManager");

                    m = m.withTemplate(
                            JavaTemplate.builder(this::getCursor, "ManagementFactory.getPlatformMXBean(PlatformLoggingMXBean.class)")
                                    .imports("java.lang.management.ManagementFactory")
                                    .imports("java.lang.management.PlatformLoggingMXBean")
                                    .build(),
                            m.getCoordinates().replace()
                    );
                }
                return m;
            }
        });
    }

}
