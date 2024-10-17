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

import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;

import java.util.ArrayList;
import java.util.List;

import static org.openrewrite.java.migrate.joda.templates.TimeClassNames.JAVA_UTIL_DATE;
import static org.openrewrite.java.migrate.joda.templates.TimeClassNames.JODA_ABSTRACT_INSTANT;

public class AbstractInstantTemplates {
    private final MethodMatcher toDate = new MethodMatcher(JODA_ABSTRACT_INSTANT + " toDate()");

    private final JavaTemplate toDateTemplate = JavaTemplate.builder("Date.from(#{any(java.time.ZonedDateTime)}.toInstant())")
            .imports(JAVA_UTIL_DATE)
            .build();

    private final List<MethodTemplate> templates = new ArrayList<MethodTemplate>() {
        {
            add(new MethodTemplate(toDate, toDateTemplate));
        }
    };

    public static List<MethodTemplate> getTemplates() {
        return new AbstractInstantTemplates().templates;
    }
}
