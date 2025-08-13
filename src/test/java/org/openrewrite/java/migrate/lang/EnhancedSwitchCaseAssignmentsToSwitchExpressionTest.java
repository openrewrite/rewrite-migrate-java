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
import static org.openrewrite.java.Assertions.version;

@SuppressWarnings({"EnhancedSwitchMigration", "RedundantLabeledSwitchRuleCodeBlock", "UnusedAssignment"})
class EnhancedSwitchCaseAssignmentsToSwitchExpressionTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipe(new SwitchCaseAssignmentsToSwitchExpression())
          .allSources(source -> version(source, 21));
    }

    @DocumentExample
    @Test
    void convertSimpleArrowCasesAssignment() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  void doFormat(Object obj) {
                      String formatted = "initialValue";
                      switch (obj) {
                          case Integer i -> formatted = String.format("int %d", i);
                          case Long l -> formatted = String.format("long %d", l);
                          default -> formatted = "unknown";
                      }
                  }
              }
              """,
            """
              class Test {
                  void doFormat(Object obj) {
                      String formatted = switch (obj) {
                          case Integer i -> String.format("int %d", i);
                          case Long l -> String.format("long %d", l);
                          default -> "unknown";
                      };
                  }
              }
              """
          )
        );
    }

    @Test
    void convertSimpleColonCasesAssignment() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  void doFormat(Object obj) {
                      String formatted = "initialValue";
                      switch (obj) {
                          case Integer i: formatted = String.format("int %d", i); break;
                          case Long l: formatted = String.format("long %d", l); break;
                          default: formatted = "unknown"; break;
                      }
                  }
              }
              """,
            """
              class Test {
                  void doFormat(Object obj) {
                      String formatted = switch (obj) {
                          case Integer i -> String.format("int %d", i);
                          case Long l -> String.format("long %d", l);
                          default -> "unknown";
                      };
                  }
              }
              """
          )
        );
    }

    @Test
    void notConvertSimpleColonCasesAssignmentWithExtraCodeInBlock() {
        // Only one statement [+break;] per case is currently supported
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  void doFormat(Object obj) {
                      String formatted = "initialValue";
                      switch (obj) {
                          case Integer i: formatted = String.format("int %d", i); break;
                          case Long l: System.out.println("long"); formatted = String.format("long %d", l); break;
                          default: formatted = "unknown"; break;
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void convertColonCasesSimpleAssignmentInBlockToSingleYield() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  void doFormat(Object obj) {
                      String formatted = "initialValue";
                      switch (obj) {
                          case Integer i: formatted = String.format("int %d", i); break;
                          case Long l: {
                              formatted = String.format("long %d", l);
                              break;
                          }
                          default: formatted = "unknown"; break;
                      }
                  }
              }
              """,
            """
              class Test {
                  void doFormat(Object obj) {
                      String formatted = switch (obj) {
                          case Integer i -> String.format("int %d", i);
                          case Long l -> String.format("long %d", l);
                          default -> "unknown";
                      };
                  }
              }
              """
          )
        );
    }

    @Test
    void convertColonCasesSimpleAssignmentInBlockToSingleYieldWithoutFinalCaseBreak() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  void doFormat(Object obj) {
                      String formatted = "initialValue";
                      switch (obj) {
                          case Integer i: formatted = String.format("int %d", i); break;
                          case Long l: {
                              formatted = String.format("long %d", l);
                              break;
                          }
                          default: formatted = "unknown";
                      }
                  }
              }
              """,
            """
              class Test {
                  void doFormat(Object obj) {
                      String formatted = switch (obj) {
                          case Integer i -> String.format("int %d", i);
                          case Long l -> String.format("long %d", l);
                          default -> "unknown";
                      };
                  }
              }
              """
          )
        );
    }

    @Test
    void convertArrowCasesSimpleAssignmentInBlockToSingleValue() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  void doFormat(Object obj) {
                      String formatted = "initialValue";
                      switch (obj) {
                          case Integer i -> formatted = String.format("int %d", i);
                          case Long l -> {
                              formatted = String.format("long %d", l);
                          }
                          default -> formatted = "unknown";
                      }
                  }
              }
              """,
            """
              class Test {
                  void doFormat(Object obj) {
                      String formatted = switch (obj) {
                          case Integer i -> String.format("int %d", i);
                          case Long l -> String.format("long %d", l);
                          default -> "unknown";
                      };
                  }
              }
              """
          )
        );
    }

    @Test
    void notConvertCasesWithMissingAssignment() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  void doFormat(Object obj) {
                      String formatted = "initialValue";
                      switch (obj) {
                          case String s: formatted = String.format("String %s", s); break;
                          case Integer i: System.out.println("Integer!"); break;
                          default: formatted = "unknown"; break;
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void notConvertCasesWithAssignmentToDifferentVariables() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  void doFormat(Object obj) {
                      String formatted = "initialValue";
                      String formatted2 = "anotherInitialValue";
                      switch (obj) {
                          case String s: formatted = String.format("String %s", s); break;
                          case Integer i: formatted2 = String.format("Integer %d", i); break;
                          default: formatted = "unknown"; break;
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void defaultAsSecondLabelColonCase() {
        rewriteRun(
          //language=java
          java(
            """
              class A {
                  void doFormat(String str) {
                      String formatted = "initialValue";
                      switch (str) {
                          case "foo": formatted = "Foo"; break;
                          case "bar": formatted = "Bar"; break;
                          case null, default: formatted = "unknown";
                      }
                  }
              }
              """,
            """
              class A {
                  void doFormat(String str) {
                      String formatted = switch (str) {
                          case "foo" -> "Foo";
                          case "bar" -> "Bar";
                          case null, default -> "unknown";
                      };
                  }
              }
              """
          )
        );
    }

    @Test
    void defaultAsSecondLabelArrowCase() {
        rewriteRun(
          //language=java
          java(
            """
              class B {
                  void doFormat(String str) {
                      String formatted = "initialValue";
                      switch (str) {
                          case "foo" -> formatted = "Foo";
                          case "bar" -> formatted = "Bar";
                          case null, default -> formatted = "Other";
                      }
                  }
              }
              """,
            """
              class B {
                  void doFormat(String str) {
                      String formatted = switch (str) {
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
}
