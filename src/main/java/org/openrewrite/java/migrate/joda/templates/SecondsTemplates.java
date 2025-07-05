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
public class SecondsTemplates implements Templates {
    final MethodMatcher secondsStaticMethod = new MethodMatcher(JODA_SECONDS + " seconds(int)");
    final MethodMatcher plusMethod = new MethodMatcher(JODA_SECONDS + " plus(org.joda.time.Seconds)");
    final MethodMatcher minusMethod = new MethodMatcher(JODA_SECONDS + " minus(org.joda.time.Seconds)");
    final MethodMatcher multipliedByMethod = new MethodMatcher(JODA_SECONDS + " multipliedBy(int)");
    final MethodMatcher dividedByMethod = new MethodMatcher(JODA_SECONDS + " dividedBy(int)");
    final MethodMatcher getSecondsMethod = new MethodMatcher(JODA_SECONDS + " getSeconds()");
    final MethodMatcher isLessThan = new MethodMatcher(JODA_SECONDS + " isLessThan(org.joda.time.Seconds)");
    final MethodMatcher isGreaterThan = new MethodMatcher(JODA_SECONDS + " isGreaterThan(org.joda.time.Seconds)");
    final MethodMatcher toStandardDuration = new MethodMatcher(JODA_SECONDS + " toStandardDuration()");
    final MethodMatcher secondsPlus = new MethodMatcher(JODA_SECONDS + " plus(int)");
    final MethodMatcher secondsMinus = new MethodMatcher(JODA_SECONDS + " minus(int)");
    final MethodMatcher secondsNegated = new MethodMatcher(JODA_SECONDS + " negated()");
    final MethodMatcher secondsCompareTo = new MethodMatcher(JODA_SECONDS + " compareTo(" + JODA_SECONDS + ")");
    final MethodMatcher secondsToStandardMinutes = new MethodMatcher(JODA_SECONDS + " toStandardMinutes()");
    final MethodMatcher secondsToString = new MethodMatcher(JODA_SECONDS + " toString()");

    final JavaTemplate.Builder secondsStaticMethodTemplate = JavaTemplate.builder("Duration.ofSeconds(#{any(int)})");
    final JavaTemplate.Builder plusMethodTemplate = JavaTemplate.builder("#{any(java.time.Duration)}.plus(#{any(java.time.Duration)})");
    final JavaTemplate.Builder minusMethodTemplate = JavaTemplate.builder("#{any(java.time.Duration)}.minus(#{any(java.time.Duration)})");
    final JavaTemplate.Builder multipliedByMethodTemplate = JavaTemplate.builder("#{any(java.time.Duration)}.multipliedBy(#{any(int)})");
    final JavaTemplate.Builder dividedByMethodTemplate = JavaTemplate.builder("#{any(java.time.Duration)}.dividedBy(#{any(int)})");
    final JavaTemplate.Builder getSecondsMethodTemplate = JavaTemplate.builder("#{any(java.time.Duration)}.getSeconds()");
    final JavaTemplate.Builder minusIsNegativeTemplate = JavaTemplate.builder("#{any(java.time.Duration)}.minus(#{any(java.time.Duration)}).isNegative()");
    final JavaTemplate.Builder minusIsPositiveTemplate = JavaTemplate.builder("#{any(java.time.Duration)}.minus(#{any(java.time.Duration)}).isPositive()");
    final JavaTemplate.Builder asDurationTemplate = JavaTemplate.builder("#{any(java.time.Duration)}");
    final JavaTemplate.Builder plusSecondsTemplate = JavaTemplate.builder("#{any(java.time.Duration)}.plusSeconds(#{any(int)})");
    final JavaTemplate.Builder minusSecondsTemplate = JavaTemplate.builder("#{any(java.time.Duration)}.minusSeconds(#{any(int)})");
    final JavaTemplate.Builder toMinutesTemplate = JavaTemplate.builder("#{any(java.time.Duration)}.toMinutes()");
    final JavaTemplate.Builder toMinutesNegatedTemplate = JavaTemplate.builder("#{any(java.time.Duration)}.toMinutes() * -1");
    final JavaTemplate.Builder compareLongTemplate = JavaTemplate.builder("Long.compare(#{any(long)}, #{any(long)})");

    final JavaTemplate.Builder plusIntTemplate = JavaTemplate.builder("#{any(java.time.Duration)}");

    @Getter
    private final List<MethodTemplate> templates = new ArrayList<MethodTemplate>() {
        {
            add(new MethodTemplate(secondsStaticMethod, build(secondsStaticMethodTemplate)));
            add(new MethodTemplate(plusMethod, build(plusMethodTemplate)));
            add(new MethodTemplate(minusMethod, build(minusMethodTemplate)));
            add(new MethodTemplate(multipliedByMethod, build(multipliedByMethodTemplate)));
            add(new MethodTemplate(dividedByMethod, build(dividedByMethodTemplate)));
            add(new MethodTemplate(getSecondsMethod, build(getSecondsMethodTemplate)));
            add(new MethodTemplate(isLessThan, build(minusIsNegativeTemplate)));
            add(new MethodTemplate(isGreaterThan, build(minusIsPositiveTemplate)));
            add(new MethodTemplate(toStandardDuration, build(asDurationTemplate)));
            add(new MethodTemplate(secondsPlus, build(plusSecondsTemplate)));
            add(new MethodTemplate(secondsMinus, build(minusSecondsTemplate)));
            add(new MethodTemplate(secondsToStandardMinutes, build(toMinutesTemplate)));
            add(new MethodTemplate(secondsNegated, build(toMinutesNegatedTemplate)));
            add(new MethodTemplate(secondsCompareTo, build(compareLongTemplate)));
        }
    };

    private JavaTemplate build(JavaTemplate.Builder builder) {
        return buildWithImport(builder, JAVA_DURATION);
    }
}
