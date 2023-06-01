/*
 * Copyright 2021 the original author or authors.
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

import java.util.Collections;
import java.util.Set;

public class NoGuavaListsNewLinkedList extends Recipe {
    private static final MethodMatcher NEW_LINKED_LIST = new MethodMatcher("com.google.common.collect.Lists newLinkedList()");
    private static final MethodMatcher NEW_LINKED_LIST_ITERABLE = new MethodMatcher("com.google.common.collect.Lists newLinkedList(java.lang.Iterable)");

    @Override
    public String getDisplayName() {
        return "Prefer `new LinkedList<>()`";
    }

    @Override
    public String getDescription() {
        return "Prefer the Java standard library over third-party usage of Guava in simple cases like this.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("guava");
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(Preconditions.or(
                new UsesMethod<>(NEW_LINKED_LIST),
                new UsesMethod<>(NEW_LINKED_LIST_ITERABLE)), new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                if (NEW_LINKED_LIST.matches(method)) {
                    maybeRemoveImport("com.google.common.collect.Lists");
                    maybeAddImport("java.util.LinkedList");
                    return JavaTemplate.builder("new LinkedList<>()")
                            .contextSensitive()
                            .imports("java.util.LinkedList")
                            .build()
                            .apply(getCursor(), method.getCoordinates().replace());
                } else if (NEW_LINKED_LIST_ITERABLE.matches(method) && method.getArguments().size() == 1 &&
                           TypeUtils.isAssignableTo("java.util.Collection", method.getArguments().get(0).getType())) {
                    maybeRemoveImport("com.google.common.collect.Lists");
                    maybeAddImport("java.util.LinkedList");
                    return JavaTemplate.builder("new LinkedList<>(#{any(java.util.Collection)})")
                            .contextSensitive()
                            .imports("java.util.LinkedList")
                            .build()
                            .apply(getCursor(), method.getCoordinates().replace(), method.getArguments().get(0));
                }
                return super.visitMethodInvocation(method, ctx);
            }
        });
    }
}
