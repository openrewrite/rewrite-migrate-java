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

public class NoGuavaDirectExecutor extends Recipe {
    private static final MethodMatcher DIRECT_EXECUTOR = new MethodMatcher("com.google.common.util.concurrent.MoreExecutors directExecutor()");

    @Getter
    final String displayName = "Prefer `Runnable::run`";

    @Getter
    final String description = "`Executor` is a SAM-compatible interface, so `Runnable::run` is just as succinct as `MoreExecutors.directExecutor()` but without the third-party library requirement.";

    @Getter
    final Set<String> tags = singleton( "guava" );

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesMethod<>(DIRECT_EXECUTOR), new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                if (DIRECT_EXECUTOR.matches(method)) {
                    maybeRemoveImport("com.google.common.util.concurrent.MoreExecutors");
                    return JavaTemplate.builder("Runnable::run")
                            .contextSensitive()
                            .imports("java.lang.Runnable")
                            .build()
                            .apply(getCursor(), method.getCoordinates().replace());
                }
                return super.visitMethodInvocation(method, ctx);
            }
        });
    }
}
