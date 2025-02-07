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
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;

import java.util.ArrayList;
import java.util.List;

import static org.openrewrite.java.migrate.joda.templates.TimeClassNames.JAVA_INSTANT;
import static org.openrewrite.java.migrate.joda.templates.TimeClassNames.JODA_INSTANT;

public class InstantTemplates implements Templates {
    private final MethodMatcher constructor = new MethodMatcher(JODA_INSTANT + " <constructor>()");
    private final MethodMatcher getMillis = new MethodMatcher(JODA_INSTANT + " getMillis()");
    private final MethodMatcher minusDuration = new MethodMatcher(JODA_INSTANT + " minus(org.joda.time.ReadableDuration)");
    private final MethodMatcher now = new MethodMatcher(JODA_INSTANT + " now()");
    private final MethodMatcher ofEpochMilli = new MethodMatcher(JODA_INSTANT + " ofEpochMilli(long)");
    private final MethodMatcher parse = new MethodMatcher(JODA_INSTANT + " parse(java.lang.String)");
    private final MethodMatcher plusDuration = new MethodMatcher(JODA_INSTANT + " plus(org.joda.time.ReadableDuration)");

    private final JavaTemplate constructorTemplate = JavaTemplate.builder("Instant.now()")
            .imports(JAVA_INSTANT).build();
    private final JavaTemplate getMillisTemplate = JavaTemplate.builder("#{any(" + JAVA_INSTANT + ")}.toEpochMilli()").build();
    private final JavaTemplate minusDurationTemplate = JavaTemplate.builder("#{any(" + JAVA_INSTANT + ")}.minus(#{any(java.time.Duration)})").build();
    private final JavaTemplate nowTemplate = JavaTemplate.builder("Instant.now()")
            .imports(JAVA_INSTANT).build();
    private final JavaTemplate ofEpochMilliTemplate = JavaTemplate.builder("Instant.ofEpochMilli(#{any(long)})")
            .imports(JAVA_INSTANT).build();
    private final JavaTemplate parseTemplate = JavaTemplate.builder("Instant.parse(#{any(java.lang.String)})")
            .imports(JAVA_INSTANT).build();
    private final JavaTemplate plusDurationTemplate = JavaTemplate.builder("#{any(" + JAVA_INSTANT + ")}.plus(#{any(java.time.Duration)})").build();

    @Getter
    private final List<MethodTemplate> templates;
    {
        templates = new ArrayList<>();
        templates.add(new MethodTemplate(constructor, constructorTemplate));
        templates.add(new MethodTemplate(getMillis, getMillisTemplate));
        templates.add(new MethodTemplate(getMillis, getMillisTemplate));
        templates.add(new MethodTemplate(minusDuration, minusDurationTemplate));
        templates.add(new MethodTemplate(now, nowTemplate));
        templates.add(new MethodTemplate(ofEpochMilli, ofEpochMilliTemplate));
        templates.add(new MethodTemplate(parse, parseTemplate));
        templates.add(new MethodTemplate(plusDuration, plusDurationTemplate));
    }
}
