/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.java.migrate.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.SearchResult;

import java.util.List;

@EqualsAndHashCode(callSuper = false)
@Value
public class FindCompletableFutureDefaultExecutor extends Recipe {

    private static final MethodMatcher COMPLETION_STAGE_ASYNC =
            new MethodMatcher("java.util.concurrent.CompletionStage *Async(..)", true);
    private static final MethodMatcher COMPLETABLE_FUTURE_ASYNC =
            new MethodMatcher("java.util.concurrent.CompletableFuture *Async(..)", true);
    private static final MethodMatcher DELAYED_EXECUTOR =
            new MethodMatcher("java.util.concurrent.CompletableFuture delayedExecutor(long, java.util.concurrent.TimeUnit)");

    String displayName = "Find `CompletableFuture` async calls without an `Executor`";

    String description = "Finds `CompletableFuture` and `CompletionStage` `*Async` calls that don't pass an `Executor`, " +
                         "and `CompletableFuture.delayedExecutor` calls without a base `Executor`. These run on " +
                         "`ForkJoinPool.commonPool()`. Before JDK 25, a common pool with parallelism of 1 or less " +
                         "(e.g. on a 2-CPU host) made `CompletableFuture` fall back to a new thread per task; from " +
                         "JDK 25 that fallback is gone, so blocking work queues behind a single worker thread. " +
                         "Review each call and pass a dedicated `Executor` where the task blocks.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                Preconditions.or(
                        new UsesType<>("java.util.concurrent.CompletableFuture", false),
                        new UsesType<>("java.util.concurrent.CompletionStage", false)
                ),
                new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                        J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                        if (DELAYED_EXECUTOR.matches(mi)) {
                            return SearchResult.found(mi, "delayedExecutor without a base Executor runs tasks on ForkJoinPool.commonPool()");
                        }
                        if ((COMPLETION_STAGE_ASYNC.matches(mi) || COMPLETABLE_FUTURE_ASYNC.matches(mi)) && !hasExecutorArgument(mi)) {
                            return SearchResult.found(mi, mi.getSimpleName() + " without an Executor runs on ForkJoinPool.commonPool()");
                        }
                        return mi;
                    }

                    private boolean hasExecutorArgument(J.MethodInvocation mi) {
                        JavaType.Method methodType = mi.getMethodType();
                        if (methodType == null) {
                            return true;
                        }
                        List<JavaType> parameterTypes = methodType.getParameterTypes();
                        return !parameterTypes.isEmpty() &&
                               TypeUtils.isAssignableTo("java.util.concurrent.Executor", parameterTypes.get(parameterTypes.size() - 1));
                    }
                }
        );
    }
}
