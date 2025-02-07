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
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;

import java.util.ArrayList;
import java.util.List;

import static org.openrewrite.java.migrate.joda.templates.TimeClassNames.JAVA_DATE_TIME;
import static org.openrewrite.java.migrate.joda.templates.TimeClassNames.JODA_ABSTRACT_DATE_TIME;

public class AbstractDateTimeTemplates implements Templates {
    private final MethodMatcher getDayOfMonth = new MethodMatcher(JODA_ABSTRACT_DATE_TIME + " getDayOfMonth()");
    private final MethodMatcher getDayOfWeek = new MethodMatcher(JODA_ABSTRACT_DATE_TIME + " getDayOfWeek()");
    private final MethodMatcher getHourOfDay = new MethodMatcher(JODA_ABSTRACT_DATE_TIME + " getHourOfDay()");
    private final MethodMatcher getMillisOfSecond = new MethodMatcher(JODA_ABSTRACT_DATE_TIME + " getMillisOfSecond()");
    private final MethodMatcher getMinuteOfDay = new MethodMatcher(JODA_ABSTRACT_DATE_TIME + " getMinuteOfDay()");
    private final MethodMatcher getMinuteOfHour = new MethodMatcher(JODA_ABSTRACT_DATE_TIME + " getMinuteOfHour()");
    private final MethodMatcher getMonthOfYear = new MethodMatcher(JODA_ABSTRACT_DATE_TIME + " getMonthOfYear()");
    private final MethodMatcher getSecondOfDay = new MethodMatcher(JODA_ABSTRACT_DATE_TIME + " getSecondOfDay()");
    private final MethodMatcher getSecondOfMinute = new MethodMatcher(JODA_ABSTRACT_DATE_TIME + " getSecondOfMinute()");
    private final MethodMatcher getWeekOfWeekyear = new MethodMatcher(JODA_ABSTRACT_DATE_TIME + " getWeekOfWeekyear()");
    private final MethodMatcher toString = new MethodMatcher(JODA_ABSTRACT_DATE_TIME + " toString()");

    private final JavaTemplate getDayOfMonthTemplate = JavaTemplate.builder("#{any(" + JAVA_DATE_TIME + ")}.getDayOfMonth()").build();
    private final JavaTemplate getDayOfWeekTemplate = JavaTemplate.builder("#{any(" + JAVA_DATE_TIME + ")}.getDayOfWeek().getValue()").build();
    private final JavaTemplate getHourOfDayTemplate = JavaTemplate.builder("#{any(" + JAVA_DATE_TIME + ")}.getHour()").build();
    private final JavaTemplate getMillisOfSecondTemplate = JavaTemplate.builder("#{any(" + JAVA_DATE_TIME + ")}.get(ChronoField.MILLI_OF_SECOND)").imports("java.time.temporal.ChronoField").build();
    private final JavaTemplate getMinuteOfDayTemplate = JavaTemplate.builder("#{any(" + JAVA_DATE_TIME + ")}.get(ChronoField.MINUTE_OF_DAY)").imports("java.time.temporal.ChronoField").build();
    private final JavaTemplate getMinuteOfHourTemplate = JavaTemplate.builder("#{any(" + JAVA_DATE_TIME + ")}.getMinute()").build();
    private final JavaTemplate getMonthOfYearTemplate = JavaTemplate.builder("#{any(" + JAVA_DATE_TIME + ")}.getMonthValue()").build();
    private final JavaTemplate getSecondOfDayTemplate = JavaTemplate.builder("#{any(" + JAVA_DATE_TIME + ")}.get(ChronoField.SECOND_OF_DAY)").imports("java.time.temporal.ChronoField").build();
    private final JavaTemplate getSecondOfMinuteTemplate = JavaTemplate.builder("#{any(" + JAVA_DATE_TIME + ")}.getSecond()").build();
    private final JavaTemplate getWeekOfWeekyearTemplate = JavaTemplate.builder("#{any(" + JAVA_DATE_TIME + ")}.get(ChronoField.ALIGNED_WEEK_OF_YEAR)").imports("java.time.temporal.ChronoField").build();
    private final JavaTemplate toStringTemplate = JavaTemplate.builder("#{any(" + JAVA_DATE_TIME + ")}.toString()").build();

    @Getter
    private final List<MethodTemplate> templates;
    {
        templates = new ArrayList<>();
        templates.add(new MethodTemplate(getDayOfMonth, getDayOfMonthTemplate));
        templates.add(new MethodTemplate(getDayOfWeek, getDayOfWeekTemplate));
        templates.add(new MethodTemplate(getHourOfDay, getHourOfDayTemplate));
        templates.add(new MethodTemplate(getMillisOfSecond, getMillisOfSecondTemplate));
        templates.add(new MethodTemplate(getMinuteOfDay, getMinuteOfDayTemplate));
        templates.add(new MethodTemplate(getMinuteOfHour, getMinuteOfHourTemplate));
        templates.add(new MethodTemplate(getMonthOfYear, getMonthOfYearTemplate));
        templates.add(new MethodTemplate(getSecondOfDay, getSecondOfDayTemplate));
        templates.add(new MethodTemplate(getSecondOfMinute, getSecondOfMinuteTemplate));
        templates.add(new MethodTemplate(getWeekOfWeekyear, getWeekOfWeekyearTemplate));
        templates.add(new MethodTemplate(toString, toStringTemplate));
    }
}
