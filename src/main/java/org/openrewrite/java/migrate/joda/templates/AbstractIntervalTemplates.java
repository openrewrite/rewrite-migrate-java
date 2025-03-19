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

import java.util.ArrayList;
import java.util.List;

import static org.openrewrite.java.migrate.joda.templates.TimeClassNames.*;

public class AbstractIntervalTemplates implements Templates {
    private final MethodMatcher getStart = new MethodMatcher(JODA_ABSTRACT_INTERVAL + " getStart()");
    private final MethodMatcher getEnd = new MethodMatcher(JODA_ABSTRACT_INTERVAL + " getEnd()");
    private final MethodMatcher toDuration = new MethodMatcher(JODA_ABSTRACT_INTERVAL + " toDuration()");
    private final MethodMatcher toDurationMillis = new MethodMatcher(JODA_ABSTRACT_INTERVAL + " toDurationMillis()");
    private final MethodMatcher contains = new MethodMatcher(JODA_ABSTRACT_INTERVAL + " contains(long)");
    private final MethodMatcher containsReadableInstant = new MethodMatcher(JODA_ABSTRACT_INTERVAL + " contains(org.joda.time.ReadableInstant)");
    private final MethodMatcher overlaps = new MethodMatcher(JODA_ABSTRACT_INTERVAL + " overlaps(org.joda.time.ReadableInterval)");
    private final MethodMatcher containsInterval = new MethodMatcher(JODA_ABSTRACT_INTERVAL + " contains(org.joda.time.ReadableInterval)");
    private final MethodMatcher equals = new MethodMatcher(JODA_ABSTRACT_INTERVAL + " equals(java.lang.Object)");
    private final MethodMatcher isBefore = new MethodMatcher(JODA_ABSTRACT_INTERVAL + " isBefore(org.joda.time.ReadableInstant)");
    private final MethodMatcher isAfter = new MethodMatcher(JODA_ABSTRACT_INTERVAL + " isAfter(org.joda.time.ReadableInstant)");
    private final MethodMatcher containsNow = new MethodMatcher(JODA_ABSTRACT_INTERVAL + " containsNow()");
    private final MethodMatcher isAfterNow = new MethodMatcher(JODA_ABSTRACT_INTERVAL + " isAfterNow()");
    private final MethodMatcher isBeforeNow = new MethodMatcher(JODA_ABSTRACT_INTERVAL + " isBeforeNow()");
    private final MethodMatcher toString = new MethodMatcher(JODA_ABSTRACT_INTERVAL + " toString()");

    private final JavaTemplate.Builder getStartTemplate = JavaTemplate.builder("#{any(" + THREE_TEN_EXTRA_INTERVAL + ")}.getStart().atZone(ZoneId.systemDefault())")
            .imports(JAVA_ZONE_ID);
    private final JavaTemplate.Builder getEndTemplate = JavaTemplate.builder("#{any(" + THREE_TEN_EXTRA_INTERVAL + ")}.getEnd().atZone(ZoneId.systemDefault())")
            .imports(JAVA_ZONE_ID);
    private final JavaTemplate.Builder toDurationTemplate = JavaTemplate.builder("#{any(" + THREE_TEN_EXTRA_INTERVAL + ")}.toDuration()");
    private final JavaTemplate.Builder toDurationMillisTemplate = JavaTemplate.builder("#{any(" + THREE_TEN_EXTRA_INTERVAL + ")}.toDuration().toMillis()");
    private final JavaTemplate.Builder containsTemplate = JavaTemplate.builder("#{any(" + THREE_TEN_EXTRA_INTERVAL + ")}.contains(Instant.ofEpochMilli(#{any(long)}))")
            .imports(JAVA_INSTANT);
    private final JavaTemplate.Builder containsInstantTemplate = JavaTemplate.builder("#{any(" + THREE_TEN_EXTRA_INTERVAL + ")}.contains(#{any(java.time.ZonedDateTime)}.toInstant())");
    private final JavaTemplate.Builder overlapsTemplate = JavaTemplate.builder("#{any(" + THREE_TEN_EXTRA_INTERVAL + ")}.overlaps(#{any(org.threeten.extra.Interval)})");
    private final JavaTemplate.Builder containsIntervalTemplate = JavaTemplate.builder("#{any(" + THREE_TEN_EXTRA_INTERVAL + ")}.encloses(#{any(org.threeten.extra.Interval)})");
    private final JavaTemplate.Builder equalsTemplate = JavaTemplate.builder("#{any(" + THREE_TEN_EXTRA_INTERVAL + ")}.equals(#{any(org.threeten.extra.Interval)})");
    private final JavaTemplate.Builder isBeforeTemplate = JavaTemplate.builder("#{any(" + THREE_TEN_EXTRA_INTERVAL + ")}.isBefore(#{any(java.time.ZonedDateTime)}.toInstant())");
    private final JavaTemplate.Builder isAfterTemplate = JavaTemplate.builder("#{any(" + THREE_TEN_EXTRA_INTERVAL + ")}.isAfter(#{any(java.time.ZonedDateTime)}.toInstant())");
    private final JavaTemplate.Builder containsNowTemplate = JavaTemplate.builder("#{any(" + THREE_TEN_EXTRA_INTERVAL + ")}.contains(Instant.now())");
    private final JavaTemplate.Builder isAfterNowTemplate = JavaTemplate.builder("#{any(" + THREE_TEN_EXTRA_INTERVAL + ")}.isAfter(Instant.now())");
    private final JavaTemplate.Builder isBeforeNowTemplate = JavaTemplate.builder("#{any(" + THREE_TEN_EXTRA_INTERVAL + ")}.isBefore(Instant.now())");
    private final JavaTemplate.Builder toStringTemplate = JavaTemplate.builder("#{any(" + THREE_TEN_EXTRA_INTERVAL + ")}.toString()");

    @Getter
    private final List<MethodTemplate> templates = new ArrayList<MethodTemplate>() {
        {
            add(new MethodTemplate(getStart, build(getStartTemplate)));
            add(new MethodTemplate(getEnd, build(getEndTemplate)));
            add(new MethodTemplate(toDuration, build(toDurationTemplate)));
            add(new MethodTemplate(toDurationMillis, build(toDurationMillisTemplate)));
            add(new MethodTemplate(containsReadableInstant, build(containsInstantTemplate)));
            add(new MethodTemplate(contains, build(containsTemplate)));
            add(new MethodTemplate(overlaps, build(overlapsTemplate)));
            add(new MethodTemplate(containsInterval, build(containsIntervalTemplate)));
            add(new MethodTemplate(equals, build(equalsTemplate)));
            add(new MethodTemplate(isBefore, build(isBeforeTemplate)));
            add(new MethodTemplate(isAfter, build(isAfterTemplate)));
            add(new MethodTemplate(containsNow, build(containsNowTemplate)));
            add(new MethodTemplate(isAfterNow, build(isAfterNowTemplate)));
            add(new MethodTemplate(isBeforeNow, build(isBeforeNowTemplate)));
            add(new MethodTemplate(toString, build(toStringTemplate)));
        }
    };

    private JavaTemplate build(JavaTemplate.Builder builder) {
        return builder
                .javaParser(JavaParser.fromJavaVersion().classpath("threeten-extra"))
                .build();
    }
}
