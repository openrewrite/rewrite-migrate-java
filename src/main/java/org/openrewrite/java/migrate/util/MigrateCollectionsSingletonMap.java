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
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.util.List;
import java.util.StringJoiner;

public class MigrateCollectionsSingletonMap extends Recipe {
    private static final MethodMatcher SINGLETON_MAP = new MethodMatcher("java.util.Collections singletonMap(..)", true);

    @Override
    public String getDisplayName() {
        return "Prefer `Map.of(..)`";
    }

    @Override
    public String getDescription() {
        return "Prefer `Map.Of(..)` instead of using `Collections.singletonMap()` in Java 9 or higher.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        TreeVisitor<?, ExecutionContext> check = Preconditions.and(new UsesJavaVersion<>(9),
                new UsesMethod<>(SINGLETON_MAP));

        return Preconditions.check(check, new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
                if (SINGLETON_MAP.matches(method)) {
                    maybeRemoveImport("java.util.Collections");
                    maybeAddImport("java.util.Map");
                    StringJoiner mapOf = new StringJoiner(", ", "Map.of(", ")");
                    List<Expression> args = m.getArguments();
                    args.forEach(o -> mapOf.add("#{any()}"));

                    return autoFormat(m.withTemplate(
                            JavaTemplate.builder(mapOf.toString())
                                    .context(getCursor())
                                    .imports("java.util.Map")
                                    .build(),
                            getCursor(),
                            m.getCoordinates().replace(),
                            m.getArguments().toArray()
                    ), ctx);
                }

                return m;
            }
        });
    }
}
