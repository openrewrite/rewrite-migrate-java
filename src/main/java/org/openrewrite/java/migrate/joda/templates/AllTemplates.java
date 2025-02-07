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

import lombok.Value;
import org.openrewrite.java.MethodMatcher;
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
    private static final MethodMatcher ANY_BASE_DURATION = new MethodMatcher(JODA_BASE_DURATION + " *(..)");
    private static final MethodMatcher ANY_ABSTRACT_INSTANT = new MethodMatcher(JODA_ABSTRACT_INSTANT + " *(..)");
    private static final MethodMatcher ANY_ABSTRACT_DATE_TIME = new MethodMatcher(JODA_ABSTRACT_DATE_TIME + " *(..)");
    private static final MethodMatcher ANY_ABSTRACT_DURATION = new MethodMatcher(JODA_ABSTRACT_DURATION + " *(..)");
    private static final MethodMatcher ANY_INSTANT = new MethodMatcher(JODA_INSTANT + " *(..)");
    private static final MethodMatcher ANY_NEW_INSTANT = new MethodMatcher(JODA_INSTANT + "<constructor>(..)");
    private static final MethodMatcher ANY_NEW_INTERVAL = new MethodMatcher(JODA_INTERVAL + "<constructor>(..)");
    private static final MethodMatcher ANY_ABSTRACT_INTERVAL = new MethodMatcher(JODA_ABSTRACT_INTERVAL + " *(..)");
    private static final MethodMatcher ANY_BASE_INTERVAL = new MethodMatcher(JODA_BASE_INTERVAL + " *(..)");

    private static final List<MatcherAndTemplates> templates;
    static {
        templates = new ArrayList<>();
        templates.add(new MatcherAndTemplates(ANY_ABSTRACT_DATE_TIME, new AbstractDateTimeTemplates()));
        templates.add(new MatcherAndTemplates(ANY_ABSTRACT_DURATION, new AbstractDurationTemplates()));
        templates.add(new MatcherAndTemplates(ANY_ABSTRACT_INSTANT, new AbstractInstantTemplates()));
        templates.add(new MatcherAndTemplates(ANY_BASE_DATETIME, new BaseDateTime()));
        templates.add(new MatcherAndTemplates(ANY_TIME_FORMAT, new DateTimeFormatTemplates()));
        templates.add(new MatcherAndTemplates(ANY_TIME_FORMATTER, new DateTimeFormatterTemplates()));
        templates.add(new MatcherAndTemplates(ANY_NEW_DATE_TIME, new DateTimeTemplates()));
        templates.add(new MatcherAndTemplates(ANY_DATE_TIME, new DateTimeTemplates()));
        templates.add(new MatcherAndTemplates(ANY_NEW_DURATION, new DurationTemplates()));
        templates.add(new MatcherAndTemplates(ANY_DURATION, new DurationTemplates()));
        templates.add(new MatcherAndTemplates(ANY_BASE_DURATION, new BaseDurationTemplates()));
        templates.add(new MatcherAndTemplates(ANY_DATE_TIMEZONE, new TimeZoneTemplates()));
        templates.add(new MatcherAndTemplates(ANY_INSTANT, new InstantTemplates()));
        templates.add(new MatcherAndTemplates(ANY_NEW_INSTANT, new InstantTemplates()));
        templates.add(new MatcherAndTemplates(ANY_NEW_INTERVAL, new IntervalTemplates()));
        templates.add(new MatcherAndTemplates(ANY_ABSTRACT_INTERVAL, new AbstractIntervalTemplates()));
        templates.add(new MatcherAndTemplates(ANY_BASE_INTERVAL, new BaseIntervalTemplates()));
    }

    public static MethodTemplate getTemplate(MethodCall method) {
        return getTemplateGroup(method).flatMap(templates -> templates.getTemplates().stream()
                .filter(template -> template.getMatcher().matches(method) && templates.matchesMethodCall(method, template))
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
