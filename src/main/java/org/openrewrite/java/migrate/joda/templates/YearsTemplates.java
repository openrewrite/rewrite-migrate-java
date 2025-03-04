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
    final MethodMatcher yearsBetween = new MethodMatcher(JODA_YEARS + " yearsBetween(org.joda.time.ReadablePartial, org.joda.time.ReadablePartial)");
    final MethodMatcher getYears = new MethodMatcher(JODA_YEARS + " getYears()");

    final JavaTemplate.Builder yearsStaticMethodTemplate = JavaTemplate.builder("Period.ofyears(#{any(int)})");
    final JavaTemplate.Builder yearsBetweenTemplate = JavaTemplate.builder("ChronoUnit.YEARS.between(#{any(java.time.LocalDate)}, #{any(java.time.LocalDate)})")
            .imports(JAVA_CHRONO_UNIT);
    final JavaTemplate.Builder getYearsTemplate = JavaTemplate.builder("(int)#{any(long)}");

    @Getter
    private final List<MethodTemplate> templates = new ArrayList<MethodTemplate>() {
        {
            add(new MethodTemplate(yearsStaticMethod, build(yearsStaticMethodTemplate)));
            add(new MethodTemplate(yearsBetween, build(yearsBetweenTemplate)));
            add(new MethodTemplate(getYears, build(getYearsTemplate)));
        }
    };

    private JavaTemplate build(JavaTemplate.Builder builder) {
        return buildWithImport(builder, JAVA_PERIOD);
    }
}
