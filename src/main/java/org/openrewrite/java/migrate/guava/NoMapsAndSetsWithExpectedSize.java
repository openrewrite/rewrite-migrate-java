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

import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaCoordinates;

public class NoMapsAndSetsWithExpectedSize extends Recipe {

    private static final MethodMatcher NEW_HASHMAP = new MethodMatcher("com.google.common.collect.Maps newHashMapWithExpectedSize(int)", false);
    private static final MethodMatcher NEW_LINKED_HASHMAP = new MethodMatcher("com.google.common.collect.Maps newLinkedHashMapWithExpectedSize(int)", false);
    private static final MethodMatcher NEW_HASHSET = new MethodMatcher("com.google.common.collect.Sets newHashSetWithExpectedSize(int)", false);
    private static final MethodMatcher NEW_LINKED_HASHSET = new MethodMatcher("com.google.common.collect.Sets newLinkedHashSetWithExpectedSize(int)", false);

    @Override
    public String getDisplayName() {
        return "Prefer JDK methods for Maps and Sets of an expected size";
    }

    @Override
    public String getDescription() {
        return "Prefer Java 19+ methods to create Maps and Sets of an expected size instead of using Guava methods.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                Preconditions.and(
                        new UsesJavaVersion<>(19),
                        Preconditions.or(
                                new UsesMethod<>(NEW_HASHMAP),
                                new UsesMethod<>(NEW_LINKED_HASHMAP),
                                new UsesMethod<>(NEW_HASHSET),
                                new UsesMethod<>(NEW_LINKED_HASHSET)
                        )
                ),
                new JavaVisitor<ExecutionContext>() {
                    @Override
                    public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                        J.MethodInvocation j = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
                        if (NEW_HASHMAP.matches(j)) {
                            maybeRemoveImport("com.google.common.collect.Maps");
                            maybeAddImport("java.util.HashMap");
                            JavaCoordinates coordinates = j.getCoordinates().replace();
                            return JavaTemplate.builder("new HashMap<>(#{any()})")
                                    .imports("java.util.HashMap")
                                    .build()
                                    .apply(getCursor(), coordinates, j.getArguments().toArray());
                        } else if (NEW_LINKED_HASHMAP.matches(j)) {
                            maybeRemoveImport("com.google.common.collect.Maps");
                            maybeAddImport("java.util.LinkedHashMap");
                            JavaCoordinates coordinates = j.getCoordinates().replace();
                            return JavaTemplate.builder("new LinkedHashMap<>(#{any()})")
                                    .imports("java.util.LinkedHashMap")
                                    .build()
                                    .apply(getCursor(), coordinates, j.getArguments().toArray());
                        } else if (NEW_HASHSET.matches(j)) {
                            maybeRemoveImport("com.google.common.collect.Sets");
                            maybeAddImport("java.util.HashSet");
                            JavaCoordinates coordinates = j.getCoordinates().replace();
                            return JavaTemplate.builder("new HashSet<>(#{any()})")
                                    .imports("java.util.HashSet")
                                    .build()
                                    .apply(getCursor(), coordinates, j.getArguments().toArray());
                        } else if (NEW_LINKED_HASHSET.matches(j)) {
                            maybeRemoveImport("com.google.common.collect.Sets");
                            maybeAddImport("java.util.LinkedHashSet");
                            JavaCoordinates coordinates = j.getCoordinates().replace();
                            return JavaTemplate.builder("new LinkedHashSet<>(#{any()})")
                                    .imports("java.util.LinkedHashSet")
                                    .build()
                                    .apply(getCursor(), coordinates, j.getArguments().toArray());
                        }
                        return j;
                    }
                }
        );
    }
}
