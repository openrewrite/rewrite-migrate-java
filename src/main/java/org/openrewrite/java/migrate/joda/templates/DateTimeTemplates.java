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
package org.openrewrite.java.migrate.joda.templates;

import lombok.NoArgsConstructor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.util.ArrayList;
import java.util.List;

import static org.openrewrite.java.migrate.joda.templates.TimeClassNames.*;

@NoArgsConstructor
public class DateTimeTemplates {
  private final MethodMatcher newDateTime = new MethodMatcher(JODA_DATE_TIME + "<constructor>()");
  private final MethodMatcher newDateTimeWithZone = new MethodMatcher(JODA_DATE_TIME + "<constructor>(" + JODA_DATE_TIME_ZONE + ")");
  private final MethodMatcher newDateTimeWithEpoch = new MethodMatcher(JODA_DATE_TIME + "<constructor>(long)");
  private final MethodMatcher newDateTimeWithEpochAndZone = new MethodMatcher(JODA_DATE_TIME + "<constructor>(long, " + JODA_DATE_TIME_ZONE + ")");
  private final MethodMatcher newDateTimeWithMin = new MethodMatcher(JODA_DATE_TIME + "<constructor>(int, int, int, int, int)");
  private final MethodMatcher newDateTimeWithMinAndZone = new MethodMatcher(JODA_DATE_TIME + "<constructor>(int, int, int, int, int, " + JODA_DATE_TIME_ZONE + ")");
  private final MethodMatcher newDateTimeWithSec = new MethodMatcher(JODA_DATE_TIME + "<constructor>(int, int, int, int, int, int)");
  private final MethodMatcher newDateTimeWithSecAndZone = new MethodMatcher(JODA_DATE_TIME + "<constructor>(int, int, int, int, int, int, " + JODA_DATE_TIME_ZONE + ")");
  private final MethodMatcher newDateTimeWithMillis = new MethodMatcher(JODA_DATE_TIME + "<constructor>(int, int, int, int, int, int, int)");
  private final MethodMatcher newDateTimeWithMillisAndZone = new MethodMatcher(JODA_DATE_TIME + "<constructor>(int, int, int, int, int, int, int, " + JODA_DATE_TIME_ZONE + ")");

  private final MethodMatcher dateTimeNow = new MethodMatcher(JODA_DATE_TIME + " now()");
  private final MethodMatcher dateTimeNowWithZone = new MethodMatcher(JODA_DATE_TIME + " now(" + JODA_DATE_TIME_ZONE + ")");
  private final MethodMatcher dateTimeParse = new MethodMatcher(JODA_DATE_TIME + " parse(String)");
  private final MethodMatcher dateTimeParseWithFormatter = new MethodMatcher(JODA_DATE_TIME + " parse(String, " + JODA_TIME_FORMATTER +")");

