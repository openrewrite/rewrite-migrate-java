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
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.ShortenFullyQualifiedTypeReferences;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.Objects;
import java.util.Set;

import static java.util.Collections.singleton;

public class NoGuavaPredicatesEqualTo extends Recipe {
    private static final MethodMatcher PREDICATES_EQUAL_TO = new MethodMatcher("com.google.common.base.Predicates equalTo(..)");

    @Getter
    final String displayName = "Prefer `Predicate.isEqual(Object)`";

    @Getter
    final String description = "Prefer `Predicate.isEqual(Object)` over `Predicates.equalTo(Object)`.";

    @Override
    public Set<String> getTags() {
        return singleton("guava");
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                new UsesMethod<>(PREDICATES_EQUAL_TO),
                new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                        if (PREDICATES_EQUAL_TO.matches(method)) {
                            maybeRemoveImport("com.google.common.base.Predicates");
                            maybeAddImport("java.util.function.Predicate");

                            JavaType argumentType = method.getArguments().get(0).getType();
                            if (argumentType == null) {
                                // Fallback if no type is found.
                                return JavaTemplate.builder("Predicate.isEqual(#{any(java.lang.Object)})")
                                        .imports("java.util.function.Predicate")
                                        .build()
                                        .apply(getCursor(),
                                                method.getCoordinates().replace(),
                                                method.getArguments().get(0));
                            }

                            String typeString = Objects.requireNonNull(argumentType).toString();
                            J.MethodInvocation genericMethod = JavaTemplate.builder("Predicate.<" + typeString + ">isEqual(#{any(java.lang.Object)})")
                                    .imports("java.util.function.Predicate")
                                    .build()
                                    .apply(getCursor(),
                                            method.getCoordinates().replace(),
                                            method.getArguments().get(0));
                            doAfterVisit(ShortenFullyQualifiedTypeReferences.modifyOnly(genericMethod));
                            return genericMethod;
                        }
                        return super.visitMethodInvocation(method, ctx);
                    }
                }
        );
    }
}
