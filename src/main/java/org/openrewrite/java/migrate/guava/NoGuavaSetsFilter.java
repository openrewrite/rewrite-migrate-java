/*
 * Copyright 2025 the original author or authors.
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
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.util.Set;

import static java.util.Collections.singleton;

public class NoGuavaSetsFilter extends Recipe {
    private static final MethodMatcher SETS_FILTER = new MethodMatcher("com.google.common.collect.Sets filter(java.util.Set, com.google.common.base.Predicate)");
    private static final MethodMatcher SETS_FILTER_SORTED_SET = new MethodMatcher("com.google.common.collect.Sets filter(java.util.SortedSet, com.google.common.base.Predicate)");

    @Getter
    final String displayName = "Prefer `Collection.stream().filter(Predicate)`";

    @Getter
    final String description = "Prefer `Collection.stream().filter(Predicate)` over `Sets.filter(Set, Predicate)`.";

    @Getter
    final Set<String> tags = singleton( "guava" );

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        TreeVisitor<?, ExecutionContext> precondition = Preconditions.or(new UsesMethod<>(SETS_FILTER), new UsesMethod<>(SETS_FILTER_SORTED_SET));
        return Preconditions.check(precondition, new JavaVisitor<ExecutionContext>() {
                @Override
                public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                    if (SETS_FILTER_SORTED_SET.matches(method) && canReadTwice(method.getArguments().get(0))) {
                        maybeRemoveImport("com.google.common.base.Predicate");
                        maybeRemoveImport("com.google.common.collect.Sets");
                        maybeAddImport("java.util.TreeSet");
                        maybeAddImport("java.util.function.Predicate");
                        maybeAddImport("java.util.stream.Collectors");

                        return JavaTemplate.builder("#{set:any(java.util.SortedSet)}.stream().filter(#{any(java.util.function.Predicate)})" +
                                                    ".collect(Collectors.toCollection(() -> new TreeSet<>(#{set}.comparator())))")
                                .imports("java.util.TreeSet")
                                .imports("java.util.stream.Collectors")
                                .build()
                                .apply(getCursor(),
                                        method.getCoordinates().replace(),
                                        method.getArguments().get(0),
                                        method.getArguments().get(1));
                    }
                    if (SETS_FILTER.matches(method)) {
                        maybeRemoveImport("com.google.common.base.Predicate");
                        maybeRemoveImport("com.google.common.collect.Sets");
                        maybeAddImport("java.util.LinkedHashSet");
                        maybeAddImport("java.util.function.Predicate");
                        maybeAddImport("java.util.stream.Collectors");

                        // A `LinkedHashSet` keeps the iteration order of the filtered set, sorted or not
                        return JavaTemplate.builder("#{any(java.util.Collection)}.stream().filter(#{any(java.util.function.Predicate)}).collect(Collectors.toCollection(LinkedHashSet::new))")
                                .imports("java.util.LinkedHashSet")
                                .imports("java.util.stream.Collectors")
                                .build()
                                .apply(getCursor(),
                                        method.getCoordinates().replace(),
                                        method.getArguments().get(0),
                                        method.getArguments().get(1));
                    }
                    return super.visitMethodInvocation(method, ctx);
                }

                /**
                 * The comparator supplier reads the set a second time, from inside a lambda.
                 */
                private boolean canReadTwice(Expression set) {
                    boolean thisField = set instanceof J.FieldAccess && ((J.FieldAccess) set).getTarget() instanceof J.Identifier &&
                                        "this".equals(((J.Identifier) ((J.FieldAccess) set).getTarget()).getSimpleName());
                    boolean variable = set instanceof J.Identifier && ((J.Identifier) set).getFieldType() != null;
                    return (thisField || variable) && LambdaCapture.canCapture(getCursor(), set);
                }
            }
        );
    }
}
