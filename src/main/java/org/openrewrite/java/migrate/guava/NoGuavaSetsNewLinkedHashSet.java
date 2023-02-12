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
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

public class NoGuavaSetsNewLinkedHashSet extends Recipe {
    private static final MethodMatcher NEW_LINKED_HASH_SET = new MethodMatcher("com.google.common.collect.Sets newLinkedHashSet()");
    private static final MethodMatcher NEW_LINKED_HASH_SET_ITERABLE = new MethodMatcher("com.google.common.collect.Sets newLinkedHashSet(java.lang.Iterable)");
    private static final MethodMatcher NEW_LINKED_HASH_SET_CAPACITY = new MethodMatcher("com.google.common.collect.Sets newLinkedHashSetWithExpectedSize(int)");

    @Override
    public String getDisplayName() {
        return "Prefer `new LinkedHashSet<>()`";
    }

    @Override
    public String getDescription() {
        return "Prefer the Java standard library over third-party usage of Guava in simple cases like this.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(5);
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("guava");
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getApplicableTest() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext executionContext) {
                doAfterVisit(new UsesMethod<>(NEW_LINKED_HASH_SET));
                doAfterVisit(new UsesMethod<>(NEW_LINKED_HASH_SET_ITERABLE));
                doAfterVisit(new UsesMethod<>(NEW_LINKED_HASH_SET_CAPACITY));
                return cu;
            }
        };
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {
            private final JavaTemplate newLinkedHashSet = JavaTemplate.builder(this::getCursor, "new LinkedHashSet<>()")
                    .imports("java.util.LinkedHashSet")
                    .build();

            private final JavaTemplate newLinkedHashSetCollection = JavaTemplate.builder(this::getCursor, "new LinkedHashSet<>(#{any(java.util.Collection)})")
                    .imports("java.util.LinkedHashSet")
                    .build();

            private final JavaTemplate newLinkedHashSetCapacity = JavaTemplate.builder(this::getCursor, "new LinkedHashSet<>(#{any(int)})")
                    .imports("java.util.LinkedHashSet")
                    .build();

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                if (NEW_LINKED_HASH_SET.matches(method)) {
                    maybeRemoveImport("com.google.common.collect.Sets");
                    maybeAddImport("java.util.LinkedHashSet");
                    return method.withTemplate(newLinkedHashSet, method.getCoordinates().replace());
                } else if (NEW_LINKED_HASH_SET_ITERABLE.matches(method) && method.getArguments().size() == 1 &&
                           TypeUtils.isAssignableTo("java.util.Collection", method.getArguments().get(0).getType())) {
                    maybeRemoveImport("com.google.common.collect.Sets");
                    maybeAddImport("java.util.LinkedHashSet");
                    return method.withTemplate(newLinkedHashSetCollection, method.getCoordinates().replace(),
                            method.getArguments().get(0));
                } else if (NEW_LINKED_HASH_SET_CAPACITY.matches(method)) {
                    maybeRemoveImport("com.google.common.collect.Sets");
                    maybeAddImport("java.util.LinkedHashSet");
                    return method.withTemplate(newLinkedHashSetCapacity, method.getCoordinates().replace(),
                            method.getArguments().get(0));
                }
                return super.visitMethodInvocation(method, executionContext);
            }
        };
    }
}
