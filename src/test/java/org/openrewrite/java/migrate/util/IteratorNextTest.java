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
package org.openrewrite.java.migrate.util;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.javaVersion;

@Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/243")
class IteratorNextTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new IteratorNext())
          .allSources(src -> src.markers(javaVersion(21)));
    }

    @DocumentExample
    @Test
    void listIteratorNextToGetFirst() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.*;

              class Foo {
                  String bar(List<String> collection) {
                      return collection.iterator().next();
                  }
              }
              """,
            """
              import java.util.*;

              class Foo {
                  String bar(List<String> collection) {
                      return collection.getFirst();
                  }
              }
              """
          )
        );
    }

    @Test
    void nonSequencedCollectionUnchanged() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.*;

              class Foo {
                  void bar(Collection<String> collection) {
                      String first = collection.iterator().next();
                  }
              }
              """
          )
        );
    }

    @Test
    void nextCommentLost() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.*;

              class Foo {
                  void bar(List<String> collection) {
                      String first = collection
                        .iterator()
                        // Next comment
                        .next();
                  }
              }
              """,
            """
              import java.util.*;

              class Foo {
                  void bar(List<String> collection) {
                      String first = collection
                        .getFirst();
                  }
              }
              """
          )
        );
    }

    @Test
    void iteratorCommentRetained() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.*;

              class Foo {
                  void bar(List<String> collection) {
                      String first = collection
                        // Iterator comment
                        .iterator()
                        .next();
                  }
              }
              """,
            """
              import java.util.*;

              class Foo {
                  void bar(List<String> collection) {
                      String first = collection
                        // Iterator comment
                        .getFirst();
                  }
              }
              """
          )
        );
    }

    @Test
    void implicitThisIteratorNextUnchanged() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.*;

              class Foo extends ArrayList<String> {
                  void bar() {
                      String first = iterator().next();
                  }
              }
              """
          )
        );
    }
}
