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
import org.openrewrite.java.tree.J;
import org.openrewrite.staticanalysis.groovy.GroovyFileChecker;
import org.openrewrite.staticanalysis.kotlin.KotlinFileChecker;

public class MigrateCollectionsSingletonSet extends Recipe {
    private static final MethodMatcher SINGLETON_SET = new MethodMatcher("java.util.Collections singleton(..)", true);

    @Getter
    final String displayName = "Prefer `Set.of(..)`";

    @Getter
    final String description = "Prefer `Set.of(..)` instead of using `Collections.singleton()` in Java 9 or higher. " +
            "Note that the resulting `Set` is not behaviorally equivalent: `Set.of(..)` throws `NullPointerException` when probed with `contains(null)`, " +
            "whereas `Collections.singleton(..)` returns `false`.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        TreeVisitor<?, ExecutionContext> check = Preconditions.and(new UsesJavaVersion<>(9),
                new UsesMethod<>(SINGLETON_SET),
                Preconditions.not(new KotlinFileChecker<>()),
                Preconditions.not(new GroovyFileChecker<>()));
        return Preconditions.check(check, new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
                if (SINGLETON_SET.matches(m) &&
                        !(J.Literal.isLiteralValue(m.getArguments().get(0), null))) {
                    maybeRemoveImport("java.util.Collections");
                    maybeAddImport("java.util.Set");
                    return JavaTemplate.builder("Set.of(#{any()})")
                            .imports("java.util.Set")
                            .build()
                            .apply(updateCursor(m), m.getCoordinates().replace(), m.getArguments().get(0));
                }
                return m;
            }
        });
    }
}
