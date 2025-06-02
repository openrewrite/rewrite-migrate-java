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

import static org.openrewrite.java.Assertions.java;

class RefineSwitchCasesTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RefineSwitchCases());
    }

    @Test
    @DocumentExample
    void refineCases() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  static void score(Object obj) {
                      switch (obj) {
                          case null -> System.out.println("You did not enter the test yet");
                          case Integer i -> {
                              if (i >= 5 && i <= 10)
                                  System.out.println("You got it");
                              else if (i >= 0 && i < 5)
                                  System.out.println("Shame");
                              else
                                  System.out.println("Sorry?");
                          }
                          case String s -> {
                              if (s.equalsIgnoreCase("YES"))
                                  System.out.println("You got it");
                              else if ("NO".equalsIgnoreCase(s))
                                  System.out.println("Shame");
                              else
                                  System.out.println("Sorry?");
                          }
                      }
                  }
              }
              """,
            """
              class Test {
                  static void score(Object obj) {
                      switch (obj) {
                          case null -> System.out.println("You did not enter the test yet");
                          case Integer i when i >= 5 && i <= 10 -> System.out.println("You got it");
                          case Integer i when i >= 0 && i < 5 -> System.out.println("Shame");
                          case Integer i -> System.out.println("Sorry?");
                          case String s when s.equalsIgnoreCase("YES") -> System.out.println("You got it");
                          case String s when "NO".equalsIgnoreCase(s) -> System.out.println("Shame");
                          case String s -> System.out.println("Sorry?");
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void preferExpressionUsage() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  static void score(Object obj) {
                      switch (obj) {
                          case null -> System.out.println("You did not enter the test yet");
                          case Integer i -> {
                              if (i >= 5 && i <= 10)
                                  System.out.println("You got it");
                              else if (i >= 0 && i < 5) {
                                  System.out.println("Shame");
                              } else
                                  System.out.println("Sorry?");
                          }
                          case String s -> {
                              System.out.println("You got it");
                          }
                      }
                  }
              }
              """,
            """
              class Test {
                  static void score(Object obj) {
                      switch (obj) {
                          case null -> System.out.println("You did not enter the test yet");
                          case Integer i when i >= 5 && i <= 10 -> System.out.println("You got it");
                          case Integer i when i >= 0 && i < 5 -> System.out.println("Shame");
                          case Integer i -> System.out.println("Sorry?");
                          case String s -> {
                              System.out.println("You got it");
                          }
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void noopBlocks() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  static void score(Object obj) {
                      switch (obj) {
                          case Integer i -> {
                              if (i >= 5 && i <= 10)
                                  System.out.println("You got it");
                              else if (i >= 0 && i < 5) {
                                  System.out.println("Shame");
                              }
                          }
                          default -> System.out.println("Sorry?");
                      }
                  }
              }
              """,
            """
            class Test {
                static void score(Object obj) {
                    switch (obj) {
                        case Integer i when i >= 5 && i <= 10 -> System.out.println("You got it");
                        case Integer i when i >= 0 && i < 5 -> System.out.println("Shame");
                        case Integer i -> {
                        }
                        default -> System.out.println("Sorry?");
                    }
                }
            }
            """
          )
        );
    }
}
