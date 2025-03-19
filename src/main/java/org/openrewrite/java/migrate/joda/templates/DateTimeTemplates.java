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
package org.openrewrite.java.migrate.joda.templates;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.util.ArrayList;
import java.util.List;

import static org.openrewrite.java.migrate.joda.templates.TimeClassNames.*;

@NoArgsConstructor
public class DateTimeTemplates implements Templates {
    private final MethodMatcher newDateTime = new MethodMatcher(JODA_DATE_TIME + "<constructor>()");
    private final MethodMatcher newDateInstant = new MethodMatcher(JODA_DATE_TIME + "<constructor>(java.lang.Object)");
    private final MethodMatcher newDateTimeWithZone = new MethodMatcher(JODA_DATE_TIME + "<constructor>(" + JODA_DATE_TIME_ZONE + ")");
    private final MethodMatcher newDateTimeWithEpoch = new MethodMatcher(JODA_DATE_TIME + "<constructor>(long)");
    private final MethodMatcher newDateTimeWithEpochAndZone = new MethodMatcher(JODA_DATE_TIME + "<constructor>(long, " + JODA_DATE_TIME_ZONE + ")");
    private final MethodMatcher newDateTimeWithEpochObjectAndZone = new MethodMatcher(JODA_DATE_TIME + "<constructor>(java.lang.Object, " + JODA_DATE_TIME_ZONE + ")");
    private final MethodMatcher newDateTimeWithMin = new MethodMatcher(JODA_DATE_TIME + "<constructor>(int, int, int, int, int)");
    private final MethodMatcher newDateTimeWithMinAndZone = new MethodMatcher(JODA_DATE_TIME + "<constructor>(int, int, int, int, int, " + JODA_DATE_TIME_ZONE + ")");
    private final MethodMatcher newDateTimeWithSec = new MethodMatcher(JODA_DATE_TIME + "<constructor>(int, int, int, int, int, int)");
    private final MethodMatcher newDateTimeWithSecAndZone = new MethodMatcher(JODA_DATE_TIME + "<constructor>(int, int, int, int, int, int, " + JODA_DATE_TIME_ZONE + ")");
    private final MethodMatcher newDateTimeWithMillis = new MethodMatcher(JODA_DATE_TIME + "<constructor>(int, int, int, int, int, int, int)");
    private final MethodMatcher newDateTimeWithMillisAndZone = new MethodMatcher(JODA_DATE_TIME + "<constructor>(int, int, int, int, int, int, int, " + JODA_DATE_TIME_ZONE + ")");
    private final MethodMatcher newDateMidnight = new MethodMatcher(JODA_DATE_MIDNIGHT + "<constructor>(int, int, int)");

    private final MethodMatcher dateTimeNow = new MethodMatcher(JODA_DATE_TIME + " now()");
    private final MethodMatcher dateTimeNowWithZone = new MethodMatcher(JODA_DATE_TIME + " now(" + JODA_DATE_TIME_ZONE + ")");
    private final MethodMatcher dateTimeParse = new MethodMatcher(JODA_DATE_TIME + " parse(String)");
    private final MethodMatcher dateTimeParseWithFormatter = new MethodMatcher(JODA_DATE_TIME + " parse(String, " + JODA_TIME_FORMATTER + ")");

    private final MethodMatcher toDateTime = new MethodMatcher(JODA_DATE_TIME + " toDateTime()");
    private final MethodMatcher toDateTimeWithZone = new MethodMatcher(JODA_DATE_TIME + " toDateTime(" + JODA_DATE_TIME_ZONE + ")");
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


