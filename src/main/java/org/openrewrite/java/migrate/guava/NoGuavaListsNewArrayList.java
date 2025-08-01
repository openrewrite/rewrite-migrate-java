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
package org.openrewrite.java.migrate.guava;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;

import java.util.Set;

import static java.util.Collections.singleton;

public class NoGuavaListsNewArrayList extends Recipe {
    private static final MethodMatcher NEW_ARRAY_LIST = new MethodMatcher("com.google.common.collect.Lists newArrayList()");
    private static final MethodMatcher NEW_ARRAY_LIST_ITERABLE = new MethodMatcher("com.google.common.collect.Lists newArrayList(java.lang.Iterable)");
    private static final MethodMatcher NEW_ARRAY_LIST_CAPACITY = new MethodMatcher("com.google.common.collect.Lists newArrayListWithCapacity(int)");

    @Override
    public String getDisplayName() {
        return "Prefer `new ArrayList<>()`";
    }

    @Override
    public String getDescription() {
        return "Prefer the Java standard library over third-party usage of Guava in simple cases like this.";
    }

    @Override
    public Set<String> getTags() {
        return singleton("guava");
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(Preconditions.or(
                new UsesMethod<>(NEW_ARRAY_LIST),
                new UsesMethod<>(NEW_ARRAY_LIST_ITERABLE),
                new UsesMethod<>(NEW_ARRAY_LIST_CAPACITY)), new JavaVisitor<ExecutionContext>() {
            private final JavaTemplate newArrayList = JavaTemplate.builder("new ArrayList<>()")
                    .contextSensitive()
                    .imports("java.util.ArrayList")
                    .build();

            private final JavaTemplate newArrayListCollection = JavaTemplate.builder("new ArrayList<>(#{any(java.util.Collection)})")
                    .contextSensitive()
                    .imports("java.util.ArrayList")
                    .build();

            private final JavaTemplate newArrayListCapacity = JavaTemplate.builder("new ArrayList<>(#{any(int)})")
                    .contextSensitive()
                    .imports("java.util.ArrayList")
                    .build();

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                if (NEW_ARRAY_LIST.matches(method)) {
                    maybeRemoveImport("com.google.common.collect.Lists");
                    maybeAddImport("java.util.ArrayList");
                    return newArrayList.apply(getCursor(), method.getCoordinates().replace());
                }
                if (NEW_ARRAY_LIST_ITERABLE.matches(method) && method.getArguments().size() == 1 &&
                        TypeUtils.isAssignableTo("java.util.Collection", method.getArguments().get(0).getType())) {
                    maybeRemoveImport("com.google.common.collect.Lists");
                    maybeAddImport("java.util.ArrayList");
                    return newArrayListCollection.apply(getCursor(), method.getCoordinates().replace(),
                            method.getArguments().get(0));
                }
                if (NEW_ARRAY_LIST_CAPACITY.matches(method)) {
                    maybeRemoveImport("com.google.common.collect.Lists");
                    maybeAddImport("java.util.ArrayList");
                    return newArrayListCapacity.apply(getCursor(), method.getCoordinates().replace(),
                            method.getArguments().get(0));
                }
                return super.visitMethodInvocation(method, ctx);
            }
        });
    }
}
