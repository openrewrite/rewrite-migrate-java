/*
 * Copyright 2024 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
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
    
    private final JavaTemplate getStartTemplate = JavaTemplate.builder("#{any(" + THREE_TEN_EXTRA_INTERVAL + ")}.getStart().atZone(ZoneId.systemDefault())")
            .javaParser(JavaParser.fromJavaVersion().classpath("threeten-extra"))
            .imports(JAVA_ZONE_ID)
            .build();
    private final JavaTemplate getEndTemplate = JavaTemplate.builder("#{any(" + THREE_TEN_EXTRA_INTERVAL + ")}.getEnd().atZone(ZoneId.systemDefault())")
            .javaParser(JavaParser.fromJavaVersion().classpath("threeten-extra"))
            .imports(JAVA_ZONE_ID)
            .build();
    private final JavaTemplate toDurationTemplate = JavaTemplate.builder("#{any(" + THREE_TEN_EXTRA_INTERVAL + ")}.toDuration()")
            .javaParser(JavaParser.fromJavaVersion().classpath("threeten-extra"))
            .build();
    private final JavaTemplate toDurationMillisTemplate = JavaTemplate.builder("#{any(" + THREE_TEN_EXTRA_INTERVAL + ")}.toDuration().toMillis()")
            .javaParser(JavaParser.fromJavaVersion().classpath("threeten-extra"))
            .build();
    private final JavaTemplate containsTemplate = JavaTemplate.builder("#{any(" + THREE_TEN_EXTRA_INTERVAL + ")}.contains(Instant.ofEpochMilli(#{any(long)}))")
            .javaParser(JavaParser.fromJavaVersion().classpath("threeten-extra"))
            .imports(JAVA_INSTANT)
            .build();
    
    @Getter
    private final List<MethodTemplate> templates = new ArrayList<MethodTemplate>() {
        {
            add(new MethodTemplate(getStart, getStartTemplate));
            add(new MethodTemplate(getEnd, getEndTemplate));
            add(new MethodTemplate(toDuration, toDurationTemplate));
            add(new MethodTemplate(toDurationMillis, toDurationMillisTemplate));
            add(new MethodTemplate(contains, containsTemplate));
        }
    };
}