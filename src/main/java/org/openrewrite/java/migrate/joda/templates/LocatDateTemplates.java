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

    final JavaTemplate localDateNoArgsTemplate = JavaTemplate.builder("LocalDate.now()")
            .imports(JAVA_LOCAL_DATE)
            .build();
    final JavaTemplate localDateEpochTemplate = JavaTemplate.builder("LocalDate.ofEpochDay(#{any(long)})")
            .imports(JAVA_LOCAL_DATE)
            .build();
    final JavaTemplate localDateYmdTemplate = JavaTemplate.builder("LocalDate.of(#{any(int)}, #{any(int)}, #{any(int)})")
            .imports(JAVA_LOCAL_DATE)
            .build();
    final JavaTemplate nowTemplate = JavaTemplate.builder("LocalDate.now()").imports(JAVA_LOCAL_DATE).build();
    final JavaTemplate parseTemplate = JavaTemplate.builder("LocalDate.parse(#{any(String)})").imports(JAVA_LOCAL_DATE).build();
    final JavaTemplate parseWithFormatterTemplate = JavaTemplate.builder("LocalDate.parse(#{any(String)}, #{any(java.time.format.DateTimeFormatter)})")
            .imports(JAVA_LOCAL_DATE)
            .build();
    final JavaTemplate plusDaysTemplate = JavaTemplate.builder("#{any(java.time.LocalDate)}.plusDays(#{any(int)})")
            .build();
    final JavaTemplate plusWeeksTemplate = JavaTemplate.builder("#{any(java.time.LocalDate)}.plusWeeks(#{any(int)})")
            .build();
    final JavaTemplate plusMonthsTemplate = JavaTemplate.builder("#{any(java.time.LocalDate)}.plusMonths(#{any(int)})")
            .build();
    final JavaTemplate plusYearsTemplate = JavaTemplate.builder("#{any(java.time.LocalDate)}.plusYears(#{any(int)})")
            .build();
    final JavaTemplate minusDaysTemplate = JavaTemplate.builder("#{any(java.time.LocalDate)}.minusDays(#{any(int)})")
            .build();
    final JavaTemplate minusWeeksTemplate = JavaTemplate.builder("#{any(java.time.LocalDate)}.minusWeeks(#{any(int)})")
            .build();
    final JavaTemplate minusMonthsTemplate = JavaTemplate.builder("#{any(java.time.LocalDate)}.minusMonths(#{any(int)})")
            .build();
    final JavaTemplate minusYearsTemplate = JavaTemplate.builder("#{any(java.time.LocalDate)}.minusYears(#{any(int)})")
            .build();
    final JavaTemplate toStartOfDayWithDefaultZoneTemplate = JavaTemplate.builder("#{any(java.time.LocalDate)}.atStartOfDay(java.time.ZoneId.systemDefault())")
            .imports(JAVA_ZONE_ID, JAVA_DATE_TIME)
            .build();
    final JavaTemplate toStartOfDateWithZoneTemplate = JavaTemplate.builder("#{any(java.time.LocalDate)}.atStartOfDay(#{any(java.time.ZoneId)})")
            .imports(JAVA_ZONE_ID, JAVA_DATE_TIME)
            .build();



    @Getter
    private final List<MethodTemplate> templates = new ArrayList<MethodTemplate>() {
        {
            add(new MethodTemplate(newLocalDateNoArgs, localDateNoArgsTemplate));
            add(new MethodTemplate(newLocalDateEpoch, localDateEpochTemplate));
            add(new MethodTemplate(newLocalDateYmd, localDateYmdTemplate));
            add(new MethodTemplate(now, nowTemplate));
            add(new MethodTemplate(parse, parseTemplate));
            add(new MethodTemplate(parseWithFormatter, parseWithFormatterTemplate));
            add(new MethodTemplate(plusDays, plusDaysTemplate));
            add(new MethodTemplate(plusWeeks, plusWeeksTemplate));
            add(new MethodTemplate(plusMonths, plusMonthsTemplate));
            add(new MethodTemplate(plusYears, plusYearsTemplate));
            add(new MethodTemplate(minusDays, minusDaysTemplate));
            add(new MethodTemplate(minusWeeks, minusWeeksTemplate));
            add(new MethodTemplate(minusMonths, minusMonthsTemplate));
            add(new MethodTemplate(minusYears, minusYearsTemplate));
            add(new MethodTemplate(toDateMidnight, toStartOfDayWithDefaultZoneTemplate,
                    m -> new Expression[]{ ((J.MethodInvocation)m).getSelect() })
            );
            add(new MethodTemplate(toDateTimeAtStartOfDay, toStartOfDayWithDefaultZoneTemplate,
                    m -> new Expression[]{ ((J.MethodInvocation)m).getSelect() })
            );
            add(new MethodTemplate(toDateTimeAtStartOfDayWithZone, toStartOfDateWithZoneTemplate,
                    m -> {
                        J.MethodInvocation mi = (J.MethodInvocation)m;
                        return new Expression[]{ mi.getSelect(), mi.getArguments().get(0) };
                    }));
        }
    };
}
