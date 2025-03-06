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
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;

import java.util.ArrayList;
import java.util.List;

import static org.openrewrite.java.migrate.joda.templates.TimeClassNames.*;

public class IntervalTemplates implements Templates {
    private final MethodMatcher interval = new MethodMatcher(JODA_INTERVAL + " <constructor>(long, long)");
    private final MethodMatcher intervalWithTimeZone = new MethodMatcher(JODA_INTERVAL + " <constructor>(long, long, " + JODA_DATE_TIME_ZONE + ")");
    private final MethodMatcher intervalWithDateTime = new MethodMatcher(JODA_INTERVAL + " <constructor>(" + JODA_READABLE_INSTANT + ", " + JODA_READABLE_INSTANT + ")");
    private final MethodMatcher intervalWithDateTimeAndDuration = new MethodMatcher(JODA_INTERVAL + " <constructor>(" + JODA_READABLE_INSTANT + ", " + JODA_READABLE_DURATION + ")");
    private final MethodMatcher withEnd = new MethodMatcher(JODA_INTERVAL + " withEnd(org.joda.time.ReadableInstant)");
    private final MethodMatcher withEndDateTime = new MethodMatcher(JODA_INTERVAL + " withEnd(org.joda.time.DateTime)");
    private final MethodMatcher withStart = new MethodMatcher(JODA_INTERVAL + " withStart(org.joda.time.ReadableInstant)");
    private final MethodMatcher abuts = new MethodMatcher(JODA_INTERVAL + " abuts(org.joda.time.ReadableInterval)");
    private final MethodMatcher overlap = new MethodMatcher(JODA_INTERVAL + " overlap(org.joda.time.ReadableInterval)");

    private final JavaTemplate.Builder intervalTemplate = JavaTemplate.builder("Interval.of(Instant.ofEpochMilli(#{any(long)}), Instant.ofEpochMilli(#{any(long)}))")
            .imports(JAVA_INSTANT);
    private final JavaTemplate.Builder intervalWithDateTimeTemplate = JavaTemplate.builder("Interval.of(#{any(" + JAVA_DATE_TIME + ")}.toInstant(), #{any(" + JAVA_DATE_TIME + ")}.toInstant())");
    private final JavaTemplate.Builder intervalWithDateTimeAndDurationTemplate = JavaTemplate.builder("Interval.of(#{any(" + JAVA_DATE_TIME + ")}.toInstant(), #{any(" + JAVA_DURATION + ")})");;
    private final JavaTemplate.Builder withEndTemplate = JavaTemplate.builder("#{any(" + THREE_TEN_EXTRA_INTERVAL + ")}.withEnd(#{any(java.time.Instant)})");
    private final JavaTemplate.Builder withEndZonedDateTimeTemplate = JavaTemplate.builder("#{any(" + THREE_TEN_EXTRA_INTERVAL + ")}.withEnd(#{any(java.time.ZonedDateTime)}.toInstant())");
    private final JavaTemplate.Builder withStartTemplate = JavaTemplate.builder("#{any(" + THREE_TEN_EXTRA_INTERVAL + ")}.withStart(#{any(java.time.Instant)})");
    private final JavaTemplate.Builder abutsTemplate = JavaTemplate.builder("#{any(" + THREE_TEN_EXTRA_INTERVAL + ")}.abuts(#{any(org.threeten.extra.Interval)})");
    private final JavaTemplate.Builder intersectionTemplate = JavaTemplate.builder("#{any(" + THREE_TEN_EXTRA_INTERVAL + ")}.intersection(#{any(org.threeten.extra.Interval)})");

    @Getter
    private final List<MethodTemplate> templates = new ArrayList<MethodTemplate>() {
        {
            add(new MethodTemplate(interval, build(intervalTemplate)));
            add(new MethodTemplate(intervalWithTimeZone, build(intervalTemplate),
                    m -> new Expression[]{m.getArguments().get(0), m.getArguments().get(1)}));
            add(new MethodTemplate(intervalWithDateTime, build(intervalWithDateTimeTemplate)));
            add(new MethodTemplate(intervalWithDateTimeAndDuration, build(intervalWithDateTimeAndDurationTemplate)));
            add(new MethodTemplate(withEnd, build(withEndTemplate)));
            add(new MethodTemplate(withEndDateTime, build(withEndZonedDateTimeTemplate)));
            add(new MethodTemplate(withStart, build(withStartTemplate)));
            add(new MethodTemplate(abuts, build(abutsTemplate)));
            add(new MethodTemplate(overlap, build(intersectionTemplate)));
        }
    };

    private JavaTemplate build(JavaTemplate.Builder builder) {
        return builder
                .javaParser(JavaParser.fromJavaVersion().classpath("threeten-extra"))
                .imports(THREE_TEN_EXTRA_INTERVAL)
                .build();
    }
}
