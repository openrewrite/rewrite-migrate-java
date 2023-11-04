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
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.util.ArrayList;
import java.util.List;

public class ListFirstAndLast extends Recipe {

    private static final MethodMatcher ADD_MATCHER = new MethodMatcher("java.util.List add(int, ..)", true); // , * fails
    private static final MethodMatcher GET_MATCHER = new MethodMatcher("java.util.List get(int)", true);
    private static final MethodMatcher REMOVE_MATCHER = new MethodMatcher("java.util.List remove(int)", true);
    private static final MethodMatcher SIZE_MATCHER = new MethodMatcher("java.util.List size()", true);

    @Override
    public String getDisplayName() {
        return "Replace `List` `get`, `add`, and `remove` with `SequencedCollection` `*First` and `*Last` methods";
    }

    @Override
    public String getDescription() {
        return "Replace `list.get(0)` with `list.getFirst()`, `list.get(list.size() - 1)` with `list.getLast()`, and similar for `add(int, E)` and `remove(int)`.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                Preconditions.and(
                        new UsesJavaVersion<>(21),
                        Preconditions.or(
                                new UsesMethod<>(ADD_MATCHER),
                                new UsesMethod<>(GET_MATCHER),
                                new UsesMethod<>(REMOVE_MATCHER)
                        )
                ),
                new FirstLastVisitor());
    }

    private static class FirstLastVisitor extends JavaIsoVisitor<ExecutionContext> {
        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
            J.MethodInvocation mi = super.visitMethodInvocation(method, executionContext);
            Expression select = mi.getSelect();
            if (select == null || !(select instanceof J.Identifier)) {
                return mi;
            }
            J.Identifier sequencedCollection = (J.Identifier) select;

            final String operation;
            if (ADD_MATCHER.matches(mi)) {
                operation = "add";
            } else if (GET_MATCHER.matches(mi)) {
                operation = "get";
            } else if (REMOVE_MATCHER.matches(mi)) {
                operation = "remove";
            } else {
                return mi;
            }

            final String firstOrLast;
            Expression expression = mi.getArguments().get(0);
            if (J.Literal.isLiteralValue(expression, 0)) {
                firstOrLast = "First";
            } else if (lastElementOfSequencedCollection(sequencedCollection, expression)) {
                firstOrLast = "Last";
            } else {
                return mi;
            }

            List<Object> arguments = new ArrayList<>();
            if ("add".equals(operation)) {
                arguments.add(mi.getArguments().get(1));
            }
            return JavaTemplate.builder(operation + firstOrLast + (arguments.isEmpty() ? "()" : "(#{})")).build()
                    .apply(updateCursor(mi), mi.getCoordinates().replaceMethod(), arguments.toArray());
        }

        /**
         * @param sequencedCollection
         * @param expression
         * @return true, if we're calling `sequencedCollection.size() - 1` in expression
         */
        private static boolean lastElementOfSequencedCollection(J.Identifier sequencedCollection, Expression expression) {
            if (expression instanceof J.Binary) {
                J.Binary binary = (J.Binary) expression;
                if (binary.getOperator() == J.Binary.Type.Subtraction
                        && J.Literal.isLiteralValue(binary.getRight(), 1)
                        && SIZE_MATCHER.matches(binary.getLeft())) {
                    Expression sizeSelect = ((J.MethodInvocation) binary.getLeft()).getSelect();
                    if (sizeSelect instanceof J.Identifier) {
                        return sequencedCollection.getSimpleName().equals(((J.Identifier) sizeSelect).getSimpleName());
                    }
                }
            }
            return false;
        }
    }
}
