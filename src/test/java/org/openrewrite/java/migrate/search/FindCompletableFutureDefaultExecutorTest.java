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

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class FindCompletableFutureDefaultExecutorTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FindCompletableFutureDefaultExecutor());
    }

    @DocumentExample
    @Test
    void supplyAsyncWithoutExecutor() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.List;
              import java.util.concurrent.CompletableFuture;
              import java.util.concurrent.CompletionStage;

              class BatchLoader {
                  CompletionStage<List<String>> load(List<String> keys) {
                      return CompletableFuture.supplyAsync(() -> doLoad(keys));
                  }

                  List<String> doLoad(List<String> keys) {
                      return keys;
                  }
              }
              """,
            """
              import java.util.List;
              import java.util.concurrent.CompletableFuture;
              import java.util.concurrent.CompletionStage;

              class BatchLoader {
                  CompletionStage<List<String>> load(List<String> keys) {
                      return /*~~(supplyAsync without an Executor runs on ForkJoinPool.commonPool(), which can lead to performance degradation for blocking tasks)~~>*/CompletableFuture.supplyAsync(() -> doLoad(keys));
                  }

                  List<String> doLoad(List<String> keys) {
                      return keys;
                  }
              }
              """
          )
        );
    }

    @Test
    void runAsyncWithoutExecutor() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.concurrent.CompletableFuture;

              class Audit {
                  void audit(Runnable post) {
                      CompletableFuture.runAsync(post);
                  }
              }
              """,
            """
              import java.util.concurrent.CompletableFuture;

              class Audit {
                  void audit(Runnable post) {
                      /*~~(runAsync without an Executor runs on ForkJoinPool.commonPool(), which can lead to performance degradation for blocking tasks)~~>*/CompletableFuture.runAsync(post);
                  }
              }
              """
          )
        );
    }

    @Test
    void chainedAsyncStageWithoutExecutor() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.concurrent.CompletableFuture;
              import java.util.concurrent.Executor;

              class Checkout {
                  CompletableFuture<String> checkout(Executor ioPool) {
                      return CompletableFuture.supplyAsync(() -> "quote", ioPool)
                        .thenComposeAsync(quote -> CompletableFuture.completedFuture(quote + " charged"));
                  }
              }
              """,
            """
              import java.util.concurrent.CompletableFuture;
              import java.util.concurrent.Executor;

              class Checkout {
                  CompletableFuture<String> checkout(Executor ioPool) {
                      return /*~~(thenComposeAsync without an Executor runs on ForkJoinPool.commonPool(), which can lead to performance degradation for blocking tasks)~~>*/CompletableFuture.supplyAsync(() -> "quote", ioPool)
                        .thenComposeAsync(quote -> CompletableFuture.completedFuture(quote + " charged"));
                  }
              }
              """
          )
        );
    }

    @Test
    void combinatorWithoutExecutor() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.concurrent.CompletableFuture;

              class Offers {
                  CompletableFuture<String> offer(CompletableFuture<String> quote, CompletableFuture<Integer> stock) {
                      return quote.thenCombineAsync(stock, (q, s) -> q + s);
                  }
              }
              """,
            """
              import java.util.concurrent.CompletableFuture;

              class Offers {
                  CompletableFuture<String> offer(CompletableFuture<String> quote, CompletableFuture<Integer> stock) {
                      return /*~~(thenCombineAsync without an Executor runs on ForkJoinPool.commonPool(), which can lead to performance degradation for blocking tasks)~~>*/quote.thenCombineAsync(stock, (q, s) -> q + s);
                  }
              }
              """
          )
        );
    }

    @Test
    void completionStageReceiver() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.concurrent.CompletionStage;

              class Tracker {
                  void track(CompletionStage<String> stage) {
                      stage.whenCompleteAsync((r, e) -> System.out.println(r));
                  }
              }
              """,
            """
              import java.util.concurrent.CompletionStage;

              class Tracker {
                  void track(CompletionStage<String> stage) {
                      /*~~(whenCompleteAsync without an Executor runs on ForkJoinPool.commonPool(), which can lead to performance degradation for blocking tasks)~~>*/stage.whenCompleteAsync((r, e) -> System.out.println(r));
                  }
              }
              """
          )
        );
    }

    @Test
    void delayedExecutorAndCompleteAsync() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.concurrent.CompletableFuture;
              import java.util.concurrent.Executor;
              import java.util.concurrent.TimeUnit;

              class Retry {
                  void retryLater(Runnable r, CompletableFuture<String> f) {
                      Executor later = CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS);
                      CompletableFuture.runAsync(r, later);
                      f.completeAsync(() -> "done");
                  }
              }
              """,
            """
              import java.util.concurrent.CompletableFuture;
              import java.util.concurrent.Executor;
              import java.util.concurrent.TimeUnit;

              class Retry {
                  void retryLater(Runnable r, CompletableFuture<String> f) {
                      Executor later = /*~~(delayedExecutor without an Executor runs on ForkJoinPool.commonPool(), which can lead to performance degradation for blocking tasks)~~>*/CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS);
                      CompletableFuture.runAsync(r, later);
                      /*~~(completeAsync without an Executor runs on ForkJoinPool.commonPool(), which can lead to performance degradation for blocking tasks)~~>*/f.completeAsync(() -> "done");
                  }
              }
              """
          )
        );
    }

    @Test
    void explicitCommonPool() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.concurrent.CompletableFuture;
              import java.util.concurrent.ForkJoinPool;
              import java.util.concurrent.TimeUnit;

              class Shared {
                  void run(CompletableFuture<String> f) {
                      CompletableFuture.supplyAsync(() -> "x", ForkJoinPool.commonPool());
                      f.thenApplyAsync(String::length, ForkJoinPool.commonPool());
                      CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS, ForkJoinPool.commonPool());
                  }
              }
              """,
            """
              import java.util.concurrent.CompletableFuture;
              import java.util.concurrent.ForkJoinPool;
              import java.util.concurrent.TimeUnit;

              class Shared {
                  void run(CompletableFuture<String> f) {
                      /*~~(supplyAsync with ForkJoinPool.commonPool() has no thread-per-task fallback from JDK 25, which can lead to performance degradation for blocking tasks)~~>*/CompletableFuture.supplyAsync(() -> "x", ForkJoinPool.commonPool());
                      /*~~(thenApplyAsync with ForkJoinPool.commonPool() has no thread-per-task fallback from JDK 25, which can lead to performance degradation for blocking tasks)~~>*/f.thenApplyAsync(String::length, ForkJoinPool.commonPool());
                      /*~~(delayedExecutor with ForkJoinPool.commonPool() has no thread-per-task fallback from JDK 25, which can lead to performance degradation for blocking tasks)~~>*/CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS, ForkJoinPool.commonPool());
                  }
              }
              """
          )
        );
    }

    @Test
    void explicitExecutorNotFlagged() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.concurrent.CompletableFuture;
              import java.util.concurrent.ExecutorService;
              import java.util.concurrent.TimeUnit;

              class Explicit {
                  void run(ExecutorService ioPool, CompletableFuture<String> f, CompletableFuture<Integer> g) {
                      CompletableFuture.supplyAsync(() -> "x", ioPool);
                      CompletableFuture.runAsync(() -> {}, ioPool);
                      f.thenApplyAsync(String::length, ioPool);
                      f.thenCombineAsync(g, (a, b) -> a + b, ioPool);
                      f.completeAsync(() -> "done", ioPool);
                      CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS, ioPool);
                  }
              }
              """
          )
        );
    }

    @Test
    void nonAsyncStagesNotFlagged() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.concurrent.CompletableFuture;

              class Sync {
                  CompletableFuture<Integer> run(CompletableFuture<String> f) {
                      return f.thenApply(String::length).whenComplete((r, e) -> {});
                  }
              }
              """
          )
        );
    }
}
