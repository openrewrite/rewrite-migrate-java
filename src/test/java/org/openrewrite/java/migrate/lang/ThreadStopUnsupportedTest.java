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
        spec.recipe(new ThreadStopUnsupported())
          .allSources(src -> src.markers(javaVersion(21)));
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/194")
    @DocumentExample
    void replaceWithThrows() {
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
              """
          )
        );
    }
}
