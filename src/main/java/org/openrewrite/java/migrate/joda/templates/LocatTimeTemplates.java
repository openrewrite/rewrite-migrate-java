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

import java.util.ArrayList;
import java.util.List;

import static org.openrewrite.java.migrate.joda.templates.TimeClassNames.JAVA_LOCAL_DATE;
import static org.openrewrite.java.migrate.joda.templates.TimeClassNames.JODA_LOCAL_TIME;

@NoArgsConstructor
public class LocatTimeTemplates implements Templates {
    final MethodMatcher newLocalTimeNoArgs = new MethodMatcher(JODA_LOCAL_TIME + "<constructor>()");
    final MethodMatcher newLocalTimeString = new MethodMatcher(JODA_LOCAL_TIME + "<constructor>(Object)");
    final MethodMatcher newLocalTimeHms = new MethodMatcher(JODA_LOCAL_TIME + "<constructor>(int,int,int)");
    final MethodMatcher newLocalTimeHmsM = new MethodMatcher(JODA_LOCAL_TIME + "<constructor>(int,int,int,int)");
    final MethodMatcher now = new MethodMatcher(JODA_LOCAL_TIME + " now()");
    final MethodMatcher parse = new MethodMatcher(JODA_LOCAL_TIME + " parse(String)");
    final MethodMatcher plusMinutes = new MethodMatcher(JODA_LOCAL_TIME + " plusMinutes(int)");
    final MethodMatcher plusSeconds = new MethodMatcher(JODA_LOCAL_TIME + " plusSeconds(int)");
    final MethodMatcher plusMillis = new MethodMatcher(JODA_LOCAL_TIME + " plusMillis(int)");
    final MethodMatcher minusMinutes = new MethodMatcher(JODA_LOCAL_TIME + " minusMinutes(int)");
    final MethodMatcher minusSeconds = new MethodMatcher(JODA_LOCAL_TIME + " minusSeconds(int)");
    final MethodMatcher minusMillis = new MethodMatcher(JODA_LOCAL_TIME + " minusMillis(int)");

    final JavaTemplate.Builder localTimeNoArgsTemplate = JavaTemplate.builder("LocalTime.now()");
    final JavaTemplate.Builder localTimeStringTemplate = JavaTemplate.builder("LocalTime.parse(#{any(Object)})");
    final JavaTemplate.Builder localTimeHmsTemplate = JavaTemplate.builder("LocalTime.of(#{any(int)}, #{any(int)}, #{any(int)})");
    final JavaTemplate.Builder localTimeHmsMTemplate = JavaTemplate.builder("LocalTime.of(#{any(int)}, #{any(int)}, #{any(int)}, #{any(int)})");
    final JavaTemplate.Builder nowTemplate = JavaTemplate.builder("LocalTime.now()");
    final JavaTemplate.Builder parseTemplate = JavaTemplate.builder("LocalTime.parse(#{any(String)})");
    final JavaTemplate.Builder plusMinutesTemplate = JavaTemplate.builder("#{any(java.time.LocalTime)}.plusMinutes(#{any(int)})");
    final JavaTemplate.Builder plusSecondsTemplate = JavaTemplate.builder("#{any(java.time.LocalTime)}.plusSeconds(#{any(int)})");
    final JavaTemplate.Builder plusMillisTemplate = JavaTemplate.builder("#{any(java.time.LocalTime)}.plusNanos(#{any(int)})");
    final JavaTemplate.Builder minusMinutesTemplate = JavaTemplate.builder("#{any(java.time.LocalTime)}.minusMinutes(#{any(int)})");
    final JavaTemplate.Builder minusSecondsTemplate = JavaTemplate.builder("#{any(java.time.LocalTime)}.minusSeconds(#{any(int)})");
    final JavaTemplate.Builder minusMillisTemplate = JavaTemplate.builder("#{any(java.time.LocalTime)}.minusNanos(#{any(int)})");

    @Getter
    private final List<MethodTemplate> templates = new ArrayList<MethodTemplate>() {
        {
            add(new MethodTemplate(newLocalTimeNoArgs, build(localTimeNoArgsTemplate)));
            add(new MethodTemplate(newLocalTimeString, build(localTimeStringTemplate)));
            add(new MethodTemplate(newLocalTimeHms, build(localTimeHmsTemplate)));
            add(new MethodTemplate(newLocalTimeHmsM, build(localTimeHmsMTemplate)));
            add(new MethodTemplate(now, build(nowTemplate)));
            add(new MethodTemplate(parse, build(parseTemplate)));
            add(new MethodTemplate(plusMinutes, build(plusMinutesTemplate)));
            add(new MethodTemplate(plusSeconds, build(plusSecondsTemplate)));
            add(new MethodTemplate(plusMillis, build(plusMillisTemplate)));
            add(new MethodTemplate(minusMinutes, build(minusMinutesTemplate)));
            add(new MethodTemplate(minusSeconds, build(minusSecondsTemplate)));
            add(new MethodTemplate(minusMillis, build(minusMillisTemplate)));
       }
    };

    private JavaTemplate build(JavaTemplate.Builder builder) {
        return buildWithImport(builder, JAVA_LOCAL_DATE);
    }
}
