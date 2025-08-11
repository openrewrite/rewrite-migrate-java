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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.version;

class NullCheckAsSwitchCaseTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipe(new NullCheckAsSwitchCase())
          .allSources(source -> version(source, 21));
    }

    @DocumentExample
    @Test
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
    void mergeNullCheckWithSwitchStatement() {
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
                          case "A", "B":
                              formatted = "Very good";
                              break;
                          case "C":
                              formatted = "Good";
                              break;
                          case "D":
                              formatted = "Hmmm...";
                              break;
                          default:
                              formatted = "unknown";
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
                          case null:
                              formatted = "You did not enter the test yet";
                              break;
                          case "A", "B":
                              formatted = "Very good";
                              break;
                          case "C":
                              formatted = "Good";
                              break;
                          case "D":
                              formatted = "Hmmm...";
                              break;
                          default:
                              formatted = "unknown";
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
                      switch (obj) {
                          case "E" -> formatted = "Very good";
                          case "F" -> formatted = "Good";
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
                      switch (obj) {
                          case "E" -> formatted = "Very good";
                          case "F" -> formatted = "Good";
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

    @Test
    void mergeWhenBoxedIsUsedAsIdentifier() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  static String score(Character obj) {
                      String formatted = "Score not translated yet";
                      if (obj == null) {
                          setObj(obj);
                          formatted = "You did not enter the test yet";
                      }
                      switch (obj) {
                          case 'A', 'B' -> formatted = "Very good";
                          case 'C' -> formatted = "Good";
                          case 'D' -> formatted = "Hmmm...";
                          default -> formatted = "unknown";
                      }
                      return formatted;
                  }

                  void setObj(Character obj) {
                      obj = 'A';
                  }
              }
              """,
            """
              class Test {
                  static String score(Character obj) {
                      String formatted = "Score not translated yet";
                      switch (obj) {
                          case null -> {
                              setObj(obj);
                              formatted = "You did not enter the test yet";
                          }
                          case 'A', 'B' -> formatted = "Very good";
                          case 'C' -> formatted = "Good";
                          case 'D' -> formatted = "Hmmm...";
                          default -> formatted = "unknown";
                      }
                      return formatted;
                  }

                  void setObj(Character obj) {
                      obj = 'A';
                  }
              }
              """
          )
        );
    }

    @Test
    void mergeWhenStringIsUsedAsIdentifier() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  static String score(String obj) {
                      String formatted = "Score not translated yet";
                      if (obj == null) {
                          setObj(obj);
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

                  void setObj(String obj) {
                      obj = "String cannot be reassigned in the original location so safe to merge";
                  }
              }
              """,
            """
              class Test {
                  static String score(String obj) {
                      String formatted = "Score not translated yet";
                      switch (obj) {
                          case null -> {
                              setObj(obj);
                              formatted = "You did not enter the test yet";
                          }
                          case "A", "B" -> formatted = "Very good";
                          case "C" -> formatted = "Good";
                          case "D" -> formatted = "Hmmm...";
                          default -> formatted = "unknown";
                      }
                      return formatted;
                  }

                  void setObj(String obj) {
                      obj = "String cannot be reassigned in the original location so safe to merge";
                  }
              }
              """
          )
        );
    }

    @Test
    void singleExpressionsUnwrapped() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  static String formatter(Object obj) {
                      String formatted = "initialValue";
                      if (obj == null) {
                          formatted = "null";
                      }
                      switch (obj) {
                          case Integer i -> formatted = String.format("int %d", i);
                          case Long l -> formatted = String.format("long %d", l);
                          default -> formatted = "unknown";
                      }
                      return formatted;
                  }
              }
              """,
            """
              class Test {
                  static String formatter(Object obj) {
                      String formatted = "initialValue";
                      switch (obj) {
                          case null -> formatted = "null";
                          case Integer i -> formatted = String.format("int %d", i);
                          case Long l -> formatted = String.format("long %d", l);
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
    void singleStatementsNotUnwrapped() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  static String formatter(Object obj) {
                      String formatted = "initialValue";
                      if (obj == null) {
                          if (formatted.equals("initialValue")) {
                              formatted = "null";
                          }
                      }
                      switch (obj) {
                          case Long l -> formatted = String.format("long %d", l);
                          default -> formatted = "unknown";
                      }
                      return formatted;
                  }
              }
              """,
            """
              class Test {
                  static String formatter(Object obj) {
                      String formatted = "initialValue";
                      switch (obj) {
                          case null -> {
                              if (formatted.equals("initialValue")) {
                                  formatted = "null";
                              }
                          }
                          case Long l -> formatted = String.format("long %d", l);
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
    void mergeWhenExistingSwitchIsCoveringAllPossibleEnumValues() {
        rewriteRun(
          java(
            """
              package suits;
              public enum Suit {
                  CLUBS, DIAMONDS, HEARTS, SPADES, JOKER
              }
              """
          ),
          //language=java
          java(
            """
              import suits.Suit;
              import static suits.Suit.HEARTS;
              import static suits.Suit.DIAMONDS;
              import static suits.Suit.JOKER;
              class Test {
                  void score(Suit obj) {
                      if (obj == null) {
                          System.out.println("no suit chosen yet.");
                      }
                      switch (obj) {
                          case Suit.CLUBS -> System.out.println(Suit.CLUBS);
                          case Suit.SPADES -> System.out.println(Suit.SPADES);
                          case HEARTS -> System.out.println(HEARTS);
                          case DIAMONDS -> System.out.println(DIAMONDS);
                          case JOKER -> System.out.println(JOKER);
                      }
                  }
              }
              """,
            """
              import suits.Suit;
              import static suits.Suit.HEARTS;
              import static suits.Suit.DIAMONDS;
              import static suits.Suit.JOKER;
              class Test {
                  void score(Suit obj) {
                      switch (obj) {
                          case null -> System.out.println("no suit chosen yet.");
                          case Suit.CLUBS -> System.out.println(Suit.CLUBS);
                          case Suit.SPADES -> System.out.println(Suit.SPADES);
                          case HEARTS -> System.out.println(HEARTS);
                          case DIAMONDS -> System.out.println(DIAMONDS);
                          case JOKER -> System.out.println(JOKER);
                      }
                  }
              }
              """
          )
        );
    }

    @Nested
    class Throws {
        @Test
        void throwsSingleLine() {
            rewriteRun(
              //language=java
              java(
                """
                  class Test {
                      static String score(String obj) {
                          String formatted = "Score not translated yet";
                          if (obj == null)
                              throw new IllegalArgumentException("You did not enter the test yet");
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
                              case null -> throw new IllegalArgumentException("You did not enter the test yet");
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
        void throwsFromBlock() {
            rewriteRun(
              //language=java
              java(
                """
                  class Test {
                      static String score(String obj) {
                          String formatted = "Score not translated yet";
                          if (obj == null) {
                              throw new IllegalArgumentException("You did not enter the test yet");
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
                              case null -> throw new IllegalArgumentException("You did not enter the test yet");
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

    @Nested
    class NoChange {

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
        void doNotMergeForExistingNullCase() {
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
                              case null -> formatted = "Already handled";
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

                      static String useNonPrimitiveAsIdentifier(Score score) {
                          String formatted = "Score not translated yet";
                          if (score.getObj() == null) {
                              setObj(score);
                          }
                          switch (score.getObj()) {
                              case "A", "B" -> formatted = "Very good";
                              default -> formatted = "unknown";
                          }
                          return formatted;
                      }

                      void setObj(Score score) {
                          score.setObj();
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
        void doNotMergeWhenExistingSwitchIsNotCoveringAllPossibleInputValuesWithDefault() {
            rewriteRun(
              //language=java
              java(
                """
                  class Test {
                      static String score(String obj) {
                          String formatted = "Score not translated yet";
                          if (obj == null) {
                              throw new IllegalArgumentException();
                          }
                          switch (obj) {
                              case "A", "B" -> formatted = "Very good";
                              case "C" -> formatted = "Good";
                              case "D" -> formatted = "Hmmm...";
                          }
                          return formatted;
                      }
                  }
                  """
              )
            );
        }

        @Test
        void doNotMergeWhenExistingSwitchIsNotCoveringAllPossibleEnumValues() {
            rewriteRun(
              java(
                """
                  package suits;
                  public enum Suit {
                      CLUBS, DIAMONDS, HEARTS, SPADES, JOKER
                  }
                  """
              ),
              //language=java
              java(
                """
                  import suits.Suit;
                  import static suits.Suit.HEARTS;
                  class Test {
                      void score(Suit obj) {
                          if (obj == null) {
                              System.out.println("no suit chosen yet.");
                          }
                          switch (obj) {
                              case Suit.SPADES -> System.out.println(Suit.SPADES);
                              case HEARTS -> System.out.println(HEARTS);
                          }
                      }
                  }
                  """
              )
            );
        }
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/785")
    @Test
    void noBreakAfterThrow() {
        rewriteRun(
          java(
            """
              class Foo {
                  String bar(String foo) {
                      if (foo == null) {
                          throw new RuntimeException("");
                      }
                      switch (foo) {
                          case "hello":
                              return "world";
                          default:
                              return "other";
                      }
                  }
              }
              """,
            """
              class Foo {
                  String bar(String foo) {
                      switch (foo) {
                          case null:
                              throw new RuntimeException("");
                          case "hello":
                              return "world";
                          default:
                              return "other";
                      }
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/785")
    @Test
    void noChangeOnReturn() {
        rewriteRun(
          java(
            """
              class Foo {
                  String bar(String foo) {
                      if (foo == null) {
                          return "";
                      }
                      switch (foo) {
                          case "hello":
                              return "world";
                          default:
                              return "other";
                      }
                  }
              }
              """
          )
        );
    }
}
