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

import static org.openrewrite.java.migrate.joda.templates.TimeClassNames.JAVA_PERIOD;
import static org.openrewrite.java.migrate.joda.templates.TimeClassNames.JODA_DAYS;

@NoArgsConstructor
public class DaysTemplates implements Templates {
    final MethodMatcher daysStaticMethod = new MethodMatcher(JODA_DAYS + " days(int)");
    final MethodMatcher daysBetweenPartial = new MethodMatcher(JODA_DAYS + " daysBetween(org.joda.time.ReadablePartial, org.joda.time.ReadablePartial)");
    final MethodMatcher daysBetweenInstant = new MethodMatcher(JODA_DAYS + " daysBetween(org.joda.time.ReadableInstant, org.joda.time.ReadableInstant)");
    final MethodMatcher getDays = new MethodMatcher(JODA_DAYS + " getDays()");
    final MethodMatcher daysOne = new MethodMatcher(JODA_DAYS + " ONE");

    final JavaTemplate.Builder daysStaticMethodTemplate = JavaTemplate.builder("Days.of(#{any(int)})");
    final JavaTemplate.Builder daysBetweenLocalTimeTemplate = JavaTemplate.builder("Days.between(#{any(java.time.LocalDate)}, #{any(java.time.LocalDate)})");
    final JavaTemplate.Builder daysBetweenZonedDateTimeTemplate = JavaTemplate.builder("Days.between(#{any(java.time.ZonedDateTime)}, #{any(java.time.ZonedDateTime)})");
    final JavaTemplate.Builder getTemplate = JavaTemplate.builder("(int)#{any(org.threeten.extra.Days)}.get(ChronoUnit.DAYS)")
            .imports(JAVA_CHRONO_UNIT);
    final JavaTemplate.Builder durationOfOneDayTemplate = JavaTemplate.builder("ONE");

    @Getter
    private final List<MethodTemplate> templates = new ArrayList<MethodTemplate>() {
        {
            add(new MethodTemplate(daysStaticMethod, build(daysStaticMethodTemplate)));
            add(new MethodTemplate(daysBetweenPartial, build(daysBetweenLocalTimeTemplate)));
            add(new MethodTemplate(daysBetweenInstant, build(daysBetweenZonedDateTimeTemplate)));
            add(new MethodTemplate(getDays, build(getTemplate)));
            add(new MethodTemplate(daysOne, build(durationOfOneDayTemplate)));
        }
    };

    private JavaTemplate build(JavaTemplate.Builder builder) {
        return buildWithImport(builder, THREE_TEN_EXTRA_DAYS);
    }
}
