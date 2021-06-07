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
import org.openrewrite.java.ChangeMethodName;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;

public class MigrateLogRecordSetMillisToSetInstant extends Recipe {
    private static final MethodMatcher SET_MILLIS_MATCHER = new MethodMatcher("java.util.logging.LogRecord setMillis(long)");

    @Override
    public String getDisplayName() {
        return "Migrate `LogRecord#setMillis(long)` to `LogRecord#setInstant(Instant)`";
    }

    @Override
    public String getDescription() {
        return "Migrates usages of the deprecated method `LogRecord#setMillis(long)` to using `LogRecord#setInstant(Instant)` via `LogRecord#setInstant(Instant.ofEpochMilli(long))`.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new UsesMethod<>(SET_MILLIS_MATCHER);
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MigrateLogRecordSetMillisToSetInstantVisitor();
    }

    private static class MigrateLogRecordSetMillisToSetInstantVisitor extends JavaIsoVisitor<ExecutionContext> {
        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation m = method;
            if (SET_MILLIS_MATCHER.matches(m)) {
                m = m.withTemplate(
                        template("Instant.ofEpochMilli(#{any(long)})")
                                .imports("java.time.Instant")
                                .build(),
                        m.getCoordinates().replaceArguments(),
                        m.getArguments().get(0)
                );
                maybeAddImport("java.time.Instant");
                doAfterVisit(new ChangeMethodName("java.util.logging.LogRecord setMillis(long)", "setInstant"));
            }
            return super.visitMethodInvocation(m, ctx);
        }
    }

}
