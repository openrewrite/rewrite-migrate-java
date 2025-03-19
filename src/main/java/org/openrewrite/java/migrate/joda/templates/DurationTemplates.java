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

import java.util.ArrayList;
import java.util.List;

import static org.openrewrite.java.migrate.joda.templates.TimeClassNames.*;

@NoArgsConstructor
public class DurationTemplates implements Templates {
    private final MethodMatcher newDuration = new MethodMatcher(JODA_DURATION + "<constructor>(long)");
    private final MethodMatcher newDurationWithObject = new MethodMatcher(JODA_DURATION + "<constructor>(java.lang.Object)");
    private final MethodMatcher newDurationWithInstants = new MethodMatcher(JODA_DURATION + "<constructor>(long,long)");
    private final MethodMatcher newDurationWithDateTimes = new MethodMatcher(JODA_DURATION + "<constructor>(org.joda.time.ReadableInstant, org.joda.time.ReadableInstant)");

    private final MethodMatcher parse = new MethodMatcher(JODA_DURATION + " parse(String)");
    private final MethodMatcher standardDays = new MethodMatcher(JODA_DURATION + " standardDays(long)");
    private final MethodMatcher standardHours = new MethodMatcher(JODA_DURATION + " standardHours(long)");
    private final MethodMatcher standardMinutes = new MethodMatcher(JODA_DURATION + " standardMinutes(long)");
    private final MethodMatcher standardSeconds = new MethodMatcher(JODA_DURATION + " standardSeconds(long)");
    private final MethodMatcher millis = new MethodMatcher(JODA_DURATION + " millis(long)");

    private final MethodMatcher getStandardDays = new MethodMatcher(JODA_DURATION + " getStandardDays()");
    private final MethodMatcher getStandardHours = new MethodMatcher(JODA_DURATION + " getStandardHours()");
    private final MethodMatcher getStandardMinutes = new MethodMatcher(JODA_DURATION + " getStandardMinutes()");
    private final MethodMatcher getStandardSeconds = new MethodMatcher(JODA_DURATION + " getStandardSeconds()");

    private final MethodMatcher toDuration = new MethodMatcher(JODA_DURATION + " toDuration()");
    private final MethodMatcher toPeriod = new MethodMatcher(JODA_DURATION + " toPeriod()");

    private final MethodMatcher toStandardDays = new MethodMatcher(JODA_DURATION + " toStandardDays()");
    private final MethodMatcher toStandardHours = new MethodMatcher(JODA_DURATION + " toStandardHours()");
    private final MethodMatcher toStandardMinutes = new MethodMatcher(JODA_DURATION + " toStandardMinutes()");
    private final MethodMatcher toStandardSeconds = new MethodMatcher(JODA_DURATION + " toStandardSeconds()");

    private final MethodMatcher withMillis = new MethodMatcher(JODA_DURATION + " withMillis(long)");
    private final MethodMatcher withDurationAdded = new MethodMatcher(JODA_DURATION + " withDurationAdded(long,int)");
    private final MethodMatcher withDurationAddedReadable = new MethodMatcher(JODA_DURATION + " withDurationAdded(" + JODA_READABLE_DURATION + ",int)");

    private final MethodMatcher plusLong = new MethodMatcher(JODA_DURATION + " plus(long)");
    private final MethodMatcher plusReadable = new MethodMatcher(JODA_DURATION + " plus(" + JODA_READABLE_DURATION + ")");
    private final MethodMatcher minusLong = new MethodMatcher(JODA_DURATION + " minus(long)");
    private final MethodMatcher minusReadable = new MethodMatcher(JODA_DURATION + " minus(" + JODA_READABLE_DURATION + ")");

    private final MethodMatcher multipliedBy = new MethodMatcher(JODA_DURATION + " multipliedBy(long)");
    private final MethodMatcher dividedBy = new MethodMatcher(JODA_DURATION + " dividedBy(long)");

    private final MethodMatcher negated = new MethodMatcher(JODA_DURATION + " negated()");
    private final MethodMatcher abs = new MethodMatcher(JODA_DURATION + " abs()");

    private final JavaTemplate.Builder durationOfMillisTemplate = JavaTemplate.builder("Duration.ofMillis(#{any(long)})");
    private final JavaTemplate.Builder durationBetweenInstantsTemplate = JavaTemplate.builder("Duration.between(Instant.ofEpochMilli(#{any(long)}), Instant.ofEpochMilli(#{any(long)}))")
            .imports(JAVA_INSTANT);
    private final JavaTemplate.Builder durationBetweenZonedDateTimesTemplate = JavaTemplate.builder("Duration.between(#{any(java.time.ZonedDateTime)}, #{any(java.time.ZonedDateTime)})")
            .imports(JAVA_INSTANT);

    private final JavaTemplate.Builder parseTemplate = JavaTemplate.builder("Duration.parse(#{any(String)})");
    private final JavaTemplate.Builder standardDaysTemplate = JavaTemplate.builder("Duration.ofDays(#{any(long)})");
    private final JavaTemplate.Builder standardHoursTemplate = JavaTemplate.builder("Duration.ofHours(#{any(long)})");
    private final JavaTemplate.Builder standardMinutesTemplate = JavaTemplate.builder("Duration.ofMinutes(#{any(long)})");
    private final JavaTemplate.Builder standardSecondsTemplate = JavaTemplate.builder("Duration.ofSeconds(#{any(long)})");
    private final JavaTemplate.Builder millisTemplate = JavaTemplate.builder("Duration.ofMillis(#{any(long)})");

