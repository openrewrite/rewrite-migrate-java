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
 */package org.openrewrite.java.migrate.joda.templates;

import lombok.Getter;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;

import java.util.ArrayList;
import java.util.List;

import static org.openrewrite.java.migrate.joda.templates.TimeClassNames.JAVA_DURATION;
import static org.openrewrite.java.migrate.joda.templates.TimeClassNames.JODA_DURATION;

public class AbstractDurationTemplates implements Templates {
    private final MethodMatcher isLongerThan = new MethodMatcher(JODA_DURATION + " isLongerThan(..)");
    private final MethodMatcher toPeriod = new MethodMatcher(JODA_DURATION + " toPeriod()");

    private final JavaTemplate isLongerThanTemplate = JavaTemplate.builder("#{any(" + JAVA_DURATION + ")}.compareTo(#{any(" + JAVA_DURATION + ")}) > 0").build();
    private final JavaTemplate toPeriodTemplate = JavaTemplate.builder("#{any(" + JAVA_DURATION + ")}.toPeriod()").build();

    @Getter
    private final List<MethodTemplate> templates = new ArrayList<MethodTemplate>() {
        {
            add(new MethodTemplate(isLongerThan, isLongerThanTemplate));
            add(new MethodTemplate(toPeriod, toPeriodTemplate));
        }
    };
}
