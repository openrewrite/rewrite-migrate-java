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

public class NoGuavaIterablesAnyFilter extends Recipe {
    private static final MethodMatcher ITERABLES_ANY = new MethodMatcher("com.google.common.collect.Iterables any(java.lang.Iterable, com.google.common.base.Predicate)");
    private static final MethodMatcher ITERABLES_FILTER = new MethodMatcher("com.google.common.collect.Iterables filter(java.lang.Iterable, com.google.common.base.Predicate)");

    @Getter
    final String displayName = "Prefer `Collection.stream().anyMatch(Predicate)`";

    @Getter
    final String description = "Prefer `Collection.stream().anyMatch(Predicate)` over `Iterables.any(Collection, Predicate)`.";

    @Getter
    final Set<String> tags = singleton( "guava" );

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        TreeVisitor<?, ExecutionContext> precondition = Preconditions.or(new UsesMethod<>(ITERABLES_ANY), new UsesMethod<>(ITERABLES_FILTER));
        return Preconditions.check(precondition, new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                if (ITERABLES_ANY.matches(method)) {
                    maybeRemoveImport("com.google.common.base.Predicate");
                    maybeRemoveImport("com.google.common.collect.Iterables");
                    maybeAddImport("java.util.function.Predicate");

                    if (TypeUtils.isAssignableTo("java.util.Collection", method.getArguments().get(0).getType())) {
                        return JavaTemplate.builder("#{any(java.util.Collection)}.stream().anyMatch(#{any(java.util.function.Predicate)})")
                                .build()
                                .apply(getCursor(),
                                        method.getCoordinates().replace(),
                                        method.getArguments().get(0),
                                        method.getArguments().get(1));
                    }
                    return method;
                }
                if (ITERABLES_FILTER.matches(method)) {
                    maybeRemoveImport("com.google.common.base.Predicate");
                    maybeRemoveImport("com.google.common.collect.Iterables");
                    maybeAddImport("java.util.function.Predicate");

                    if (TypeUtils.isAssignableTo("java.util.Collection", method.getArguments().get(0).getType())) {
                        return JavaTemplate.builder("#{any(java.util.Collection)}.stream().filter(#{any(java.util.function.Predicate)}).toList()")
                                .build()
                                .apply(getCursor(),
                                        method.getCoordinates().replace(),
                                        method.getArguments().get(0),
                                        method.getArguments().get(1));
                    }
                    return method;
                }
                return super.visitMethodInvocation(method, ctx);
            }
        });
    }
}
