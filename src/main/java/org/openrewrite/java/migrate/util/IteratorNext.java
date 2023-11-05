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
import org.openrewrite.java.tree.TypeUtils;

public class IteratorNext extends Recipe {
    private static final MethodMatcher ITERATOR_MATCHER = new MethodMatcher("java.util.Collection iterator()", true);
    private static final MethodMatcher NEXT_MATCHER = new MethodMatcher("java.util.Iterator next()", true);

    @Override
    public String getDisplayName() {
        return "Replace `iterator().next()` with `getFirst()`";
    }

    @Override
    public String getDescription() {
        return "Replace `SequencedCollection.iterator().next()` with `getFirst()`.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                Preconditions.and(
                        new UsesJavaVersion<>(21),
                        Preconditions.and(
                                new UsesMethod<>(ITERATOR_MATCHER),
                                new UsesMethod<>(NEXT_MATCHER)
                        )
                ),
                new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                        J.MethodInvocation nextInvocation = super.visitMethodInvocation(method, executionContext);
                        if (NEXT_MATCHER.matches(nextInvocation) && ITERATOR_MATCHER.matches(nextInvocation.getSelect())) {
                            J.MethodInvocation iteratorInvocation = (J.MethodInvocation) nextInvocation.getSelect();
                            if (TypeUtils.isAssignableTo("java.util.SequencedCollection", iteratorInvocation.getSelect().getType())) {
                                J.MethodInvocation getFirstInvocation = nextInvocation.withSelect(iteratorInvocation.getSelect())
                                        .withName(nextInvocation.getName().withSimpleName("getFirst"))
                                        .withMethodType(nextInvocation.getMethodType()
                                                .withName("getFirst")
                                                .withDeclaringType(iteratorInvocation.getMethodType().getDeclaringType()));
                                return getFirstInvocation;
                            }
                        }
                        return nextInvocation;
                    }
                }
        );
    }
}
