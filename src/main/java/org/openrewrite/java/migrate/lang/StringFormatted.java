/*
 * Copyright 2024 the original author or authors.
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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

import java.time.Duration;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.openrewrite.Tree.randomId;

@Value
@EqualsAndHashCode(callSuper = false)
public class StringFormatted extends Recipe {

    private static final MethodMatcher STRING_FORMAT = new MethodMatcher("java.lang.String format(String, ..)");

    @Option(displayName = "Add parentheses around the first argument",
            description = "Add parentheses around the first argument if it is not a simple expression. " +
                          "Default true; if false no change will be made. ",
            required = false)
    @Nullable
    Boolean addParentheses;

    @Override
    public String getDisplayName() {
        return "Prefer `String.formatted(Object...)`";
    }

    @Override
    public String getDescription() {
        return "Prefer `String.formatted(Object...)` over `String.format(String, Object...)` in Java 17 or higher.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(1);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        TreeVisitor<?, ExecutionContext> check = Preconditions.and(new UsesJavaVersion<>(17), new UsesMethod<>(STRING_FORMAT));
        return Preconditions.check(check, new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation methodInvocation, ExecutionContext ctx) {
                methodInvocation = (J.MethodInvocation) super.visitMethodInvocation(methodInvocation, ctx);
                if (!STRING_FORMAT.matches(methodInvocation) || methodInvocation.getMethodType() == null) {
                    return methodInvocation;
                }

                // No change when change might be controversial, such as string concatenation
                List<Expression> arguments = methodInvocation.getArguments();
                boolean wrapperNeeded = wrapperNeeded(arguments.get(0));
                if (Boolean.FALSE.equals(addParentheses) && wrapperNeeded) {
                    return methodInvocation;
                }

                maybeRemoveImport("java.lang.String.format");
                J.MethodInvocation mi = methodInvocation.withName(methodInvocation.getName().withSimpleName("formatted"));
                mi = mi.withMethodType(methodInvocation.getMethodType().getDeclaringType().getMethods().stream()
                        .filter(it -> it.getName().equals("formatted"))
                        .findAny()
                        .orElse(null));
                if (mi.getName().getType() != null) {
                    mi = mi.withName(mi.getName().withType(mi.getMethodType()));
                }
                mi = mi.withSelect(wrapperNeeded ?
                        new J.Parentheses<>(randomId(), Space.EMPTY, Markers.EMPTY,
                                JRightPadded.build(arguments.get(0))) :
                        arguments.get(0).withPrefix(Space.EMPTY));
                mi = mi.withArguments(arguments.subList(1, arguments.size()));
                if (mi.getArguments().isEmpty()) {
                    // To store spaces between the parenthesis of a method invocation argument list
                    // Ensures formatting recipes chained together with this one will still work as expected
                    mi = mi.withArguments(singletonList(new J.Empty(randomId(), Space.EMPTY, Markers.EMPTY)));
                }
                return maybeAutoFormat(methodInvocation, mi, ctx);
            }

            private boolean wrapperNeeded(Expression expression) {
                return !(expression instanceof J.Identifier ||
                         expression instanceof J.Literal ||
                         expression instanceof J.MethodInvocation ||
                         expression instanceof J.FieldAccess);
            }
        });
    }
}
