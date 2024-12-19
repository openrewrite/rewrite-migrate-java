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
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class JodaTimeRecipeTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipe(new JodaTimeRecipe())
          .parser(JavaParser.fromJavaVersion().classpath("joda-time"));
    }

    @DocumentExample
    @Test
    void migrateSafeVariable() {
        //language=java
        rewriteRun(
          java(
            """
              import org.joda.time.DateTime;

              class A {
                  public void foo() {
                      DateTime dt = new DateTime();
                      System.out.println(dt.toDateTime());
                  }
              }
              """,
            """
              import java.time.ZonedDateTime;

              class A {
                  public void foo() {
                      ZonedDateTime dt = ZonedDateTime.now();
                      System.out.println(dt);
                  }
              }
              """
          )
        );
    }

    @Test
    void dontChangeClassVariable() {
        // not supported yet
        //language=java
        rewriteRun(
          java(
            """
              import org.joda.time.DateTime;

              class A {
                  public void foo() {
                      DateTime dt = new DateTime();
                      System.out.println(dt.toDateTime());
                      System.out.println(new B().dateTime.toDateTime());
                  }
                  public static class B {
                      DateTime dateTime = new DateTime();
                  }
              }
              """,
            """
              import org.joda.time.DateTime;

              import java.time.ZonedDateTime;

              class A {
                  public void foo() {
                      ZonedDateTime dt = ZonedDateTime.now();
                      System.out.println(dt);
                      System.out.println(new B().dateTime.toDateTime());
                  }
                  public static class B {
                      DateTime dateTime = new DateTime();
                  }
              }
              """
          )
        );
    }

    @Test
    void safeMethodParamMigrationAcrossClassBoundary() {
        //language=java
        rewriteRun(
          java(
            """
              import org.joda.time.DateTime;

              class A {
                  public void foo() {
                      new B().print(new DateTime());
                      System.out.println(new B().dateTime); // dateTime is class variable, not handled yet
                  }
              }

              class B {
                  DateTime dateTime = new DateTime();
                  public void print(DateTime dateTime) {
                      System.out.println(dateTime);
                  }
              }
              """,
            """
              import org.joda.time.DateTime;

              import java.time.ZonedDateTime;

              class A {
                  public void foo() {
                      new B().print(ZonedDateTime.now());
                      System.out.println(new B().dateTime); // dateTime is class variable, not handled yet
                  }
              }

              class B {
                  DateTime dateTime = new DateTime();
                  public void print(ZonedDateTime dateTime) {
                      System.out.println(dateTime);
                  }
              }
              """
          )
        );
    }

    @Test
    void noUnsafeVar() {
        //language=java
        rewriteRun(
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
              """,
            """
              import java.time.ZoneId;
              import java.time.ZonedDateTime;
              import java.util.Date;

              class A {
                  public void foo(String city) {
                      ZoneId dtz;
                      if ("london".equals(city)) {
                          dtz = ZoneId.of("Europe/London");
                      } else {
                          dtz = ZoneId.of("America/New_York");
                      }
                      ZonedDateTime dt = ZonedDateTime.now(dtz);
                      print(Date.from(dt.toInstant()));
                  }
                  private void print(Date date) {
                      System.out.println(date);
                  }
              }
              """
          )
        );
    }

    @Test
    void localVarUsedReferencedInReturnStatement() {
        // language=java
        rewriteRun(
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
              """,
            """
              import java.time.Duration;
              import java.time.ZoneId;
              import java.time.ZonedDateTime;
              
              class A {
                   public ZonedDateTime foo(String city) {
                       ZoneId dtz;
                       if ("london".equals(city)) {
                           dtz = ZoneId.of("Europe/London");
                       } else {
                           dtz = ZoneId.of("America/New_York");
                       }
                       ZonedDateTime dt = ZonedDateTime.now(dtz);
                       return dt.plus(Duration.ofMillis(2));
                   }
              }
              """
          )
        );
    }

    @Test
    void migrateSafeMethodParam() {
        //language=java
        rewriteRun(
          java(
            """
              import org.joda.time.DateTime;

              class A {
                  public void foo() {
                      new Bar().bar(new DateTime());
                  }

                  private static class Bar {
                      public void bar(DateTime dt) {
                          dt.getMillis();
                      }
                  }
              }
              """,
            """
              import java.time.ZonedDateTime;

              class A {
                  public void foo() {
                      new Bar().bar(ZonedDateTime.now());
                  }

                  private static class Bar {
                      public void bar(ZonedDateTime dt) {
                          dt.toInstant().toEpochMilli();
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void migrationWithRecord() {
        //language=java
        rewriteRun(
          java(
            """
              import org.joda.time.DateTime;

              record A(String x) {
                  public void foo() {
                      new Bar().bar(new DateTime());
                  }

                  private static class Bar {
                      public void bar(DateTime dt) {
                          dt.getMillis();
                      }
                  }
              }
              """,
            """
              import java.time.ZonedDateTime;

              record A(String x) {
                  public void foo() {
                      new Bar().bar(ZonedDateTime.now());
                  }

                  private static class Bar {
                      public void bar(ZonedDateTime dt) {
                          dt.toInstant().toEpochMilli();
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void migrateMethodWithSafeReturnExpression() {
        //language=java
        rewriteRun(
          java(
            """
              import org.joda.time.DateTime;
              import org.joda.time.Interval;

              class A {
                  public DateTime foo(DateTime dt) {
                      Interval interval = new Interval(dt, dt.plusDays(1));
                      return interval.getEnd();
                  }

                  private static class Bar {
                      public void bar(DateTime dt) {
                          DateTime d = foo(dt);
                          System.out.println(d.getMillis());
                      }
                  }
              }
              """,
            """
              import org.threeten.extra.Interval;

              import java.time.ZoneId;
              import java.time.ZonedDateTime;

              class A {
                  public ZonedDateTime foo(ZonedDateTime dt) {
                      Interval interval = Interval.of(dt.toInstant(), dt.plusDays(1).toInstant());
                      return interval.getEnd().atZone(ZoneId.systemDefault());
                  }

                  private static class Bar {
                      public void bar(ZonedDateTime dt) {
                          ZonedDateTime d = foo(dt);
                          System.out.println(d.toInstant().toEpochMilli());
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void migrateMethodWithSafeReturnExpressionAndUnsafeParam() {
        //language=java
        rewriteRun(
          java(
            """
              import org.joda.time.DateTime;
              import org.joda.time.Interval;

              class A {
                  public DateTime foo(DateTime dt) {
                      DateTime d = dt.toDateMidnight();
                      DateTime d2 = DateTime.now();
                      Interval interval = new Interval(d2, d2.plusDays(1));
                      return interval.getEnd();
                  }

                  private static class Bar {
                      public void bar() {
                          DateTime d = foo(new DateTime());
                          System.out.println(d.getMillis());
                      }
                  }
              }
              """,
            """
              import org.joda.time.DateTime;
              import org.threeten.extra.Interval;

              import java.time.ZoneId;
              import java.time.ZonedDateTime;

              class A {
                  public ZonedDateTime foo(DateTime dt) {
                      DateTime d = dt.toDateMidnight();
                      ZonedDateTime d2 = ZonedDateTime.now();
                      Interval interval = Interval.of(d2.toInstant(), d2.plusDays(1).toInstant());
                      return interval.getEnd().atZone(ZoneId.systemDefault());
                  }

                  private static class Bar {
                      public void bar() {
                          ZonedDateTime d = foo(new DateTime());
                          System.out.println(d.toInstant().toEpochMilli());
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotMigrateUnsafeMethodParam() {
        //language=java
        rewriteRun(
          java(
            """
              import org.joda.time.DateTime;

              class A {
                  public void foo() {
                      new Bar().bar(new DateTime());
                  }

                  private static class Bar {
                      public void bar(DateTime dt) {
                          dt.toDateMidnight(); // template doesn't exist for toDateMidnight
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void dontMigrateMethodInvocationIfSelectExprIsNotMigrated() {
        //language=java
        rewriteRun(
          java(
            """
             import org.joda.time.Interval;

             class A {
                 private Query query = new Query();
                 public void foo() {
                     query.interval().getEndMillis();
                 }
                 static class Query {
                     private Interval interval;

                     public Interval interval() {
                         return interval;
                     }
                 }
             }
             """
          )
        );
    }
}
