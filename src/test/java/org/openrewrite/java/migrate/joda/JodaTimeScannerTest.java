/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.java.migrate.joda;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

class JodaTimeScannerTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().classpath("joda-time"));
    }

    @Test
    void noUnsafeVar() {
        JodaTimeScanner scanner = new JodaTimeScanner(new JodaTimeRecipe.Accumulator());
        // language=java
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> scanner)),
          java(
            """
               import org.joda.time.DateTime;
               import org.joda.time.DateTimeZone;
               import java.util.Date;

               class A {
                   public void foo(String city) {
                       DateTimeZone dtz;
                       if ("london".equals(city)) {
                           dtz = DateTimeZone.forID("Europe/London");
                       } else {
                           dtz = DateTimeZone.forID("America/New_York");
                       }
                       DateTime dt = new DateTime(dtz);
                       print(dt.toDate());
                   }
                   private void print(Date date) {
                       System.out.println(date);
                   }
              }
              """
          )
        );
        assertTrue(scanner.getAcc().getUnsafeVars().isEmpty());
    }

    @Test
    void hasUnsafeVars() {
        JodaTimeScanner scanner = new JodaTimeScanner(new JodaTimeRecipe.Accumulator());
        // language=java
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> scanner)),
          java(
            """
               import org.joda.time.DateTime;
               import org.joda.time.DateTimeZone;

               class A {
                   DateTime dateTime; // class Variable not handled yet
                   public void foo(String city) {
                       DateTimeZone dtz;
                       if ("london".equals(city)) {
                           dtz = DateTimeZone.forID("Europe/London");
                       } else {
                           dtz = DateTimeZone.forID("America/New_York");
                       }
                       dateTime = new DateTime(dtz);
                       print(dateTime.toDateTime());
                   }
                   private void print(DateTime dt) {
                       System.out.println(dt);
                   }
              }
              """
          )
        );
        // The variable 'dtz' is unsafe due to the class variable 'dateTime'.
        // The parameter 'dt' in the 'print' method is also unsafe because one of its method calls is unsafe.
        assertEquals(3, scanner.getAcc().getUnsafeVars().size());
        for (J.VariableDeclarations.NamedVariable var : scanner.getAcc().getUnsafeVars()) {
            assertTrue(var.getSimpleName().equals("dtz") ||
                       var.getSimpleName().equals("dt") ||
                       var.getSimpleName().equals("dateTime")
            );
        }
    }

    @Test
    void localVarReferencingClassVar() { // not supported yet
        JodaTimeScanner scanner = new JodaTimeScanner(new JodaTimeRecipe.Accumulator());
        // language=java
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> scanner)),
          java(
            """
               import org.joda.time.DateTime;
               import org.joda.time.DateTimeZone;

               class A {
                   DateTime dateTime;
                   public void foo(String city) {
                       DateTimeZone dtz;
                       if ("london".equals(city)) {
                           dtz = DateTimeZone.forID("Europe/London");
                       } else {
                           dtz = DateTimeZone.forID("America/New_York");
                       }
                       DateTime dt = dateTime.minus(2);
                       System.out.println(dt);
                   }
              }
              """
          )
        );
        // The local variable dt is unsafe due to class var datetime.
        assertEquals(2, scanner.getAcc().getUnsafeVars().size());
        for (J.VariableDeclarations.NamedVariable var : scanner.getAcc().getUnsafeVars()) {
            assertTrue(var.getSimpleName().equals("dateTime") || var.getSimpleName().equals("dt"));
        }
    }

    @Test
    void localVarUsedReferencedInReturnStatement() {
        JodaTimeScanner scanner = new JodaTimeScanner(new JodaTimeRecipe.Accumulator());
        // language=java
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> scanner)),
          java(
            """
               import org.joda.time.DateTime;
               import org.joda.time.DateTimeZone;

               class A {
                   public DateTime foo(String city) {
                       DateTimeZone dtz;
                       if ("london".equals(city)) {
                           dtz = DateTimeZone.forID("Europe/London");
                       } else {
                           dtz = DateTimeZone.forID("America/New_York");
                       }
                       DateTime dt = new DateTime(dtz);
                       return dt.plus(2);
                   }
              }
              """
          )
        );
        assertThat(scanner.getAcc().getUnsafeVars()).isEmpty();
    }

    @Test
    void unsafeMethodParam() {
        JodaTimeScanner scanner = new JodaTimeScanner(new JodaTimeRecipe.Accumulator());
        // language=java
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> scanner)),
          java(
            """
               import org.joda.time.DateTime;

              class A {
                  public void foo() {
                      new Bar().bar(new DateTime());
                  }

                  private static class Bar {
                      public void bar(DateTime dt) {
                          dt.toDateMidnight();
                      }
                  }
              }
              """
          )
        );
        // The bar method parameter dt is unsafe because migration of toDateMidnight() is not yet implemented.
        assertEquals(1, scanner.getAcc().getUnsafeVars().size());
        for (J.VariableDeclarations.NamedVariable var : scanner.getAcc().getUnsafeVars()) {
            assertEquals("dt", var.getSimpleName());
        }
    }

    @Test
    void detectUnsafeVarsInInitializer() {
        JodaTimeScanner scanner = new JodaTimeScanner(new JodaTimeRecipe.Accumulator());
        // language=java
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> scanner)),
          java(
            """
              import org.joda.time.Period;
              import java.util.stream.Collectors;
              import java.util.stream.Stream;
              import java.util.List;
              
              class A {
                  public Period period() {
                      return new Period();
                  }
              
                  public void foo() {
                      List<Integer> list = Stream.of(1, 2, 3).peek(i -> {
                            Period p1 = period();
                            Period p2 = new Period(i, 100);
                            if (p1 != null && p1.plus(p2).getDays() > 10) {
                                System.out.println("Hello world!");
                            }
                      }).toList();
                  }
              }
              """
          )
        );
        assertThat(scanner.getAcc().getUnsafeVars().stream().map(J.VariableDeclarations.NamedVariable::getSimpleName))
          .hasSize(2)
          .containsExactlyInAnyOrder("p1", "p2");
    }

    @Test
    void detectUnsafeVarsInChainedLambdaExpressions() {
        JodaTimeScanner scanner = new JodaTimeScanner(new JodaTimeRecipe.Accumulator());
        // language=java
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> scanner)),
          java(
            """
              import org.joda.time.Period;
              import java.util.stream.Collectors;
              import java.util.stream.Stream;
              import java.util.List;
              
              class A {
                  public Period period() {
                      return new Period();
                  }
              
                  public void foo() {
                      List<Integer> list = Stream.of(1, 2, 3).peek(i -> {
                            Period p1 = period();
                            Period p2 = new Period(i, 100);
                            if (p1 != null && p1.plus(p2).getDays() > 10) {
                                System.out.println("Hello world!");
                            }
                      }).peek(i -> {
                            Period p3 = period();
                            Period p4 = new Period(i, 100);
                            if (p3 != null && p3.plus(p4).getDays() > 10) {
                                System.out.println("Hello world!");
                            }
                      }).toList();
                  }
              }
              """
          )
        );
        assertThat(scanner.getAcc().getUnsafeVars().stream().map(J.VariableDeclarations.NamedVariable::getSimpleName))
          .hasSize(4)
          .containsExactlyInAnyOrder("p1", "p2", "p3", "p4");
    }

    @Test
    void hasSafeMethods() {
        JodaTimeScanner scanner = new JodaTimeScanner(new JodaTimeRecipe.Accumulator());
        // language=java
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> scanner)),
          java(
            """
               import org.joda.time.DateTime;

               class A {
                   private DateTime dateTime() {
                       DateTime dt = new DateTime();
                       return dt;
                   }
                   public void print() {
                       System.out.println(dateTime());
                   }
              }
              """
          )
        );
        assertThat(scanner.getAcc().getSafeMethodMap()).hasSize(1);
        assertThat(scanner.getAcc().getSafeMethodMap().entrySet().stream().filter(Map.Entry::getValue).map(e -> e.getKey().toString()))
                .containsExactlyInAnyOrder("A{name=dateTime,return=org.joda.time.DateTime,parameters=[]}");
        assertThat(scanner.getAcc().getUnsafeVars()).isEmpty();
    }

    @Test
    void methodInvocationBeforeDeclaration() {
        JodaTimeScanner scanner = new JodaTimeScanner(new JodaTimeRecipe.Accumulator());
        // language=java
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> scanner)),
          java(
            """
               import org.joda.time.DateTime;

               class A {
                   public void print() {
                       System.out.println(dateTime());
                   }
                   private DateTime dateTime() {
                       DateTime dt = new DateTime();
                       return dt;
                   }
              }
              """
          )
        );
        assertThat(scanner.getAcc().getSafeMethodMap()).hasSize(1);
        assertThat(scanner.getAcc().getSafeMethodMap().entrySet().stream().filter(Map.Entry::getValue).map(e -> e.getKey().toString()))
          .containsExactlyInAnyOrder("A{name=dateTime,return=org.joda.time.DateTime,parameters=[]}");
        assertThat(scanner.getAcc().getUnsafeVars()).isEmpty();
    }

    @Test
    void safeMethodWithUnsafeParam() {
        JodaTimeScanner scanner = new JodaTimeScanner(new JodaTimeRecipe.Accumulator());
        // language=java
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> scanner)),
          java(
            """
              import org.joda.time.DateTime;
              import org.joda.time.Period;

              class A {
                  private DateTime dateTime(Period period) {
                      DateTime dt = new DateTime();
                      System.out.println(period);
                      return dt;
                  }
                  public void print() {
                      System.out.println(dateTime(new Period()));
                  }
              }
              """
          )
        );
        assertThat(scanner.getAcc().getSafeMethodMap()).hasSize(1);
        assertThat(scanner.getAcc().getSafeMethodMap().entrySet().stream().filter(Map.Entry::getValue).map(e -> e.getKey().toString()))
          .containsExactlyInAnyOrder("A{name=dateTime,return=org.joda.time.DateTime,parameters=[org.joda.time.Period]}");
        assertThat(scanner.getAcc().getUnsafeVars().stream().map(J.VariableDeclarations.NamedVariable::getSimpleName))
                .containsExactlyInAnyOrder("period");
    }

    @Test
    void unsafeMethodDueToUnhandledUsage() {
        JodaTimeScanner scanner = new JodaTimeScanner(new JodaTimeRecipe.Accumulator());
        // language=java
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> scanner)),
          java(
            """
              import org.joda.time.DateTime;

              class A {
                  private DateTime dateTime() {
                      DateTime dt = new DateTime();
                      return dt;
                  }
                  public void print() {
                      dateTime().toDateMidnight();
                  }
              }
              """
          )
        );
        assertThat(scanner.getAcc().getSafeMethodMap()).hasSize(1);
        assertThat(scanner.getAcc().getSafeMethodMap().entrySet().stream().filter(Map.Entry::getValue)).isEmpty();
        assertThat(scanner.getAcc().getUnsafeVars().stream().map(J.VariableDeclarations.NamedVariable::getSimpleName))
                .containsExactlyInAnyOrder("dt");
    }

    @Test
    void unsafeMethodDueToIndirectUnhandledUsage() {
        JodaTimeScanner scanner = new JodaTimeScanner(new JodaTimeRecipe.Accumulator());
        // language=java
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> scanner)),
          java(
            """
              import org.joda.time.DateTime;

              class A {
                  private DateTime dateTime() {
                      DateTime dt = new DateTime();
                      return dt;
                  }
                  public void print() {
                      DateTime dt = dateTime();
                      dt.toDateMidnight();
                  }
              }
              """
          )
        );
        assertThat(scanner.getAcc().getSafeMethodMap()).hasSize(1);
        assertThat(scanner.getAcc().getSafeMethodMap().entrySet().stream().filter(Map.Entry::getValue)).isEmpty();
        assertThat(scanner.getAcc().getUnsafeVars().stream().map(J.VariableDeclarations.NamedVariable::getSimpleName))
          .containsExactlyInAnyOrder("dt", "dt");
    }
}
