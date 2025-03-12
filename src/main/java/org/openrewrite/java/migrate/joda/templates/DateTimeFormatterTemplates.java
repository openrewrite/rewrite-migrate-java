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
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.util.ArrayList;
import java.util.List;

import static org.openrewrite.java.migrate.joda.templates.TimeClassNames.*;

public class DateTimeFormatterTemplates implements Templates {
    private final MethodMatcher parseDateTime = new MethodMatcher(JODA_TIME_FORMATTER + " parseDateTime(java.lang.String)");
    private final MethodMatcher parseMillis = new MethodMatcher(JODA_TIME_FORMATTER + " parseMillis(java.lang.String)");
    private final MethodMatcher printLong = new MethodMatcher(JODA_TIME_FORMATTER + " print(long)");
    private final MethodMatcher printDateTime = new MethodMatcher(JODA_TIME_FORMATTER + " print(org.joda.time.ReadableInstant)");
    private final MethodMatcher printLocalDate = new MethodMatcher(JODA_TIME_FORMATTER + " print(org.joda.time.ReadablePartial)");
    private final MethodMatcher withZone = new MethodMatcher(JODA_TIME_FORMATTER + " withZone(org.joda.time.DateTimeZone)");
    private final MethodMatcher withZoneUTC = new MethodMatcher(JODA_TIME_FORMATTER + " withZoneUTC()");

    private final JavaTemplate parseDateTimeTemplate = JavaTemplate.builder("ZonedDateTime.parse(#{any(java.lang.String)}, #{any(" + JAVA_TIME_FORMATTER + ")})")
            .imports(JAVA_DATE_TIME).build();
    private final JavaTemplate parseMillisTemplate = JavaTemplate.builder("ZonedDateTime.parse(#{any(java.lang.String)}, #{any(" + JAVA_TIME_FORMATTER + ")}).toInstant().toEpochMilli()")
            .imports(JAVA_DATE_TIME).build();
    private final JavaTemplate printLongTemplate = JavaTemplate.builder("ZonedDateTime.ofInstant(Instant.ofEpochMilli(#{any(long)}), ZoneId.systemDefault()).format( #{any(" + JAVA_TIME_FORMATTER + ")})")
            .imports(JAVA_DATE_TIME, JAVA_INSTANT, JAVA_ZONE_ID)
            .build();
    private final JavaTemplate printDateTimeTemplate = JavaTemplate.builder("#{any(" + JAVA_DATE_TIME + ")}.format(#{any(" + JAVA_TIME_FORMATTER + ")})").build();
    private final JavaTemplate printLocalDateTemplate = JavaTemplate.builder("#{any(" + JAVA_LOCAL_DATE + ")}.format(#{any(" + JAVA_TIME_FORMATTER + ")})").build();
    private final JavaTemplate withZoneTemplate = JavaTemplate.builder("#{any(" + JAVA_TIME_FORMATTER + ")}.withZone(#{any(" + JAVA_ZONE_ID + ")})").build();
    private final JavaTemplate withZoneUTCTemplate = JavaTemplate.builder("#{any(" + JAVA_TIME_FORMATTER + ")}.withZone(ZoneOffset.UTC)")
            .imports(JAVA_ZONE_OFFSET).build();

    @Getter
    private final List<MethodTemplate> templates = new ArrayList<MethodTemplate>() {
        {
            add(new MethodTemplate(parseDateTime, parseDateTimeTemplate, m -> {
                J.MethodInvocation mi = (J.MethodInvocation) m;
                return new Expression[]{mi.getArguments().get(0), mi.getSelect()};
            }));
            add(new MethodTemplate(parseMillis, parseMillisTemplate, m -> {
                J.MethodInvocation mi = (J.MethodInvocation) m;
                return new Expression[]{mi.getArguments().get(0), mi.getSelect()};
            }));
            add(new MethodTemplate(printLong, printLongTemplate, m -> {
                J.MethodInvocation mi = (J.MethodInvocation) m;
                return new Expression[]{mi.getArguments().get(0), mi.getSelect()};
            }));
            add(new MethodTemplate(printDateTime, printDateTimeTemplate, m -> {
                J.MethodInvocation mi = (J.MethodInvocation) m;
                return new Expression[]{mi.getArguments().get(0), mi.getSelect()};
            }));
            add(new MethodTemplate(printLocalDate, printLocalDateTemplate, m -> {
                J.MethodInvocation mi = (J.MethodInvocation) m;
                return new Expression[]{mi.getArguments().get(0), mi.getSelect()};
            }));
            add(new MethodTemplate(withZone, withZoneTemplate));
            add(new MethodTemplate(withZoneUTC, withZoneUTCTemplate));
        }
    };
}
