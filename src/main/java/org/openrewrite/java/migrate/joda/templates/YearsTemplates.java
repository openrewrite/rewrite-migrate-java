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
public class YearsTemplates implements Templates {
    final MethodMatcher yearsStaticMethod = new MethodMatcher(JODA_YEARS + " years(int)");
    final MethodMatcher yearsBetweenPartial = new MethodMatcher(JODA_YEARS + " yearsBetween(org.joda.time.ReadablePartial, org.joda.time.ReadablePartial)");
    final MethodMatcher yearsBetweenInstant = new MethodMatcher(JODA_YEARS + " yearsBetween(org.joda.time.ReadableInstant, org.joda.time.ReadableInstant)");
    final MethodMatcher getYears = new MethodMatcher(JODA_YEARS + " getYears()");

    final JavaTemplate.Builder yearsStaticMethodTemplate = JavaTemplate.builder("Years.of(#{any(int)})");
    final JavaTemplate.Builder yearsBetweenLocalTemplate = JavaTemplate.builder("Years.between(#{any(java.time.LocalDate)}, #{any(java.time.LocalDate)})");
    final JavaTemplate.Builder yearsBetweenZonedDateTimeTemplate = JavaTemplate.builder("Years.between(#{any(java.time.ZonedDateTime)}, #{any(java.time.ZonedDateTime)})");
    final JavaTemplate.Builder getTemplate = JavaTemplate.builder("(int)#{any(org.threeten.extra.Years)}.get(ChronoUnit.YEARS)")
            .imports(JAVA_CHRONO_UNIT);

    @Getter
    private final List<MethodTemplate> templates = new ArrayList<MethodTemplate>() {
        {
            add(new MethodTemplate(yearsStaticMethod, build(yearsStaticMethodTemplate)));
            add(new MethodTemplate(yearsBetweenPartial, build(yearsBetweenLocalTemplate)));
            add(new MethodTemplate(yearsBetweenInstant, build(yearsBetweenZonedDateTimeTemplate)));
            add(new MethodTemplate(getYears, build(getTemplate)));
        }
    };

    private JavaTemplate build(JavaTemplate.Builder builder) {
        return buildWithImport(builder, THREE_TEN_EXTRA_YEARS);
    }
}
