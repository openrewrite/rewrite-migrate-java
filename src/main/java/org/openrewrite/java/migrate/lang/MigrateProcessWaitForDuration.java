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
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.util.Collections;
import java.util.List;

public class MigrateProcessWaitForDuration extends Recipe {

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
        return new JavaVisitor<ExecutionContext>() {
                    private final MethodMatcher waitForMatcher = new MethodMatcher("java.lang.Process waitFor(long, java.util.concurrent.TimeUnit)");

                    @Override
                    public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                        J.MethodInvocation mi = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);

                        if (waitForMatcher.matches(mi)) {
                            List<Expression> arguments = mi.getArguments();
                            if (arguments.size() == 2) {
                                Expression timeoutArg = arguments.get(0);
                                Expression timeUnitArg = arguments.get(1);

                                maybeAddImport("java.time.Duration");

                                // Check if we can determine the TimeUnit value
                                String timeUnitName = getTimeUnitName(timeUnitArg);
                                boolean isSimpleTimeout = isSimpleValue(timeoutArg);

                                Expression durationArg;
                                if (timeUnitName != null && isSimpleTimeout) {
                                    // Try to use expressive Duration methods
                                    String durationMethod = null;
                                    boolean needsChronoUnit = false;

                                    switch (timeUnitName) {
                                        case "NANOSECONDS":
                                            durationMethod = "ofNanos";
                                            break;
                                        case "MICROSECONDS":
                                            // Special case: Duration doesn't have ofMicros
                                            needsChronoUnit = true;
                                            break;
                                        case "MILLISECONDS":
                                            durationMethod = "ofMillis";
                                            break;
                                        case "SECONDS":
                                            durationMethod = "ofSeconds";
                                            break;
                                        case "MINUTES":
                                            durationMethod = "ofMinutes";
                                            break;
                                        case "HOURS":
                                            durationMethod = "ofHours";
                                            break;
                                        case "DAYS":
                                            durationMethod = "ofDays";
                                            break;
                                    }

                                    if (needsChronoUnit) {
                                        // Use Duration.of with ChronoUnit for MICROSECONDS
                                        maybeAddImport("java.time.temporal.ChronoUnit");
                                        maybeRemoveImport("java.util.concurrent.TimeUnit");
                                        JavaTemplate template = JavaTemplate.builder("Duration.of(#{any(long)}, ChronoUnit.MICROS)")
                                                .imports("java.time.Duration", "java.time.temporal.ChronoUnit")
                                                .build();
                                        mi = template.apply(getCursor(), method.getCoordinates().replaceArguments(), timeoutArg);
                                    } else if (durationMethod != null) {
                                        // Use expressive Duration method
                                        maybeRemoveImport("java.util.concurrent.TimeUnit");
                                        // Also remove static imports if present
                                        maybeRemoveImport("java.util.concurrent.TimeUnit." + timeUnitName);
                                        JavaTemplate template = JavaTemplate.builder("Duration." + durationMethod + "(#{any(long)})")
                                                .imports("java.time.Duration")
                                                .build();
                                        mi = template.apply(getCursor(), method.getCoordinates().replaceArguments(), timeoutArg);
                                    } else {
                                        // Shouldn't happen, but fallback to generic conversion
                                        JavaTemplate template = JavaTemplate.builder("Duration.of(#{any(long)}, #{any(java.util.concurrent.TimeUnit)}.toChronoUnit())")
                                                .imports("java.time.Duration")
                                                .build();
                                        mi = template.apply(getCursor(), method.getCoordinates().replaceArguments(), timeoutArg, timeUnitArg);
                                    }
                                } else {
                                    // Complex case: use Duration.of with toChronoUnit
                                    JavaTemplate template = JavaTemplate.builder("Duration.of(#{any(long)}, #{any(java.util.concurrent.TimeUnit)}.toChronoUnit())")
                                            .imports("java.time.Duration")
                                            .build();
                                    mi = template.apply(getCursor(), method.getCoordinates().replaceArguments(), timeoutArg, timeUnitArg);
                                }
                            }
                        }
                        return mi;
                    }

                    private String getTimeUnitName(Expression timeUnitArg) {
                        if (timeUnitArg instanceof J.FieldAccess) {
                            J.FieldAccess fa = (J.FieldAccess) timeUnitArg;
                            return fa.getSimpleName();
                        } else if (timeUnitArg instanceof J.Identifier) {
                            // Handle static imports
                            J.Identifier id = (J.Identifier) timeUnitArg;
                            String name = id.getSimpleName();
                            // Check if it's a known TimeUnit constant
                            if (isTimeUnitConstant(name)) {
                                return name;
                            }
                        }
                        return null;
                    }

                    private boolean isTimeUnitConstant(String name) {
                        return "NANOSECONDS".equals(name) ||
                               "MICROSECONDS".equals(name) ||
                               "MILLISECONDS".equals(name) ||
                               "SECONDS".equals(name) ||
                               "MINUTES".equals(name) ||
                               "HOURS".equals(name) ||
                               "DAYS".equals(name);
                    }

                    private boolean isSimpleValue(Expression expr) {
                        // Check if the expression is a literal or a simple identifier (not a method call or complex expression)
                        return expr instanceof J.Literal || expr instanceof J.Identifier;
                    }
                };
    }
}
