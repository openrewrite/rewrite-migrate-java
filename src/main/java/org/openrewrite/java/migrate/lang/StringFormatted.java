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

import org.openrewrite.*;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

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
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                Preconditions.and(new UsesJavaVersion<>(17),new UsesMethod<>(STRING_FORMAT)),
                new StringFormattedVisitor());
    }

    private static class StringFormattedVisitor extends JavaVisitor<ExecutionContext> {
        @Override
        public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            method = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
            if (!STRING_FORMAT.matches(method) || method.getMethodType() == null) {
                return method;
            }

            List<Expression> arguments = method.getArguments();
            boolean wrapperNotNeeded = wrapperNotNeeded(arguments.get(0));
            maybeRemoveImport("java.lang.String.format");
            J.MethodInvocation mi = method.withName(method.getName().withSimpleName("formatted"));
            Optional<JavaType.Method> formatted = method.getMethodType().getDeclaringType().getMethods().stream()
                .filter(m -> m.getName().equals("formatted")).findAny();
            mi = mi.withMethodType(formatted.orElse(null));
            Expression select = wrapperNotNeeded ? arguments.get(0) :
                new J.Parentheses<>(Tree.randomId(), Space.EMPTY, Markers.EMPTY, JRightPadded.build(arguments.get(0)));
            mi = mi.withSelect(select);
            mi = mi.withArguments(arguments.subList(1, arguments.size()));
            return maybeAutoFormat(method, mi, ctx);
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
