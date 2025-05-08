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

import static org.openrewrite.java.migrate.joda.templates.TimeClassNames.JAVA_DURATION;
import static org.openrewrite.java.migrate.joda.templates.TimeClassNames.JODA_MINUTES;

@NoArgsConstructor
public class MinutesTemplates implements Templates {
    final MethodMatcher minutesStaticMethod = new MethodMatcher(JODA_MINUTES + " minutes(int)");
    final MethodMatcher plusMethod = new MethodMatcher(JODA_MINUTES + " plus(org.joda.time.Minutes)");
    final MethodMatcher minusMethod = new MethodMatcher(JODA_MINUTES + " minus(org.joda.time.Minutes)");
    final MethodMatcher multipliedByMethod = new MethodMatcher(JODA_MINUTES + " multipliedBy(int)");
    final MethodMatcher dividedByMethod = new MethodMatcher(JODA_MINUTES + " dividedBy(int)");
    final MethodMatcher getMinutesMethod = new MethodMatcher(JODA_MINUTES + " getMinutes()");
    final MethodMatcher isLessThan = new MethodMatcher(JODA_MINUTES + " isLessThan(org.joda.time.Minutes)");
    final MethodMatcher isGreaterThan = new MethodMatcher(JODA_MINUTES + " isGreaterThan(org.joda.time.Minutes)");
    final MethodMatcher toStandardDuration = new MethodMatcher(JODA_MINUTES + " toStandardDuration()");
    //Minutes.ZERO

    final JavaTemplate.Builder minutesStaticMethodTemplate = JavaTemplate.builder("Duration.ofMinutes(#{any(int)})");
    final JavaTemplate.Builder plusMethodTemplate = JavaTemplate.builder("#{any(java.time.Duration)}.plus(#{any(java.time.Duration)})");
    final JavaTemplate.Builder minusMethodTemplate = JavaTemplate.builder("#{any(java.time.Duration)}.minus(#{any(java.time.Duration)})");
    final JavaTemplate.Builder multipliedByMethodTemplate = JavaTemplate.builder("#{any(java.time.Duration)}.multipliedBy(#{any(int)})");
    final JavaTemplate.Builder dividedByMethodTemplate = JavaTemplate.builder("#{any(java.time.Duration)}.dividedBy(#{any(int)})");
    final JavaTemplate.Builder getMinutesMethodTemplate = JavaTemplate.builder("(int)#{any(java.time.Duration)}.toMinutes()");
    final JavaTemplate.Builder minusIsNegativeTemplate = JavaTemplate.builder("#{any(java.time.Duration)}.minus(#{any(java.time.Duration)}).isNegative()");
    final JavaTemplate.Builder minusIsPositiveTemplate = JavaTemplate.builder("#{any(java.time.Duration)}.minus(#{any(java.time.Duration)}).isPositive()");
    final JavaTemplate.Builder asDurationTemplate = JavaTemplate.builder("#{any(java.time.Duration)}");

    @Getter
    private final List<MethodTemplate> templates = new ArrayList<MethodTemplate>() {
        {
            add(new MethodTemplate(minutesStaticMethod, build(minutesStaticMethodTemplate)));
            add(new MethodTemplate(plusMethod, build(plusMethodTemplate)));
            add(new MethodTemplate(minusMethod, build(minusMethodTemplate)));
            add(new MethodTemplate(multipliedByMethod, build(multipliedByMethodTemplate)));
            add(new MethodTemplate(dividedByMethod, build(dividedByMethodTemplate)));
            add(new MethodTemplate(getMinutesMethod, build(getMinutesMethodTemplate)));
            add(new MethodTemplate(isLessThan, build(minusIsNegativeTemplate)));
            add(new MethodTemplate(isGreaterThan, build(minusIsPositiveTemplate)));
            add(new MethodTemplate(toStandardDuration, build(asDurationTemplate)));
        }
    };

    private JavaTemplate build(JavaTemplate.Builder builder) {
        return buildWithImport(builder, JAVA_DURATION);
    }
}
