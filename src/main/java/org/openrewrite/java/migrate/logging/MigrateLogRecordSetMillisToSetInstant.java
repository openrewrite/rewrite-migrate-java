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

import org.openrewrite.*;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;

import java.util.Collections;
import java.util.Set;

public class MigrateLogRecordSetMillisToSetInstant extends Recipe {
    private static final MethodMatcher MATCHER = new MethodMatcher("java.util.logging.LogRecord setMillis(long)");

    @Override
    public String getDisplayName() {
        return "Use `LogRecord#setInstant(Instant)`";
    }

    @Override
    public String getDescription() {
        return "Use `LogRecord#setInstant(Instant)` instead of the deprecated `LogRecord#setMillis(long)` in Java 9 or higher.";
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
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = method;
                if (MATCHER.matches(m)) {
                    m = m.withName(m.getName().withSimpleName("setInstant"));
                    m = JavaTemplate.builder("Instant.ofEpochMilli(#{any(long)})")
                            .imports("java.time.Instant")
                            .build()
                            .apply(updateCursor(m),
                                    m.getCoordinates().replaceArguments(),
                                    m.getArguments().get(0));
                    maybeAddImport("java.time.Instant");
                }
                return super.visitMethodInvocation(m, ctx);
            }
        });
    }
}
