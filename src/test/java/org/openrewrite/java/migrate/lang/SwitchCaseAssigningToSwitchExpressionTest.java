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
import org.junitpioneer.jupiter.ExpectedToFail;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.version;

@SuppressWarnings({"EnhancedSwitchMigration", "RedundantLabeledSwitchRuleCodeBlock", "StringOperationCanBeSimplified", "SwitchStatementWithTooFewBranches", "UnnecessaryReturnStatement", "UnusedAssignment"})
class SwitchCaseAssigningToSwitchExpressionTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new SwitchCaseAssigningToSwitchExpression()).allSources(source -> version(source, 21)
        );
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
                          case Integer i: yield String.format("int %d", i);
                          case Long l: yield String.format("long %d", l);
                          default: yield "unknown";
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
    void convertColonCasesSimpleAssignationInBlockToSingleYield() {
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
                          case Integer i: yield String.format("int %d", i);
                          case Long l: yield String.format("long %d", l);
                          default: yield "unknown";
                      };
                  }
              }
              """
          )
        );
    }

    @Test
    void convertArrowCasesSimpleAssignationInBlockToSingleValue() {
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
    void notConvertCasesWhenColonCaseHasNoStatementsAndNextCaseIsntAssignation() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  void doFormat(Object obj) {
                      String status = "initialValue";
                      switch (obj) {
                          case null:
                          default: System.out.println("default"); break;
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void convertCasesWhenColonCaseHasNoStatementsAndNextCaseIsAssignation() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  enum TrafficLight {
                      RED, GREEN, YELLOW
                  }
                  void doFormat(TrafficLight light) {
                      String status = "initialValue";
                      switch (light) {
                          case RED:
                          case GREEN:
                          case YELLOW: status = "unsure"; break;
                          default: status = "unknown"; break;
                      }
                  }
              }
              """,
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

    @Test
    void convertCasesWithAddedDefault() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  enum TrafficLight {
                      RED, GREEN, YELLOW
                  }
                  void doFormat(TrafficLight light) {
                      String status = "initialValue";
                      switch (light) {
                          case RED: status = "stop"; break;
                          case GREEN: status = "go"; break;
                      }
                  }
              }
              """,
            """
              class Test {
                  enum TrafficLight {
                      RED, GREEN, YELLOW
                  }
                  void doFormat(TrafficLight light) {
                      String status = switch (light) {
                          case RED: yield "stop";
                          case GREEN: yield "go";
                          default: yield "initialValue";
                      };
                  }
              }
              """
          )
        );
    }

    @Test
    void notConvertColonCasesWithMultipleBlocks() {
        // More than one block statement per case is not yet supported.
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  void doFormat(Object obj) {
                      String status = "initialValue";
                      switch (obj) {
                          case null: {
                              status = "none";
                          }
                          {
                              break;
                          }
                          default: status = "default status"; break;
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void noDefaultAddedIfAlreadyExhaustive() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  enum TrafficLight {
                      RED, GREEN, YELLOW
                  }
                  void doFormat(TrafficLight light) {
                      String status = "initialValue";
                      switch (light) {
                          case RED: status = "stop"; break;
                          case GREEN: status = "go"; break;
                          case YELLOW: status = "unsure"; break;
                      }
                  }
              }
              """,
            """
              class Test {
                  enum TrafficLight {
                      RED, GREEN, YELLOW
                  }
                  void doFormat(TrafficLight light) {
                      String status = switch (light) {
                          case RED: yield "stop";
                          case GREEN: yield "go";
                          case YELLOW: yield "unsure";
                      };
                  }
              }
              """
          )
        );
    }

    @Test
    void notConvertWhenOriginalVariableIsUsedInCaseAssignment() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  void doFormat(int i) {
                      String orig = "initialValue";
                      switch (i) {
                          default: orig = orig.toLowerCase(); break;
                      }
                  }

                  void doFormat2(int i) {
                      String orig = "initialValue";
                      switch (i) {
                          default: orig = String.format("%s %s", orig, "foo"); break;
                      }
                  }

                  void doFormat3(int i) {
                      String orig = "initialValue";
                      switch (i) {
                          default: orig = "foo" + orig; break;
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void notConvertWhenOriginalVariableAssignationHasSideEffects() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  void methodInvocation(int i) {
                      String orig = "initialValue".toLowerCase();
                      switch (i) {
                          default: orig = "hello"; break;
                      }
                  }

                  void newClass(int i) {
                      String orig = new String("initialValue");
                      switch (i) {
                          default: orig = "hello"; break;
                      }
                  }

                  void newClassInBinaryExpression(int i) {
                      String orig = "initialValue" + new String("more");
                      switch (i) {
                          default: orig = "hello"; break;
                      }
                  }

                  void implicitToStringInvocation(int i, Test o) {
                      String orig = "initialValue" + o;
                      switch (i) {
                          default: orig = "hello"; break;
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void convertWhenOriginalVariableAssignationIsComplexExpressionButNoSideEffects() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  String field = "strawberry";

                  void doFormat(int i) {
                      String variable = "var";
                      String orig = "initialValue" + "test" + 45 + true + field + this.field;
                      switch (i) {
                          default: orig = "hello"; break;
                      }
                  }
              }
              """,
            """
              class Test {
                  String field = "strawberry";

                  void doFormat(int i) {
                      String variable = "var";
                      String orig = switch (i) {
                          default: yield "hello";
                      };
                  }
              }
              """
          )
        );
    }

    @Test
    void notConvertColonSwitchWithEmptyLastCase() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  enum TrafficLight {
                      RED, GREEN, YELLOW
                  }
                  void doFormat(TrafficLight light) {
                      String status = "initialValue";
                      switch (light) {
                          case RED: status = "stop"; break;
                          case GREEN: status = "go"; break;
                          case YELLOW:
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void notConvertSwitchOnUninitializedOriginalVariableAndNonExhaustiveSwitch() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  enum TrafficLight {
                      RED, GREEN, YELLOW
                  }
                  void exhaustiveButCantAddDefaultAndMissingAssignment(TrafficLight light) {
                      String status;
                      switch (light) {
                          case RED: status = "stop"; break;
                          case GREEN: status = "go"; break;
                          case YELLOW:
                      }
                  }

                  void exhaustiveButMissingAssignment(TrafficLight light) {
                      String status;
                      switch (light) {
                          case RED: status = "stop"; break;
                          case GREEN: status = "go"; break;
                          case YELLOW:
                          default: System.out.println("foo");
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void inlineWhenVariableOnlyToBeReturned() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  String doFormat() {
                      String formatted;
                      switch (1) {
                          default: formatted = "foo"; break;
                      }
                      return formatted;
                  }
              }
              """,
            """
              class Test {
                  String doFormat() {
                      return switch (1) {
                          default: yield "foo";
                      };
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotinlineWhenInappropriate() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  String originalVariableNotReturned() {
                      String formatted;
                      switch (1) {
                          default: formatted = "foo"; break;
                      }
                      return "string";
                  }

                  String codeBetweenSwitchAndReturn() {
                      String formatted;
                      switch (1) {
                          default: formatted = "foo"; break;
                      }
                      System.out.println("Hey");
                      return formatted;
                  }

                  void noReturnedExpression() {
                      String formatted;
                      switch (1) {
                          default: formatted = "foo"; break;
                      }
                      return;
                  }
              }
              """,
            """
              class Test {
                  String originalVariableNotReturned() {
                      String formatted= switch (1) {
                          default: yield "foo";
                      };
                      return "string";
                  }

                  String codeBetweenSwitchAndReturn() {
                      String formatted= switch (1) {
                          default: yield "foo";
                      };
                      System.out.println("Hey");
                      return formatted;
                  }

                  void noReturnedExpression() {
                      String formatted= switch (1) {
                          default: yield "foo";
                      };
                      return;
                  }
              }
              """
          )
        );
    }

    @Test
    @ExpectedToFail
    void failsToFormatWithASpaceWhenOriginalVariableHasNoInitializer() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  void doFormat() {
                      String formatted;
                      switch (1) {
                          default: formatted = "foo"; break;
                      }
                  }
              }
              """,
            """
              class Test {
                  void doFormat() {
                      String formatted = switch (1) {
                          default: yield "foo";
                      };
                  }
              }
              """
          )
        );
    }

    @Test
    void commentsArePreserved() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  void doFormat(Object obj) {
                      // line before the original variable
                      String formatted = "initialValue"; // original variable after code
                      // line before the switch
                      switch (obj) { // first line of the switch
                          // before the cases
                          case Integer i -> formatted = String.format("int %d", i); // first case
                          // between the 1st and 2nd case
                          /* before the 2nd case */ case Long l -> formatted = String.format("long %d", l);
                          default -> formatted = "unknown";
                          // after the last case
                      } // last line of the switch
                  }
              }
              """,
            """
              class Test {
                  void doFormat(Object obj) {
                      // line before the original variable
                      // original variable after code
                      // line before the switch
                      String formatted = switch (obj) { // first line of the switch
                          // before the cases
                          case Integer i -> String.format("int %d", i); // first case
                          // between the 1st and 2nd case
                          /* before the 2nd case */ case Long l -> String.format("long %d", l);
                          default -> "unknown";
                          // after the last case
                      }; // last line of the switch
                  }
              }
              """
          )
        );
    }

    @Test
    void commentsArePreservedWhenInlining() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  String doFormat() {
                      // line before original variable
                      String formatted; // original variable after code
                      // between the original variable and the switch
                      switch (1) { // on the switch after code
                          default: formatted = "foo"; break;
                      } // last line of the switch
                      // between switch and return
                      return formatted; // after return on the same line
                  }
              }
              """,
            """
              class Test {
                  String doFormat() {
                      // line before original variable
                      // original variable after code
                      // between the original variable and the switch
                      // last line of the switch
                      // between switch and return
                      return switch (1) { // on the switch after code
                          default: yield "foo";
                      }; // after return on the same line
                  }
              }
              """
          )
        );
    }
}
