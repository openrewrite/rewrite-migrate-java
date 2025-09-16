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
package org.openrewrite.java.migrate.io;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.search.FindMissingTypes;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.javaVersion;

class ReplaceSystemOutWithIOPrintTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ReplaceSystemOutWithIOPrint())
          .afterTypeValidationOptions(TypeValidation.all().allowMissingType(o -> {
              assert o instanceof FindMissingTypes.MissingTypeResult;
              FindMissingTypes.MissingTypeResult result = (FindMissingTypes.MissingTypeResult) o;
              return result.getPrintedTree().contains("IO");
          })) // TODO remove once tests run on Java 25+
          .parser(JavaParser.fromJavaVersion())
          .allSources(s -> s.markers(javaVersion(25)));
    }

    @DocumentExample
    @Test
    void replaceSystemOutPrint() {
        rewriteRun(
          java(
            """
              class Example {
                  void test() {
                      System.out.print("Hello");
                  }
              }
              """,
            """
              class Example {
                  void test() {
                      IO.print("Hello");
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceSystemOutPrintln() {
        rewriteRun(
          java(
            """
              class Example {
                  void test() {
                      System.out.println("Hello");
                  }
              }
              """,
            """
              class Example {
                  void test() {
                      IO.println("Hello");
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceSystemOutPrintlnWithStaticImport() {
        rewriteRun(
          java(
            """
              import static java.lang.System.out;

              class Example {
                  void test() {
                      out.println("Hello");
                  }
              }
              """,
            """
              class Example {
                  void test() {
                      IO.println("Hello");
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceSystemOutPrintWithVariable() {
        rewriteRun(
          java(
            """
              class Example {
                  void test() {
                      String message = "Hello World";
                      System.out.print(message);
                  }
              }
              """,
            """
              class Example {
                  void test() {
                      String message = "Hello World";
                      IO.print(message);
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceSystemOutPrintlnEmpty() {
        rewriteRun(
          java(
            """
              class Example {
                  void test() {
                      System.out.println();
                  }
              }
              """,
            """
              class Example {
                  void test() {
                      IO.println();
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceMultipleSystemOutCalls() {
        rewriteRun(
          java(
            """
              class Example {
                  void test() {
                      System.out.print("Hello");
                      System.out.println(" World");
                      System.out.print(42);
                      System.out.println();
                  }
              }
              """,
            """
              class Example {
                  void test() {
                      IO.print("Hello");
                      IO.println(" World");
                      IO.print(42);
                      IO.println();
                  }
              }
              """
          )
        );
    }

    @Test
    void handlesPrintWithComplexExpressions() {
        rewriteRun(
          java(
            """
              class Example {
                  void test() {
                      String name = "John";
                      int age = 30;
                      System.out.print("Name: " + name + ", Age: " + age);
                      System.out.println(String.format("Formatted: %s is %d years old", name, age));
                  }
              }
              """,
            """
              class Example {
                  void test() {
                      String name = "John";
                      int age = 30;
                      IO.print("Name: " + name + ", Age: " + age);
                      IO.println(String.format("Formatted: %s is %d years old", name, age));
                  }
              }
              """
          )
        );
    }

    @Test
    void doesNotReplaceSystemErrCalls() {
        rewriteRun(
          java(
            """
              class Example {
                  void test() {
                      System.err.print("Error message");
                      System.err.println("Error message");
                  }
              }
              """
          )
        );
    }

    @Test
    void doesNotReplaceOtherPrintStreams() {
        rewriteRun(
          java(
            """
              import java.io.PrintStream;

              class Example {
                  void test() {
                      PrintStream ps = new PrintStream(System.out);
                      ps.print("Should not change");
                      ps.println("Should not change");
                  }
              }
              """
          )
        );
    }
}
