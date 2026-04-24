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
package org.openrewrite.java.migrate.util;

import lombok.Getter;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;

import java.time.Duration;

public class UsePredicateNot extends Recipe {

    private static final String PREDICATE_FQN = "java.util.function.Predicate";
    private static final MethodMatcher PREDICATE_NEGATE = new MethodMatcher(PREDICATE_FQN + " negate()");

    @Getter
    final String displayName = "Prefer `Predicate.not(..)` over casting to `Predicate` and calling `negate()`";

    @Getter
    final String description = "Replace `((Predicate<T>) lambdaOrMethodRef).negate()` with `Predicate.not(lambdaOrMethodRef)` as of Java 11.";

    @Getter
    final Duration estimatedEffortPerOccurrence = Duration.ofMinutes(1);

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                Preconditions.and(new UsesJavaVersion<>(11), new UsesMethod<>(PREDICATE_NEGATE)),
                new JavaVisitor<ExecutionContext>() {
                    @Override
                    public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                        J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
                        if (!PREDICATE_NEGATE.matches(m) || m.getSelect() == null) {
                            return m;
                        }
                        Expression unwrapped = m.getSelect().unwrap();
                        if (!(unwrapped instanceof J.TypeCast)) {
                            return m;
                        }
                        J.TypeCast cast = (J.TypeCast) unwrapped;
                        if (!TypeUtils.isAssignableTo(PREDICATE_FQN, cast.getType())) {
                            return m;
                        }
                        maybeAddImport(PREDICATE_FQN);
                        return JavaTemplate.builder("Predicate.not(#{any()})")
                                .imports(PREDICATE_FQN)
                                .build()
                                .apply(getCursor(), m.getCoordinates().replace(), cast.getExpression());
                    }
                }
        );
    }
}
