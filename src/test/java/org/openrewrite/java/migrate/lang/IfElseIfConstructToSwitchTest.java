package org.openrewrite.java.migrate.lang;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class IfElseIfConstructToSwitchTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new IfElseIfConstructToSwitch());
    }

    @Test
    @DocumentExample
    void defaultSwitchBlockWithNullCheckAndFinalElseStatement() {
        rewriteRun(
          java(
            """
              package org.openrewrite.java.migrate.lang;

              class Test {

                  static String formatter(Object obj) {
                      String formatted = "initialValue";
                      if (obj == null) {
                          formatted = "null";
                      } else if (obj instanceof Integer i)
                          formatted = String.format("int %d", i);
                      else if (obj instanceof Long l) {
                          formatted = String.format("long %d", l);
                      } else if (obj instanceof Double d) {
                          formatted = String.format("double %f", d);
                      } else if (obj instanceof String s) {
                          String str = "String";
                          formatted = String.format("%s %s", str, s);
                      } else {
                          formatted = "unknown";
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
    void defaultSwitchBlockWithSeparateNullCheckAndFinalElseStatement() {
        rewriteRun(
          java(
            """
              package org.openrewrite.java.migrate.lang;

              class Test {

                  static String formatter(Object obj) {
                      String formatted = "initialValue";
                      if (obj == null) {
                          formatted = "null";
                      }
                      if (obj instanceof Integer i)
                          formatted = String.format("int %d", i);
                      else if (obj instanceof Long l) {
                          formatted = String.format("long %d", l);
                      } else if (obj instanceof Double d) {
                          formatted = String.format("double %f", d);
                      } else if (obj instanceof String s) {
                          String str = "String";
                          formatted = String.format("%s %s", str, s);
                      } else {
                          formatted = "unknown";
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
                    if (obj == null) {
                        formatted = "null";
                    }
                    switch (obj) {
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
    void defaultSwitchBlockWithNullCheck() {
        rewriteRun(
          java(
            """
              package org.openrewrite.java.migrate.lang;

              class Test {

                  static String formatter(Object obj) {
                      String formatted = "initialValue";
                      if (obj == null) {
                          formatted = "null";
                      } else if (obj instanceof Integer i)
                          formatted = String.format("int %d", i);
                      else if (obj instanceof Long l) {
                          formatted = String.format("long %d", l);
                      } else if (obj instanceof Double d) {
                          formatted = String.format("double %f", d);
                      } else if (obj instanceof String s) {
                          String str = "String";
                          formatted = String.format("%s %s", str, s);
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
                    }
                    return formatted;
                }
            }
            """
          )
        );
    }

    @Test
    void defaultSwitchBlockFinalElseStatement() {
        rewriteRun(
          java(
            """
              package org.openrewrite.java.migrate.lang;

              class Test {

                  static String formatter(Object obj) {
                      String formatted = "initialValue";
                      if (obj instanceof Integer i)
                          formatted = String.format("int %d", i);
                      else if (obj instanceof Long l) {
                          formatted = String.format("long %d", l);
                      } else if (obj instanceof Double d) {
                          formatted = String.format("double %f", d);
                      } else if (obj instanceof String s) {
                          String str = "String";
                          formatted = String.format("%s %s", str, s);
                      } else {
                          formatted = "unknown";
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
    void defaultSwitchBlockWithNoPatternsSpecified() {
        rewriteRun(
          java(
            """
              package org.openrewrite.java.migrate.lang;

              class Test {

                  static String formatter(Object obj) {
                      String formatted = "initialValue";
                      if (obj == null) {
                          formatted = "null";
                      } else if (obj instanceof Integer)
                          formatted = String.format("int %d", (Integer) obj);
                      else if (obj instanceof Long) {
                          formatted = String.format("long %d", (Long) obj);
                      } else if (obj instanceof Double) {
                          formatted = String.format("double %f", (Double) obj);
                      } else if (obj instanceof String) {
                          String str = "String";
                          formatted = String.format("%s %s", str, (String) obj);
                      } else {
                          formatted = "unknown";
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
                        case Integer -> formatted = String.format("int %d", (Integer) obj);
                        case Long -> formatted = String.format("long %d", (Long) obj);
                        case Double -> formatted = String.format("double %f", (Double) obj);
                        case String -> {
                            String str = "String";
                            formatted = String.format("%s %s", str, (String) obj);
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
    void noSwitchBlockWhenOnly2Branches() {
        rewriteRun(
          java(
            """
              package org.openrewrite.java.migrate.lang;

              class Test {

                  static String nullCheckAndIf(Object obj) {
                      String formatted = "initialValue";
                      if (obj == null) {
                          formatted = "null";
                      } else if (obj instanceof Integer)
                          formatted = String.format("int %d", (Integer) obj);
                      return formatted;
                  }

                  static String ifElse(Object obj) {
                      String formatted = "initialValue";
                      if (obj instanceof Integer) {
                          formatted = String.format("int %d", (Integer) obj);
                      } else {
                          formatted = "unknown";
                      }
                      return formatted;
                  }

                  static String ifElseIf(Object obj) {
                      String formatted = "initialValue";
                      if (obj instanceof Integer)
                          formatted = String.format("int %d", (Integer) obj);
                      else if (obj instanceof Long) {
                          formatted = String.format("long %d", (Long) obj);
                      }
                      return formatted;
                  }
              }
              """
          )
        );
    }
}
