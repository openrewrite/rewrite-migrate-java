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

import java.util.ArrayList;
import java.util.List;

import static org.openrewrite.java.migrate.joda.templates.TimeClassNames.*;

@NoArgsConstructor
public class LocatDateTimeTemplates implements Templates {
    final MethodMatcher newLocalDateTimeNoArgs = new MethodMatcher(JODA_LOCAL_DATE_TIME + "<constructor>()");
    final MethodMatcher newLocalDateTimeEpoch = new MethodMatcher(JODA_LOCAL_DATE_TIME + "<constructor>(long)");
    final MethodMatcher newLocalDateTimeYmdHm = new MethodMatcher(JODA_LOCAL_DATE_TIME + "<constructor>(int,int,int,int,int)");
    final MethodMatcher newLocalDateTimeYmdHms = new MethodMatcher(JODA_LOCAL_DATE_TIME + "<constructor>(int,int,int,int,int,int)");
    final MethodMatcher newLocalDateTimeYmdHmsMillis = new MethodMatcher(JODA_LOCAL_DATE_TIME + "<constructor>(int,int,int,int,int,int,int)");
    final MethodMatcher now = new MethodMatcher(JODA_LOCAL_DATE_TIME + " now()");
    final MethodMatcher parse = new MethodMatcher(JODA_LOCAL_DATE_TIME + " parse(String)");
    final MethodMatcher parseWithFormatter = new MethodMatcher(JODA_LOCAL_DATE_TIME + " parse(String, org.joda.time.format.DateTimeFormatter)");
    final MethodMatcher plusYears = new MethodMatcher(JODA_LOCAL_DATE_TIME + " plusYears(int)");
    final MethodMatcher plusMonths = new MethodMatcher(JODA_LOCAL_DATE_TIME + " plusMonths(int)");
    final MethodMatcher plusDays = new MethodMatcher(JODA_LOCAL_DATE_TIME + " plusDays(int)");
    final MethodMatcher plusHours = new MethodMatcher(JODA_LOCAL_DATE_TIME + " plusHours(int)");
    final MethodMatcher plusMinutes = new MethodMatcher(JODA_LOCAL_DATE_TIME + " plusMinutes(int)");
    final MethodMatcher plusSeconds = new MethodMatcher(JODA_LOCAL_DATE_TIME + " plusSeconds(int)");
    final MethodMatcher plusMillis = new MethodMatcher(JODA_LOCAL_DATE_TIME + " plusMillis(int)");
    final MethodMatcher minusYears = new MethodMatcher(JODA_LOCAL_DATE_TIME + " minusYears(int)");
    final MethodMatcher minusMonths = new MethodMatcher(JODA_LOCAL_DATE_TIME + " minusMonths(int)");
    final MethodMatcher minusDays = new MethodMatcher(JODA_LOCAL_DATE_TIME + " minusDays(int)");
    final MethodMatcher minusHours = new MethodMatcher(JODA_LOCAL_DATE_TIME + " minusHours(int)");
    final MethodMatcher minusMinutes = new MethodMatcher(JODA_LOCAL_DATE_TIME + " minusMinutes(int)");
    final MethodMatcher minusSeconds = new MethodMatcher(JODA_LOCAL_DATE_TIME + " minusSeconds(int)");
    final MethodMatcher minusMillis = new MethodMatcher(JODA_LOCAL_DATE_TIME + " minusMillis(int)");

