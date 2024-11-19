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

import lombok.Value;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.MethodCall;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.openrewrite.java.migrate.joda.templates.TimeClassNames.*;

public class AllTemplates {
    private static final MethodMatcher ANY_BASE_DATETIME = new MethodMatcher(JODA_BASE_DATE_TIME + " *(..)");
    private static final MethodMatcher ANY_NEW_DATE_TIME = new MethodMatcher(JODA_DATE_TIME + "<constructor>(..)");
    private static final MethodMatcher ANY_DATE_TIME = new MethodMatcher(JODA_DATE_TIME + " *(..)");
    private static final MethodMatcher ANY_DATE_TIMEZONE = new MethodMatcher(JODA_DATE_TIME_ZONE + " *(..)");
    private static final MethodMatcher ANY_TIME_FORMAT = new MethodMatcher(JODA_TIME_FORMAT + " *(..)");
    private static final MethodMatcher ANY_TIME_FORMATTER = new MethodMatcher(JODA_TIME_FORMATTER + " *(..)");
    private static final MethodMatcher ANY_NEW_DURATION = new MethodMatcher(JODA_DURATION + "<constructor>(..)");
    private static final MethodMatcher ANY_DURATION = new MethodMatcher(JODA_DURATION + " *(..)");
    private static final MethodMatcher ANY_ABSTRACT_INSTANT = new MethodMatcher(JODA_ABSTRACT_INSTANT + " *(..)");
    private static final MethodMatcher ANY_ABSTRACT_DATE_TIME = new MethodMatcher(JODA_ABSTRACT_DATE_TIME + " *(..)");
    private static final MethodMatcher ANY_ABSTRACT_DURATION = new MethodMatcher(JODA_ABSTRACT_DURATION + " *(..)");
    private static final MethodMatcher ANY_INSTANT = new MethodMatcher(JODA_INSTANT + " *(..)");
    private static final MethodMatcher ANY_NEW_INSTANT = new MethodMatcher(JODA_INSTANT +  "<constructor>(..)");

    private static List<MatcherAndTemplates> templates = new ArrayList<MatcherAndTemplates>() {
        {
            add(new MatcherAndTemplates(ANY_ABSTRACT_DATE_TIME, new AbstractDateTimeTemplates()));
            add(new MatcherAndTemplates(ANY_ABSTRACT_DURATION, new AbstractDurationTemplates()));
            add(new MatcherAndTemplates(ANY_ABSTRACT_INSTANT, new AbstractInstantTemplates()));
            add(new MatcherAndTemplates(ANY_BASE_DATETIME, new BaseDateTime()));
            add(new MatcherAndTemplates(ANY_TIME_FORMAT, new DateTimeFormatTemplates()));
            add(new MatcherAndTemplates(ANY_TIME_FORMATTER, new DateTimeFormatterTemplates()));
            add(new MatcherAndTemplates(ANY_NEW_DATE_TIME, new DateTimeTemplates()));
            add(new MatcherAndTemplates(ANY_DATE_TIME, new DateTimeTemplates()));
            add(new MatcherAndTemplates(ANY_NEW_DURATION, new DurationTemplates()));
            add(new MatcherAndTemplates(ANY_DURATION, new DurationTemplates()));
            add(new MatcherAndTemplates(ANY_DATE_TIMEZONE, new TimeZoneTemplates()));
            add(new MatcherAndTemplates(ANY_INSTANT, new InstantTemplates()));
            add(new MatcherAndTemplates(ANY_NEW_INSTANT, new InstantTemplates()));
        }
    };

    public static MethodTemplate getTemplate(J.MethodInvocation method) {
        return getTemplateGroup(method).flatMap(templates -> templates.getTemplates().stream()
                .filter(template -> template.getMatcher().matches(method) && templates.matchesMethodCall(method, template))
                .findFirst()).orElse(null);
    }

    public static MethodTemplate getTemplate(J.NewClass newClass) {
        return getTemplateGroup(newClass).flatMap(templates -> templates.getTemplates().stream()
                .filter(template -> template.getMatcher().matches(newClass) && templates.matchesMethodCall(newClass, template))
                .findFirst()).orElse(null);
    }

    private static Optional<Templates> getTemplateGroup(MethodCall method) {
        for (MatcherAndTemplates matcherAndTemplates : templates) {
            if (matcherAndTemplates.getMatcher().matches(method)) {
                return Optional.of(matcherAndTemplates.getTemplates());
            }
        }
        return Optional.empty();
    }

    @Value
    private static class MatcherAndTemplates {
        MethodMatcher matcher;
        Templates templates;
    }
}
