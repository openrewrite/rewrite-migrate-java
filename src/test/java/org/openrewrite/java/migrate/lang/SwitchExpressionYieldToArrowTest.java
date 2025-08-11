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
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.javaVersion;

class SwitchExpressionYieldToArrowTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new SwitchExpressionYieldToArrow())
          .allSources(s -> s.markers(javaVersion(17)));
    }

    @DocumentExample
    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/799")
    @Test
    void convertSwitchExpressionWithYield() {
        rewriteRun(
            //language=java
            java(
                """
                class Test {
                    String format(String str) {
                        String formatted = switch (str) {
                            case "foo": yield "Foo";
                            case "bar": yield "Bar";
                            case null, default: yield "unknown";
                        };
                        return formatted;
                    }
                }
                """,
                """
                class Test {
                    String format(String str) {
                        String formatted = switch (str) {
                            case "foo" -> "Foo";
                            case "bar" -> "Bar";
                            case null, default -> "unknown";
                        };
                        return formatted;
                    }
                }
                """
            )
        );
    }

    @Test
    void convertSwitchExpressionWithSimpleYields() {
        rewriteRun(
            //language=java
            java(
                """
                class Test {
                    int getValue(String str) {
                        return switch (str) {
                            case "one": yield 1;
                            case "two": yield 2;
                            default: yield 0;
                        };
                    }
                }
                """,
                """
                class Test {
                    int getValue(String str) {
                        return switch (str) {
                            case "one" -> 1;
                            case "two" -> 2;
                            default -> 0;
                        };
                    }
                }
                """
            )
        );
    }

    @Test
    void convertEnumSwitchExpression() {
        rewriteRun(
          //language=java
          java(
            """
            class Test {
                enum Color { RED, GREEN, BLUE }

                String colorName(Color color) {
                    return switch (color) {
                        case RED: yield "Red";
                        case GREEN: yield "Green";
                        case BLUE: yield "Blue";
                    };
                }
            }
            """,
            """
            class Test {
                enum Color { RED, GREEN, BLUE }

                String colorName(Color color) {
                    return switch (color) {
                        case RED -> "Red";
                        case GREEN -> "Green";
                        case BLUE -> "Blue";
                    };
                }
            }
            """
          )
        );
    }

    @Test
    void doNotConvertArrowCases() {
        rewriteRun(
            //language=java
            java(
                """
                class Test {
                    String format(String str) {
                        return switch (str) {
                            case "foo" -> "Foo";
                            case "bar" -> "Bar";
                            default -> "Other";
                        };
                    }
                }
                """
            )
        );
    }

    @Test
    void doNotConvertComplexYieldCases() {
        rewriteRun(
            //language=java
            java(
                """
                class Test {
                    String process(String str) {
                        return switch (str) {
                            case "foo":
                                System.out.println("Processing foo");
                                yield "Foo";
                            case "bar": yield "Bar";
                            default: yield "Other";
                        };
                    }
                }
                """
            )
        );
    }

    @Test
    void doNotConvertEmptyCases() {
        rewriteRun(
          java(
            """
              class Test {
                  enum TrafficLight {
                      RED, GREEN, YELLOW
                  }
                  void doFormat(TrafficLight light) {
                      String status = switch (light) {
                          case RED:
                          case GREEN:
                          case YELLOW: yield "unsure";
                          default: yield "unknown";
                      };
                  }
              }
              """
          )
        );
    }
}
