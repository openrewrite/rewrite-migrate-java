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
package org.openrewrite.java.migrate.util;

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

public class MigrateCollectionsSingletonSet extends Recipe {
    private static final MethodMatcher SINGLETON_SET = new MethodMatcher("java.util.Collections singleton(..)", true);

    @Override
    public String getDisplayName() {
        return "Prefer `Set.of(..)`";
    }

    @Override
    public String getDescription() {
        return "Prefer `Set.Of(..)` instead of using `Collections.singleton()` in Java 9 or higher.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        TreeVisitor<?, ExecutionContext> check = Preconditions.and(new UsesJavaVersion<>(9),
                new UsesMethod<>(SINGLETON_SET));
        return Preconditions.check(check, new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
                if (SINGLETON_SET.matches(m) && isNotLiteralNull(m)) {
                    maybeRemoveImport("java.util.Collections");
                    maybeAddImport("java.util.Set");
                    return JavaTemplate.builder("Set.of(#{any()})")
                            .contextSensitive()
                            .imports("java.util.Set")
                            .build()
                            .apply(updateCursor(m), m.getCoordinates().replace(), m.getArguments().get(0));
                }
                return m;
            }

            private boolean isNotLiteralNull(J.MethodInvocation m) {
                return !(m.getArguments().get(0) instanceof J.Literal &&
                         ((J.Literal) m.getArguments().get(0)).getValue() == null);
            }
        });
    }
}