  private final MethodMatcher toDateTime = new MethodMatcher(JODA_DATE_TIME + " toDateTime()");
  private final MethodMatcher toDateTimeWithZone = new MethodMatcher(JODA_DATE_TIME + " toDateTime(" + JODA_DATE_TIME_ZONE + ")");
  private final MethodMatcher getMillis = new MethodMatcher(JODA_BASE_DATE_TIME + " getMillis()");
  private final MethodMatcher withMillis = new MethodMatcher(JODA_DATE_TIME + " withMillis(long)");
  private final MethodMatcher withZone = new MethodMatcher(JODA_DATE_TIME + " withZone(" + JODA_DATE_TIME_ZONE + ")");
  private final MethodMatcher withZoneRetainFields = new MethodMatcher(JODA_DATE_TIME + " withZoneRetainFields(" + JODA_DATE_TIME_ZONE + ")");
  private final MethodMatcher withEarlierOffsetAtOverlap = new MethodMatcher(JODA_DATE_TIME + " withEarlierOffsetAtOverlap()");
  private final MethodMatcher withLaterOffsetAtOverlap = new MethodMatcher(JODA_DATE_TIME + " withLaterOffsetAtOverlap()");
  private final MethodMatcher withDate = new MethodMatcher(JODA_DATE_TIME + " withDate(int, int, int)");
  private final MethodMatcher withDateLocalDate = new MethodMatcher(JODA_DATE_TIME + " withDate(" + JODA_LOCAL_DATE + ")");
  private final MethodMatcher withTime = new MethodMatcher(JODA_DATE_TIME + " withTime(int, int, int, int)");
  private final MethodMatcher withTimeLocalTime = new MethodMatcher(JODA_DATE_TIME + " withTime(" + JODA_LOCAL_TIME + ")");
  private final MethodMatcher withTimeAtStartOfDay = new MethodMatcher(JODA_DATE_TIME + " withTimeAtStartOfDay()");
  private final MethodMatcher withField = new MethodMatcher(JODA_DATE_TIME + " withField(" + JODA_DATE_TIME_FIELD_TYPE + ", int)");
  private final MethodMatcher withFieldAdded = new MethodMatcher(JODA_DATE_TIME + " withFieldAdded(" + JODA_DURATION_FIELD_TYPE + ", int)");
  private final MethodMatcher withDurationAdded = new MethodMatcher(JODA_DATE_TIME + " withDurationAdded(long, int)");
  private final MethodMatcher plusLong = new MethodMatcher(JODA_DATE_TIME + " plus(long)");
  private final MethodMatcher plusReadableDuration = new MethodMatcher(JODA_DATE_TIME + " plus(" + JODA_READABLE_DURATION + ")");
  private final MethodMatcher plusYears = new MethodMatcher(JODA_DATE_TIME + " plusYears(int)");
  private final MethodMatcher plusMonths = new MethodMatcher(JODA_DATE_TIME + " plusMonths(int)");
  private final MethodMatcher plusWeeks = new MethodMatcher(JODA_DATE_TIME + " plusWeeks(int)");
  private final MethodMatcher plusDays = new MethodMatcher(JODA_DATE_TIME + " plusDays(int)");
  private final MethodMatcher plusHours = new MethodMatcher(JODA_DATE_TIME + " plusHours(int)");
  private final MethodMatcher plusMinutes = new MethodMatcher(JODA_DATE_TIME + " plusMinutes(int)");
  private final MethodMatcher plusSeconds = new MethodMatcher(JODA_DATE_TIME + " plusSeconds(int)");
  private final MethodMatcher plusMillis = new MethodMatcher(JODA_DATE_TIME + " plusMillis(int)");
  private final MethodMatcher minusLong = new MethodMatcher(JODA_DATE_TIME + " minus(long)");
  private final MethodMatcher minusReadableDuration = new MethodMatcher(JODA_DATE_TIME + " minus(" + JODA_READABLE_DURATION + ")");
  private final MethodMatcher minusYears = new MethodMatcher(JODA_DATE_TIME + " minusYears(int)");
  private final MethodMatcher minusMonths = new MethodMatcher(JODA_DATE_TIME + " minusMonths(int)");
  private final MethodMatcher minusWeeks = new MethodMatcher(JODA_DATE_TIME + " minusWeeks(int)");
  private final MethodMatcher minusDays = new MethodMatcher(JODA_DATE_TIME + " minusDays(int)");
  private final MethodMatcher minusHours = new MethodMatcher(JODA_DATE_TIME + " minusHours(int)");
  private final MethodMatcher minusMinutes = new MethodMatcher(JODA_DATE_TIME + " minusMinutes(int)");
  private final MethodMatcher minusSeconds = new MethodMatcher(JODA_DATE_TIME + " minusSeconds(int)");
  private final MethodMatcher minusMillis = new MethodMatcher(JODA_DATE_TIME + " minusMillis(int)");
  private final MethodMatcher toDateMidnight = new MethodMatcher(JODA_DATE_TIME + " toDateMidnight()");
  private final MethodMatcher toYearMonthDay = new MethodMatcher(JODA_DATE_TIME + " toYearMonthDay()");
  private final MethodMatcher toTimeOfDay = new MethodMatcher(JODA_DATE_TIME + " toTimeOfDay()");
  private final MethodMatcher toLocalDateTime = new MethodMatcher(JODA_DATE_TIME + " toLocalDateTime()");
  private final MethodMatcher toLocalDate = new MethodMatcher(JODA_DATE_TIME + " toLocalDate()");
  private final MethodMatcher toLocalTime = new MethodMatcher(JODA_DATE_TIME + " toLocalTime()");
  private final MethodMatcher withEra = new MethodMatcher(JODA_DATE_TIME + " withEra(int)");
  private final MethodMatcher withCenturyOfEra = new MethodMatcher(JODA_DATE_TIME + " withCenturyOfEra(int)");
  private final MethodMatcher withYearOfEra = new MethodMatcher(JODA_DATE_TIME + " withYearOfEra(int)");
  private final MethodMatcher withYearOfCentury = new MethodMatcher(JODA_DATE_TIME + " withYearOfCentury(int)");
  private final MethodMatcher withYear = new MethodMatcher(JODA_DATE_TIME + " withYear(int)");
  private final MethodMatcher withWeekyear = new MethodMatcher(JODA_DATE_TIME + " withWeekyear(int)");
  private final MethodMatcher withMonthOfYear = new MethodMatcher(JODA_DATE_TIME + " withMonthOfYear(int)");
  private final MethodMatcher withWeekOfWeekyear = new MethodMatcher(JODA_DATE_TIME + " withWeekOfWeekyear(int)");
  private final MethodMatcher withDayOfYear = new MethodMatcher(JODA_DATE_TIME + " withDayOfYear(int)");
  private final MethodMatcher withDayOfMonth = new MethodMatcher(JODA_DATE_TIME + " withDayOfMonth(int)");
  private final MethodMatcher withDayOfWeek = new MethodMatcher(JODA_DATE_TIME + " withDayOfWeek(int)");
  private final MethodMatcher withHourOfDay = new MethodMatcher(JODA_DATE_TIME + " withHourOfDay(int)");
  private final MethodMatcher withMinuteOfHour = new MethodMatcher(JODA_DATE_TIME + " withMinuteOfHour(int)");
  private final MethodMatcher withSecondOfMinute = new MethodMatcher(JODA_DATE_TIME + " withSecondOfMinute(int)");
  private final MethodMatcher withMillisOfSecond = new MethodMatcher(JODA_DATE_TIME + " withMillisOfSecond(int)");
  private final MethodMatcher withMillisOfDay = new MethodMatcher(JODA_DATE_TIME + " withMillisOfDay(int)");
  private final MethodMatcher era = new MethodMatcher(JODA_DATE_TIME + " era()");
  private final MethodMatcher centuryOfEra = new MethodMatcher(JODA_DATE_TIME + " centuryOfEra()");
  private final MethodMatcher yearOfCentury = new MethodMatcher(JODA_DATE_TIME + " yearOfCentury()");
  private final MethodMatcher yearOfEra = new MethodMatcher(JODA_DATE_TIME + " yearOfEra()");
  private final MethodMatcher year = new MethodMatcher(JODA_DATE_TIME + " year()");
  private final MethodMatcher weekyear = new MethodMatcher(JODA_DATE_TIME + " weekyear()");
  private final MethodMatcher monthOfYear = new MethodMatcher(JODA_DATE_TIME + " monthOfYear()");
  private final MethodMatcher weekOfWeekyear = new MethodMatcher(JODA_DATE_TIME + " weekOfWeekyear()");
  private final MethodMatcher dayOfYear = new MethodMatcher(JODA_DATE_TIME + " dayOfYear()");
  private final MethodMatcher dayOfMonth = new MethodMatcher(JODA_DATE_TIME + " dayOfMonth()");
  private final MethodMatcher dayOfWeek = new MethodMatcher(JODA_DATE_TIME + " dayOfWeek()");
  private final MethodMatcher hourOfDay = new MethodMatcher(JODA_DATE_TIME + " hourOfDay()");
  private final MethodMatcher minuteOfDay = new MethodMatcher(JODA_DATE_TIME + " minuteOfDay()");
  private final MethodMatcher minuteOfHour = new MethodMatcher(JODA_DATE_TIME + " minuteOfHour()");
  private final MethodMatcher secondOfDay = new MethodMatcher(JODA_DATE_TIME + " secondOfDay()");
  private final MethodMatcher secondOfMinute = new MethodMatcher(JODA_DATE_TIME + " secondOfMinute()");
  private final MethodMatcher millisOfDay = new MethodMatcher(JODA_DATE_TIME + " millisOfDay()");
  private final MethodMatcher millisOfSecond = new MethodMatcher(JODA_DATE_TIME + " millisOfSecond()");


