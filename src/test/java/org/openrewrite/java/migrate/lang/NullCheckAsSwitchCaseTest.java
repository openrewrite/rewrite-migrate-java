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

class NullCheckAsSwitchCaseTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new NullCheckAsSwitchCase());
    }

    @Test
    @DocumentExample
    void mergeNullCheckWithSwitch() {
        rewriteRun(
          java(
            """
              package org.openrewrite.java.migrate.lang;

              class Test {

                  static String score(String obj) {
                      String formatted = "Score not translated yet";
                      if (obj == null) {
                          formatted = "You did not enter the test yet";
                      }
                      switch (obj) {
                          case "A", "B" -> formatted = "Very good";
                          case "C" -> formatted = "Good";
                          case "D" -> formatted = "Hmmm...";
                          default -> formatted = "unknown";
                      }
                      return formatted;
                  }
              }
              """,
            """
            package org.openrewrite.java.migrate.lang;

            class Test {

                static String formatter(Object obj) {
                    String formatted = "initialValue";
                    switch (obj) {
                        case null -> formatted = "null";
                        case Integer i -> formatted = String.format("int %d", i);
                        case Long l -> formatted = String.format("long %d", l);
                        case Double d -> formatted = String.format("double %f", d);
                        case String s -> {
                            String str = "String";
                            formatted = String.format("%s %s", str, s);
                        }
                        default -> formatted = "unknown";
                    }
                    return formatted;
                }
            }
            """
          )
        );
    }

    @Test
    void doNotMergeWhenNullBlockAssignsSwitchedVariable() {
        rewriteRun(
          java(
            """
              package org.openrewrite.java.migrate.lang;

              class Test {

                  static String score(String obj) {
                      String formatted = "Score not translated yet";
                      if (obj == null) {
                          obj = "null";
                      }
                      switch (obj) {
                          case "A", "B" -> formatted = "Very good";
                          case "C" -> formatted = "Good";
                          case "D" -> formatted = "Hmmm...";
                          default -> formatted = "unknown";
                      }
                      return formatted;
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotMergeWhenNullBlockReturnsSomething() {
        rewriteRun(
          java(
            """
              package org.openrewrite.java.migrate.lang;

              class Test {

                  static String score(String obj) {
                      String formatted = "Score not translated yet";
                      if (obj == null) {
                          return "You did not enter the test yet";
                      }
                      switch (obj) {
                          case "A", "B" -> formatted = "Very good";
                          case "C" -> formatted = "Good";
                          case "D" -> formatted = "Hmmm...";
                          default -> formatted = "unknown";
                      }
                      return formatted;
                  }
              }
              """
          )
        );
    }
}
