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
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

public class IteratorNext extends Recipe {
    private static final MethodMatcher ITERATOR_MATCHER = new MethodMatcher("java.util.Collection iterator()", true);
    private static final MethodMatcher NEXT_MATCHER = new MethodMatcher("java.util.Iterator next()", true);

    @Getter
    final String displayName = "Replace `iterator().next()` with `getFirst()`";

    @Getter
    final String description = "Replace `SequencedCollection.iterator().next()` with `getFirst()`.";

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
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                        J.MethodInvocation nextInvocation = super.visitMethodInvocation(method, ctx);
                        if (NEXT_MATCHER.matches(nextInvocation) && ITERATOR_MATCHER.matches(nextInvocation.getSelect())) {
                            J.MethodInvocation iteratorInvocation = (J.MethodInvocation) nextInvocation.getSelect();
                            Expression iteratorSelect = iteratorInvocation.getSelect();
                            if (iteratorSelect != null && TypeUtils.isAssignableTo("java.util.SequencedCollection", iteratorSelect.getType())) {
                                JavaType.Method getFirst = iteratorInvocation.getMethodType().withName("getFirst");
                                return iteratorInvocation
                                        .withName(iteratorInvocation.getName().withSimpleName("getFirst").withType(getFirst))
                                        .withMethodType(getFirst)
                                        .withPrefix(nextInvocation.getPrefix());
                            }
                        }
                        return nextInvocation;
                    }
                }
        );
    }
}
