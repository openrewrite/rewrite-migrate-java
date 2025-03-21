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

import static org.openrewrite.java.migrate.joda.templates.TimeClassNames.JODA_BASE_INTERVAL;
import static org.openrewrite.java.migrate.joda.templates.TimeClassNames.THREE_TEN_EXTRA_INTERVAL;

public class BaseIntervalTemplates implements Templates {
    private final MethodMatcher getStartMillis = new MethodMatcher(JODA_BASE_INTERVAL + " getStartMillis()");
    private final MethodMatcher getEndMillis = new MethodMatcher(JODA_BASE_INTERVAL + " getEndMillis()");

    private final JavaTemplate getStartMillisTemplate = JavaTemplate.builder("#{any(" + THREE_TEN_EXTRA_INTERVAL + ")}.getStart().toEpochMilli()")
            .javaParser(JavaParser.fromJavaVersion().classpath("threeten-extra"))
            .build();
    private final JavaTemplate getEndMillisTemplate = JavaTemplate.builder("#{any(" + THREE_TEN_EXTRA_INTERVAL + ")}.getEnd().toEpochMilli()")
            .javaParser(JavaParser.fromJavaVersion().classpath("threeten-extra"))
            .build();

    @Getter
    private final List<MethodTemplate> templates;
    {
        templates = new ArrayList<>();
        templates.add(new MethodTemplate(getStartMillis, getStartMillisTemplate));
        templates.add(new MethodTemplate(getEndMillis, getEndMillisTemplate));
    }
}
