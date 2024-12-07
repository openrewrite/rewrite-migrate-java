/*
 * Copyright 2024 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
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
    void localVarUsedReferencedInReturnStatement() { // not supported yet
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
        // The local variable dt used in return statement.
        assertEquals(2, scanner.getAcc().getUnsafeVars().size());
        for (J.VariableDeclarations.NamedVariable var : scanner.getAcc().getUnsafeVars()) {
            assertTrue(var.getSimpleName().equals("dtz") || var.getSimpleName().equals("dt"));
        }
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
              import org.joda.time.Interval;
              import java.util.stream.Collectors;
              import java.util.stream.Stream;
              import java.util.List;
              
              class A {
                  public Interval interval() {
                      return new Interval(10, 20);
                  }
              
                  public void foo() {
                      List<Integer> list = Stream.of(1, 2, 3).peek(i -> {
                            Interval i1 = interval();
                            Interval i2 = new Interval(i, 100);
                            if (i1 != null && !i1.contains(i2)) {
                                System.out.println("i1 does not contain i2");
                            }
                      }).toList();
                  }
              }
              """
          )
        );
        assertThat(scanner.getAcc().getUnsafeVars().stream().map(J.VariableDeclarations.NamedVariable::getSimpleName))
          .hasSize(2)
          .containsExactlyInAnyOrder("i1", "i2");
    }

    @Test
    // TODO remove when https://github.com/openrewrite/rewrite-analysis/issues/72 is fixed
    void dataFlowBug() {
        JodaTimeScanner scanner = new JodaTimeScanner(new JodaTimeRecipe.Accumulator());
        // language=java
        rewriteRun(
          spec -> spec.recipe(toRecipe(() -> scanner)),
          java(
            """
              import org.joda.time.Interval;
              import java.util.stream.Collectors;
              import java.util.stream.Stream;
              import java.util.List;
              
              class A {
                  public Interval interval() {
                      return new Interval(10, 20);
                  }
              
                  public void foo() {
                      List<Integer> list = Stream.of(1, 2, 3).peek(i -> {
                            Interval i1 = interval();
                            Interval i2 = new Interval(i, 100);
                            if (i1 != null && !i1.contains(i2)) {
                                System.out.println("i1 does not contain i2");
                            }
                      }).peek(i -> {
                            Interval i3 = interval();
                            Interval i4 = new Interval(i, 100);
                            if (i3 != null && !i3.contains(i4)) {
                                System.out.println("i3 does not contain i4");
                            }
                      }).toList();
                  }
              }
              """
          )
        );
        assertThat(scanner.getAcc().getUnsafeVars().stream().map(J.VariableDeclarations.NamedVariable::getSimpleName))
          .hasSize(2)
          // i3 and i4 are also unsafe but there is a bug in the data flow analysis. due to which it
          // skips the second peek.
          .containsExactlyInAnyOrder("i1", "i2");
    }
}
