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
import org.openrewrite.java.tree.MethodCall;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static org.openrewrite.java.migrate.joda.templates.TimeClassNames.*;

public class AbstractInstantTemplates implements Templates {
    private final MethodMatcher equals = new MethodMatcher(JODA_ABSTRACT_INSTANT + " equals(java.lang.Object)");
    private final MethodMatcher getZone = new MethodMatcher(JODA_ABSTRACT_INSTANT + " getZone()");
    private final MethodMatcher isAfterLong = new MethodMatcher(JODA_ABSTRACT_INSTANT + " isAfter(long)");
    private final MethodMatcher isAfter = new MethodMatcher(JODA_ABSTRACT_INSTANT + " isAfter(org.joda.time.ReadableInstant)");
    private final MethodMatcher isAfterNow = new MethodMatcher(JODA_ABSTRACT_INSTANT + " isAfterNow()");
    private final MethodMatcher isBeforeLong = new MethodMatcher(JODA_ABSTRACT_INSTANT + " isBefore(long)");
    private final MethodMatcher isBefore = new MethodMatcher(JODA_ABSTRACT_INSTANT + " isBefore(org.joda.time.ReadableInstant)");
    private final MethodMatcher isBeforeNow = new MethodMatcher(JODA_ABSTRACT_INSTANT + " isBeforeNow()");
    private final MethodMatcher isEqualLong = new MethodMatcher(JODA_ABSTRACT_INSTANT + " isEqual(long)");
    private final MethodMatcher isEqualReadableInstant = new MethodMatcher(JODA_ABSTRACT_INSTANT + " isEqual(org.joda.time.ReadableInstant)");
    private final MethodMatcher toDate = new MethodMatcher(JODA_ABSTRACT_INSTANT + " toDate()");
    private final MethodMatcher toDateTime = new MethodMatcher(JODA_ABSTRACT_INSTANT + " toDateTime()");
    private final MethodMatcher toInstant = new MethodMatcher(JODA_ABSTRACT_INSTANT + " toInstant()");
    private final MethodMatcher toString = new MethodMatcher(JODA_ABSTRACT_INSTANT + " toString()");
    private final MethodMatcher toStringFormatter = new MethodMatcher(JODA_ABSTRACT_INSTANT + " toString(org.joda.time.format.DateTimeFormatter)");
    private final MethodMatcher compareTo = new MethodMatcher(JODA_ABSTRACT_INSTANT + " compareTo(org.joda.time.ReadableInstant)");

