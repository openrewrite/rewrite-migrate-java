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

import java.time.Duration;

public class NoGuavaMapsNewLinkedHashMap extends Recipe {
    private static final MethodMatcher NEW_LINKED_HASH_MAP = new MethodMatcher("com.google.common.collect.Maps newLinkedHashMap()");
    private static final MethodMatcher NEW_LINKED_HASH_MAP_WITH_MAP = new MethodMatcher("com.google.common.collect.Maps newLinkedHashMap(java.util.Map)");
    private static final MethodMatcher NEW_LINKED_HASH_MAP_CAPACITY = new MethodMatcher("com.google.common.collect.Maps newLinkedHashMapWithExpectedSize(int)");

    @Override
    public String getDisplayName() {
        return "Use `new LinkedHashMap<>()` instead of Guava";
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
    protected TreeVisitor<?, ExecutionContext> getApplicableTest() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext executionContext) {
                doAfterVisit(new UsesMethod<>(NEW_LINKED_HASH_MAP));
                doAfterVisit(new UsesMethod<>(NEW_LINKED_HASH_MAP_WITH_MAP));
                doAfterVisit(new UsesMethod<>(NEW_LINKED_HASH_MAP_CAPACITY));
                return cu;
            }
        };
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {
            private final JavaTemplate newLinkedHashMap = JavaTemplate.builder(this::getCursor, "new LinkedHashMap<>()")
                    .imports("java.util.LinkedHashMap")
                    .build();

            private final JavaTemplate newLinkedHashMapWithMap = JavaTemplate.builder(this::getCursor, "new LinkedHashMap<>(#{any(java.util.Map)})")
                    .imports("java.util.LinkedHashMap")
                    .build();

            private final JavaTemplate newLinkedHashMapCapacity = JavaTemplate.builder(this::getCursor, "new LinkedHashMap<>(#{any(int)})")
                    .imports("java.util.LinkedHashMap")
                    .build();

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                if (NEW_LINKED_HASH_MAP.matches(method)) {
                    maybeRemoveImport("com.google.common.collect.Maps");
                    maybeAddImport("java.util.LinkedHashMap");
                    return method.withTemplate(newLinkedHashMap, method.getCoordinates().replace());
                } else if (NEW_LINKED_HASH_MAP_WITH_MAP.matches(method)) {
                    maybeRemoveImport("com.google.common.collect.Maps");
                    maybeAddImport("java.util.LinkedHashMap");
                    return method.withTemplate(newLinkedHashMapWithMap, method.getCoordinates().replace(),
                            method.getArguments().get(0));
                } else if (NEW_LINKED_HASH_MAP_CAPACITY.matches(method)) {
                    maybeRemoveImport("com.google.common.collect.Maps");
                    maybeAddImport("java.util.LinkedHashMap");
                    return method.withTemplate(newLinkedHashMapCapacity, method.getCoordinates().replace(),
                            method.getArguments().get(0));
                }
                return super.visitMethodInvocation(method, executionContext);
            }
        };
    }
}
