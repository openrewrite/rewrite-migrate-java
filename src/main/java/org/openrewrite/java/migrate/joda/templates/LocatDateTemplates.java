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
public class LocatDateTemplates implements Templates {
    final MethodMatcher newLocalDateNoArgs = new MethodMatcher(JODA_LOCAL_DATE + "<constructor>()");
    final MethodMatcher newLocalDateEpoch = new MethodMatcher(JODA_LOCAL_DATE + "<constructor>(long)");
    final MethodMatcher newLocalDateYmd = new MethodMatcher(JODA_LOCAL_DATE + "<constructor>(int,int,int)");
    final MethodMatcher now = new MethodMatcher(JODA_LOCAL_DATE + " now()");
    final MethodMatcher parse = new MethodMatcher(JODA_LOCAL_DATE + " parse(String)");
    final MethodMatcher parseWithFormatter = new MethodMatcher(JODA_LOCAL_DATE + " parse(String, org.joda.time.format.DateTimeFormatter)");
    final MethodMatcher plusDays = new MethodMatcher(JODA_LOCAL_DATE + " plusDays(int)");
    final MethodMatcher plusWeeks = new MethodMatcher(JODA_LOCAL_DATE + " plusWeeks(int)");
    final MethodMatcher plusMonths = new MethodMatcher(JODA_LOCAL_DATE + " plusMonths(int)");
    final MethodMatcher plusYears = new MethodMatcher(JODA_LOCAL_DATE + " plusYears(int)");
    final MethodMatcher minusDays = new MethodMatcher(JODA_LOCAL_DATE + " minusDays(int)");
    final MethodMatcher minusWeeks = new MethodMatcher(JODA_LOCAL_DATE + " minusWeeks(int)");
    final MethodMatcher minusMonths = new MethodMatcher(JODA_LOCAL_DATE + " minusMonths(int)");
    final MethodMatcher minusYears = new MethodMatcher(JODA_LOCAL_DATE + " minusYears(int)");
    final MethodMatcher toDateMidnight = new MethodMatcher(JODA_LOCAL_DATE + " toDateMidnight()");
    final MethodMatcher toDateTimeAtStartOfDay = new MethodMatcher(JODA_LOCAL_DATE + " toDateTimeAtStartOfDay()");
    final MethodMatcher toDateTimeAtStartOfDayWithZone = new MethodMatcher(JODA_LOCAL_DATE + " toDateTimeAtStartOfDay(org.joda.time.DateTimeZone)");

    final JavaTemplate.Builder localDateNoArgsTemplate = JavaTemplate.builder("LocalDate.now()");
    final JavaTemplate.Builder localDateEpochTemplate = JavaTemplate.builder("LocalDate.ofEpochDay(#{any(long)})");
    final JavaTemplate.Builder localDateYmdTemplate = JavaTemplate.builder("LocalDate.of(#{any(int)}, #{any(int)}, #{any(int)})");
    final JavaTemplate.Builder nowTemplate = JavaTemplate.builder("LocalDate.now()");
    final JavaTemplate.Builder parseTemplate = JavaTemplate.builder("LocalDate.parse(#{any(String)})");
    final JavaTemplate.Builder parseWithFormatterTemplate = JavaTemplate.builder("LocalDate.parse(#{any(String)}, #{any(java.time.format.DateTimeFormatter)})")
            .imports(JAVA_TIME_FORMATTER);
    final JavaTemplate.Builder plusDaysTemplate = JavaTemplate.builder("#{any(java.time.LocalDate)}.plusDays(#{any(int)})");
    final JavaTemplate.Builder plusWeeksTemplate = JavaTemplate.builder("#{any(java.time.LocalDate)}.plusWeeks(#{any(int)})");
    final JavaTemplate.Builder plusMonthsTemplate = JavaTemplate.builder("#{any(java.time.LocalDate)}.plusMonths(#{any(int)})");
    final JavaTemplate.Builder plusYearsTemplate = JavaTemplate.builder("#{any(java.time.LocalDate)}.plusYears(#{any(int)})");
    final JavaTemplate.Builder minusDaysTemplate = JavaTemplate.builder("#{any(java.time.LocalDate)}.minusDays(#{any(int)})");
    final JavaTemplate.Builder minusWeeksTemplate = JavaTemplate.builder("#{any(java.time.LocalDate)}.minusWeeks(#{any(int)})");
    final JavaTemplate.Builder minusMonthsTemplate = JavaTemplate.builder("#{any(java.time.LocalDate)}.minusMonths(#{any(int)})");
    final JavaTemplate.Builder minusYearsTemplate = JavaTemplate.builder("#{any(java.time.LocalDate)}.minusYears(#{any(int)})");
    final JavaTemplate.Builder toStartOfDayWithDefaultZoneTemplate = JavaTemplate.builder("#{any(java.time.LocalDate)}.atStartOfDay(java.time.ZoneId.systemDefault())")
            .imports(JAVA_ZONE_ID);
    final JavaTemplate.Builder toStartOfDateWithZoneTemplate = JavaTemplate.builder("#{any(java.time.LocalDate)}.atStartOfDay(#{any(java.time.ZoneId)})")
            .imports(JAVA_ZONE_ID);

    @Getter
    private final List<MethodTemplate> templates = new ArrayList<MethodTemplate>() {
        {
            add(new MethodTemplate(newLocalDateNoArgs, build(localDateNoArgsTemplate)));
            add(new MethodTemplate(newLocalDateEpoch, build(localDateEpochTemplate)));
            add(new MethodTemplate(newLocalDateYmd, build(localDateYmdTemplate)));
            add(new MethodTemplate(now, build(nowTemplate)));
            add(new MethodTemplate(parse, build(parseTemplate)));
            add(new MethodTemplate(parseWithFormatter, build(parseWithFormatterTemplate)));
            add(new MethodTemplate(plusDays, build(plusDaysTemplate)));
            add(new MethodTemplate(plusWeeks, build(plusWeeksTemplate)));
            add(new MethodTemplate(plusMonths, build(plusMonthsTemplate)));
            add(new MethodTemplate(plusYears, build(plusYearsTemplate)));
            add(new MethodTemplate(minusDays, build(minusDaysTemplate)));
            add(new MethodTemplate(minusWeeks, build(minusWeeksTemplate)));
            add(new MethodTemplate(minusMonths, build(minusMonthsTemplate)));
            add(new MethodTemplate(minusYears, build(minusYearsTemplate)));
            add(new MethodTemplate(toDateMidnight, build(toStartOfDayWithDefaultZoneTemplate),
                    m -> new Expression[]{ ((J.MethodInvocation)m).getSelect() })
            );
            add(new MethodTemplate(toDateTimeAtStartOfDay, build(toStartOfDayWithDefaultZoneTemplate),
                    m -> new Expression[]{ ((J.MethodInvocation)m).getSelect() })
            );
            add(new MethodTemplate(toDateTimeAtStartOfDayWithZone, build(toStartOfDateWithZoneTemplate),
                    m -> {
                        J.MethodInvocation mi = (J.MethodInvocation)m;
                        return new Expression[]{ mi.getSelect(), mi.getArguments().get(0) };
                    }));
        }
    };

    private JavaTemplate build(JavaTemplate.Builder builder) {
        return buildWithImport(builder, JAVA_LOCAL_DATE);
    }
}
