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
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.javaVersion;

class ReplaceUnusedVariablesWithUnderscoreTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipe(new ReplaceUnusedVariablesWithUnderscore())
          .parser(JavaParser.fromJavaVersion())
          .allSources(s -> s.markers(javaVersion(22)));
    }

    @DocumentExample
    @Test
    void replaceUnusedForEachVariable() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.List;

              class Test {
                  int countOrders(List<String> orders) {
                      int total = 0;
                      for (String order : orders) {
                          total++;
                      }
                      return total;
                  }
              }
              """,
            """
              import java.util.List;

              class Test {
                  int countOrders(List<String> orders) {
                      int total = 0;
                      for (String _ : orders) {
                          total++;
                      }
                      return total;
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotReplaceUsedForEachVariable() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.List;

              class Test {
                  void processOrders(List<String> orders) {
                      for (String order : orders) {
                          System.out.println(order);
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceUnusedCatchVariable() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  void parseNumber(String s) {
                      try {
                          Integer.parseInt(s);
                      } catch (NumberFormatException ex) {
                          System.out.println("Bad number: " + s);
                      }
                  }
              }
              """,
            """
              class Test {
                  void parseNumber(String s) {
                      try {
                          Integer.parseInt(s);
                      } catch (NumberFormatException _) {
                          System.out.println("Bad number: " + s);
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotReplaceUsedCatchVariable() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  void parseNumber(String s) {
                      try {
                          Integer.parseInt(s);
                      } catch (NumberFormatException ex) {
                          System.out.println("Error: " + ex.getMessage());
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceUnusedLambdaParameter() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.stream.Stream;
              import java.util.stream.Collectors;

              class Test {
                  void example() {
                      Stream.of("a", "b", "c")
                          .collect(Collectors.toMap(String::toUpperCase, item -> "NODATA"));
                  }
              }
              """,
            """
              import java.util.stream.Stream;
              import java.util.stream.Collectors;

              class Test {
                  void example() {
                      Stream.of("a", "b", "c")
                          .collect(Collectors.toMap(String::toUpperCase, _ -> "NODATA"));
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotReplaceUsedLambdaParameter() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.stream.Stream;
              import java.util.stream.Collectors;

              class Test {
                  void example() {
                      Stream.of("a", "b", "c")
                          .collect(Collectors.toMap(String::toUpperCase, item -> item.length()));
                  }
              }
              """
          )
        );
    }

    @Test
    void noChangeOnJava21() {
        rewriteRun(
          spec -> spec.allSources(s -> s.markers(javaVersion(21))),
          //language=java
          java(
            """
              import java.util.List;

              class Test {
                  int countOrders(List<String> orders) {
                      int total = 0;
                      for (String order : orders) {
                          total++;
                      }
                      return total;
                  }
              }
              """
          )
        );
    }

    @Test
    void handleNestedLoops() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.List;

              class Test {
                  void example(List<List<String>> matrix) {
                      for (List<String> row : matrix) {
                          for (String item : row) {
                              System.out.println();
                          }
                      }
                  }
              }
              """,
            """
              import java.util.List;

              class Test {
                  void example(List<List<String>> matrix) {
                      for (List<String> row : matrix) {
                          for (String _ : row) {
                              System.out.println();
                          }
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void handleNestedLoopsBothUnused() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.List;

              class Test {
                  void test(List<List<String>> matrix) {
                      int count = 0;
                      for (List<String> row : matrix) {
                          for (String item : matrix.get(0)) {
                              count++;
                          }
                      }
                  }
              }
              """,
            """
              import java.util.List;

              class Test {
                  void test(List<List<String>> matrix) {
                      int count = 0;
                      for (List<String> _ : matrix) {
                          for (String _ : matrix.get(0)) {
                              count++;
                          }
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceBiConsumerBothParametersUnused() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.function.BiConsumer;

              class Test {
                  void test() {
                      BiConsumer<String, String> consumer = (first, second) -> System.out.println();
                  }
              }
              """,
            """
              import java.util.function.BiConsumer;

              class Test {
                  void test() {
                      BiConsumer<String, String> consumer = (_, _) -> System.out.println();
                  }
              }
              """
          )
        );
    }
}
