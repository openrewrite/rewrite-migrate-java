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
          //language=java
          java(
            """
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
              class Test {
                  static String score(String obj) {
                      String formatted = "Score not translated yet";
                      switch (obj) {
                          case null -> formatted = "You did not enter the test yet";
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
    void doNotMergeWhenNullBlockAssignsSwitchedVariable() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  static String reassignIdentifier(String obj) {
                      String formatted = "Score not translated yet";
                      if (obj == null) {
                          obj = "null";
                      }
                      switch (obj) {
                          case "A", "B" -> formatted = "Very good";
                          default -> formatted = "unknown";
                      }
                      return formatted;
                  }

                  static String reassignFieldAccess(Score score) {
                      String formatted = "Score not translated yet";
                      if (score.obj == null) {
                          score.obj = "null";
                      }
                      switch (score.obj) {
                          case "A", "B" -> formatted = "Very good";
                          default -> formatted = "unknown";
                      }
                      return formatted;
                  }

                  static String accessesTarget(Score score) {
                      String formatted = "Score not translated yet";
                      if (score.obj == null) {
                          score.setObj();
                      }
                      switch (score.obj) {
                          case "A", "B" -> formatted = "Very good";
                          default -> formatted = "unknown";
                      }
                      return formatted;
                  }

                  static String accessSameTargetAsFieldAccess(Score score) {
                      String formatted = "Score not translated yet";
                      if (score.getObj() == null) {
                          score.obj = "null";
                      }
                      switch (score.getObj()) {
                          case "A", "B" -> formatted = "Very good";
                          default -> formatted = "unknown";
                      }
                      return formatted;
                  }

                  static String accessSameTargetAsMethodInvocation(Score score) {
                      String formatted = "Score not translated yet";
                      if (score.getObj() == null) {
                          score.setObj();
                      }
                      switch (score.getObj()) {
                          case "A", "B" -> formatted = "Very good";
                          default -> formatted = "unknown";
                      }
                      return formatted;
                  }

                  private static class Score {
                      String obj;

                      private Score(String obj) {
                          this.obj =  obj;
                      }

                      private void setObj() {
                          obj = "null";
                      }

                      public String getObj() {
                          return obj;
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotMergeWhenNullBlockReturnsSomething() {
        rewriteRun(
          //language=java
          java(
            """
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


    @Test
    void doNotAffectSecondSwitchFollowingFirst() {
        rewriteRun(
          //language=java
          java(
            """
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
              class Test {
                  static String score(String obj) {
                      String formatted = "Score not translated yet";
                      switch (obj) {
                          case null -> formatted = "You did not enter the test yet";
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
    void doMergeWhenNullBlockAssignsOtherVariables() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  static String reassignIdentifier(String obj) {
                      String formatted = "Score not translated yet";
                      if (obj == null) {
                          formatted = "null";
                      }
                      switch (obj) {
                          case "A", "B" -> formatted = "Very good";
                          default -> formatted = "unknown";
                      }
                      return formatted;
                  }

                  static String reassignFieldAccess(Score score) {
                      String formatted = "Score not translated yet";
                      if (score.obj == null) {
                          formatted = "null";
                      }
                      switch (score.obj) {
                          case "A", "B" -> formatted = "Very good";
                          default -> formatted = "unknown";
                      }
                      return formatted;
                  }

                  static String accessesTarget(Score score) {
                      String formatted = "Score not translated yet";
                      if (score.obj == null) {
                          Other other = new Other(formatted);
                          other.setObj();
                      }
                      switch (score.obj) {
                          case "A", "B" -> formatted = "Very good";
                          default -> formatted = "unknown";
                      }
                      return formatted;
                  }

                  static String accessSameTargetAsFieldAccess(Score score) {
                      String formatted = "Score not translated yet";
                      if (score.getObj() == null) {
                          Other other = new Other(formatted);
                          other.setObj();
                      }
                      switch (score.getObj()) {
                          case "A", "B" -> formatted = "Very good";
                          default -> formatted = "unknown";
                      }
                      return formatted;
                  }

                  static String accessSameTargetAsMethodInvocation(Score score) {
                      String formatted = "Score not translated yet";
                      if (score.getObj() == null) {
                          Other other = new Other(formatted);
                          other.setObj();
                      }
                      switch (score.getObj()) {
                          case "A", "B" -> formatted = "Very good";
                          default -> formatted = "unknown";
                      }
                      return formatted;
                  }

                  private static class Score {
                      String obj;

                      private Score(String obj) {
                          this.obj =  obj;
                      }

                      private void setObj() {
                          obj = "null";
                      }

                      public String getObj() {
                          return obj;
                      }
                  }

                  private static class Other {
                      String obj;

                      private Other(String obj) {
                          this.obj =  obj;
                      }

                      private void setObj() {
                          obj = "null";
                      }

                      public String getObj() {
                          return obj;
                      }
                  }
              }
              """,
            """
            class Test {
                static String reassignIdentifier(String obj) {
                    String formatted = "Score not translated yet";
                    switch (obj) {
                        case null -> formatted = "null";
                        case "A", "B" -> formatted = "Very good";
                        default -> formatted = "unknown";
                    }
                    return formatted;
                }

                static String reassignFieldAccess(Score score) {
                    String formatted = "Score not translated yet";
                    switch (score.obj) {
                        case null -> formatted = "null";
                        case "A", "B" -> formatted = "Very good";
                        default -> formatted = "unknown";
                    }
                    return formatted;
                }

                static String accessesTarget(Score score) {
                    String formatted = "Score not translated yet";
                    switch (score.obj) {
                        case null -> {
                            Other other = new Other(formatted);
                            other.setObj();
                        }
                        case "A", "B" -> formatted = "Very good";
                        default -> formatted = "unknown";
                    }
                    return formatted;
                }

                static String accessSameTargetAsFieldAccess(Score score) {
                    String formatted = "Score not translated yet";
                    switch (score.getObj()) {
                        case null -> {
                            Other other = new Other(formatted);
                            other.setObj();
                        }
                        case "A", "B" -> formatted = "Very good";
                        default -> formatted = "unknown";
                    }
                    return formatted;
                }

                static String accessSameTargetAsMethodInvocation(Score score) {
                    String formatted = "Score not translated yet";
                    switch (score.getObj()) {
                        case null -> {
                            Other other = new Other(formatted);
                            other.setObj();
                        }
                        case "A", "B" -> formatted = "Very good";
                        default -> formatted = "unknown";
                    }
                    return formatted;
                }

                private static class Score {
                    String obj;

                    private Score(String obj) {
                        this.obj =  obj;
                    }

                    private void setObj() {
                        obj = "null";
                    }

                    public String getObj() {
                        return obj;
                    }
                }

                private static class Other {
                    String obj;

                    private Other(String obj) {
                        this.obj =  obj;
                    }

                    private void setObj() {
                        obj = "null";
                    }

                    public String getObj() {
                        return obj;
                    }
                }
            }
            """
          )
        );
    }
}
