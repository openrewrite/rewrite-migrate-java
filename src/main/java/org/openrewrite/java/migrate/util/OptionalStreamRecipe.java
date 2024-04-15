/*
 * Copyright 2023 the original author or authors.
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
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.*;

import java.util.ArrayList;
import java.util.List;

public class OptionalStreamRecipe extends Recipe {
    @Override
    public String getDisplayName() {
        return "`Stream<Optional>` idiom recipe";
    }

    @Override
    public String getDescription() {
        return "Migrate Java 8 `Optional<Stream>.filter(Optional::isPresent).map(Optional::get)` to Java 11 `.flatMap(Optional::stream)`.";
    }

    private static final MethodMatcher mapMatcher = new MethodMatcher("java.util.stream.Stream map(java.util.function.Function)");
    private static final MethodMatcher filterMatcher = new MethodMatcher("java.util.stream.Stream filter(java.util.function.Predicate)");
    private static final MethodMatcher optionalGetMatcher = new MethodMatcher("java.util.Optional get()");
    private static final MethodMatcher optionalIsPresentMatcher = new MethodMatcher("java.util.Optional isPresent()");

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesMethod<>(optionalIsPresentMatcher), new OptionalStreamVisitor());
    }

    private static class OptionalStreamVisitor extends JavaIsoVisitor<ExecutionContext> {
        private static final JavaTemplate template =
                JavaTemplate.builder("#{any(java.util.stream.Stream)}.flatMap(Optional::stream)")
                        .imports("java.util.Optional")
                        .build();

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation invocation, ExecutionContext ctx) {
            J.MethodInvocation mapInvocation = super.visitMethodInvocation(invocation, ctx);
            // .map(Optional::get)
            if (!mapMatcher.matches(mapInvocation) || !optionalGetMatcher.matches(mapInvocation.getArguments().get(0))) {
                return mapInvocation;
            }
            // .filter
            Expression mapSelectExpr = mapInvocation.getSelect();
            if (!filterMatcher.matches(mapSelectExpr)) {
                return mapInvocation;
            }
            // Optional::isPresent
            J.MethodInvocation filterInvocation = (J.MethodInvocation) mapSelectExpr;
            if (!optionalIsPresentMatcher.matches(filterInvocation.getArguments().get(0))) {
                return mapInvocation;
            }

            JRightPadded<Expression> filterSelect = filterInvocation.getPadding().getSelect();
            JRightPadded<Expression> mapSelect = mapInvocation.getPadding().getSelect();
            JavaType.Method mapInvocationType = mapInvocation.getMethodType();
            Space flatMapComments = getFlatMapComments(mapSelect, filterSelect);
            J.MethodInvocation flatMapInvocation = template
                    .apply(updateCursor(mapInvocation), mapInvocation.getCoordinates().replace(), filterInvocation.getSelect());
            return flatMapInvocation.getPadding()
                    .withSelect(filterSelect.withAfter(flatMapComments))
                    .withMethodType(mapInvocationType.withName("flatMap"))
                    .withPrefix(mapInvocation.getPrefix());
        }

        private static Space getFlatMapComments(JRightPadded<Expression> mapSelect, JRightPadded<Expression> filterSelect) {
            List<Comment> comments = new ArrayList<>();
            comments.addAll(filterSelect.getAfter().getComments());
            comments.addAll(mapSelect.getAfter().getComments());
            return filterSelect.getAfter().withComments(comments);
        }
    }
}
