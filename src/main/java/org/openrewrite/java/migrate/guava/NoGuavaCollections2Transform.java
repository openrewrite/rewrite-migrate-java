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
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;

import java.util.Set;

import static java.util.Collections.singleton;

public class NoGuavaCollections2Transform extends Recipe {
    private static final MethodMatcher COLLECTIONS2_TRANSFORM = new MethodMatcher("com.google.common.collect.Collections2 transform(java.util.Collection, com.google.common.base.Function)");

    @Override
    public String getDisplayName() {
        return "Prefer `Collection.stream().map(Function)` over `Collections2.transform`";
    }

    @Override
    public String getDescription() {
        return "Prefer `Collection.stream().map(Function)` over `Collections2.transform(Collection, Function)`.";
    }

    @Override
    public Set<String> getTags() {
        return singleton("guava");
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                new UsesMethod<>(COLLECTIONS2_TRANSFORM),
                new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                        if (COLLECTIONS2_TRANSFORM.matches(method)) {
                            maybeRemoveImport("com.google.common.base.Function");
                            maybeRemoveImport("com.google.common.collect.Collections2");
                            maybeAddImport("java.util.function.Function");
                            maybeAddImport("java.util.stream.Collectors");

                            if (TypeUtils.isAssignableTo("java.util.Collection", method.getArguments().get(0).getType())) {
                                return JavaTemplate.builder("#{any(java.util.Collection)}.stream().map(#{any(java.util.function.Function)}).collect(Collectors.toList())")
                                        .imports("java.util.stream.Collectors")
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
                }
        );
    }
}
