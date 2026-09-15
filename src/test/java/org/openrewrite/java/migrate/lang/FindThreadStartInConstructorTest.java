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
package org.openrewrite.java.migrate.lang;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class FindThreadStartInConstructorTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FindThreadStartInConstructor());
    }

    @DocumentExample
    @Test
    void findsStartInConstructor() {
        rewriteRun(
          //language=java
          java(
            """
              class Worker {
                  private final Thread thread;

                  Worker(Runnable r) {
                      thread = new Thread(r);
                      thread.start();
                  }
              }
              """,
            """
              class Worker {
                  private final Thread thread;

                  Worker(Runnable r) {
                      thread = new Thread(r);
                      /*~~(`Thread.start()` called during construction of a non-final class. The new thread can observe a partially-initialised object, and any subclass' fields are guaranteed unset. Move the call to a separate method or declare the class `final`.)~~>*/thread.start();
                  }
              }
              """
          )
        );
    }

    @Test
    void findsStartInInstanceInitializerBlock() {
        rewriteRun(
          //language=java
          java(
            """
              class Worker {
                  private final Thread t = new Thread();
                  {
                      t.start();
                  }
              }
              """,
            """
              class Worker {
                  private final Thread t = new Thread();
                  {
                      /*~~(`Thread.start()` called during construction of a non-final class. The new thread can observe a partially-initialised object, and any subclass' fields are guaranteed unset. Move the call to a separate method or declare the class `final`.)~~>*/t.start();
                  }
              }
              """
          )
        );
    }

    @Test
    void findsStartOnThreadSubclassInConstructor() {
        rewriteRun(
          //language=java
          java(
            """
              class MyThread extends Thread {}

              class Worker {
                  private final MyThread t;

                  Worker() {
                      t = new MyThread();
                      t.start();
                  }
              }
              """,
            """
              class MyThread extends Thread {}

              class Worker {
                  private final MyThread t;

                  Worker() {
                      t = new MyThread();
                      /*~~(`Thread.start()` called during construction of a non-final class. The new thread can observe a partially-initialised object, and any subclass' fields are guaranteed unset. Move the call to a separate method or declare the class `final`.)~~>*/t.start();
                  }
              }
              """
          )
        );
    }

    @Test
    void allowsStartInConstructorOfFinalClass() {
        rewriteRun(
          //language=java
          java(
            """
              final class Worker {
                  private final Thread thread;

                  Worker(Runnable r) {
                      thread = new Thread(r);
                      thread.start();
                  }
              }
              """
          )
        );
    }

    @Test
    void allowsStartInRegularMethod() {
        rewriteRun(
          //language=java
          java(
            """
              class Worker {
                  void run(Runnable r) {
                      Thread t = new Thread(r);
                      t.start();
                  }
              }
              """
          )
        );
    }

    @Test
    void allowsStartInStaticInitializer() {
        rewriteRun(
          //language=java
          java(
            """
              class Worker {
                  private static final Thread background;
                  static {
                      background = new Thread();
                      background.start();
                  }
              }
              """
          )
        );
    }

    @Test
    void allowsStartInRecord() {
        // Records are implicitly final; the subclass concern doesn't apply.
        rewriteRun(
          //language=java
          java(
            """
              record Worker(Thread t) {
                  Worker {
                      t.start();
                  }
              }
              """
          )
        );
    }

    @Test
    void allowsStartInAnonymousClassMethod() {
        // Anonymous classes can't be extended, so the S2693 subclass concern doesn't apply.
        rewriteRun(
          //language=java
          java(
            """
              class Worker {
                  Runnable make(Thread t) {
                      return new Runnable() {
                          @Override
                          public void run() {
                              t.start();
                          }
                      };
                  }
              }
              """
          )
        );
    }

    @Test
    void allowsStartInLambdaBodyDeferredFromConstructor() {
        // The lambda body executes later, outside the constructor scope.
        rewriteRun(
          //language=java
          java(
            """
              class Worker {
                  private final Runnable deferred;

                  Worker(Thread t) {
                      deferred = () -> t.start();
                  }
              }
              """
          )
        );
    }
}
