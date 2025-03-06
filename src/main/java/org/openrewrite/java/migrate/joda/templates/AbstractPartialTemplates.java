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

import static org.openrewrite.java.migrate.joda.templates.TimeClassNames.JODA_ABSTRACT_PARTIAL;

public class AbstractPartialTemplates implements Templates {
    private final MethodMatcher isAfter = new MethodMatcher(JODA_ABSTRACT_PARTIAL + " isAfter(org.joda.time.ReadablePartial)");
    private final MethodMatcher isBefore = new MethodMatcher(JODA_ABSTRACT_PARTIAL + " isBefore(org.joda.time.ReadablePartial)");
    private final MethodMatcher isEqual = new MethodMatcher(JODA_ABSTRACT_PARTIAL + " isEqual(org.joda.time.ReadablePartial)");

    private final JavaTemplate.Builder isAfterTemplate = JavaTemplate.builder("#{any(java.time.chrono.ChronoLocalDate)}.isAfter(#{any(java.time.chrono.ChronoLocalDate)})");
    private final JavaTemplate.Builder isBeforeTemplate = JavaTemplate.builder("#{any(java.time.chrono.ChronoLocalDate)}.isBefore(#{any(java.time.chrono.ChronoLocalDate)})");
    private final JavaTemplate.Builder isEqualTemplate = JavaTemplate.builder("#{any(java.time.chrono.ChronoLocalDate)}.isEqual(#{any(java.time.chrono.ChronoLocalDate)})");

    @Getter
    private final List<MethodTemplate> templates = new ArrayList<MethodTemplate>() {
        {
            add(new MethodTemplate(isAfter, build(isAfterTemplate)));
            add(new MethodTemplate(isBefore, build(isBeforeTemplate)));
            add(new MethodTemplate(isEqual, build(isEqualTemplate)));
        }
    };

    private JavaTemplate build(JavaTemplate.Builder builder) {
        return builder.build();
    }
}
