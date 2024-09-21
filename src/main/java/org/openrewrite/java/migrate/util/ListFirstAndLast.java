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
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ListFirstAndLast extends Recipe {

    // While more SequencedCollections have `*First` and `*Last` methods, only list has `get`, `add`, and `remove` methods that take an index
    private static final MethodMatcher ADD_MATCHER = new MethodMatcher("java.util.List add(int, ..)", true); // , * fails
    private static final MethodMatcher GET_MATCHER = new MethodMatcher("java.util.List get(int)", true);
    private static final MethodMatcher REMOVE_MATCHER = new MethodMatcher("java.util.List remove(int)", true);
    private static final MethodMatcher SIZE_MATCHER = new MethodMatcher("java.util.List size()", true);

    @Override
    public String getDisplayName() {
        return "Replace `List.get(int)`, `add(int, Object)`, and `remove(int)` with `SequencedCollection` `*First` and `*Last` methods";
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
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);

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

            // Limit *Last to identifiers for now, as x.get(x.size() - 1) requires the same reference for x
            if (mi.getSelect() instanceof J.Identifier) {
                return handleSelectIdentifier((J.Identifier) mi.getSelect(), mi, operation);
            }

            // XXX Maybe handle J.FieldAccess explicitly as well to support *Last on fields too

            // For anything else support limited cases, as we can't guarantee the same reference for the collection
            if (J.Literal.isLiteralValue(mi.getArguments().get(0), 0)) {
                return getMethodInvocation(mi, operation, "First");
            }

            return mi;
        }

        private static J.MethodInvocation handleSelectIdentifier(J.Identifier sequencedCollection, J.MethodInvocation mi, String operation) {
            final String firstOrLast;
            Expression expression = mi.getArguments().get(0);
            if (J.Literal.isLiteralValue(expression, 0)) {
                firstOrLast = "First";
            } else if (!"add".equals(operation) && lastElementOfSequencedCollection(sequencedCollection, expression)) {
                firstOrLast = "Last";
            } else {
                return mi;
            }
            return getMethodInvocation(mi, operation, firstOrLast);
        }

        private static J.MethodInvocation getMethodInvocation(J.MethodInvocation mi, String operation, String firstOrLast) {
            List<Expression> arguments = new ArrayList<>();
            final JavaType.Method newMethodType;
            JavaType.Method originalMethodType = mi.getMethodType();
            if ("add".equals(operation)) {
                arguments.add(mi.getArguments().get(1).withPrefix(Space.EMPTY));
                newMethodType = originalMethodType
                        .withName(operation + firstOrLast)
                        .withParameterNames(Collections.singletonList(originalMethodType.getParameterNames().get(1)))
                        .withParameterTypes(Collections.singletonList(originalMethodType.getParameterTypes().get(1)));
            } else {
                newMethodType = originalMethodType
                        .withName(operation + firstOrLast)
                        .withParameterNames(null)
                        .withParameterTypes(null);
            }
            return mi.withName(mi.getName().withSimpleName(operation + firstOrLast).withType(newMethodType))
                    .withArguments(arguments)
                    .withMethodType(newMethodType);
        }

        /**
         * @param sequencedCollection the identifier of the collection we're calling `get` on
         * @param expression          the expression we're passing to `get`
         * @return true, if we're calling `sequencedCollection.size() - 1` in expression on the same collection
         */
        private static boolean lastElementOfSequencedCollection(J.Identifier sequencedCollection, Expression expression) {
            if (expression instanceof J.Binary) {
                J.Binary binary = (J.Binary) expression;
                if (binary.getOperator() == J.Binary.Type.Subtraction &&
                    J.Literal.isLiteralValue(binary.getRight(), 1) &&
                    SIZE_MATCHER.matches(binary.getLeft())) {
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
