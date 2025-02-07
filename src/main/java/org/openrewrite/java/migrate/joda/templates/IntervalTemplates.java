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

    private final JavaTemplate intervalTemplate = JavaTemplate.builder("Interval.of(Instant.ofEpochMilli(#{any(long)}), Instant.ofEpochMilli(#{any(long)}))")
            .javaParser(JavaParser.fromJavaVersion().classpath("threeten-extra"))
            .imports(JAVA_INSTANT, THREE_TEN_EXTRA_INTERVAL)
            .build();
    private final JavaTemplate intervalWithDateTimeTemplate = JavaTemplate.builder("Interval.of(#{any(" + JAVA_DATE_TIME + ")}.toInstant(), #{any(" + JAVA_DATE_TIME + ")}.toInstant())")
            .javaParser(JavaParser.fromJavaVersion().classpath("threeten-extra"))
            .imports(THREE_TEN_EXTRA_INTERVAL)
            .build();
    private final JavaTemplate intervalWithDateTimeAndDurationTemplate = JavaTemplate.builder("Interval.of(#{any(" + JAVA_DATE_TIME + ")}.toInstant(), #{any(" + JAVA_DURATION + ")})")
            .javaParser(JavaParser.fromJavaVersion().classpath("threeten-extra"))
            .imports(THREE_TEN_EXTRA_INTERVAL)
            .build();

    @Getter
    private final List<MethodTemplate> templates;
    {
        templates = new ArrayList<>();
        templates.add(new MethodTemplate(interval, intervalTemplate));
        templates.add(new MethodTemplate(intervalWithTimeZone, intervalTemplate,
                m -> new Expression[]{m.getArguments().get(0), m.getArguments().get(1)}));
        templates.add(new MethodTemplate(intervalWithDateTime, intervalWithDateTimeTemplate));
        templates.add(new MethodTemplate(intervalWithDateTimeAndDuration, intervalWithDateTimeAndDurationTemplate));
    }
}
