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
import org.openrewrite.java.tree.J;

import java.util.Set;

import static java.util.Collections.singleton;

public class NoGuavaOptionalAsSet extends Recipe {
    private static final MethodMatcher OPTIONAL_AS_SET = new MethodMatcher("com.google.common.base.Optional asSet()");

    @Getter
    final String displayName = "Prefer `Optional.stream().collect(Collectors.toSet())`";

    @Getter
    final String description = "Prefer `Optional.stream().collect(Collectors.toSet())` over `Optional.asSet()`.";

    @Getter
    final Set<String> tags = singleton( "guava" );

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesMethod<>(OPTIONAL_AS_SET), new JavaVisitor<ExecutionContext>() {
                    @Override
                    public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                        if (OPTIONAL_AS_SET.matches(method)) {
                            maybeAddImport("java.util.stream.Collectors");
                            return JavaTemplate.builder("#{any(java.util.Optional)}.stream().collect(Collectors.toSet())")
                                    .imports("java.util.stream.Collectors")
                                    .build()
                                    .apply(getCursor(),
                                            method.getCoordinates().replace(),
                                            method.getSelect());
                        }
                        return super.visitMethodInvocation(method, ctx);
                    }
                }
        );
    }
}
