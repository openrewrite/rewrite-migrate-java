/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.java.migrate.guava;

import lombok.Getter;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.List;
import java.util.Set;

import static java.util.Collections.nCopies;
import static java.util.Collections.singleton;

public class NoGuavaPreconditionsCheckNotNullFormat extends Recipe {
    private static final MethodMatcher CHECK_NOT_NULL_FORMAT = new MethodMatcher("com.google.common.base.Preconditions checkNotNull(*, java.lang.String, ..)");

    @Getter
    final String displayName = "Prefer `Objects.requireNonNull(T, Supplier<String>)`";

    @Getter
    final String description = "Replace Guava's `Preconditions.checkNotNull(reference, template, args..)` with " +
            "`Objects.requireNonNull(reference, () -> String.format(template, args..))`, which formats the message only " +
            "when the reference is null, as Guava does. A template converts only when it is a string literal whose " +
            "placeholders are all `%s` and match the arguments, and only when the lambda can capture every argument.";

    @Getter
    final Set<String> tags = singleton("guava");

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesMethod<>(CHECK_NOT_NULL_FORMAT), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                if (!CHECK_NOT_NULL_FORMAT.matches(mi)) {
                    return mi;
                }
                List<Expression> message = mi.getArguments().subList(1, mi.getArguments().size());
                if (!isLenientFormatOnlyTemplate(message) || !message.stream().allMatch(this::canDefer)) {
                    return mi;
                }
                maybeRemoveImport("com.google.common.base.Preconditions");
                maybeRemoveImport("com.google.common.base.Preconditions.checkNotNull");
                maybeAddImport("java.util.Objects", "requireNonNull");
                return JavaTemplate.builder("requireNonNull(#{any()}, () -> String.format(#{any(java.lang.String)}" +
                                            String.join("", nCopies(message.size() - 1, ", #{any()}")) + "))")
                        .staticImports("java.util.Objects.requireNonNull")
                        .build()
                        .apply(getCursor(), mi.getCoordinates().replace(), mi.getArguments().toArray());
            }

            /**
             * The lambda evaluates the arguments only when the reference is null, so method calls in them are assumed
             * to have no side effects. An array argument would spread into as many arguments as it has elements.
             */
            private boolean canDefer(Expression argument) {
                return !(argument.getType() instanceof JavaType.Array) && LambdaCapture.canCapture(getCursor(), argument);
            }
        });
    }

    /**
     * Guava substitutes only `%s` and appends unmatched arguments, where `String.format` interprets every `%`.
     */
    private static boolean isLenientFormatOnlyTemplate(List<Expression> message) {
        if (!(message.get(0) instanceof J.Literal) || !(((J.Literal) message.get(0)).getValue() instanceof String)) {
            return false;
        }
        String template = (String) ((J.Literal) message.get(0)).getValue();
        return template.replace("%s", "").indexOf('%') < 0 &&
               template.split("%s", -1).length - 1 == message.size() - 1;
    }
}
