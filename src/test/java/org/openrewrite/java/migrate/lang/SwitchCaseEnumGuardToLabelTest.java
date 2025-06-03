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

class SwitchCaseEnumGuardToLabelTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new SwitchCaseEnumGuardToLabel());
    }

    @Test
    @DocumentExample
    void enumValuesPatternMatching() {
        rewriteRun(
          java(
            """
              package suits;
              public enum Suit {
                  CLUBS, DIAMONDS, HEARTS, SPADES, JOKER, SCORECARD
              }
              """
          ),
          //language=java
          java(
            """
              import suits.Suit;
              class Test {
                  void score(Object obj) {
                      switch (obj) {
                          case null -> System.out.println("You did not enter the test yet");
                          case Suit s when s == Suit.CLUBS -> System.out.println("Clubs");
                          case Suit s when s.equals(Suit.DIAMONDS) -> System.out.println("Diamonds");
                          case Suit s when Suit.HEARTS.equals(s) -> {
                              System.out.println("Hearts");
                          }
                          case Suit s when Suit.SPADES == s -> System.out.println("Spades");
                          case Suit s when Suit.JOKER == s -> System.out.println(s);
                          case Integer i -> System.out.println("Sorry?");
                          case String s -> System.out.println("Sorry?");
                          default -> System.out.println("Sorry?");
                      }
                  }
              }
              """,
            """
              import suits.Suit;
              class Test {
                  void score(Object obj) {
                      switch (obj) {
                          case null -> System.out.println("You did not enter the test yet");
                          case Suit.CLUBS -> System.out.println("Clubs");
                          case Suit.DIAMONDS -> System.out.println("Diamonds");
                          case Suit.HEARTS -> {
                              System.out.println("Hearts");
                          }
                          case Suit.SPADES -> System.out.println("Spades");
                          case Suit.JOKER -> System.out.println(Suit.JOKER);
                          case Integer i -> System.out.println("Sorry?");
                          case String s -> System.out.println("Sorry?");
                          default -> System.out.println("Sorry?");
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void staticImportedEnumValue() {
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
              import static suits.Suit.JOKER;
              class Test {
                  void score(Object obj) {
                      switch (obj) {
                          case Suit s when JOKER == s -> System.out.println(s);
                      }
                  }
              }
              """,
            """
              import suits.Suit;
              import static suits.Suit.JOKER;
              class Test {
                  void score(Object obj) {
                      switch (obj) {
                          case JOKER -> System.out.println(JOKER);
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void noChangeWithIntendedUse() {
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
                  void score(Object obj) {
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
