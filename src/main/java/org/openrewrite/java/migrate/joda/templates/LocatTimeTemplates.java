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
public class LocatTimeTemplates implements Templates {
    final MethodMatcher newLocalTimeNoArgs = new MethodMatcher(JODA_LOCAL_TIME + "<constructor>()");
    final MethodMatcher newLocalTimeString = new MethodMatcher(JODA_LOCAL_TIME + "<constructor>(Object)");
    final MethodMatcher newLocalTimeWithInstanceAndTimeZone = new MethodMatcher(JODA_LOCAL_TIME + "<constructor>(java.lang.Object,org.joda.time.DateTimeZone)");
    final MethodMatcher newLocalTimeHm = new MethodMatcher(JODA_LOCAL_TIME + "<constructor>(int,int)");
    final MethodMatcher newLocalTimeHms = new MethodMatcher(JODA_LOCAL_TIME + "<constructor>(int,int,int)");
    final MethodMatcher newLocalTimeHmsM = new MethodMatcher(JODA_LOCAL_TIME + "<constructor>(int,int,int,int)");

    final MethodMatcher fromMillisOfDay = new MethodMatcher(JODA_LOCAL_TIME + " fromMillisOfDay(long)");
    final MethodMatcher now = new MethodMatcher(JODA_LOCAL_TIME + " now()");

    final MethodMatcher toDateTimeToday = new MethodMatcher(JODA_LOCAL_TIME + " toDateTimeToday()");
    final MethodMatcher toString = new MethodMatcher(JODA_LOCAL_TIME + " toString()");

    final MethodMatcher getHourOfDay = new MethodMatcher(JODA_LOCAL_TIME + " getHourOfDay()");
    final MethodMatcher getMinuteOfHour = new MethodMatcher(JODA_LOCAL_TIME + " getMinuteOfHour()");
    final MethodMatcher getSecondOfMinute = new MethodMatcher(JODA_LOCAL_TIME + " getSecondOfMinute()");
    final MethodMatcher getMillisOfDay = new MethodMatcher(JODA_LOCAL_TIME + " getMillisOfDay()");

    final MethodMatcher parse = new MethodMatcher(JODA_LOCAL_TIME + " parse(String)");
    final MethodMatcher plusMinutes = new MethodMatcher(JODA_LOCAL_TIME + " plusMinutes(int)");
    final MethodMatcher plusSeconds = new MethodMatcher(JODA_LOCAL_TIME + " plusSeconds(int)");
    final MethodMatcher plusMillis = new MethodMatcher(JODA_LOCAL_TIME + " plusMillis(int)");
    final MethodMatcher minusMinutes = new MethodMatcher(JODA_LOCAL_TIME + " minusMinutes(int)");
    final MethodMatcher minusSeconds = new MethodMatcher(JODA_LOCAL_TIME + " minusSeconds(int)");
    final MethodMatcher minusMillis = new MethodMatcher(JODA_LOCAL_TIME + " minusMillis(int)");

    final MethodMatcher equals = new MethodMatcher(JODA_LOCAL_TIME + " equals(java.lang.Object)");

    final JavaTemplate.Builder localTimeNoArgsTemplate = JavaTemplate.builder("LocalTime.now()");
    final JavaTemplate.Builder localTimeStringTemplate = JavaTemplate.builder("LocalTime.parse(#{any(Object)})");
    final JavaTemplate.Builder localTimeOfInstantTemplate = JavaTemplate.builder("LocalTime.ofInstant(#{any(Object)}, #{any(java.time.ZoneId)})");
    final JavaTemplate.Builder localTimeHmTemplate = JavaTemplate.builder("LocalTime.of(#{any(int)}, #{any(int)})");
    final JavaTemplate.Builder localTimeHmsTemplate = JavaTemplate.builder("LocalTime.of(#{any(int)}, #{any(int)}, #{any(int)})");
    final JavaTemplate.Builder localTimeHmsMTemplate = JavaTemplate.builder("LocalTime.of(#{any(int)}, #{any(int)}, #{any(int)}, #{any(int)})");

    final JavaTemplate.Builder ofSecondOfDayTemplate = JavaTemplate.builder("LocalTime.ofSecondOfDay(#{any(long)} / 1000)");
    final JavaTemplate.Builder nowTemplate = JavaTemplate.builder("LocalTime.now()");

