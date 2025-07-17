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

class SwitchCaseAssigningToSwitchExpressionTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new SwitchCaseAssigningToSwitchExpression()).allSources(source -> version(source, 21));
    }

    @DocumentExample
    @Test
    void convertSimpleArrowCasesAssignations() {
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
          ));
    }

    @Test
    void convertSimpleColonCasesAssignations() {
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
          ));
    }

    @Test
    void notConvertSimpleColonCasesAssignationsWithExtraCodeInBlock() {
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
          ));
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
          ));
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
          ));
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
          ));
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
          ));
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
          ));
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
          ));
    }

    @Test
    void convertCasesWhenColonCaseHasNoStatementsAndNextCaseIsAssignationByAddedDefault() {
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
                          default: yield "initialValue";
                      };
                  }
              }
              """
          ));
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
          ));
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
          ));
    }

    @Test
    void convertCasesInstanceVariableAssignment() {
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
          ));
    }
}