    private final JavaTemplate.Builder dateTimeTemplate = JavaTemplate.builder("ZonedDateTime.now()");
    private final JavaTemplate.Builder dateTimeWithZoneTemplate = JavaTemplate.builder("ZonedDateTime.now(#{any(java.time.ZoneOffset)})");
    private final JavaTemplate.Builder dateTimeWithEpochTemplate = JavaTemplate.builder("ZonedDateTime.ofInstant(Instant.ofEpochMilli(#{any(long)}), ZoneId.systemDefault())")
            .imports(JAVA_ZONE_ID, JAVA_INSTANT);
    private final JavaTemplate.Builder dateTimeWithEpochAndZoneTemplate = JavaTemplate.builder("ZonedDateTime.ofInstant(Instant.ofEpochMilli(#{any(long)}), #{any(java.time.ZoneId)})")
            .imports(JAVA_ZONE_ID, JAVA_INSTANT);
    private final JavaTemplate.Builder dateTimeWithMinTemplate = JavaTemplate.builder("ZonedDateTime.of(#{any(int)}, #{any(int)}, #{any(int)}, #{any(int)}, #{any(int)}, 0, 0, ZoneId.systemDefault())")
            .imports(JAVA_ZONE_ID);
    private final JavaTemplate.Builder dateTimeWithMinAndZoneTemplate = JavaTemplate.builder("ZonedDateTime.of(#{any(int)}, #{any(int)}, #{any(int)}, #{any(int)}, #{any(int)}, 0, 0, #{any(java.time.ZoneId)})")
            .imports(JAVA_ZONE_ID);
    private final JavaTemplate.Builder dateTimeWithSecTemplate = JavaTemplate.builder("ZonedDateTime.of(#{any(int)}, #{any(int)}, #{any(int)}, #{any(int)}, #{any(int)}, #{any(int)}, 0, ZoneId.systemDefault())")
            .imports(JAVA_ZONE_ID);
    private final JavaTemplate.Builder dateTimeWithSecAndZoneTemplate = JavaTemplate.builder("ZonedDateTime.of(#{any(int)}, #{any(int)}, #{any(int)}, #{any(int)}, #{any(int)}, #{any(int)}, 0, #{any(java.time.ZoneId)})")
            .imports(JAVA_ZONE_ID);
    private final JavaTemplate.Builder dateTimeWithMillisTemplate = JavaTemplate.builder("ZonedDateTime.of(#{any(int)}, #{any(int)}, #{any(int)}, #{any(int)}, #{any(int)}, #{any(int)}, #{any(int)} * 1_000_000, ZoneId.systemDefault())")
            .imports(JAVA_ZONE_ID);
    private final JavaTemplate.Builder dateTimeWithMillisAndZoneTemplate = JavaTemplate.builder("ZonedDateTime.of(#{any(int)}, #{any(int)}, #{any(int)}, #{any(int)}, #{any(int)}, #{any(int)}, #{any(int)} * 1_000_000, #{any(java.time.ZoneId)})");
    private final JavaTemplate.Builder dateTimeWithYMD = JavaTemplate.builder("ZonedDateTime.of(#{any(int)}, #{any(int)}, #{any(int)}, 0, 0, 0, 0, ZoneId.systemDefault())")
            .imports(JAVA_ZONE_ID);
    private final JavaTemplate.Builder dateTimeParseTemplate = JavaTemplate.builder("ZonedDateTime.parse(#{any(String)})");
    private final JavaTemplate.Builder dateTimeParseWithFormatterTemplate = JavaTemplate.builder("ZonedDateTime.parse(#{any(String)}, #{any(java.time.format.DateTimeFormatter)})");
    private final JavaTemplate.Builder toDateTimeTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}");
    private final JavaTemplate.Builder getMillisTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.toInstant().toEpochMilli()");
    private final JavaTemplate.Builder withMillisTemplate = JavaTemplate.builder("ZonedDateTime.ofInstant(Instant.ofEpochMilli(#{any(long)}),#{any(java.time.ZonedDateTime)}.getZone())")
            .imports(JAVA_DATE_TIME, JAVA_INSTANT);
    private final JavaTemplate.Builder withZoneTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.withZoneSameInstant(#{any(java.time.ZoneId)})");
    private final JavaTemplate.Builder withZoneRetainFieldsTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.withZoneSameLocal(#{any(java.time.ZoneId)})");
    private final JavaTemplate.Builder withEarlierOffsetAtOverlapTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.withEarlierOffsetAtOverlap()");
    private final JavaTemplate.Builder withLaterOffsetAtOverlapTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.withLaterOffsetAtOverlap()");
    private final JavaTemplate.Builder withDateTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.withYear(#{any(int)}).withMonth(#{any(int)}).withDayOfMonth(#{any(int)})");
    private final JavaTemplate.Builder withTemporalAdjusterTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.with(#{any(java.time.temporal.TemporalAdjuster)})")
            .imports(JAVA_TEMPORAL_ADJUSTER);
    private final JavaTemplate.Builder withTimeTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.withHour(#{any(int)}).withMinute(#{any(int)}).withSecond(#{any(int)}).withNano(#{any(int)} * 1_000_000)");
    private final JavaTemplate.Builder withTimeAtStartOfDayTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.toLocalDate().atStartOfDay(#{any(java.time.ZonedDateTime)}.getZone())");
    private final JavaTemplate.Builder withDurationAddedTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.plus(Duration.ofMillis(#{any(long)}).multipliedBy(#{any(int)}))")
            .imports(JAVA_DURATION);
    private final JavaTemplate.Builder plusReadableDurationTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.plus(#{any(java.time.Duration)})")
            .imports(JAVA_DURATION);
    private final JavaTemplate.Builder plusYearsTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.plusYears(#{any(int)})");
    private final JavaTemplate.Builder plusMonthsTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.plusMonths(#{any(int)})");
    private final JavaTemplate.Builder plusWeeksTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.plusWeeks(#{any(int)})");
    private final JavaTemplate.Builder plusDaysTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.plusDays(#{any(int)})");
    private final JavaTemplate.Builder plusHoursTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.plusHours(#{any(int)})");
    private final JavaTemplate.Builder plusMinutesTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.plusMinutes(#{any(int)})");
    private final JavaTemplate.Builder plusSecondsTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.plusSeconds(#{any(int)})");
    private final JavaTemplate.Builder plusMillisTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.plus(Duration.ofMillis(#{any(int)}))")
            .imports(JAVA_DURATION);
    private final JavaTemplate.Builder minusMillisTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.minus(Duration.ofMillis(#{any(int)}))")
            .imports(JAVA_DURATION);
    private final JavaTemplate.Builder minusReadableDurationTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.minus(#{any(java.time.Duration)})")
            .imports(JAVA_DATE_TIME, JAVA_DURATION);
    private final JavaTemplate.Builder minusYearsTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.minusYears(#{any(int)})");
    private final JavaTemplate.Builder minusMonthsTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.minusMonths(#{any(int)})");
    private final JavaTemplate.Builder minusWeeksTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.minusWeeks(#{any(int)})");
    private final JavaTemplate.Builder minusDaysTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.minusDays(#{any(int)})");
    private final JavaTemplate.Builder minusHoursTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.minusHours(#{any(int)})");
    private final JavaTemplate.Builder minusMinutesTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.minusMinutes(#{any(int)})");
    private final JavaTemplate.Builder minusSecondsTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.minusSeconds(#{any(int)})");
    private final JavaTemplate.Builder toLocalDateTimeTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.toLocalDateTime()");
    private final JavaTemplate.Builder toLocalDateTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.toLocalDate()");
    private final JavaTemplate.Builder toLocalTimeTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.toLocalTime()");
    private final JavaTemplate.Builder withYearTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.withYear(#{any(int)})");
    private final JavaTemplate.Builder withWeekyearTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.with(IsoFields.WEEK_BASED_YEAR, #{any(int)})")
            .imports(JAVA_TEMPORAL_ISO_FIELDS);
    private final JavaTemplate.Builder withMonthOfYearTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.withMonth(#{any(int)})");
    private final JavaTemplate.Builder withWeekOfWeekyearTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.with(ChronoField.ALIGNED_WEEK_OF_YEAR, #{any(int)})")
            .imports(JAVA_CHRONO_FIELD);
    private final JavaTemplate.Builder withDayOfYearTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.withDayOfYear(#{any(int)})");
    private final JavaTemplate.Builder withDayOfMonthTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.withDayOfMonth(#{any(int)})");
    private final JavaTemplate.Builder withDayOfWeekTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.with(ChronoField.DAY_OF_WEEK, #{any(int)})")
            .imports(JAVA_CHRONO_FIELD);
    private final JavaTemplate.Builder withHourOfDayTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.withHour(#{any(int)})");
    private final JavaTemplate.Builder withMinuteOfHourTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.withMinute(#{any(int)})");
    private final JavaTemplate.Builder withSecondOfMinuteTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.withSecond(#{any(int)})");
    private final JavaTemplate.Builder withMillisOfSecondTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.withNano(#{any(int)} * 1_000_000)");
    private final JavaTemplate.Builder withMillisOfDayTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.with(ChronoField.MILLI_OF_DAY, #{any(int)})")
            .imports(JAVA_CHRONO_FIELD);

    @Getter
    private final List<MethodTemplate> templates = new ArrayList<MethodTemplate>() {
        {
            add(new MethodTemplate(newDateTime, build(dateTimeTemplate)));
            add(new MethodTemplate(newDateInstant, JODA_MULTIPLE_MAPPING_POSSIBLE_TEMPLATE));
            add(new MethodTemplate(newDateTimeWithZone, build(dateTimeWithZoneTemplate)));
            add(new MethodTemplate(newDateTimeWithEpoch, build(dateTimeWithEpochTemplate)));
            add(new MethodTemplate(newDateTimeWithEpochAndZone, build(dateTimeWithEpochAndZoneTemplate)));
            add(new MethodTemplate(newDateTimeWithEpochObjectAndZone, build(dateTimeWithEpochAndZoneTemplate)));
            add(new MethodTemplate(newDateTimeWithMin, build(dateTimeWithMinTemplate)));
            add(new MethodTemplate(newDateTimeWithMinAndZone, build(dateTimeWithMinAndZoneTemplate)));
            add(new MethodTemplate(newDateTimeWithSec, build(dateTimeWithSecTemplate)));
            add(new MethodTemplate(newDateTimeWithSecAndZone, build(dateTimeWithSecAndZoneTemplate)));
            add(new MethodTemplate(newDateTimeWithMillis, build(dateTimeWithMillisTemplate)));
            add(new MethodTemplate(newDateTimeWithMillisAndZone, build(dateTimeWithMillisAndZoneTemplate)));
            add(new MethodTemplate(newDateMidnight, build(dateTimeWithYMD)));

            add(new MethodTemplate(dateTimeNow, build(dateTimeTemplate)));
            add(new MethodTemplate(dateTimeNowWithZone, build(dateTimeWithZoneTemplate)));
            add(new MethodTemplate(dateTimeParse, build(dateTimeParseTemplate)));
            add(new MethodTemplate(dateTimeParseWithFormatter, build(dateTimeParseWithFormatterTemplate)));
            add(new MethodTemplate(toDateTime, build(toDateTimeTemplate)));
            add(new MethodTemplate(toDateTimeWithZone, build(withZoneTemplate)));
            add(new MethodTemplate(withMillis, build(withMillisTemplate), m -> {
                J.MethodInvocation mi = (J.MethodInvocation) m;
                return new Expression[]{mi.getArguments().get(0), mi.getSelect()};
            }));
            add(new MethodTemplate(withZone, build(withZoneTemplate)));
            add(new MethodTemplate(withZoneRetainFields, build(withZoneRetainFieldsTemplate)));
            add(new MethodTemplate(withEarlierOffsetAtOverlap, build(withEarlierOffsetAtOverlapTemplate)));
            add(new MethodTemplate(withLaterOffsetAtOverlap, build(withLaterOffsetAtOverlapTemplate)));
            add(new MethodTemplate(withDate, build(withDateTemplate)));
            add(new MethodTemplate(withDateLocalDate, build(withTemporalAdjusterTemplate)));
            add(new MethodTemplate(withTime, build(withTimeTemplate)));
            add(new MethodTemplate(withTimeLocalTime, build(withTemporalAdjusterTemplate)));
            add(new MethodTemplate(withTimeAtStartOfDay, build(withTimeAtStartOfDayTemplate), m -> {
                J.MethodInvocation mi = (J.MethodInvocation) m;
                return new Expression[]{mi.getSelect(), mi.getSelect()};
            }));
            add(new MethodTemplate(withDurationAdded, build(withDurationAddedTemplate)));
            add(new MethodTemplate(plusLong, build(plusMillisTemplate)));
            add(new MethodTemplate(plusReadableDuration, build(plusReadableDurationTemplate)));
            add(new MethodTemplate(plusYears, build(plusYearsTemplate)));
            add(new MethodTemplate(plusMonths, build(plusMonthsTemplate)));
            add(new MethodTemplate(plusWeeks, build(plusWeeksTemplate)));
            add(new MethodTemplate(plusDays, build(plusDaysTemplate)));
            add(new MethodTemplate(plusHours, build(plusHoursTemplate)));
            add(new MethodTemplate(plusMinutes, build(plusMinutesTemplate)));
            add(new MethodTemplate(plusSeconds, build(plusSecondsTemplate)));
            add(new MethodTemplate(plusMillis, build(plusMillisTemplate)));
            add(new MethodTemplate(minusLong, build(minusMillisTemplate)));
            add(new MethodTemplate(minusReadableDuration, build(minusReadableDurationTemplate)));
            add(new MethodTemplate(minusYears, build(minusYearsTemplate)));
            add(new MethodTemplate(minusMonths, build(minusMonthsTemplate)));
            add(new MethodTemplate(minusWeeks, build(minusWeeksTemplate)));
            add(new MethodTemplate(minusDays, build(minusDaysTemplate)));
            add(new MethodTemplate(minusHours, build(minusHoursTemplate)));
            add(new MethodTemplate(minusMinutes, build(minusMinutesTemplate)));
            add(new MethodTemplate(minusSeconds, build(minusSecondsTemplate)));
            add(new MethodTemplate(minusMillis, build(minusMillisTemplate)));
            add(new MethodTemplate(toLocalDateTime, build(toLocalDateTimeTemplate)));
            add(new MethodTemplate(toLocalDate, build(toLocalDateTemplate)));
            add(new MethodTemplate(toLocalTime, build(toLocalTimeTemplate)));
            add(new MethodTemplate(withYear, build(withYearTemplate)));
            add(new MethodTemplate(withWeekyear, build(withWeekyearTemplate)));
            add(new MethodTemplate(withMonthOfYear, build(withMonthOfYearTemplate)));
            add(new MethodTemplate(withWeekOfWeekyear, build(withWeekOfWeekyearTemplate)));
            add(new MethodTemplate(withDayOfYear, build(withDayOfYearTemplate)));
            add(new MethodTemplate(withDayOfMonth, build(withDayOfMonthTemplate)));
            add(new MethodTemplate(withDayOfWeek, build(withDayOfWeekTemplate)));
            add(new MethodTemplate(withHourOfDay, build(withHourOfDayTemplate)));
            add(new MethodTemplate(withMinuteOfHour, build(withMinuteOfHourTemplate)));
            add(new MethodTemplate(withSecondOfMinute, build(withSecondOfMinuteTemplate)));
            add(new MethodTemplate(withMillisOfSecond, build(withMillisOfSecondTemplate)));
            add(new MethodTemplate(withMillisOfDay, build(withMillisOfDayTemplate)));
        }
    };

    private JavaTemplate build(JavaTemplate.Builder builder) {
        return buildWithImport(builder, JAVA_DATE_TIME);
    }
}