  private final JavaTemplate dateTimeTemplate = JavaTemplate.builder("ZonedDateTime.now()")
      .imports(JAVA_DATE_TIME)
      .build();
  private final JavaTemplate dateTimeWithZoneTemplate = JavaTemplate.builder("ZonedDateTime.now(#{any(java.time.ZoneOffset)})")
      .imports(JAVA_DATE_TIME)
      .build();
  private final JavaTemplate dateTimeWithEpochTemplate = JavaTemplate.builder("ZonedDateTime.ofInstant(Instant.ofEpochMilli(#{any(long)}), ZoneId.systemDefault())")
      .imports(JAVA_DATE_TIME, JAVA_ZONE_ID, JAVA_INSTANT)
      .build();
  private final JavaTemplate dateTimeWithEpochAndZoneTemplate = JavaTemplate.builder("ZonedDateTime.ofInstant(Instant.ofEpochMilli(#{any(long)}), #{any(java.time.ZoneId)})")
      .imports(JAVA_DATE_TIME, JAVA_ZONE_OFFSET, JAVA_ZONE_ID, JAVA_INSTANT)
      .build();
  private final JavaTemplate dateTimeWithMinTemplate = JavaTemplate.builder("ZonedDateTime.of(#{any(int)}, #{any(int)}, #{any(int)}, #{any(int)}, #{any(int)}, 0, 0, ZoneId.systemDefault())")
      .imports(JAVA_DATE_TIME, JAVA_ZONE_ID)
      .build();
  private final JavaTemplate dateTimeWithMinAndZoneTemplate = JavaTemplate.builder("ZonedDateTime.of(#{any(int)}, #{any(int)}, #{any(int)}, #{any(int)}, #{any(int)}, 0, 0, #{any(java.time.ZoneId)})")
      .imports(JAVA_DATE_TIME, JAVA_ZONE_ID)
      .build();
  private final JavaTemplate dateTimeWithSecTemplate = JavaTemplate.builder("ZonedDateTime.of(#{any(int)}, #{any(int)}, #{any(int)}, #{any(int)}, #{any(int)}, #{any(int)}, 0, ZoneId.systemDefault())")
      .imports(JAVA_DATE_TIME, JAVA_ZONE_ID)
      .build();
  private final JavaTemplate dateTimeWithSecAndZoneTemplate = JavaTemplate.builder("ZonedDateTime.of(#{any(int)}, #{any(int)}, #{any(int)}, #{any(int)}, #{any(int)}, #{any(int)}, 0, #{any(java.time.ZoneId)})")
      .imports(JAVA_DATE_TIME, JAVA_ZONE_ID)
      .build();
  private final JavaTemplate dateTimeWithMillisTemplate = JavaTemplate.builder("ZonedDateTime.of(#{any(int)}, #{any(int)}, #{any(int)}, #{any(int)}, #{any(int)}, #{any(int)}, #{any(int)} * 1_000_000, ZoneId.systemDefault())")
      .imports(JAVA_DATE_TIME, JAVA_ZONE_ID)
      .build();
  private final JavaTemplate dateTimeWithMillisAndZoneTemplate = JavaTemplate.builder("ZonedDateTime.of(#{any(int)}, #{any(int)}, #{any(int)}, #{any(int)}, #{any(int)}, #{any(int)}, #{any(int)} * 1_000_000, #{any(java.time.ZoneId)})")
      .imports(JAVA_DATE_TIME)
      .build();
  private final JavaTemplate dateTimeParseTemplate = JavaTemplate.builder("ZonedDateTime.parse(#{any(String)})")
      .imports(JAVA_DATE_TIME)
      .build();
  private final JavaTemplate dateTimeParseWithFormatterTemplate = JavaTemplate.builder("ZonedDateTime.parse(#{any(String)}, #{any(java.time.format.DateTimeFormatter)})")
      .imports(JAVA_DATE_TIME)
      .build();
  private final JavaTemplate toDateTimeTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}")
      .build();
  private final JavaTemplate getMillisTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.toInstant().toEpochMilli()")
      .build();
  private final JavaTemplate withMillisTemplate = JavaTemplate.builder("ZonedDateTime.ofInstant(Instant.ofEpochMilli(#{any(long)}),#{any(java.time.ZonedDateTime)}.getZone())")
      .imports(JAVA_DATE_TIME, JAVA_INSTANT)
      .build();
  private final JavaTemplate withZoneTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.withZoneSameInstant(#{any(java.time.ZoneId)})")
      .build();
  private final JavaTemplate withZoneRetainFieldsTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.withZoneSameLocal(#{any(java.time.ZoneId)})")
      .build();
  private final JavaTemplate withEarlierOffsetAtOverlapTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.withEarlierOffsetAtOverlap()")
      .build();
  private final JavaTemplate withLaterOffsetAtOverlapTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.withLaterOffsetAtOverlap()")
      .build();
  private final JavaTemplate withDateTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.withYear(#{any(int)}).withMonth(#{any(int)}).withDayOfMonth(#{any(int)})")
      .build();
  private final JavaTemplate withTemporalAdjusterTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.with(#{any(java.time.temporal.TemporalAdjuster)})")
      .imports(JAVA_TEMPORAL_ADJUSTER)
      .build();
  private final JavaTemplate withTimeTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.withHour(#{any(int)}).withMinute(#{any(int)}).withSecond(#{any(int)}).withNano(#{any(int)} * 1_000_000)")
      .build();
  private final JavaTemplate withTimeAtStartOfDayTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.atStartOfDay(#{any(java.time.ZonedDateTime)}.getZone())")
      .build();
  private final JavaTemplate withDurationAddedTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.plus(Duration.ofMillis(#{any(long)}).multipliedBy(#{any(int)}))")
      .imports(JAVA_DURATION)
      .build();
  private final JavaTemplate plusReadableDurationTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.plus(#{any(java.time.Duration)})")
      .build();
  private final JavaTemplate plusYearsTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.plusYears(#{any(int)})")
      .imports(JAVA_DATE_TIME)
      .build();
  private final JavaTemplate plusMonthsTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.plusMonths(#{any(int)})")
      .build();
  private final JavaTemplate plusWeeksTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.plusWeeks(#{any(int)})")
      .build();
  private final JavaTemplate plusDaysTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.plusDays(#{any(int)})")
      .build();
  private final JavaTemplate plusHoursTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.plusHours(#{any(int)})")
      .build();
  private final JavaTemplate plusMinutesTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.plusMinutes(#{any(int)})")
      .build();
  private final JavaTemplate plusSecondsTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.plusSeconds(#{any(int)})")
      .build();
  private final JavaTemplate plusMillisTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.plus(Duration.ofMillis(#{any(int)}))")
      .imports(JAVA_DURATION)
      .build();
  private final JavaTemplate minusMillisTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.minus(Duration.ofMillis(#{any(int)}))")
      .imports(JAVA_DURATION)
      .build();
  private final JavaTemplate minusReadableDurationTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.minus(#{any(java.time.Duration)})")
      .imports(JAVA_DATE_TIME, JAVA_DURATION)
      .build();
  private final JavaTemplate minusYearsTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.minusYears(#{any(int)})")
      .imports(JAVA_DATE_TIME)
      .build();
  private final JavaTemplate minusMonthsTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.minusMonths(#{any(int)})")
      .imports(JAVA_DATE_TIME)
      .build();
  private final JavaTemplate minusWeeksTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.minusWeeks(#{any(int)})")
      .imports(JAVA_DATE_TIME)
      .build();
  private final JavaTemplate minusDaysTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.minusDays(#{any(int)})")
      .imports(JAVA_DATE_TIME)
      .build();
  private final JavaTemplate minusHoursTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.minusHours(#{any(int)})")
      .imports(JAVA_DATE_TIME)
      .build();
  private final JavaTemplate minusMinutesTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.minusMinutes(#{any(int)})")
      .imports(JAVA_DATE_TIME)
      .build();
  private final JavaTemplate minusSecondsTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.minusSeconds(#{any(int)})")
      .imports(JAVA_DATE_TIME)
      .build();
  private final JavaTemplate toLocalDateTimeTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.toLocalDateTime()")
      .imports(JAVA_DATE_TIME)
      .build();
  private final JavaTemplate toLocalDateTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.toLocalDate()")
      .imports(JAVA_DATE_TIME)
      .build();
  private final JavaTemplate toLocalTimeTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.toLocalTime()")
      .imports(JAVA_DATE_TIME)
      .build();
  private final JavaTemplate withYearTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.withYear(#{any(int)})")
      .build();
  private final JavaTemplate withWeekyearTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.with(IsoFields.WEEK_BASED_YEAR, #{any(int)})")
      .imports(JAVA_TEMPORAL_ISO_FIELDS)
      .build();
  private final JavaTemplate withMonthOfYearTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.withMonth(#{any(int)})")
      .build();
  private final JavaTemplate withWeekOfWeekyearTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.with(ChronoField.ALIGNED_WEEK_OF_YEAR, #{any(int)})")
        .imports(JAVA_CHRONO_FIELD)
      .build();
  private final JavaTemplate withDayOfYearTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.withDayOfYear(#{any(int)})")
      .build();
  private final JavaTemplate withDayOfMonthTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.withDayOfMonth(#{any(int)})")
      .build();
  private final JavaTemplate withDayOfWeekTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.with(ChronoField.DAY_OF_WEEK, #{any(int)})")
      .imports(JAVA_CHRONO_FIELD)
      .build();
  private final JavaTemplate withHourOfDayTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.withHour(#{any(int)})")
      .build();
  private final JavaTemplate withMinuteOfHourTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.withMinute(#{any(int)})")
      .build();
  private final JavaTemplate withSecondOfMinuteTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.withSecond(#{any(int)})")
      .build();
  private final JavaTemplate withMillisOfSecondTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.withNano(#{any(int)} * 1_000_000)")
      .build();
  private final JavaTemplate withMillisOfDayTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.with(ChronoField.MILLI_OF_DAY, #{any(int)})")
      .imports(JAVA_CHRONO_FIELD)
      .build();

  private final List<MethodTemplate> templates = new ArrayList<MethodTemplate>() {
    {
      add(new MethodTemplate(newDateTime, dateTimeTemplate));
      add(new MethodTemplate(newDateTimeWithZone, dateTimeWithZoneTemplate));
      add(new MethodTemplate(newDateTimeWithEpoch, dateTimeWithEpochTemplate));
      add(new MethodTemplate(newDateTimeWithEpochAndZone, dateTimeWithEpochAndZoneTemplate));
      add(new MethodTemplate(newDateTimeWithMin, dateTimeWithMinTemplate));
      add(new MethodTemplate(newDateTimeWithMinAndZone, dateTimeWithMinAndZoneTemplate));
      add(new MethodTemplate(newDateTimeWithSec, dateTimeWithSecTemplate));
      add(new MethodTemplate(newDateTimeWithSecAndZone, dateTimeWithSecAndZoneTemplate));
      add(new MethodTemplate(newDateTimeWithMillis, dateTimeWithMillisTemplate));
      add(new MethodTemplate(newDateTimeWithMillisAndZone, dateTimeWithMillisAndZoneTemplate));
      add(new MethodTemplate(dateTimeNow, dateTimeTemplate));
      add(new MethodTemplate(dateTimeNowWithZone, dateTimeWithZoneTemplate));
      add(new MethodTemplate(dateTimeParse, dateTimeParseTemplate));
      add(new MethodTemplate(dateTimeParseWithFormatter, dateTimeParseWithFormatterTemplate));
      add(new MethodTemplate(toDateTime, toDateTimeTemplate));
      add(new MethodTemplate(toDateTimeWithZone, withZoneTemplate));
      add(new MethodTemplate(getMillis, getMillisTemplate));
      add(new MethodTemplate(withMillis, withMillisTemplate, m -> {
        J.MethodInvocation mi = (J.MethodInvocation) m;
        return new Expression[] {mi.getArguments().get(0), mi.getSelect()};
      }));
      add(new MethodTemplate(withZone, withZoneTemplate));
      add(new MethodTemplate(withZoneRetainFields, withZoneRetainFieldsTemplate));
      add(new MethodTemplate(withEarlierOffsetAtOverlap, withEarlierOffsetAtOverlapTemplate));
      add(new MethodTemplate(withLaterOffsetAtOverlap, withLaterOffsetAtOverlapTemplate));
      add(new MethodTemplate(withDate, withDateTemplate));
      add(new MethodTemplate(withDateLocalDate, withTemporalAdjusterTemplate));
      add(new MethodTemplate(withTime, withTimeTemplate));
      add(new MethodTemplate(withTimeLocalTime, withTemporalAdjusterTemplate));
      add(new MethodTemplate(withTimeAtStartOfDay, withTimeAtStartOfDayTemplate));
      add(new MethodTemplate(withDurationAdded, withDurationAddedTemplate));
      add(new MethodTemplate(plusLong, plusMillisTemplate));
      add(new MethodTemplate(plusReadableDuration, plusReadableDurationTemplate));
      add(new MethodTemplate(plusYears, plusYearsTemplate));
      add(new MethodTemplate(plusMonths, plusMonthsTemplate));
      add(new MethodTemplate(plusWeeks, plusWeeksTemplate));
      add(new MethodTemplate(plusDays, plusDaysTemplate));
      add(new MethodTemplate(plusHours, plusHoursTemplate));
      add(new MethodTemplate(plusMinutes, plusMinutesTemplate));
      add(new MethodTemplate(plusSeconds, plusSecondsTemplate));
      add(new MethodTemplate(plusMillis, plusMillisTemplate));
      add(new MethodTemplate(minusLong, minusMillisTemplate));
      add(new MethodTemplate(minusReadableDuration, minusReadableDurationTemplate));
      add(new MethodTemplate(minusYears, minusYearsTemplate));
      add(new MethodTemplate(minusMonths, minusMonthsTemplate));
      add(new MethodTemplate(minusWeeks, minusWeeksTemplate));
      add(new MethodTemplate(minusDays, minusDaysTemplate));
      add(new MethodTemplate(minusHours, minusHoursTemplate));
      add(new MethodTemplate(minusMinutes, minusMinutesTemplate));
      add(new MethodTemplate(minusSeconds, minusSecondsTemplate));
      add(new MethodTemplate(minusMillis, minusMillisTemplate));
      add(new MethodTemplate(toLocalDateTime, toLocalDateTimeTemplate));
      add(new MethodTemplate(toLocalDate, toLocalDateTemplate));
      add(new MethodTemplate(toLocalTime, toLocalTimeTemplate));
      add(new MethodTemplate(withYear, withYearTemplate));
      add(new MethodTemplate(withWeekyear, withWeekyearTemplate));
      add(new MethodTemplate(withMonthOfYear, withMonthOfYearTemplate));
      add(new MethodTemplate(withWeekOfWeekyear, withWeekOfWeekyearTemplate));
      add(new MethodTemplate(withDayOfYear, withDayOfYearTemplate));
      add(new MethodTemplate(withDayOfMonth, withDayOfMonthTemplate));
      add(new MethodTemplate(withDayOfWeek, withDayOfWeekTemplate));
      add(new MethodTemplate(withHourOfDay, withHourOfDayTemplate));
      add(new MethodTemplate(withMinuteOfHour, withMinuteOfHourTemplate));
      add(new MethodTemplate(withSecondOfMinute, withSecondOfMinuteTemplate));
      add(new MethodTemplate(withMillisOfSecond, withMillisOfSecondTemplate));
      add(new MethodTemplate(withMillisOfDay, withMillisOfDayTemplate));
    }
  };

  public static List<MethodTemplate> getTemplates() {
    return new DateTimeTemplates().templates;
  }
}
