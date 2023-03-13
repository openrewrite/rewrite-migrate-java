/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.java.migrate.lang;

import org.openrewrite.Applicability;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.*;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

public class StringFormatted extends Recipe {

    private static final MethodMatcher STRING_FORMAT = new MethodMatcher("java.lang.String format(String, ..)");

    @Override
    public String getDisplayName() {
        return "Prefer `String#formatted(Object...)`";
    }

    @Override
    public String getDescription() {
        return "Prefer `String#formatted(Object...)` over `String#format(String, Object...)` in Java 17 or higher.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return Applicability.and(new UsesJavaVersion<>(17),new UsesMethod<>(STRING_FORMAT));
    }

    @Override
    protected StringFormattedVisitor getVisitor() {
        return new StringFormattedVisitor();
    }

    private static class StringFormattedVisitor extends JavaVisitor<ExecutionContext> {
        @Override
        public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext p) {
            J.MethodInvocation mi = (J.MethodInvocation) super.visitMethodInvocation(method, p);
            if (!STRING_FORMAT.matches(mi)) {
                return mi;
            }

            List<Expression> arguments = mi.getArguments();
            String template = String.format(wrapperNotNeeded(arguments.get(0))
                    ? "#{any(java.lang.String)}.formatted(%s)"
                    : "(#{any(java.lang.String)}).formatted(%s)",
                    String.join(", ", Collections.nCopies(arguments.size() - 1, "#{any()}")));
            maybeRemoveImport("java.lang.String.format");
            mi = mi.withTemplate(
                    JavaTemplate.builder(this::getCursor, template)
                            .javaParser(() -> JavaParser.fromJavaVersion().build())
                            .build(),
                    mi.getCoordinates().replace(),
                    arguments.toArray());
            if (arguments.size() > 1) {
                arguments.remove(0);
                mi = maybeAutoFormat(mi, mi.withArguments(
                        ListUtils.map(mi.getArguments(), (a, b) -> b.withPrefix(arguments.get(a).getPrefix()))), p);
            }
            return mi;
        }

        private static boolean wrapperNotNeeded(Expression expression) {
            return expression instanceof J.Identifier
                    || expression instanceof J.Literal
                    || expression instanceof J.MethodInvocation;
        }
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(1);
    }

}
