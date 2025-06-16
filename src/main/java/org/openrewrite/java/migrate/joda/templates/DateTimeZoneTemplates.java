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

import java.util.ArrayList;
import java.util.List;

import static org.openrewrite.java.migrate.joda.templates.TimeClassNames.*;

public class DateTimeZoneTemplates implements Templates {
    private final MethodMatcher zoneForID = new MethodMatcher(JODA_DATE_TIME_ZONE + " forID(String)");
    private final MethodMatcher getDefault = new MethodMatcher(JODA_DATE_TIME_ZONE + " getDefault()");
    private final MethodMatcher zoneForOffsetHours = new MethodMatcher(JODA_DATE_TIME_ZONE + " forOffsetHours(int)");
    private final MethodMatcher zoneForOffsetHoursMinutes = new MethodMatcher(JODA_DATE_TIME_ZONE + " forOffsetHoursMinutes(int, int)");
    private final MethodMatcher zoneForOffsetMillis = new MethodMatcher(JODA_DATE_TIME_ZONE + " forOffsetMillis(int)");
    private final MethodMatcher zoneForTimeZone = new MethodMatcher(JODA_DATE_TIME_ZONE + " forTimeZone(java.util.TimeZone)");
    private final MethodMatcher getOffset = new MethodMatcher(JODA_DATE_TIME_ZONE + " getOffset(org.joda.time.ReadableInstant)");
    private final MethodMatcher getStandardOffset = new MethodMatcher(JODA_DATE_TIME_ZONE + " getStandardOffset(long)");
    private final MethodMatcher toTimeZone = new MethodMatcher(JODA_DATE_TIME_ZONE + " toTimeZone()");
    private final MethodMatcher getId = new MethodMatcher(JODA_DATE_TIME_ZONE + " getID()");
    private final MethodMatcher equals = new MethodMatcher(JODA_DATE_TIME_ZONE + " equals(java.lang.Object)");
    private final MethodMatcher zoneGetOffsetFromLocal = new MethodMatcher(JODA_DATE_TIME_ZONE + " getOffsetFromLocal(long)");
    private final MethodMatcher zoneIsLocalDateTimeGap = new MethodMatcher(JODA_DATE_TIME_ZONE + " isLocalDateTimeGap(org.joda.time.LocalDateTime)");

    //getOffsetFromLocal(long)
    //timezone.isLocalDateTimeGap(endSafe)

    private final JavaTemplate.Builder zoneIdOfTemplate = JavaTemplate.builder("ZoneId.of(#{any(String)})")
            .imports(JAVA_ZONE_ID);
    private final JavaTemplate.Builder systemDefaultTemplate = JavaTemplate.builder("ZoneId.systemDefault()")
            .imports(JAVA_ZONE_ID);
    private final JavaTemplate.Builder zoneOffsetHoursTemplate = JavaTemplate.builder("ZoneOffset.ofHours(#{any(int)})");
    private final JavaTemplate.Builder zoneOffsetHoursMinutesTemplate = JavaTemplate.builder("ZoneOffset.ofHoursMinutes(#{any(int)}, #{any(int)})");
    private final JavaTemplate.Builder zoneOffsetMillisTemplate = JavaTemplate.builder("ZoneOffset.ofTotalSeconds(#{any(int)} / 1000)");
    private final JavaTemplate.Builder timeZoneToZoneIdTemplate = JavaTemplate.builder("#{any(java.util.TimeZone)}.toZoneId()");
    private final JavaTemplate.Builder getOffsetInMillisTemplate = JavaTemplate.builder("#{any(java.time.ZonedDateTime)}.getOffset().getTotalSeconds() * 1000");
    private final JavaTemplate.Builder getStandardOffsetTemplate = JavaTemplate.builder("(int)Duration.ofSeconds(#{any(java.time.ZoneId)}.getRules().getStandardOffset(Instant.ofEpochMilli(#{any(long)})).getTotalSeconds()).toMillis()")
            .imports(JAVA_ZONE_ID, JAVA_DURATION);
    private final JavaTemplate.Builder getTimeZone = JavaTemplate.builder("TimeZone.getTimeZone(#{any(java.time.ZoneId)})");
    private final JavaTemplate.Builder getIdTemplate = JavaTemplate.builder("#{any(java.time.ZoneId)}.getId()")
            .imports(JAVA_ZONE_ID);
    private final JavaTemplate.Builder equalsTemplate = JavaTemplate.builder("#{any(java.time.ZoneId)}.equals()");
    private final JavaTemplate.Builder getOffsetTemplate = JavaTemplate.builder("#{any(java.time.ZoneId)}.getRules().getOffset(Instant.ofEpochMilli(#{any(long)}))");
    private final JavaTemplate.Builder isGapTemplate = JavaTemplate.builder("#{any(java.time.ZoneId)}.getRules().getValidOffsets(#{any(java.time.LocalDateTime)}).isEmpty()");

    @Getter
    private final List<MethodTemplate> templates = new ArrayList<MethodTemplate>() {
        {
            add(new MethodTemplate(zoneForID, build(zoneIdOfTemplate)));
            add(new MethodTemplate(getDefault, build(systemDefaultTemplate)));
            add(new MethodTemplate(zoneForOffsetHours, build(zoneOffsetHoursTemplate)));
            add(new MethodTemplate(zoneForOffsetHoursMinutes, build(zoneOffsetHoursMinutesTemplate)));
            add(new MethodTemplate(zoneForOffsetMillis, build(zoneOffsetMillisTemplate)));
            add(new MethodTemplate(zoneForTimeZone, build(timeZoneToZoneIdTemplate)));
            add(new MethodTemplate(getOffset, build(getOffsetInMillisTemplate),
                    m -> new Expression[]{m.getArguments().get(0)}));
            add(new MethodTemplate(getStandardOffset, build(getStandardOffsetTemplate)));
            add(new MethodTemplate(toTimeZone, build(getTimeZone)));
            add(new MethodTemplate(getId, build(getIdTemplate)));
            add(new MethodTemplate(equals, build(equalsTemplate)));
            add(new MethodTemplate(zoneGetOffsetFromLocal, build(getOffsetTemplate)));
            add(new MethodTemplate(zoneIsLocalDateTimeGap, build(isGapTemplate)));
        }
    };

    private JavaTemplate build(JavaTemplate.Builder builder) {
        return buildWithImport(builder, JAVA_ZONE_OFFSET);
    }
}
