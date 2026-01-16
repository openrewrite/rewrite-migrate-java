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

import java.util.Set;

import static java.util.Collections.singleton;

public class NoGuavaAtomicsNewReference extends Recipe {
    private static final MethodMatcher NEW_ATOMIC_REFERENCE = new MethodMatcher("com.google.common.util.concurrent.Atomics newReference(..)");

    @Getter
    final String displayName = "Prefer `new AtomicReference<>()`";

    @Getter
    final String description = "Prefer the Java standard library over third-party usage of Guava in simple cases like this.";

    @Getter
    final Set<String> tags = singleton( "guava" );

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesMethod<>(NEW_ATOMIC_REFERENCE), new JavaVisitor<ExecutionContext>() {
            private final JavaTemplate newAtomicReference = JavaTemplate.builder("new AtomicReference<>()")
                    .imports("java.util.concurrent.atomic.AtomicReference")
                    .build();

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                if (NEW_ATOMIC_REFERENCE.matches(method)) {
                    maybeRemoveImport("com.google.common.util.concurrent.Atomics");
                    maybeAddImport("java.util.concurrent.atomic.AtomicReference");
                    return ((J.NewClass) newAtomicReference.apply(getCursor(), method.getCoordinates().replace()))
                            .withArguments(method.getArguments());
                }
                return super.visitMethodInvocation(method, ctx);
            }
        });
    }
}
