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

class PatternMatchingWithInstanceOfTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new PatternMatchingWithInstanceOf());
    }

    @Test
    @DocumentExample
    void addsPatternMatchingToInstanceOf() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  static class Cat {}
                  static class Dog {}
                  void test(Object o) {
                      if (o instanceof String) {
                          System.out.println((String) o);
                      }
                      if (o instanceof Integer) {
                          Integer number = (Integer) o;
                          System.out.println(number);
                      }
                      if (o instanceof Cat) {
                          System.out.println("My pet is a" + (Cat) o);
                      }
                      if (o instanceof Dog) {
                          Dog dog = new Dog();
                          System.out.println("My pet is a" + (Dog) o);
                      }
                  }
              }
              """,
            """
            class Test {
                static class Cat {}
                static class Dog {}
                void test(Object o) {
                    if (o instanceof String s) {
                        System.out.println(s);
                    }
                    if (o instanceof Integer number) {
                        System.out.println(number);
                    }
                    if (o instanceof Cat cat) {
                        System.out.println("My pet is a" + cat);
                    }
                    if (o instanceof Dog oAsDog) {
                        Dog dog = new Dog();
                        System.out.println("My pet is a" + oAsDog);
                    }
                }
            }
            """
          )
        );
    }

    @Test
    void prefersInternallyDeclaredVariableIfPresent() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  void test(Object o) {
                      if (o instanceof Integer) {
                          Integer number = (Integer) o;
                          System.out.println(number);
                      }
                  }
              }
              """,
            """
            class Test {
                void test(Object o) {
                    if (o instanceof Integer number) {
                        System.out.println(number);
                    }
                }
            }
            """
          )
        );
    }

    @Test
    void fallbackToPrimitiveFirstLetter() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  void test(Object o) {
                      if (o instanceof String) {
                          System.out.println((String) o);
                      }
                  }
              }
              """,
            """
            class Test {
                void test(Object o) {
                    if (o instanceof String s) {
                        System.out.println(s);
                    }
                }
            }
            """
          )
        );
    }

    @Test
    void fallbackToLongNotationIfPrimitiveFirstLetterIsAlreadyUsed() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  void test(Object o) {
                      Float f;
                      if (o instanceof Float) {
                          f = (Float) o;
                          System.out.println(f);
                      }
                  }
              }
              """,
            """
            class Test {
                void test(Object o) {
                    Float f;
                    if (o instanceof Float oAsFloat) {
                        f = oAsFloat;
                        System.out.println(f);
                    }
                }
            }
            """
          )
        );
    }

    @Test
    void doNotAddPatternIfNotUsed() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  void test(Object o) {
                      if (o instanceof Boolean) {
                          System.out.println("we ignore the unused values");
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void preferClassTypeNameCamelCased() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  static class Cat {}
                  void test(Object o) {
                      if (o instanceof Cat) {
                          System.out.println("My pet is a" + (Cat) o);
                      }
                  }
              }
              """,
            """
            class Test {
                static class Cat {}
                void test(Object o) {
                    if (o instanceof Cat cat) {
                        System.out.println("My pet is a" + cat);
                    }
                }
            }
            """
          )
        );
    }

    @Test
    void fallbackToLongNotationIfClassTypeNameInUse() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  static class Dog {}
                  void test(Object o) {
                      if (o instanceof Dog) {
                          Dog dog = new Dog();
                          System.out.println("My pet is a" + (Dog) o);
                      }
                  }
              }
              """,
            """
            class Test {
                static class Dog {}
                void test(Object o) {
                    if (o instanceof Dog oAsDog) {
                        Dog dog = new Dog();
                        System.out.println("My pet is a" + oAsDog);
                    }
                }
            }
            """
          )
        );
    }

    @Test
    void doNotTouchIfCandidatePatternNameCannotBeFound() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  static class Dog {}
                  void test(Object o) {
                      if (o instanceof Dog) {
                          Dog dog = new Dog();
                          Dog oAsDog = new Dog();
                          System.out.println("My pet is a" + (Dog) o);
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void trackAllVariableNamesInScope() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  void test(Object o) {
                      Float f = 0f; // This variable is declared but not used in the block
                      if (o instanceof Float) {
                          System.out.println((Float) o);
                      }
                  }
              }
              """,
            """
            class Test {
                void test(Object o) {
                    Float f = 0f; // This variable is declared but not used in the block
                    if (o instanceof Float oAsFloat) {
                        System.out.println(oAsFloat);
                    }
                }
            }
            """
          )
        );
    }
}
