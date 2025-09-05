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
import org.openrewrite.java.tree.JavaType;

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
        return Preconditions.check(
                new UsesJavaVersion<>(25),
                new JavaVisitor<ExecutionContext>() {
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
                                
                                // Check if the TimeUnit is a constant we can map to a specific Duration method
                                String durationMethod = null;
                                if (timeUnitArg instanceof J.FieldAccess) {
                                    J.FieldAccess fa = (J.FieldAccess) timeUnitArg;
                                    if (fa.getSimpleName().equals("NANOSECONDS")) {
                                        durationMethod = "ofNanos";
                                    } else if (fa.getSimpleName().equals("MICROSECONDS")) {
                                        durationMethod = "ofNanos"; // will multiply by 1000
                                        timeoutArg = JavaTemplate.builder("#{any(long)} * 1000")
                                                .build()
                                                .apply(getCursor(), timeoutArg.getCoordinates().replace(), timeoutArg);
                                    } else if (fa.getSimpleName().equals("MILLISECONDS")) {
                                        durationMethod = "ofMillis";
                                    } else if (fa.getSimpleName().equals("SECONDS")) {
                                        durationMethod = "ofSeconds";
                                    } else if (fa.getSimpleName().equals("MINUTES")) {
                                        durationMethod = "ofMinutes";
                                    } else if (fa.getSimpleName().equals("HOURS")) {
                                        durationMethod = "ofHours";
                                    } else if (fa.getSimpleName().equals("DAYS")) {
                                        durationMethod = "ofDays";
                                    }
                                }

                                Expression durationArg;
                                if (durationMethod != null && !durationMethod.equals("ofNanos") && 
                                    (timeoutArg instanceof J.Literal || timeoutArg instanceof J.Identifier)) {
                                    // Use the more expressive Duration method for simple timeout values
                                    maybeRemoveImport("java.util.concurrent.TimeUnit");
                                    JavaTemplate template = JavaTemplate.builder("Duration." + durationMethod + "(#{any(long)})")
                                            .imports("java.time.Duration")
                                            .build();
                                    durationArg = template.apply(getCursor(), mi.getCoordinates().replaceArguments(), timeoutArg);
                                } else {
                                    // Fall back to Duration.of with toChronoUnit conversion
                                    JavaTemplate template = JavaTemplate.builder("Duration.of(#{any(long)}, #{any(java.util.concurrent.TimeUnit)}.toChronoUnit())")
                                            .imports("java.time.Duration")
                                            .build();
                                    durationArg = template.apply(getCursor(), mi.getCoordinates().replaceArguments(), timeoutArg, timeUnitArg);
                                }

                                return mi.withArguments(Collections.singletonList(durationArg));
                            }
                        }
                        return mi;
                    }
                }
        );
    }
}
