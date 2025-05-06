/*
 * Copyright 2025 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;

import static org.openrewrite.java.migrate.joda.templates.TimeClassNames.*;

//todo mapping is not working
@NoArgsConstructor
public class ChronologyTemplates implements Templates{
    final MethodMatcher getInstanceUTC = new MethodMatcher(JODA_GEORGIAN_CHRONOLOGY + " getInstanceUTC()");
    final MethodMatcher getDateTimeMillis = new MethodMatcher(JODA_BASIC_CHRONOLOGY + " getDateTimeMillis(int,int,int,int)");

    final JavaTemplate.Builder instanceTemplate = JavaTemplate.builder("IsoChronology.INSTANCE");
    final JavaTemplate.Builder epochSecondTemplate = JavaTemplate.builder("#{any(java.time.Duration)}.epochSecond(#{any(int)},#{any(int)},#{any(int)},#{any(int)},0,0,0,ZoneOffset.ofTotalSeconds(0))")
            .imports(JAVA_ZONE_OFFSET);

    @Getter
    private final List<MethodTemplate> templates = new ArrayList<MethodTemplate>() {
        {
            add(new MethodTemplate(getInstanceUTC, build(instanceTemplate)));
            add(new MethodTemplate(getDateTimeMillis, build(epochSecondTemplate)));
        }
    };

    private JavaTemplate build(JavaTemplate.Builder builder) {
        return buildWithImport(builder, JAVA_ISA_CHRONOLOGY);
    }
}