    final JavaTemplate.Builder toZonedDateTimeStartOfDayTemplate = JavaTemplate.builder("#{any(java.time.LocalTime)}.atStartOfDay(ZoneId.systemDefault())");
    final JavaTemplate.Builder toStringTemplate = JavaTemplate.builder("#{any(java.time.LocalTime)}.toString()");

    final JavaTemplate.Builder getHourTemplate = JavaTemplate.builder("#{any(java.time.LocalTime)}.getHour()");
    final JavaTemplate.Builder getMinuteTemplate = JavaTemplate.builder("#{any(java.time.LocalTime)}.getMinute()");
    final JavaTemplate.Builder getSecondTemplate = JavaTemplate.builder("#{any(java.time.LocalTime)}.getSecond()");
    final JavaTemplate.Builder getMilliOfDayTemplate = JavaTemplate.builder("#{any(java.time.LocalTime)}.get(ChronoField.MILLI_OF_DAY)")
            .imports(JAVA_CHRONO_FIELD);

    final JavaTemplate.Builder parseTemplate = JavaTemplate.builder("LocalTime.parse(#{any(String)})");
    final JavaTemplate.Builder plusMinutesTemplate = JavaTemplate.builder("#{any(java.time.LocalTime)}.plusMinutes(#{any(int)})");
    final JavaTemplate.Builder plusSecondsTemplate = JavaTemplate.builder("#{any(java.time.LocalTime)}.plusSeconds(#{any(int)})");
    final JavaTemplate.Builder plusMillisTemplate = JavaTemplate.builder("#{any(java.time.LocalTime)}.plusNanos(#{any(int)})");
    final JavaTemplate.Builder minusMinutesTemplate = JavaTemplate.builder("#{any(java.time.LocalTime)}.minusMinutes(#{any(int)})");
    final JavaTemplate.Builder minusSecondsTemplate = JavaTemplate.builder("#{any(java.time.LocalTime)}.minusSeconds(#{any(int)})");
    final JavaTemplate.Builder minusMillisTemplate = JavaTemplate.builder("#{any(java.time.LocalTime)}.minusNanos(#{any(int)})");

    @Getter
    private final List<MethodTemplate> templates = new ArrayList<MethodTemplate>() {
        {
            add(new MethodTemplate(newLocalTimeNoArgs, build(localTimeNoArgsTemplate)));
            add(new MethodTemplate(newLocalTimeString, build(localTimeStringTemplate)));
            add(new MethodTemplate(newLocalTimeWithInstanceAndTimeZone, build(localTimeOfInstantTemplate)));
            add(new MethodTemplate(newLocalTimeHm, build(localTimeHmTemplate)));
            add(new MethodTemplate(newLocalTimeHms, build(localTimeHmsTemplate)));
            add(new MethodTemplate(newLocalTimeHmsM, build(localTimeHmsMTemplate)));

            add(new MethodTemplate(fromMillisOfDay, build(ofSecondOfDayTemplate)));
            add(new MethodTemplate(now, build(nowTemplate)));

            add(new MethodTemplate(toDateTimeToday, build(toZonedDateTimeStartOfDayTemplate)));
            add(new MethodTemplate(toString, build(toStringTemplate)));

            add(new MethodTemplate(getHourOfDay, build(getHourTemplate)));
            add(new MethodTemplate(getMinuteOfHour, build(getMinuteTemplate)));
            add(new MethodTemplate(getSecondOfMinute, build(getSecondTemplate)));
            add(new MethodTemplate(getMillisOfDay, build(getMilliOfDayTemplate)));

            add(new MethodTemplate(parse, build(parseTemplate)));
            add(new MethodTemplate(plusMinutes, build(plusMinutesTemplate)));
            add(new MethodTemplate(plusSeconds, build(plusSecondsTemplate)));
            add(new MethodTemplate(plusMillis, build(plusMillisTemplate)));
            add(new MethodTemplate(minusMinutes, build(minusMinutesTemplate)));
            add(new MethodTemplate(minusSeconds, build(minusSecondsTemplate)));
            add(new MethodTemplate(minusMillis, build(minusMillisTemplate)));

            add(new MethodTemplate(equals, JODA_MULTIPLE_MAPPING_POSSIBLE_TEMPLATE));
       }
    };

    private JavaTemplate build(JavaTemplate.Builder builder) {
        return buildWithImport(builder, JAVA_LOCAL_DATE);
    }
}