    final JavaTemplate.Builder newLocalDateTimeNoArgsTemplate = JavaTemplate.builder("LocalDateTime.now()");
    final JavaTemplate.Builder newLocalDateTimeEpochTemplate = JavaTemplate.builder("LocalDateTime.ofInstant(Instant.ofEpochMilli(#{any(long)}), ZoneId.systemDefault())")
            .imports(JAVA_INSTANT, JAVA_ZONE_ID);
    final JavaTemplate.Builder newLocalDateTimeYmdHmTemplate = JavaTemplate.builder("LocalDateTime.of(#{any(int)}, #{any(int)}, #{any(int)}, #{any(int)}, #{any(int)})");
    final JavaTemplate.Builder newLocalDateTimeYmdHmsTemplate = JavaTemplate.builder("LocalDateTime.of(#{any(int)}, #{any(int)}, #{any(int)}, #{any(int)}, #{any(int)}, #{any(int)})");
    final JavaTemplate.Builder newLocalDateTimeYmdHmsMillisTemplate = JavaTemplate.builder("LocalDateTime.of(#{any(int)}, #{any(int)}, #{any(int)}, #{any(int)}, #{any(int)}, #{any(int)}, #{any(int)})");
    final JavaTemplate.Builder nowTemplate = JavaTemplate.builder("LocalDateTime.now()");
    final JavaTemplate.Builder parseTemplate = JavaTemplate.builder("LocalDateTime.parse(#{any(String)})");
    final JavaTemplate.Builder parseWithFormatterTemplate = JavaTemplate.builder("LocalDateTime.parse(#{any(String)}, #{any(java.time.format.DateTimeFormatter)})");
    final JavaTemplate.Builder plusYearsTemplate = JavaTemplate.builder("#{any(java.time.LocalDateTime)}.plusYears(#{any(int)})");
    final JavaTemplate.Builder plusMonthsTemplate = JavaTemplate.builder("#{any(java.time.LocalDateTime)}.plusMonths(#{any(int)})");
    final JavaTemplate.Builder plusDaysTemplate = JavaTemplate.builder("#{any(java.time.LocalDateTime)}.plusDays(#{any(int)})");
    final JavaTemplate.Builder plusHoursTemplate = JavaTemplate.builder("#{any(java.time.LocalDateTime)}.plusHours(#{any(int)})");
    final JavaTemplate.Builder plusMinutesTemplate = JavaTemplate.builder("#{any(java.time.LocalDateTime)}.plusMinutes(#{any(int)})");
    final JavaTemplate.Builder plusSecondsTemplate = JavaTemplate.builder("#{any(java.time.LocalDateTime)}.plusSeconds(#{any(int)})");
    final JavaTemplate.Builder plusMillisTemplate = JavaTemplate.builder("#{any(java.time.LocalDateTime)}.plusNanos(#{any(int)} * 1_000_000L)");
    final JavaTemplate.Builder minusYearsTemplate = JavaTemplate.builder("#{any(java.time.LocalDateTime)}.minusYears(#{any(int)})");
    final JavaTemplate.Builder minusMonthsTemplate = JavaTemplate.builder("#{any(java.time.LocalDateTime)}.minusMonths(#{any(int)})");
    final JavaTemplate.Builder minusDaysTemplate = JavaTemplate.builder("#{any(java.time.LocalDateTime)}.minusDays(#{any(int)})");
    final JavaTemplate.Builder minusHoursTemplate = JavaTemplate.builder("#{any(java.time.LocalDateTime)}.minusHours(#{any(int)})");
    final JavaTemplate.Builder minusMinutesTemplate = JavaTemplate.builder("#{any(java.time.LocalDateTime)}.minusMinutes(#{any(int)})");
    final JavaTemplate.Builder minusSecondsTemplate = JavaTemplate.builder("#{any(java.time.LocalDateTime)}.minusSeconds(#{any(int)})");
    final JavaTemplate.Builder minusMillisTemplate = JavaTemplate.builder("#{any(java.time.LocalDateTime)}.minusNanos(#{any(int)} * 1_000_000L)");

    @Getter
    private final List<MethodTemplate> templates = new ArrayList<MethodTemplate>() {
        {
            add(new MethodTemplate(newLocalDateTimeNoArgs, build(newLocalDateTimeNoArgsTemplate)));
            add(new MethodTemplate(newLocalDateTimeEpoch, build(newLocalDateTimeEpochTemplate)));
            add(new MethodTemplate(newLocalDateTimeYmdHm, build(newLocalDateTimeYmdHmTemplate)));
            add(new MethodTemplate(newLocalDateTimeYmdHms, build(newLocalDateTimeYmdHmsTemplate)));
            add(new MethodTemplate(newLocalDateTimeYmdHmsMillis, build(newLocalDateTimeYmdHmsMillisTemplate)));
            add(new MethodTemplate(now, build(nowTemplate)));
            add(new MethodTemplate(parse, build(parseTemplate)));
            add(new MethodTemplate(parseWithFormatter, build(parseWithFormatterTemplate)));
            add(new MethodTemplate(plusYears, build(plusYearsTemplate)));
            add(new MethodTemplate(plusMonths, build(plusMonthsTemplate)));
            add(new MethodTemplate(plusDays, build(plusDaysTemplate)));
            add(new MethodTemplate(plusHours, build(plusHoursTemplate)));
            add(new MethodTemplate(plusMinutes, build(plusMinutesTemplate)));
            add(new MethodTemplate(plusSeconds, build(plusSecondsTemplate)));
            add(new MethodTemplate(plusMillis, build(plusMillisTemplate)));
            add(new MethodTemplate(minusYears, build(minusYearsTemplate)));
            add(new MethodTemplate(minusMonths, build(minusMonthsTemplate)));
            add(new MethodTemplate(minusDays, build(minusDaysTemplate)));
            add(new MethodTemplate(minusHours, build(minusHoursTemplate)));
            add(new MethodTemplate(minusMinutes, build(minusMinutesTemplate)));
            add(new MethodTemplate(minusSeconds, build(minusSecondsTemplate)));
            add(new MethodTemplate(minusMillis, build(minusMillisTemplate)));
        }
    };

    private JavaTemplate build(JavaTemplate.Builder builder) {
        return buildWithImport(builder, JODA_LOCAL_DATE_TIME);
    }
}
