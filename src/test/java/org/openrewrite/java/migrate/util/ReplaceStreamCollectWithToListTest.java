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

class ReplaceStreamCollectWithToListTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipe(new ReplaceStreamCollectWithToList(false))
          .allSources(s -> s.markers(javaVersion(17)));
    }

    @DocumentExample
    @Test
    void replacesToUnmodifiableList() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.stream.Collectors;
              import java.util.stream.Stream;
              import java.util.List;

              class Example {
                  List<String> test(Stream<String> stream) {
                      return stream.collect(Collectors.toUnmodifiableList());
                  }
              }
              """,
            """
              import java.util.stream.Stream;
              import java.util.List;

              class Example {
                  List<String> test(Stream<String> stream) {
                      return stream.toList();
                  }
              }
              """
          )
        );
    }

    @Test
    void doesNotReplaceToListByDefault() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.stream.Collectors;
              import java.util.stream.Stream;
              import java.util.List;

              class Example {
                  List<String> test(Stream<String> stream) {
                      return stream.collect(Collectors.toList());
                  }
              }
              """
          )
        );
    }

    @Test
    void doesReplaceToListWhenFlagSetToTrue() {
        rewriteRun(
          recipeSpec -> recipeSpec.recipe(new ReplaceStreamCollectWithToList(true)),
          //language=java
          java(
            """
              import java.util.stream.Collectors;
              import java.util.stream.Stream;
              import java.util.List;

              class Example {
                  List<String> test(Stream<String> stream) {
                      return stream.collect(Collectors.toList());
                  }
              }
              """,
            """
              import java.util.stream.Stream;
              import java.util.List;

              class Example {
                  List<String> test(Stream<String> stream) {
                      return stream.toList();
                  }
              }
              """
          )
        );
    }

    @Test
    void retainWhitespace() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.stream.Collectors;
              import java.util.stream.Stream;
              import java.util.List;

              class Example {
                  List<String> test(Stream<String> stream) {
                      return stream
                          .collect(Collectors.toUnmodifiableList());
                  }
              }
              """,
            """
              import java.util.stream.Stream;
              import java.util.List;

              class Example {
                  List<String> test(Stream<String> stream) {
                      return stream
                          .toList();
                  }
              }
              """
          )
        );
    }

    @Test
    void retainComment() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.stream.Collectors;
              import java.util.stream.Stream;
              import java.util.List;

              class Example {
                  List<String> test(Stream<String> stream) {
                      return stream
                          // Convert to list
                          .collect(Collectors.toUnmodifiableList());
                  }
              }
              """,
            """
              import java.util.stream.Stream;
              import java.util.List;

              class Example {
                  List<String> test(Stream<String> stream) {
                      return stream
                          // Convert to list
                          .toList();
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/791")
    @Test
    void doesNotReplaceWhenReturnTypeIsIncompatible() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.stream.Collectors;
              import java.util.stream.Stream;
              import java.util.List;

              class Example {
                  List<Number> foo() {
                      return Stream.of(Integer.valueOf(1)).collect(Collectors.toUnmodifiableList());
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/791")
    @Test
    void replacesWhenTypesAreCompatible() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.stream.Collectors;
              import java.util.stream.Stream;
              import java.util.List;

              class Example {
                  List<Integer> foo() {
                      return Stream.of(Integer.valueOf(1)).collect(Collectors.toUnmodifiableList());
                  }
              }
              """,
            """
              import java.util.stream.Stream;
              import java.util.List;

              class Example {
                  List<Integer> foo() {
                      return Stream.of(Integer.valueOf(1)).toList();
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/791")
    @Test
    void doesNotReplaceInVariableAssignmentWithIncompatibleTypes() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.stream.Collectors;
              import java.util.stream.Stream;
              import java.util.List;

              class Example {
                  void foo() {
                      List<Number> numbers = Stream.of(Integer.valueOf(1)).collect(Collectors.toUnmodifiableList());
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/791")
    @Test
    void doesNotReplaceWithToListWhenConvertToListFlagIsTrue() {
        rewriteRun(
          recipeSpec -> recipeSpec.recipe(new ReplaceStreamCollectWithToList(true)),
          //language=java
          java(
            """
              import java.util.stream.Collectors;
              import java.util.stream.Stream;
              import java.util.List;

              class Example {
                  List<Number> foo() {
                      return Stream.of(Integer.valueOf(1)).collect(Collectors.toList());
                  }
              }
              """
          )
        );
    }

}
