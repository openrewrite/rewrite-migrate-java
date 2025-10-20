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

import java.util.Set;

import static java.util.Collections.singleton;

public class NoGuavaPredicatesAndOr extends Recipe {
    private static final MethodMatcher PREDICATES_AND = new MethodMatcher("com.google.common.base.Predicates and(com.google.common.base.Predicate, com.google.common.base.Predicate)");
    private static final MethodMatcher PREDICATES_OR = new MethodMatcher("com.google.common.base.Predicates or(com.google.common.base.Predicate, com.google.common.base.Predicate)");

    @Override
    public String getDisplayName() {
        return "Prefer `Predicate.and(Predicate)`";
    }

    @Override
    public String getDescription() {
        return "Prefer `Predicate.and(Predicate)` over `Predicates.and(Predicate, Predicate)`.";
    }

    @Override
    public Set<String> getTags() {
        return singleton("guava");
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        TreeVisitor<?, ExecutionContext> precondition = Preconditions.or(new UsesMethod<>(PREDICATES_AND), new UsesMethod<>(PREDICATES_OR));
        return Preconditions.check(precondition, new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                if (PREDICATES_AND.matches(method)) {
                    maybeRemoveImport("com.google.common.base.Predicate");
                    maybeRemoveImport("com.google.common.base.Predicates");
                    maybeAddImport("java.util.function.Predicate");
                    if (method.getArguments().size() == 2) {
                        return JavaTemplate.builder("#{any(java.util.function.Predicate)}.and(#{any(java.util.function.Predicate)})")
                                .build()
                                .apply(getCursor(),
                                        method.getCoordinates().replace(),
                                        method.getArguments().get(0),
                                        method.getArguments().get(1));
                    }
                }
                if (PREDICATES_OR.matches(method)) {
                    maybeRemoveImport("com.google.common.base.Predicate");
                    maybeRemoveImport("com.google.common.base.Predicates");
                    maybeAddImport("java.util.function.Predicate");
                    if (method.getArguments().size() == 2) {
                        return JavaTemplate.builder("#{any(java.util.function.Predicate)}.or(#{any(java.util.function.Predicate)})")
                                .build()
                                .apply(getCursor(),
                                        method.getCoordinates().replace(),
                                        method.getArguments().get(0),
                                        method.getArguments().get(1));
                    }
                }

                return super.visitMethodInvocation(method, ctx);
            }
        });
    }
}
