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
public class MonthsTemplates implements Templates {
    final MethodMatcher monthsStaticMethod = new MethodMatcher(JODA_MONTHS + " months(int)");
    final MethodMatcher monthsBetween = new MethodMatcher(JODA_MONTHS + " monthsBetween(org.joda.time.ReadablePartial, org.joda.time.ReadablePartial)");
    final MethodMatcher getMonths = new MethodMatcher(JODA_MONTHS + " getMonths()");

    final JavaTemplate.Builder monthsStaticMethodTemplate = JavaTemplate.builder("Period.ofMonths(#{any(int)})");
    final JavaTemplate.Builder monthsBetweenTemplate = JavaTemplate.builder("ChronoUnit.MONTHS.between(#{any(java.time.LocalDate)}, #{any(java.time.LocalDate)})")
            .imports(JAVA_CHRONO_UNIT);
    final JavaTemplate.Builder getMonthsTemplate = JavaTemplate.builder("(int)#{any(long)}");

    @Getter
    private final List<MethodTemplate> templates = new ArrayList<MethodTemplate>() {
        {
            add(new MethodTemplate(monthsStaticMethod, build(monthsStaticMethodTemplate)));
            add(new MethodTemplate(monthsBetween, build(monthsBetweenTemplate)));
            add(new MethodTemplate(getMonths, build(getMonthsTemplate)));
        }
    };

    private JavaTemplate build(JavaTemplate.Builder builder) {
        return buildWithImport(builder, JAVA_PERIOD);
    }
}
