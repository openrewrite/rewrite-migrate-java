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
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;

import java.util.Set;

import static java.util.Collections.singleton;

public class NoGuavaSetsFilter extends Recipe {
    private static final MethodMatcher SETS_FILTER = new MethodMatcher("com.google.common.collect.Sets filter(java.util.Set, com.google.common.base.Predicate)");

    @Override
    public String getDisplayName() {
        return "Prefer `Collection.stream().filter(Predicate)`";
    }

    @Override
    public String getDescription() {
        return "Prefer `Collection.stream().filter(Predicate)` over `Sets.filter(Set, Predicate)`.";
    }

    @Override
    public Set<String> getTags() {
        return singleton("guava");
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                new UsesMethod<>("com.google.common.collect.Sets filter(java.util.Set, com.google.common.base.Predicate)"),
                new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                        if (SETS_FILTER.matches(method)) {
                            maybeRemoveImport("com.google.common.base.Predicate");
                            maybeRemoveImport("com.google.common.collect.Sets");
                            maybeAddImport("java.util.function.Predicate");
                            maybeAddImport("java.util.stream.Collectors");

                            return JavaTemplate.builder("#{any(java.util.Collection)}.stream().filter(#{any(java.util.function.Predicate)}).collect(Collectors.toSet())")
                                    .imports("java.util.stream.Collectors")
                                    .build()
                                    .apply(getCursor(),
                                            method.getCoordinates().replace(),
                                            method.getArguments().get(0),
                                            method.getArguments().get(1));
                        }
                        return super.visitMethodInvocation(method, ctx);
                    }
                }
        );
    }
}
