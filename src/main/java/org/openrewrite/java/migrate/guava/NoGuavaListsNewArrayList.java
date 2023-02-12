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
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.TypeUtils;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

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
            public JavaSourceFile visitJavaSourceFile(JavaSourceFile cu, ExecutionContext context) {
                doAfterVisit(new UsesMethod<>(NEW_ARRAY_LIST));
                doAfterVisit(new UsesMethod<>(NEW_ARRAY_LIST_ITERABLE));
                doAfterVisit(new UsesMethod<>(NEW_ARRAY_LIST_CAPACITY));
                return cu;
            }
        };
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {
            private final JavaTemplate newArrayList = JavaTemplate.builder(this::getCursor, "new ArrayList<>()")
                    .imports("java.util.ArrayList")
                    .build();

            private final JavaTemplate newArrayListCollection = JavaTemplate.builder(this::getCursor, "new ArrayList<>(#{any(java.util.Collection)})")
                    .imports("java.util.ArrayList")
                    .build();

            private final JavaTemplate newArrayListCapacity = JavaTemplate.builder(this::getCursor, "new ArrayList<>(#{any(int)})")
                    .imports("java.util.ArrayList")
                    .build();

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                if (NEW_ARRAY_LIST.matches(method)) {
                    maybeRemoveImport("com.google.common.collect.Lists");
                    maybeAddImport("java.util.ArrayList");
                    return method.withTemplate(newArrayList, method.getCoordinates().replace());
                } else if (NEW_ARRAY_LIST_ITERABLE.matches(method) && method.getArguments().size() == 1 &&
                           TypeUtils.isAssignableTo("java.util.Collection", method.getArguments().get(0).getType())) {
                    maybeRemoveImport("com.google.common.collect.Lists");
                    maybeAddImport("java.util.ArrayList");
                    return method.withTemplate(newArrayListCollection, method.getCoordinates().replace(),
                            method.getArguments().get(0));
                } else if (NEW_ARRAY_LIST_CAPACITY.matches(method)) {
                    maybeRemoveImport("com.google.common.collect.Lists");
                    maybeAddImport("java.util.ArrayList");
                    return method.withTemplate(newArrayListCapacity, method.getCoordinates().replace(),
                            method.getArguments().get(0));
                }
                return super.visitMethodInvocation(method, executionContext);
            }
        };
    }
}
