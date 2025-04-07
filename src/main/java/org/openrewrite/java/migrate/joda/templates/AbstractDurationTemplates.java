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

import static org.openrewrite.java.migrate.joda.templates.TimeClassNames.*;

public class AbstractDurationTemplates implements Templates {
    private final MethodMatcher isLongerThan = new MethodMatcher(JODA_ABSTRACT_DURATION + " isLongerThan(..)");
    private final MethodMatcher toPeriod = new MethodMatcher(JODA_ABSTRACT_DURATION + " toPeriod()");
    private final MethodMatcher toString = new MethodMatcher(JODA_ABSTRACT_DURATION + " toString()");
    private final MethodMatcher compareTo = new MethodMatcher(JODA_ABSTRACT_DURATION + " compareTo(org.joda.time.ReadableDuration)");
    private final MethodMatcher isEquals = new MethodMatcher(JODA_ABSTRACT_DURATION + " isEqual(org.joda.time.ReadableDuration)");
    private final MethodMatcher equals = new MethodMatcher(JODA_ABSTRACT_DURATION + " equals(Object)");

    private final JavaTemplate isLongerThanTemplate = JavaTemplate.builder("#{any(" + JAVA_DURATION + ")}.compareTo(#{any(" + JAVA_DURATION + ")}) > 0").build();
    private final JavaTemplate toPeriodTemplate = JavaTemplate.builder("#{any(" + JAVA_DURATION + ")}.toPeriod()").build();
    private final JavaTemplate toStringTemplate = JavaTemplate.builder("#{any(" + JAVA_DURATION + ")}.toString()").build();
    private final JavaTemplate compareToTemplate = JavaTemplate.builder("#{any(java.time.Duration)}.compareTo(#{any(java.time.Duration)})").build();
    private final JavaTemplate equalsTemplate = JavaTemplate.builder("#{any(" + JAVA_DURATION + ")}.equals(#{any(Object)})").build();

    @Getter
    private final List<MethodTemplate> templates = new ArrayList<MethodTemplate>() {
        {
            add(new MethodTemplate(isLongerThan, isLongerThanTemplate));
            add(new MethodTemplate(toPeriod, toPeriodTemplate));
            add(new MethodTemplate(toString, toStringTemplate));
            add(new MethodTemplate(compareTo, compareToTemplate));
            add(new MethodTemplate(isEquals, equalsTemplate));
            add(new MethodTemplate(equals, equalsTemplate));
        }
    };
}