    private final JavaTemplate equalsTemplate = JavaTemplate.builder("#{any(" + JAVA_DATE_TIME + ")}.equals(#{any(java.lang.Object)})").build();
    private final JavaTemplate getZoneTemplate = JavaTemplate.builder("#{any(" + JAVA_DATE_TIME + ")}.getZone()").build();
    private final JavaTemplate isAfterLongTemplate = JavaTemplate.builder("#{any(" + JAVA_DATE_TIME + ")}.isAfter(Instant.ofEpochMilli(#{any(long)}).atZone(ZoneId.systemDefault()))")
            .imports(JAVA_INSTANT, JAVA_ZONE_ID).build();
    private final JavaTemplate isAfterLongTemplateWithInstant = JavaTemplate.builder("#{any(" + JAVA_INSTANT + ")}.isAfter(Instant.ofEpochMilli(#{any(long)}))")
            .imports(JAVA_INSTANT).build();
    private final JavaTemplate isAfterTemplate = JavaTemplate.builder("#{any(" + JAVA_DATE_TIME + ")}.isAfter(#{any(" + JAVA_DATE_TIME + ")})").build();
    private final JavaTemplate isAfterTemplateWithInstant = JavaTemplate.builder("#{any(" + JAVA_INSTANT + ")}.isAfter(#{any(" + JAVA_INSTANT + ")})").build();
    private final JavaTemplate isAfterNowTemplate = JavaTemplate.builder("#{any(" + JAVA_DATE_TIME + ")}.isAfter(ZonedDateTime.now())").build();
    private final JavaTemplate isBeforeLongTemplate = JavaTemplate.builder("#{any(" + JAVA_DATE_TIME + ")}.isBefore(Instant.ofEpochMilli(#{any(long)}).atZone(ZoneId.systemDefault()))")
            .imports(JAVA_INSTANT, JAVA_ZONE_ID).build();
    private final JavaTemplate isBeforeLongTemplateWithInstant = JavaTemplate.builder("#{any(" + JAVA_INSTANT + ")}.isBefore(Instant.ofEpochMilli(#{any(long)}))")
            .imports(JAVA_INSTANT).build();
    private final JavaTemplate isBeforTemplate = JavaTemplate.builder("#{any(" + JAVA_DATE_TIME + ")}.isBefore(#{any(" + JAVA_DATE_TIME + ")})").build();
    private final JavaTemplate isBeforeTemplateWithInstant = JavaTemplate.builder("#{any(" + JAVA_INSTANT + ")}.isBefore(#{any(" + JAVA_INSTANT + ")})").build();
    private final JavaTemplate isBeforeNowTemplate = JavaTemplate.builder("#{any(" + JAVA_DATE_TIME + ")}.isBefore(ZonedDateTime.now())")
            .imports(JAVA_DATE_TIME).build();
    private final JavaTemplate isEqualLongTemplate = JavaTemplate.builder("#{any(" + JAVA_DATE_TIME + ")}.isEqual(Instant.ofEpochMilli(#{any(long)}).atZone(ZoneId.systemDefault()))")
            .imports(JAVA_INSTANT, JAVA_ZONE_ID).build();
    private final JavaTemplate isEqualReadableInstantTemplate = JavaTemplate.builder("#{any(" + JAVA_DATE_TIME + ")}.isEqual(#{any(" + JAVA_DATE_TIME + ")})").build();
    private final JavaTemplate toDateTemplate = JavaTemplate.builder("Date.from(#{any(" + JAVA_DATE_TIME + ")}.toInstant())")
            .imports(JAVA_UTIL_DATE)
            .build();
    private final JavaTemplate toDateTimeTemplate = JavaTemplate.builder("#{any(" + JAVA_DATE_TIME + ")}").build();
    private final JavaTemplate toInstantTemplate = JavaTemplate.builder("#{any(" + JAVA_DATE_TIME + ")}.toInstant()").build();
    private final JavaTemplate toStringTemplate = JavaTemplate.builder("#{any(" + JAVA_DATE_TIME + ")}.toString()").build();
    private final JavaTemplate toStringFormatterTemplate = JavaTemplate.builder("#{any(" + JAVA_DATE_TIME + ")}.format(#{any(" + JAVA_TIME_FORMATTER + ")})").build();
    private final JavaTemplate compareToTemplate = JavaTemplate.builder("#{any(" + JAVA_DATE_TIME + ")}.compareTo(#{any(java.time.ZonedDateTime)})").build();

    @Getter
    private final List<MethodTemplate> templates = new ArrayList<MethodTemplate>() {
        {
            add(new MethodTemplate(equals, equalsTemplate));
            add(new MethodTemplate(getZone, getZoneTemplate));
            add(new MethodTemplate(isAfterLong, isAfterLongTemplate));
            add(new MethodTemplate(isAfterLong, isAfterLongTemplateWithInstant));
            add(new MethodTemplate(isAfter, isAfterTemplate));
            add(new MethodTemplate(isAfter, isAfterTemplateWithInstant));
            add(new MethodTemplate(isAfterNow, isAfterNowTemplate));
            add(new MethodTemplate(isBeforeLong, isBeforeLongTemplate));
            add(new MethodTemplate(isBeforeLong, isBeforeLongTemplateWithInstant));
            add(new MethodTemplate(isBefore, isBeforTemplate));
            add(new MethodTemplate(isBefore, isBeforeTemplateWithInstant));
            add(new MethodTemplate(isBeforeNow, isBeforeNowTemplate));
            add(new MethodTemplate(isEqualLong, isEqualLongTemplate));
            add(new MethodTemplate(isEqualReadableInstant, isEqualReadableInstantTemplate));
            add(new MethodTemplate(toDate, toDateTemplate));
            add(new MethodTemplate(toDateTime, toDateTimeTemplate));
            add(new MethodTemplate(toInstant, toInstantTemplate));
            add(new MethodTemplate(toString, toStringTemplate));
            add(new MethodTemplate(toStringFormatter, toStringFormatterTemplate));
            add(new MethodTemplate(compareTo, compareToTemplate));
        }
    };

    @Override
    public boolean matchesMethodCall(MethodCall method, MethodTemplate template) {
        if (method instanceof J.NewClass) {
            return true;
        }
        Expression select = ((J.MethodInvocation) method).getSelect();
        if (select != null && select.getType() != null && select.getType().isAssignableFrom(Pattern.compile(JODA_INSTANT))) {
            return Arrays.asList(
                    isAfterLongTemplateWithInstant,
                    isAfterTemplateWithInstant,
                    isBeforeLongTemplateWithInstant,
                    isBeforeTemplateWithInstant
            ).contains(template.getTemplate());
        }
        return true;
    }
}
