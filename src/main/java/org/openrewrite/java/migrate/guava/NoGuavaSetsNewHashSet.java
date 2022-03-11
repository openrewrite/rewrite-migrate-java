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
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.time.Duration;
import java.util.stream.Collectors;

public class NoGuavaSetsNewHashSet extends Recipe {
    private static final MethodMatcher NEW_HASH_SET = new MethodMatcher("com.google.common.collect.Sets newHashSet(..)");

    @Override
    public String getDisplayName() {
        return "Use `new HashSet<>()` instead of Guava";
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
        return new UsesMethod<>(NEW_HASH_SET);
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {
            private final JavaTemplate newHashSet = JavaTemplate.builder(this::getCursor, "new HashSet<>()")
                    .imports("java.util.HashSet")
                    .build();

            private final JavaTemplate newHashSetIterable = JavaTemplate.builder(this::getCursor, "new HashSet<>(#{any(java.lang.Iterable)})")
                    .imports("java.util.HashSet")
                    .build();

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                if (NEW_HASH_SET.matches(method)) {
                    maybeRemoveImport("com.google.common.collect.Sets");
                    maybeAddImport("java.util.HashSet");
                    if (method.getArguments().isEmpty() || (!method.getArguments().isEmpty() && method.getArguments().get(0) instanceof J.Empty)) {
                        return method.withTemplate(newHashSet, method.getCoordinates().replace());
                    } else if (method.getArguments().size() == 1 && TypeUtils.isAssignableTo(JavaType.ShallowClass.build("java.lang.Iterable"),
                            method.getArguments().get(0).getType())) {
                        return method.withTemplate(newHashSetIterable, method.getCoordinates().replace(),
                                method.getArguments().get(0));
                    } else {
                        maybeAddImport("java.util.Arrays");
                        JavaTemplate newHashSetVarargs = JavaTemplate.builder(this::getCursor, "new HashSet<>(Arrays.asList(" + method.getArguments().stream().map(a -> "#{any()}").collect(Collectors.joining(",")) + "))")
                                .imports("java.util.Arrays")
                                .imports("java.util.HashSet")
                                .build();
                        return method.withTemplate(newHashSetVarargs, method.getCoordinates().replace(),
                                method.getArguments().toArray());
                    }
                }
                return super.visitMethodInvocation(method, executionContext);
            }
        };
    }
}
