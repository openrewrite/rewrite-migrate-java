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

class IfElseIfConstructToSwitchTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new IfElseIfConstructToSwitch());
    }

    @Test
    @DocumentExample
    void defaultSwitchBlockWithNullCheckAndFinalElseStatement() {
        rewriteRun(
          //language=java
          java(
            """
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
          //language=java
          java(
            """
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
          //language=java
          java(
            """
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
                          default -> {}
                      }
                      return formatted;
                  }
              }
              """
          )
        );
    }

    @Test
    void worksWithExpressionsOtherThanIdentifier() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  static String fieldAccess(Tester test) {
                      String formatted = "initialValue";
                      if (test.obj == null) {
                          formatted = "null";
                      } else if (test.obj instanceof Integer i)
                          formatted = String.format("int %d", i);
                      else if (test.obj instanceof Long l) {
                          formatted = String.format("long %d", l);
                      } else if (test.obj instanceof Double d) {
                          formatted = String.format("double %f", d);
                      } else if (test.obj instanceof String s) {
                          String str = "String";
                          formatted = String.format("%s %s", str, s);
                      }
                      return formatted;
                  }

                  static String methodInvocation(Tester test) {
                      String formatted = "initialValue";
                      if (test.getObj() == null) {
                          formatted = "null";
                      } else if (test.getObj() instanceof Integer i)
                          formatted = String.format("int %d", i);
                      else if (test.getObj() instanceof Long l) {
                          formatted = String.format("long %d", l);
                      } else if (test.getObj() instanceof Double d) {
                          formatted = String.format("double %f", d);
                      } else if (test.getObj() instanceof String s) {
                          String str = "String";
                          formatted = String.format("%s %s", str, s);
                      }
                      return formatted;
                  }

                  private static class Tester {
                      private Object obj;

                      public Object getObj() {
                          return obj;
                      }
                  }
              }
              """,
            """
              class Test {
                  static String fieldAccess(Tester test) {
                      String formatted = "initialValue";
                      switch (test.obj) {
                          case null -> formatted = "null";
                          case Integer i -> formatted = String.format("int %d", i);
                          case Long l -> formatted = String.format("long %d", l);
                          case Double d -> formatted = String.format("double %f", d);
                          case String s -> {
                              String str = "String";
                              formatted = String.format("%s %s", str, s);
                          }
                          default -> {}
                      }
                      return formatted;
                  }

                  static String methodInvocation(Tester test) {
                      String formatted = "initialValue";
                      switch (test.getObj()) {
                          case null -> formatted = "null";
                          case Integer i -> formatted = String.format("int %d", i);
                          case Long l -> formatted = String.format("long %d", l);
                          case Double d -> formatted = String.format("double %f", d);
                          case String s -> {
                              String str = "String";
                              formatted = String.format("%s %s", str, s);
                          }
                          default -> {}
                      }
                      return formatted;
                  }

                  private static class Tester {
                      private Object obj;

                      public Object getObj() {
                          return obj;
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void defaultSwitchBlockFinalElseStatement() {
        rewriteRun(
          //language=java
          java(
            """
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
          //language=java
          java(
            """
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
    void switchBlockForNestedClasses() {
        rewriteRun(
          java(
            """
              public class Tester {
                  public static class A {}

                  public static class B {}
              }
              """
          ),
          //language=java
          java(
            """
              class Test {
                  static String formatter(Object obj) {
                      String formatted;
                      if (obj == null) {
                          formatted = "null";
                      } else if (obj instanceof Tester.A)
                          formatted = "nested1";
                      else if (obj instanceof Tester.B) {
                          formatted = "nested2";
                      } else {
                          formatted = "unknown";
                      }
                      return formatted;
                  }
              }
              """,
            """
              class Test {
                  static String formatter(Object obj) {
                      String formatted;
                      switch (obj) {
                          case null -> formatted = "null";
                          case Tester.A -> formatted = "nested1";
                          case Tester.B -> formatted = "nested2";
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
          //language=java
          java(
            """
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

    @Test
    void noSwitchBlockWithDifferentVariablesBeingChecked() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  static String formatter(Object obj) {
                      String formatted = "initialValue";
                      Object anotherObj = obj;
                      if (obj == null) {
                          formatted = "null";
                      } else if (obj instanceof Integer i)
                          formatted = String.format("int %d", i);
                      else if (obj instanceof Long l) {
                          formatted = String.format("long %d", l);
                      } else if (obj instanceof Double d) {
                          formatted = String.format("double %f", d);
                      } else if (anotherObj instanceof String s) {
                          String str = "String";
                          formatted = String.format("%s %s", str, s);
                      } else {
                          formatted = "unknown";
                      }
                      return formatted;
                  }
              }
              """
          )
        );
    }
}
