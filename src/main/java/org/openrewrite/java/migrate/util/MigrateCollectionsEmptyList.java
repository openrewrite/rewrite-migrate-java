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
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;

public class MigrateCollectionsEmptyList extends Recipe {
    private static final MethodMatcher EMPTY_LIST = new MethodMatcher("java.util.Collections emptyList()");

    @Getter
    final String displayName = "Prefer `List.of()`";

    @Getter
    final String description = "Prefer `List.of()` instead of using `Collections.emptyList()` in Java 9 or higher.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        TreeVisitor<?, ExecutionContext> check = Preconditions.and(new UsesJavaVersion<>(9),
                new UsesMethod<>(EMPTY_LIST));
        return Preconditions.check(check, new JavaIsoVisitor<ExecutionContext>() {

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);

                if (EMPTY_LIST.matches(m)) {
                    maybeRemoveImport("java.util.Collections");
                    maybeAddImport("java.util.List");
                    return JavaTemplate.builder("List.of()")
                            .contextSensitive()
                            .imports("java.util.List")
                            .build()
                            .apply(updateCursor(m), m.getCoordinates().replace());
                }

                return m;
            }
        });
    }
}
