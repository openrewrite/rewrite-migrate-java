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
package org.openrewrite.java.migrate.lang;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;

import static org.openrewrite.java.Assertions.java;

class UseThreadsOrExecutorsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResource("/META-INF/rewrite/java-virtual-threads.yml", "org.openrewrite.java.migrate.lang.FindVirtualThreadOpportunities");
    }

    @DocumentExample
    @Test
    void findThreadConstructorWithRunnable() {
        rewriteRun(
          java(
            """
              class Test {
                  Thread t = new Thread(new Runnable() {
                    @Override
                    public void run() {}
                  });
              }
              """,
            """
              class Test {
                  Thread t = /*~~>*/new Thread(new Runnable() {
                    @Override
                    public void run() {}
                  });
              }
              """
          )
        );
    }

    @Test
    void findThreadConstructorWithClassImplementingRunnable() {
        rewriteRun(
          java(
            """
              class MyRunnable implements Runnable {
                @Override
                public void run() {}
              }
              """,
                SourceSpec::skip
          ),
          java(
            """
              class Test {
                  Thread t = new Thread(new MyRunnable());
              }
              """,
            """
              class Test {
                  Thread t = /*~~>*/new Thread(new MyRunnable());
              }
              """
          )
        );
    }

    @Test
    void findThreadConstructorWithLambda() {
        rewriteRun(
          java(
            """
              class Test {
                  Thread t = new Thread(() -> {});
              }
              """,
            """
              class Test {
                  Thread t = /*~~>*/new Thread(() -> {});
              }
              """
          )
        );
    }

    @Test
    void findThreadConstructorWithThreadGroupAndRunnable() {
        rewriteRun(
          java(
            """
              class Test {
                  Thread t = new Thread(new ThreadGroup("ThreadGroup"), new Runnable() {
                    @Override
                    public void run() {}
                  }, "name");
              }
              """,
            """
              class Test {
                  Thread t = /*~~>*/new Thread(new ThreadGroup("ThreadGroup"), new Runnable() {
                    @Override
                    public void run() {}
                  }, "name");
              }
              """
          )
        );
    }

    @Test
    void findThreadConstructorWithNullThreadGroupAndRunnable() {
        rewriteRun(
          java(
            """
              class Test {
                  Thread t = new Thread(null, new Runnable() {
                    @Override
                    public void run() {}
                  }, "name");
              }
              """,
            """
              class Test {
                  Thread t = /*~~>*/new Thread(null, new Runnable() {
                    @Override
                    public void run() {}
                  }, "name");
              }
              """
          )
        );
    }

    @Test
    void findClassExtendsThreadAndOverrideRun() {
        rewriteRun(
          java(
            """
              public class MyThread extends Thread {
                public void run() {}
              }
              """,
            """
              /*~~>*/public class MyThread extends Thread {
                public void run() {}
              }
              """
          )
        );
    }

    @Test
    void findEmptyThreadConstructor() {
        rewriteRun(
          java(
            """
              class Test {
                  Thread t = new Thread();
              }
              """,
            """
              class Test {
                  Thread t = /*~~>*/new Thread();
              }
              """
          )
        );
    }

    @Test
    void findEmptyThreadConstructorNonSuper() {
        rewriteRun(
          java(
            """
              class Test extends Thread {
                  Thread t = new Test();

                  Test() {
                    super();
                  }
              }
              """,
            """
              /*~~>*/class Test extends Thread {
                  Thread t = new Test();

                  Test() {
                    /*~~>*/super();
                  }
              }
              """
          )
        );
    }

    @Test
    void findClassExtendsThread() {
        rewriteRun(
          java(
            """
              public class MyThread extends Thread {
              }
              """,
            """
              /*~~>*/public class MyThread extends Thread {
              }
              """
          )
        );
    }

    @Test
    void findExecutors() {
        rewriteRun(
          java(
            """
              import java.util.concurrent.ExecutorService;
              import java.util.concurrent.Executors;

              class Test {
                  ExecutorService executorService1 = Executors.newFixedThreadPool(10);
                  ExecutorService executorService2 = Executors.newCachedThreadPool();
                  ExecutorService executorService3 = Executors.newSingleThreadExecutor();
                  ExecutorService executorService4 = Executors.newWorkStealingPool();
                  ExecutorService executorService5 = Executors.newScheduledThreadPool(1);
                  ExecutorService executorService6 = Executors.newSingleThreadScheduledExecutor();
              }
              """,
            """
              import java.util.concurrent.ExecutorService;
              import java.util.concurrent.Executors;

              class Test {
                  ExecutorService executorService1 = /*~~>*/Executors.newFixedThreadPool(10);
                  ExecutorService executorService2 = /*~~>*/Executors.newCachedThreadPool();
                  ExecutorService executorService3 = /*~~>*/Executors.newSingleThreadExecutor();
                  ExecutorService executorService4 = /*~~>*/Executors.newWorkStealingPool();
                  ExecutorService executorService5 = /*~~>*/Executors.newScheduledThreadPool(1);
                  ExecutorService executorService6 = /*~~>*/Executors.newSingleThreadScheduledExecutor();
              }
              """
          )
        );
    }
}
