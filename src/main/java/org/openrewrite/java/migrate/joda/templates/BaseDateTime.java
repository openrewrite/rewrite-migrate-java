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

import static org.openrewrite.java.migrate.joda.templates.TimeClassNames.JODA_BASE_DATE_TIME;

public class BaseDateTime implements Templates {
    private final MethodMatcher getMillis = new MethodMatcher(JODA_BASE_DATE_TIME + " getMillis()");

    private final JavaTemplate getMillisTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.toInstant().toEpochMilli()")
            .build();

    @Getter
    private final List<MethodTemplate> templates;
    {
        templates = new ArrayList<>();
        templates.add(new MethodTemplate(getMillis, getMillisTemplate));
    }
}
