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
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.test.RewriteTest.toRecipe;

class JodaTimeVisitorTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipe(toRecipe(JodaTimeVisitor::new))
          .parser(JavaParser.fromJavaVersion().classpath("joda-time"));
    }

    @DocumentExample
    @Test
    void migrateNewDateTime() {
        //language=java
        rewriteRun(
          java(
            """
              import org.joda.time.DateTime;
              import org.joda.time.DateTimeZone;
              import java.util.TimeZone;

              class A {
                  public void foo() {
                      System.out.println(new DateTime());
                      System.out.println(new DateTime(DateTimeZone.UTC));
                      System.out.println(new DateTime(1234567890L));
                      System.out.println(new DateTime(1234567890L, DateTimeZone.forID("America/New_York")));
                      System.out.println(new DateTime(2024, 9, 30, 12, 58));
                      System.out.println(new DateTime(2024, 9, 30, 12, 58, DateTimeZone.forOffsetHours(2)));
                      System.out.println(new DateTime(2024, 9, 30, 13, 3, 15));
                      System.out.println(new DateTime(2024, 9, 30, 13, 3, 15, DateTimeZone.forOffsetHoursMinutes(5, 30)));
                      System.out.println(new DateTime(2024, 9, 30, 13, 49, 15, 545));
                      System.out.println(new DateTime(2024, 9, 30, 13, 49, 15, 545, DateTimeZone.forTimeZone(TimeZone.getTimeZone("America/New_York"))));
                  }
              }
              """,
            """
              import java.time.Instant;
              import java.time.ZoneId;
              import java.time.ZoneOffset;
              import java.time.ZonedDateTime;
              import java.util.TimeZone;

              class A {
                  public void foo() {
                      System.out.println(ZonedDateTime.now());
                      System.out.println(ZonedDateTime.now(ZoneOffset.UTC));
                      System.out.println(ZonedDateTime.ofInstant(Instant.ofEpochMilli(1234567890L), ZoneId.systemDefault()));
                      System.out.println(ZonedDateTime.ofInstant(Instant.ofEpochMilli(1234567890L), ZoneId.of("America/New_York")));
                      System.out.println(ZonedDateTime.of(2024, 9, 30, 12, 58, 0, 0, ZoneId.systemDefault()));
                      System.out.println(ZonedDateTime.of(2024, 9, 30, 12, 58, 0, 0, ZoneOffset.ofHours(2)));
                      System.out.println(ZonedDateTime.of(2024, 9, 30, 13, 3, 15, 0, ZoneId.systemDefault()));
                      System.out.println(ZonedDateTime.of(2024, 9, 30, 13, 3, 15, 0, ZoneOffset.ofHoursMinutes(5, 30)));
                      System.out.println(ZonedDateTime.of(2024, 9, 30, 13, 49, 15, 545 * 1_000_000, ZoneId.systemDefault()));
                      System.out.println(ZonedDateTime.of(2024, 9, 30, 13, 49, 15, 545 * 1_000_000, TimeZone.getTimeZone("America/New_York").toZoneId()));
                  }
              }
              """
          )
        );
    }

    @Test
    void migrateDateTimeStaticCalls() {
        //language=java
        rewriteRun(
          java(
            """
              import org.joda.time.DateTime;
              import org.joda.time.DateTimeZone;
              import org.joda.time.format.DateTimeFormat;
              import java.util.TimeZone;

              class A {
                  public void foo() {
                      System.out.println(DateTime.now());
                      System.out.println(DateTime.now(DateTimeZone.forTimeZone(TimeZone.getTimeZone("America/New_York"))));
                      System.out.println(DateTime.parse("2024-09-30T23:03:00.000Z"));
                      System.out.println(DateTime.parse("2024-09-30T23:03:00.000Z", DateTimeFormat.shortDate()));
                  }
              }
              """,
            """
              import java.time.ZonedDateTime;
              import java.time.format.DateTimeFormatter;
              import java.time.format.FormatStyle;
              import java.util.TimeZone;

              class A {
                  public void foo() {
                      System.out.println(ZonedDateTime.now());
                      System.out.println(ZonedDateTime.now(TimeZone.getTimeZone("America/New_York").toZoneId()));
                      System.out.println(ZonedDateTime.parse("2024-09-30T23:03:00.000Z"));
                      System.out.println(ZonedDateTime.parse("2024-09-30T23:03:00.000Z", DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)));
                  }
              }
              """
          )
        );
    }

    @Test
    void migrateDateTimeInstanceCalls() {
        //language=java
        rewriteRun(
          java(
            """
              import org.joda.time.DateTime;
              import org.joda.time.DateTimeZone;
              import org.joda.time.Duration;
              import java.util.TimeZone;

              class A {
                  public void foo() {
                      System.out.println(new DateTime().toDateTime());
                      System.out.println(new DateTime().toDateTime(DateTimeZone.forTimeZone(TimeZone.getTimeZone("America/New_York"))));
                      System.out.println(new DateTime().withMillis(1234567890L));
                      System.out.println(new DateTime().withZone(DateTimeZone.forID("America/New_York")));
                      System.out.println(new DateTime().withZoneRetainFields(DateTimeZone.forID("America/New_York")));
                      System.out.println(new DateTime().withEarlierOffsetAtOverlap());
                      System.out.println(new DateTime().withLaterOffsetAtOverlap());
                      System.out.println(new DateTime().withDate(2024, 9, 30));
                      System.out.println(new DateTime().withTime(12, 58, 57, 550));
                      System.out.println(new DateTime().withDurationAdded(1234567890L, 2));
                      System.out.println(new DateTime().plus(1234567890L));
                      System.out.println(new DateTime().plus(Duration.standardDays(1)));
                      System.out.println(new DateTime().plusYears(1));
                      System.out.println(new DateTime().plusMonths(1));
                      System.out.println(new DateTime().plusWeeks(1));
                      System.out.println(new DateTime().plusDays(1));
                      System.out.println(new DateTime().plusHours(1));
                      System.out.println(new DateTime().plusMinutes(1));
                      System.out.println(new DateTime().plusSeconds(1));
                      System.out.println(new DateTime().plusMillis(1));
                      System.out.println(new DateTime().minus(1234567890L));
                      System.out.println(new DateTime().minus(Duration.standardDays(1)));
                      System.out.println(new DateTime().minusYears(1));
                      System.out.println(new DateTime().minusMonths(1));
                      System.out.println(new DateTime().minusWeeks(1));
                      System.out.println(new DateTime().minusDays(1));
                      System.out.println(new DateTime().minusHours(1));
                      System.out.println(new DateTime().minusMinutes(1));
                      System.out.println(new DateTime().minusSeconds(1));
                      System.out.println(new DateTime().minusMillis(1));
                      System.out.println(new DateTime().toLocalDateTime());
                      System.out.println(new DateTime().toLocalDate());
                      System.out.println(new DateTime().toLocalTime());
                      System.out.println(new DateTime().withYear(2024));
                      System.out.println(new DateTime().withWeekyear(2024));
                      System.out.println(new DateTime().withMonthOfYear(9));
                      System.out.println(new DateTime().withWeekOfWeekyear(39));
                      System.out.println(new DateTime().withDayOfYear(273));
                      System.out.println(new DateTime().withDayOfMonth(30));
                      System.out.println(new DateTime().withDayOfWeek(1));
                      System.out.println(new DateTime().withHourOfDay(12));
                      System.out.println(new DateTime().withMinuteOfHour(58));
                      System.out.println(new DateTime().withSecondOfMinute(57));
                      System.out.println(new DateTime().withMillisOfSecond(550));
                      System.out.println(new DateTime().withMillisOfDay(123456));
                  }
              }
              """,
            """
              import java.time.Duration;
              import java.time.Instant;
              import java.time.ZoneId;
              import java.time.ZonedDateTime;
              import java.time.temporal.ChronoField;
              import java.time.temporal.IsoFields;
              import java.util.TimeZone;

              class A {
                  public void foo() {
                      System.out.println(ZonedDateTime.now());
                      System.out.println(ZonedDateTime.now().withZoneSameInstant(TimeZone.getTimeZone("America/New_York").toZoneId()));
                      System.out.println(ZonedDateTime.ofInstant(Instant.ofEpochMilli(1234567890L), ZonedDateTime.now().getZone()));
                      System.out.println(ZonedDateTime.now().withZoneSameInstant(ZoneId.of("America/New_York")));
                      System.out.println(ZonedDateTime.now().withZoneSameLocal(ZoneId.of("America/New_York")));
                      System.out.println(ZonedDateTime.now().withEarlierOffsetAtOverlap());
                      System.out.println(ZonedDateTime.now().withLaterOffsetAtOverlap());
                      System.out.println(ZonedDateTime.now().withYear(2024).withMonth(9).withDayOfMonth(30));
                      System.out.println(ZonedDateTime.now().withHour(12).withMinute(58).withSecond(57).withNano(550 * 1_000_000));
                      System.out.println(ZonedDateTime.now().plus(Duration.ofMillis(1234567890L).multipliedBy(2)));
                      System.out.println(ZonedDateTime.now().plus(Duration.ofMillis(1234567890L)));
                      System.out.println(ZonedDateTime.now().plus(Duration.ofDays(1)));
                      System.out.println(ZonedDateTime.now().plusYears(1));
                      System.out.println(ZonedDateTime.now().plusMonths(1));
                      System.out.println(ZonedDateTime.now().plusWeeks(1));
                      System.out.println(ZonedDateTime.now().plusDays(1));
                      System.out.println(ZonedDateTime.now().plusHours(1));
                      System.out.println(ZonedDateTime.now().plusMinutes(1));
                      System.out.println(ZonedDateTime.now().plusSeconds(1));
                      System.out.println(ZonedDateTime.now().plus(Duration.ofMillis(1)));
                      System.out.println(ZonedDateTime.now().minus(Duration.ofMillis(1234567890L)));
                      System.out.println(ZonedDateTime.now().minus(Duration.ofDays(1)));
                      System.out.println(ZonedDateTime.now().minusYears(1));
                      System.out.println(ZonedDateTime.now().minusMonths(1));
                      System.out.println(ZonedDateTime.now().minusWeeks(1));
                      System.out.println(ZonedDateTime.now().minusDays(1));
                      System.out.println(ZonedDateTime.now().minusHours(1));
                      System.out.println(ZonedDateTime.now().minusMinutes(1));
                      System.out.println(ZonedDateTime.now().minusSeconds(1));
                      System.out.println(ZonedDateTime.now().minus(Duration.ofMillis(1)));
                      System.out.println(ZonedDateTime.now().toLocalDateTime());
                      System.out.println(ZonedDateTime.now().toLocalDate());
                      System.out.println(ZonedDateTime.now().toLocalTime());
                      System.out.println(ZonedDateTime.now().withYear(2024));
                      System.out.println(ZonedDateTime.now().with(IsoFields.WEEK_BASED_YEAR, 2024));
                      System.out.println(ZonedDateTime.now().withMonth(9));
                      System.out.println(ZonedDateTime.now().with(ChronoField.ALIGNED_WEEK_OF_YEAR, 39));
                      System.out.println(ZonedDateTime.now().withDayOfYear(273));
                      System.out.println(ZonedDateTime.now().withDayOfMonth(30));
                      System.out.println(ZonedDateTime.now().with(ChronoField.DAY_OF_WEEK, 1));
                      System.out.println(ZonedDateTime.now().withHour(12));
                      System.out.println(ZonedDateTime.now().withMinute(58));
                      System.out.println(ZonedDateTime.now().withSecond(57));
                      System.out.println(ZonedDateTime.now().withNano(550 * 1_000_000));
                      System.out.println(ZonedDateTime.now().with(ChronoField.MILLI_OF_DAY, 123456));
                  }
              }
              """
          )
        );
    }

    @Test
    void migrateDateTimeZone() {
        //language=java
        rewriteRun(
          java(
            """
              import org.joda.time.DateTimeZone;
              import java.util.TimeZone;

              class A {
                  public void foo() {
                      System.out.println(DateTimeZone.UTC);
                      System.out.println(DateTimeZone.forID("America/New_York"));
                      System.out.println(DateTimeZone.forOffsetHours(2));
                      System.out.println(DateTimeZone.forOffsetHoursMinutes(5, 30));
                      System.out.println(DateTimeZone.forTimeZone(TimeZone.getTimeZone("America/New_York")));
                  }
              }
              """,
            """             
              import java.time.ZoneId;
              import java.time.ZoneOffset;
              import java.util.TimeZone;

              class A {
                  public void foo() {
                      System.out.println(ZoneOffset.UTC);
                      System.out.println(ZoneId.of("America/New_York"));
                      System.out.println(ZoneOffset.ofHours(2));
                      System.out.println(ZoneOffset.ofHoursMinutes(5, 30));
                      System.out.println(TimeZone.getTimeZone("America/New_York").toZoneId());
                  }
              }
              """
          )
        );
    }

    @Test
    void migrateDateTimeFormatter() {
        //language=java
        rewriteRun(
          java(
            """
              import org.joda.time.format.DateTimeFormat;

              class A {
                  public void foo() {
                      System.out.println(DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));
                      // System.out.println(DateTimeFormat.forStyle("SS"));  unhandled case
                      // System.out.println(DateTimeFormat.patternForStyle("SS", Locale.US)); unhandled case
                      System.out.println(DateTimeFormat.shortDate());
                      System.out.println(DateTimeFormat.mediumDate());
                      System.out.println(DateTimeFormat.longDate());
                      System.out.println(DateTimeFormat.fullDate());
                      System.out.println(DateTimeFormat.shortTime());
                      System.out.println(DateTimeFormat.mediumTime());
                      System.out.println(DateTimeFormat.longTime());
                      System.out.println(DateTimeFormat.fullTime());
                      System.out.println(DateTimeFormat.shortDateTime());
                      System.out.println(DateTimeFormat.mediumDateTime());
                      System.out.println(DateTimeFormat.longDateTime());
                      System.out.println(DateTimeFormat.fullDateTime());
                  }
              }
              """,
            """
              import java.time.format.DateTimeFormatter;
              import java.time.format.FormatStyle;

              class A {
                  public void foo() {
                      System.out.println(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));
                      // System.out.println(DateTimeFormat.forStyle("SS"));  unhandled case
                      // System.out.println(DateTimeFormat.patternForStyle("SS", Locale.US)); unhandled case
                      System.out.println(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT));
                      System.out.println(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM));
                      System.out.println(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG));
                      System.out.println(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL));
                      System.out.println(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT));
                      System.out.println(DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM));
                      System.out.println(DateTimeFormatter.ofLocalizedTime(FormatStyle.LONG));
                      System.out.println(DateTimeFormatter.ofLocalizedTime(FormatStyle.FULL));
                      System.out.println(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.SHORT));
                      System.out.println(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.MEDIUM));
                      System.out.println(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG, FormatStyle.LONG));
                      System.out.println(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.FULL));
                  }
              }
              """
          )
        );
    }

    @Test
    void migrateJodaDuration() {
        // language=java
        rewriteRun(
          java(
            """
              import org.joda.time.Duration;

              class A {
                  public void foo() {
                      System.out.println(Duration.standardDays(1L));
                      System.out.println(Duration.standardHours(1L));
                      System.out.println(Duration.standardMinutes(1L));
                      System.out.println(Duration.standardSeconds(1L));
                      System.out.println(Duration.millis(1000L));
                      System.out.println(new Duration(1000L));
                      System.out.println(new Duration(1000L, 2000L));
                      System.out.println(new Duration(1000L).getStandardDays());
                      System.out.println(new Duration(1000L).getStandardHours());
                      System.out.println(new Duration(1000L).getStandardMinutes());
                      System.out.println(new Duration(1000L).getStandardSeconds());
                      System.out.println(new Duration(1000L).toDuration());
                      System.out.println(new Duration(1000L).withMillis(2000L));
                      System.out.println(new Duration(1000L).withDurationAdded(550L, 2));
                      System.out.println(new Duration(1000L).withDurationAdded(new Duration(550L), 2));
                      System.out.println(new Duration(1000L).plus(550L));
                      System.out.println(new Duration(1000L).plus(new Duration(550L)));
                      System.out.println(new Duration(1000L).minus(550L));
                      System.out.println(new Duration(1000L).minus(new Duration(550L)));
                      System.out.println(new Duration(1000L).multipliedBy(2));
                      System.out.println(new Duration(1000L).dividedBy(2));
                      System.out.println(new Duration(1000L).negated());
                      System.out.println(new Duration(1000L).abs());
                  }
              }
              """,
            """
              import java.time.Duration;
              import java.time.Instant;

              class A {
                  public void foo() {
                      System.out.println(Duration.ofDays(1L));
                      System.out.println(Duration.ofHours(1L));
                      System.out.println(Duration.ofMinutes(1L));
                      System.out.println(Duration.ofSeconds(1L));
                      System.out.println(Duration.ofMillis(1000L));
                      System.out.println(Duration.ofMillis(1000L));
                      System.out.println(Duration.between(Instant.ofEpochMilli(1000L), Instant.ofEpochMilli(2000L)));
                      System.out.println(Duration.ofMillis(1000L).toDays());
                      System.out.println(Duration.ofMillis(1000L).toHours());
                      System.out.println(Duration.ofMillis(1000L).toMinutes());
                      System.out.println(Duration.ofMillis(1000L).getSeconds());
                      System.out.println(Duration.ofMillis(1000L));
                      System.out.println(Duration.ofMillis(2000L));
                      System.out.println(Duration.ofMillis(1000L).plusMillis(550L * 2));
                      System.out.println(Duration.ofMillis(1000L).plus(Duration.ofMillis(550L).multipliedBy(2)));
                      System.out.println(Duration.ofMillis(1000L).plusMillis(550L));
                      System.out.println(Duration.ofMillis(1000L).plus(Duration.ofMillis(550L)));
                      System.out.println(Duration.ofMillis(1000L).minusMillis(550L));
                      System.out.println(Duration.ofMillis(1000L).minus(Duration.ofMillis(550L)));
                      System.out.println(Duration.ofMillis(1000L).multipliedBy(2));
                      System.out.println(Duration.ofMillis(1000L).dividedBy(2));
                      System.out.println(Duration.ofMillis(1000L).negated());
                      System.out.println(Duration.ofMillis(1000L).abs());
                  }
              }
              """
          )
        );
    }

    @Test
    void migrateClassesWithFqn() {
        // language=java
        rewriteRun(
          java(
            """
              class A {
                  public void foo() {
                      System.out.println(org.joda.time.format.DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));
                  }
              }
              """,
            """
              import java.time.format.DateTimeFormatter;

              class A {
                  public void foo() {
                      System.out.println(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));
                  }
              }
              """
          )
        );
    }

    @Test
    void migrateJodaTypeExpressionReferencingNonJodaTypeVar() {
        //language=java
        rewriteRun(
          java(
            """
              import org.joda.time.DateTime;

              class A {
                  public void foo() {
                      long millis = DateTime.now().getMillis();
                      System.out.println(millis);
                  }
              }
              """,
            """
              import java.time.ZonedDateTime;
              
              class A {
                  public void foo() {
                      long millis = ZonedDateTime.now().toInstant().toEpochMilli();
                      System.out.println(millis);
                  }
              }
              """
          )
        );
    }

    // Test will be removed once safe variable migration is implemented
    @Test
    void dontChangeMethodAccessOnVariable() {
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
                  }
              }

              class B {
                  public void print(DateTime dateTime) {
                      System.out.println(dateTime);
                  }
              }
              """
          )
        );
    }

    @Test
    void dontChangeMethodsWithUnhandledArguments() {
        //language=java
        rewriteRun(
          java(
            """
              import org.joda.time.DateTime;
              import org.joda.time.format.DateTimeFormat;

              class A {
                  public void foo() {
                      // DateTimeFormat.forStyle is unhandled so parent method should not be changed
                      System.out.println(DateTime.parse("2024-09-30T23:03:00.000Z", DateTimeFormat.forStyle("SS")));
                  }
              }
              """
          )
        );
    }
}
