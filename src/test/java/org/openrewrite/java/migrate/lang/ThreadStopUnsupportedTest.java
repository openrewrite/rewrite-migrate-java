/*
 * Copyright 2023 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
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
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.javaVersion;

class ThreadStopUnsupportedTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ThreadStopUnsupported());
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/194")
    @DocumentExample
    void addCommentPreJava21() {
        rewriteRun(
          //language=java
          java(
            """
              class Foo {
                  void bar() {
                      Thread.currentThread().stop();
                  }
              }
              """,
            """
              class Foo {
                  void bar() {
                      /*
                       * `Thread.stop()` always throws a `new UnsupportedOperationException()` in Java 21+.
                       * For detailed migration instructions see the migration guide available at
                       * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/doc-files/threadPrimitiveDeprecation.html
                       */
                      Thread.currentThread().stop();
                  }
              }
              """,
            src -> src.markers(javaVersion(17))
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/194")
    @DocumentExample
    void retainCommentIfPresent() {
        rewriteRun(
          //language=java
          java(
            """
              class Foo {
                  void bar() {
                      // I know, I know, but it's a legacy codebase and we're not ready to migrate yet
                      Thread.currentThread().stop();
                  }
              }
              """,
            src -> src.markers(javaVersion(8))
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/194")
    @DocumentExample
    void replaceStopWithThrowsOnJava21() {
        rewriteRun(
          //language=java
          java(
            """
              class Foo {
                  void bar() {
                      Thread.currentThread().stop();
                  }
              }
              """,
            """
              class Foo {
                  void bar() {
                      /*
                       * `Thread.stop()` always throws a `new UnsupportedOperationException()` in Java 21+.
                       * For detailed migration instructions see the migration guide available at
                       * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/doc-files/threadPrimitiveDeprecation.html
                       */
                      throw new UnsupportedOperationException();
                  }
              }
              """,
            src -> src.markers(javaVersion(21))
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/194")
    @DocumentExample
    void replaceResumeWithThrowsOnJava21() {
        rewriteRun(
          //language=java
          java(
            """
              class Foo {
                  void bar() {
                      Thread.currentThread().resume();
                  }
              }
              """,
            """
              class Foo {
                  void bar() {
                      /*
                       * `Thread.resume()` always throws a `new UnsupportedOperationException()` in Java 21+.
                       * For detailed migration instructions see the migration guide available at
                       * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/doc-files/threadPrimitiveDeprecation.html
                       */
                      throw new UnsupportedOperationException();
                  }
              }
              """,
            src -> src.markers(javaVersion(21))
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/194")
    @DocumentExample
    void replaceSuspendWithThrowsOnJava21() {
        rewriteRun(
          //language=java
          java(
            """
              class Foo {
                  void bar() {
                      Thread.currentThread().suspend();
                  }
              }
              """,
            """
              class Foo {
                  void bar() {
                      /*
                       * `Thread.suspend()` always throws a `new UnsupportedOperationException()` in Java 21+.
                       * For detailed migration instructions see the migration guide available at
                       * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/doc-files/threadPrimitiveDeprecation.html
                       */
                      throw new UnsupportedOperationException();
                  }
              }
              """,
            src -> src.markers(javaVersion(21))
          )
        );
    }
}
