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

class SwitchCaseReturnsToSwitchExpressionTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new SwitchCaseReturnsToSwitchExpression())
          .allSources(s -> s.markers(javaVersion(17)));
    }

    @DocumentExample
    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/800")
    @Test
    void convertSimpleSwitchWithReturns() {
        rewriteRun(
            //language=java
            java(
                """
                class Test {
                    String doFormat(String str) {
                        switch (str) {
                            case "foo": return "Foo";
                            case "bar": return "Bar";
                            case null, default: return "Other";
                        }
                    }
                }
                """,
                """
                class Test {
                    String doFormat(String str) {
                        return switch (str) {
                            case "foo" -> "Foo";
                            case "bar" -> "Bar";
                            case null, default -> "Other";
                        };
                    }
                }
                """
            )
        );
    }

    @Test
    void convertSimpleSwitchWithReturnsAfterOtherStatements() {
        rewriteRun(
          //language=java
          java(
            """
            class Test {
                String doFormat(String str) {
                    System.out.println("Formatting: " + str);
                    switch (str) {
                        case "foo": return "Foo";
                        case "bar": return "Bar";
                        case null, default: return "Other";
                    }
                }
            }
            """,
            """
            class Test {
                String doFormat(String str) {
                    System.out.println("Formatting: " + str);
                    return switch (str) {
                        case "foo" -> "Foo";
                        case "bar" -> "Bar";
                        case null, default -> "Other";
                    };
                }
            }
            """
          )
        );
    }

    @Test
    void convertSwitchWithColonCases() {
        rewriteRun(
            //language=java
            java(
                """
                class Test {
                    int getValue(String str) {
                        switch (str) {
                            case "one":
                                return 1;
                            case "two":
                                return 2;
                            default:
                                return 0;
                        }
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
    void convertSwitchWithBlocksContainingReturns() {
        rewriteRun(
            //language=java
            java(
                """
                class Test {
                    String process(int value) {
                        switch (value) {
                            case 1: {
                                return "One";
                            }
                            case 2: {
                                return "Two";
                            }
                            default: {
                                return "Many";
                            }
                        }
                    }
                }
                """,
                """
                class Test {
                    String process(int value) {
                        return switch (value) {
                            case 1 -> "One";
                            case 2 -> "Two";
                            default -> "Many";
                        };
                    }
                }
                """
            )
        );
    }

    @Test
    void convertSwitchWithArrowCases() {
        rewriteRun(
            //language=java
            java(
                """
                class Test {
                    String format(String str) {
                        switch (str) {
                            case "foo" -> { return "Foo"; }
                            case "bar" -> { return "Bar"; }
                            default -> { return "Other"; }
                        }
                    }
                }
                """,
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
    void convertEnumSwitchThatIsExhaustive() {
        rewriteRun(
            //language=java
            java(
                """
                class Test {
                    enum Color { RED, GREEN, BLUE }

                    String colorName(Color color) {
                        switch (color) {
                            case RED: return "Red";
                            case GREEN: return "Green";
                            case BLUE: return "Blue";
                        }
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
    void doNotConvertWhenNotAllCasesReturn() {
        rewriteRun(
          //language=java
          java(
            """
            class Test {
                String process(String str) {
                    switch (str) {
                        case "foo":
                            return "Foo";
                        case "bar":
                            System.out.println("Bar case");
                            break;
                        default:
                            return "Other";
                    }
                    return "End";
                }
            }
            """
          )
        );
    }

    @Test
    void doNotConvertWhenNoDefaultAndNotExhaustive() {
        rewriteRun(
          //language=java
          java(
            """
            class Test {
                String format(String str) {
                    switch (str) {
                        case "foo": return "Foo";
                        case "bar": return "Bar";
                    }
                    return "Not found";
                }
            }
            """
          )
        );
    }

    @Test
    void doNotConvertIfNotOnlyStatementInBlock() {
        rewriteRun(
            //language=java
            java(
                """
                class Test {
                    String process(String str) {
                        switch (str) {
                            case "foo":
                                System.out.println("Processing: " + str);
                                return "Foo";
                            case "bar": return "Bar";
                            default: return "Other";
                        }
                    }
                }
                """
            )
        );
    }
}
