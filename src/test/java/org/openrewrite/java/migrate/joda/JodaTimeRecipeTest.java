package org.openrewrite.java.migrate.joda;

import org.junit.jupiter.api.Test;
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
    void dontChangeIncompatibleType() {
        //language=java
        rewriteRun(
          java(
            """
              import org.joda.time.DateTime;

              class A {
                  public void foo() {
                      new B().print(new DateTime()); // print is public method accepting DateTime, not handled yet
                      System.out.println(new B().dateTime);
                  }
              }

              class B {
                  DateTime dateTime = new DateTime();
                  public void print(DateTime dateTime) {
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
    void localVarUsedReferencedInReturnStatement() { // not supported yet
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
              """
          )
        );
    }
}