    private final JavaTemplate.Builder toDurationTemplate = JavaTemplate.builder("#{any(java.time.Duration)}");
    private final JavaTemplate.Builder toPeriodTemplate = JavaTemplate.builder("Period.from(#{any(java.time.Duration)})")
            .imports(JAVA_PERIOD);

    private final JavaTemplate.Builder toDaysTemplate = JavaTemplate.builder("#{any(java.time.Duration)}.toDays()");
    private final JavaTemplate.Builder toHoursTemplate = JavaTemplate.builder("#{any(java.time.Duration)}.toHours()");
    private final JavaTemplate.Builder toMinutesTemplate = JavaTemplate.builder("#{any(java.time.Duration)}.toMinutes()");
    private final JavaTemplate.Builder getSecondsTemplate = JavaTemplate.builder("#{any(java.time.Duration)}.getSeconds()");
    private final JavaTemplate.Builder ofMillisTemplate = JavaTemplate.builder("Duration.ofMillis(#{any(long)})");
    private final JavaTemplate.Builder withDurationAddedTemplate = JavaTemplate.builder("#{any(java.time.Duration)}.plusMillis(#{any(long)} * #{any(int)})");
    private final JavaTemplate.Builder withDurationAddedReadableTemplate = JavaTemplate.builder("#{any(java.time.Duration)}.plus(#{any(java.time.Duration)}.multipliedBy(#{any(int)}))");
    private final JavaTemplate.Builder plusLongTemplate = JavaTemplate.builder("#{any(java.time.Duration)}.plusMillis(#{any(long)})");
    private final JavaTemplate.Builder plusReadableTemplate = JavaTemplate.builder("#{any(java.time.Duration)}.plus(#{any(java.time.Duration)})");
    private final JavaTemplate.Builder minusLongTemplate = JavaTemplate.builder("#{any(java.time.Duration)}.minusMillis(#{any(long)})");
    private final JavaTemplate.Builder minusReadableTemplate = JavaTemplate.builder("#{any(java.time.Duration)}.minus(#{any(java.time.Duration)})");
    private final JavaTemplate.Builder multipliedByTemplate = JavaTemplate.builder("#{any(java.time.Duration)}.multipliedBy(#{any(long)})");
    private final JavaTemplate.Builder dividedByTemplate = JavaTemplate.builder("#{any(java.time.Duration)}.dividedBy(#{any(long)})");
    private final JavaTemplate.Builder negatedTemplate = JavaTemplate.builder("#{any(java.time.Duration)}.negated()");
    private final JavaTemplate.Builder absTemplate = JavaTemplate.builder("#{any(java.time.Duration)}.abs()");

    @Getter
    private final List<MethodTemplate> templates = new ArrayList<MethodTemplate>() {
        {
            add(new MethodTemplate(newDuration, build(durationOfMillisTemplate)));
            add(new MethodTemplate(newDurationWithObject, build(parseTemplate)));
            add(new MethodTemplate(newDurationWithInstants, build(durationBetweenInstantsTemplate)));
            add(new MethodTemplate(newDurationWithDateTimes, build(durationBetweenZonedDateTimesTemplate)));

            add(new MethodTemplate(parse, build(parseTemplate)));
            add(new MethodTemplate(standardDays, build(standardDaysTemplate)));
            add(new MethodTemplate(standardHours, build(standardHoursTemplate)));
            add(new MethodTemplate(standardMinutes, build(standardMinutesTemplate)));
            add(new MethodTemplate(standardSeconds, build(standardSecondsTemplate)));
            add(new MethodTemplate(millis, build(millisTemplate)));

            add(new MethodTemplate(getStandardDays, build(toDaysTemplate)));
            add(new MethodTemplate(getStandardHours, build(toHoursTemplate)));
            add(new MethodTemplate(getStandardMinutes, build(toMinutesTemplate)));
            add(new MethodTemplate(getStandardSeconds, build(getSecondsTemplate)));

            add(new MethodTemplate(toDuration, build(toDurationTemplate)));
            add(new MethodTemplate(toPeriod, build(toPeriodTemplate)));

            add(new MethodTemplate(toStandardDays, build(toDaysTemplate)));
            add(new MethodTemplate(toStandardHours, build(toHoursTemplate)));
            add(new MethodTemplate(toStandardMinutes, build(toMinutesTemplate)));
            add(new MethodTemplate(toStandardSeconds, build(getSecondsTemplate)));

            add(new MethodTemplate(withMillis, build(ofMillisTemplate), m -> new Expression[]{m.getArguments().get(0)}));
            add(new MethodTemplate(withDurationAdded, build(withDurationAddedTemplate)));
            add(new MethodTemplate(withDurationAddedReadable, build(withDurationAddedReadableTemplate)));

            add(new MethodTemplate(plusLong, build(plusLongTemplate)));
            add(new MethodTemplate(plusReadable, build(plusReadableTemplate)));
            add(new MethodTemplate(minusLong, build(minusLongTemplate)));
            add(new MethodTemplate(minusReadable, build(minusReadableTemplate)));

            add(new MethodTemplate(multipliedBy, build(multipliedByTemplate)));
            add(new MethodTemplate(dividedBy, build(dividedByTemplate)));

            add(new MethodTemplate(negated, build(negatedTemplate)));
            add(new MethodTemplate(abs, build(absTemplate)));
        }
    };

    private JavaTemplate build(JavaTemplate.Builder builder) {
        return buildWithImport(builder, JAVA_DURATION);
    }
}
