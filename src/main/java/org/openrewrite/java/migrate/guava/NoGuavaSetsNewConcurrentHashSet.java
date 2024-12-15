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

import java.util.Collections;
import java.util.Set;

public class NoGuavaSetsNewConcurrentHashSet extends Recipe {
    private static final MethodMatcher NEW_HASH_SET = new MethodMatcher("com.google.common.collect.Sets newConcurrentHashSet()");

    @Override
    public String getDisplayName() {
        return "Prefer `new ConcurrentHashMap<>()`";
    }

    @Override
    public String getDescription() {
        return "Prefer the Java standard library over third-party usage of Guava in simple cases like this.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("guava");
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesMethod<>(NEW_HASH_SET), new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                if (NEW_HASH_SET.matches(method)) {
                    maybeRemoveImport("com.google.common.collect.Sets");
                    maybeAddImport("java.util.Collections");
                    maybeAddImport("java.util.concurrent.ConcurrentHashMap");
                    return JavaTemplate.builder("Collections.newSetFromMap(new ConcurrentHashMap<>())")
                            .contextSensitive()
                            .imports("java.util.Collections")
                            .imports("java.util.concurrent.ConcurrentHashMap")
                            .build()
                            .apply(getCursor(), method.getCoordinates().replace());
                }
                return super.visitMethodInvocation(method, ctx);
            }
        });
    }
}
