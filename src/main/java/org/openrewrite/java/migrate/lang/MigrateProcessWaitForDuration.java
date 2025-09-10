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

import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.staticanalysis.SimplifyDurationCreationUnits;

public class MigrateProcessWaitForDuration extends Recipe {

    private static final MethodMatcher PROCESS_WAIT_FOR_MATCHER = new MethodMatcher("java.lang.Process waitFor(long, java.util.concurrent.TimeUnit)");

    @Override
    public String getDisplayName() {
        return "Use `Process#waitFor(Duration)`";
    }

    @Override
    public String getDescription() {
        return "Use `Process#waitFor(Duration)` instead of `Process#waitFor(long, TimeUnit)` in Java 25 or higher.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesJavaVersion<>(25), new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation mi = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);

                if (PROCESS_WAIT_FOR_MATCHER.matches(mi)) {
                    Expression valueArg = mi.getArguments().get(0);
                    Expression unitArg = mi.getArguments().get(1);
                    String timeUnitName = getTimeUnitName(unitArg);
                    String durationMethod = getDurationMethod(timeUnitName);

                    boolean isSimpleValue = valueArg instanceof J.Literal || valueArg instanceof J.Identifier;

                    maybeRemoveImport("java.util.concurrent.TimeUnit");
                    maybeRemoveImport("java.util.concurrent.TimeUnit." + timeUnitName);
                    maybeAddImport("java.time.Duration");
                    maybeAddImport("java.time.temporal.ChronoUnit");

                    doAfterVisit(new SimplifyDurationCreationUnits().getVisitor());

                    if (isSimpleValue && "MICROSECONDS".equals(timeUnitName)) {
                        return JavaTemplate.builder("Duration.of(#{any(long)}, ChronoUnit.MICROS)")
                                .imports("java.time.Duration", "java.time.temporal.ChronoUnit")
                                .build()
                                .apply(getCursor(), mi.getCoordinates().replaceArguments(), valueArg);
                    }
                    if (isSimpleValue && durationMethod != null) {
                        return JavaTemplate.builder("Duration." + durationMethod + "(#{any(long)})")
                                .imports("java.time.Duration")
                                .build()
                                .apply(getCursor(), mi.getCoordinates().replaceArguments(), valueArg);
                    }
                    return JavaTemplate.builder("Duration.of(#{any(long)}, #{any(java.util.concurrent.TimeUnit)}.toChronoUnit())")
                            .imports("java.time.Duration")
                            .build()
                            .apply(getCursor(), mi.getCoordinates().replaceArguments(), valueArg, unitArg);
                }
                return mi;
            }

            private @Nullable String getTimeUnitName(Expression timeUnitArg) {
                if (timeUnitArg instanceof J.FieldAccess) {
                    J.FieldAccess fa = (J.FieldAccess) timeUnitArg;
                    return fa.getSimpleName();
                }
                if (timeUnitArg instanceof J.Identifier) {
                    J.Identifier id = (J.Identifier) timeUnitArg;
                    return id.getSimpleName();
                }
                return null;
            }

            private @Nullable String getDurationMethod(@Nullable String timeUnitName) {
                if (timeUnitName == null) {
                    return null;
                }
                switch (timeUnitName) {
                    case "NANOSECONDS":
                        return "ofNanos";
                    case "MILLISECONDS":
                        return "ofMillis";
                    case "SECONDS":
                        return "ofSeconds";
                    case "MINUTES":
                        return "ofMinutes";
                    case "HOURS":
                        return "ofHours";
                    case "DAYS":
                        return "ofDays";
                    default:
                        return null;
                }
            }
        });
    }
}
