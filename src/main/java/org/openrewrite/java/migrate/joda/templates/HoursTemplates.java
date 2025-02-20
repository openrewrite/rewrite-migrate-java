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
public class HoursTemplates implements Templates {
    final MethodMatcher staticHoursMethod = new MethodMatcher(JODA_HOURS + " hours(int)");
    final MethodMatcher plusMethod = new MethodMatcher(JODA_HOURS + " plus(org.joda.time.Hours)");
    final MethodMatcher minusMethod = new MethodMatcher(JODA_HOURS + " minus(org.joda.time.Hours)");
    final MethodMatcher multipliedByMethod = new MethodMatcher(JODA_HOURS + " multipliedBy(int)");
    final MethodMatcher dividedByMethod = new MethodMatcher(JODA_HOURS + " dividedBy(int)");
    final MethodMatcher getHoursMethod = new MethodMatcher(JODA_HOURS + " getHours()");

    final JavaTemplate.Builder staticHoursTemplate = JavaTemplate.builder("Duration.ofHours(#{any(int)})");
    final JavaTemplate.Builder plusTemplate = JavaTemplate.builder("#{any(java.time.Duration)}.plus(#{any(java.time.Duration)})");
    final JavaTemplate.Builder minusTemplate = JavaTemplate.builder("#{any(java.time.Duration)}.minus(#{any(java.time.Duration)})");
    final JavaTemplate.Builder multipliedByTemplate = JavaTemplate.builder("#{any(java.time.Duration)}.multipliedBy(#{any(int)})");
    final JavaTemplate.Builder dividedByTemplate = JavaTemplate.builder("#{any(java.time.Duration)}.dividedBy(#{any(int)})");
    final JavaTemplate.Builder getHoursTemplate = JavaTemplate.builder("((int) #{any(java.time.Duration)}.toHours())");

    @Getter
    private final List<MethodTemplate> templates = new ArrayList<MethodTemplate>() {
        {
            add(new MethodTemplate(staticHoursMethod, build(staticHoursTemplate)));
            add(new MethodTemplate(plusMethod,  build(plusTemplate)));
            add(new MethodTemplate(minusMethod,  build(minusTemplate)));
            add(new MethodTemplate(multipliedByMethod,  build(multipliedByTemplate)));
            add(new MethodTemplate(dividedByMethod,  build(dividedByTemplate)));
            add(new MethodTemplate(getHoursMethod,  build(getHoursTemplate)));
        }
    };

    private JavaTemplate build(JavaTemplate.Builder builder) {
        return buildWithImport(builder, JAVA_DURATION);
    }
}
