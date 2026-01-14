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

public class NoGuavaSetsNewLinkedHashSet extends Recipe {
    private static final MethodMatcher NEW_LINKED_HASH_SET = new MethodMatcher("com.google.common.collect.Sets newLinkedHashSet()");
    private static final MethodMatcher NEW_LINKED_HASH_SET_ITERABLE = new MethodMatcher("com.google.common.collect.Sets newLinkedHashSet(java.lang.Iterable)");
    private static final MethodMatcher NEW_LINKED_HASH_SET_CAPACITY = new MethodMatcher("com.google.common.collect.Sets newLinkedHashSetWithExpectedSize(int)");

    @Getter
    final String displayName = "Prefer `new LinkedHashSet<>()`";

    @Getter
    final String description = "Prefer the Java standard library over third-party usage of Guava in simple cases like this.";

    @Override
    public Set<String> getTags() {
        return singleton("guava");
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(Preconditions.or(
                new UsesMethod<>(NEW_LINKED_HASH_SET),
                new UsesMethod<>(NEW_LINKED_HASH_SET_ITERABLE),
                new UsesMethod<>(NEW_LINKED_HASH_SET_CAPACITY)), new JavaVisitor<ExecutionContext>() {

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                if (NEW_LINKED_HASH_SET.matches(method)) {
                    maybeRemoveImport("com.google.common.collect.Sets");
                    maybeAddImport("java.util.LinkedHashSet");
                    return JavaTemplate.builder("new LinkedHashSet<>()")
                            .contextSensitive()
                            .imports("java.util.LinkedHashSet")
                            .build()
                            .apply(getCursor(), method.getCoordinates().replace());
                }
                if (NEW_LINKED_HASH_SET_ITERABLE.matches(method) && method.getArguments().size() == 1 &&
                        TypeUtils.isAssignableTo("java.util.Collection", method.getArguments().get(0).getType())) {
                    maybeRemoveImport("com.google.common.collect.Sets");
                    maybeAddImport("java.util.LinkedHashSet");
                    return JavaTemplate.builder("new LinkedHashSet<>(#{any(java.util.Collection)})")
                            .contextSensitive()
                            .imports("java.util.LinkedHashSet")
                            .build()
                            .apply(getCursor(), method.getCoordinates().replace(), method.getArguments().get(0));
                }
                if (NEW_LINKED_HASH_SET_CAPACITY.matches(method)) {
                    maybeRemoveImport("com.google.common.collect.Sets");
                    maybeAddImport("java.util.LinkedHashSet");
                    return JavaTemplate.builder("new LinkedHashSet<>(#{any(int)})")
                            .contextSensitive()
                            .imports("java.util.LinkedHashSet")
                            .build()
                            .apply(getCursor(), method.getCoordinates().replace(), method.getArguments().get(0));
                }
                return super.visitMethodInvocation(method, ctx);
            }
        });
    }
}
