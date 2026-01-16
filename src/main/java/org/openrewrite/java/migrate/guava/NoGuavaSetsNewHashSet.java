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

import lombok.Getter;
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
import static java.util.stream.Collectors.joining;

public class NoGuavaSetsNewHashSet extends Recipe {
    private static final MethodMatcher NEW_HASH_SET = new MethodMatcher("com.google.common.collect.Sets newHashSet(..)");

    @Getter
    final String displayName = "Prefer `new HashSet<>()`";

    @Getter
    final String description = "Prefer the Java standard library over third-party usage of Guava in simple cases like this.";

    @Getter
    final Set<String> tags = singleton( "guava" );

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesMethod<>(NEW_HASH_SET), new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                if (NEW_HASH_SET.matches(method)) {
                    if (method.getArguments().isEmpty() || method.getArguments().get(0) instanceof J.Empty) {
                        maybeRemoveImport("com.google.common.collect.Sets");
                        maybeAddImport("java.util.HashSet");
                        return JavaTemplate.builder("new HashSet<>()")
                                .contextSensitive()
                                .imports("java.util.HashSet")
                                .build()
                                .apply(getCursor(), method.getCoordinates().replace());
                    }
                    if (method.getArguments().size() == 1) {
                        // Only handle if it's a Collection (not just any Iterable)
                        if (TypeUtils.isAssignableTo("java.util.Collection", method.getArguments().get(0).getType())) {
                            maybeRemoveImport("com.google.common.collect.Sets");
                            maybeAddImport("java.util.HashSet");
                            return JavaTemplate.builder("new HashSet<>(#{any(java.util.Collection)})")
                                    .contextSensitive()
                                    .imports("java.util.HashSet")
                                    .build()
                                    .apply(getCursor(), method.getCoordinates().replace(), method.getArguments().get(0));
                        }
                        // Skip Iterable-only and Iterator-only cases to avoid generating broken code
                        if (TypeUtils.isAssignableTo("java.lang.Iterable", method.getArguments().get(0).getType()) || TypeUtils.isAssignableTo("java.util.Iterator", method.getArguments().get(0).getType())) {
                            return method;
                        }
                    }
                    maybeRemoveImport("com.google.common.collect.Sets");
                    maybeAddImport("java.util.HashSet");
                    maybeAddImport("java.util.Arrays");
                    JavaTemplate newHashSetVarargs = JavaTemplate.builder("new HashSet<>(Arrays.asList(" + method.getArguments().stream().map(a -> "#{any()}").collect(joining(",")) + "))")
                            .contextSensitive()
                            .imports("java.util.Arrays")
                            .imports("java.util.HashSet")
                            .build();
                    return newHashSetVarargs.apply(getCursor(), method.getCoordinates().replace(),
                            method.getArguments().toArray());
                }
                return super.visitMethodInvocation(method, ctx);
            }
        });
    }
}
