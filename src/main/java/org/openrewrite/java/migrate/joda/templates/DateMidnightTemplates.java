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
import lombok.NoArgsConstructor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.util.ArrayList;
import java.util.List;

import static org.openrewrite.java.migrate.joda.templates.TimeClassNames.*;

@NoArgsConstructor
public class DateMidnightTemplates implements Templates {
    private final MethodMatcher newDateMidnight = new MethodMatcher(JODA_DATE_MIDNIGHT + "<constructor>()");
    private final MethodMatcher newDateMidnightAtDate = new MethodMatcher(JODA_DATE_MIDNIGHT + "<constructor>(int, int, int)");

    private final JavaTemplate.Builder dateTimeTemplate = JavaTemplate.builder("LocalDate.now().atStartOfDay(ZoneId.systemDefault())")
            .imports(JAVA_ZONE_ID);
    private final JavaTemplate.Builder dateTimeWithYMD = JavaTemplate.builder("LocalDate.of(#{any(int)}, #{any(int)}, #{any(int)}).atStartOfDay(ZoneId.systemDefault())")
            .imports(JAVA_ZONE_ID);

    @Getter
    private final List<MethodTemplate> templates = new ArrayList<MethodTemplate>() {
        {
            add(new MethodTemplate(newDateMidnight, build(dateTimeTemplate)));
            add(new MethodTemplate(newDateMidnightAtDate, build(dateTimeWithYMD)));
        }
    };

    private JavaTemplate build(JavaTemplate.Builder builder) {
        return buildWithImport(builder, JAVA_DATE_TIME);
    }
}
