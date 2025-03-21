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
    private static final MethodMatcher ANY_NEW_DATE_MIDNIGHT = new MethodMatcher(JODA_DATE_MIDNIGHT + "<constructor>(..)");
    private static final MethodMatcher ANY_DATE_MIDNIGHT = new MethodMatcher(JODA_DATE_MIDNIGHT + "*(..)");
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
    private static final MethodMatcher ANY_INTERVAL = new MethodMatcher(JODA_INTERVAL + "*(..)");
    private static final MethodMatcher ANY_ABSTRACT_INTERVAL = new MethodMatcher(JODA_ABSTRACT_INTERVAL + " *(..)");
    private static final MethodMatcher ANY_ABSTRACT_PARTIAL = new MethodMatcher(JODA_ABSTRACT_PARTIAL + " *(..)");
    private static final MethodMatcher ANY_BASE_INTERVAL = new MethodMatcher(JODA_BASE_INTERVAL + " *(..)");
    private static final MethodMatcher ANY_NEW_LOCAL_DATE = new MethodMatcher(JODA_LOCAL_DATE + "<constructor>(..)");
    private static final MethodMatcher ANY_LOCAL_DATE = new MethodMatcher(JODA_LOCAL_DATE + " *(..)");
    private static final MethodMatcher ANY_NEW_LOCAL_DATE_TIME = new MethodMatcher(JODA_LOCAL_DATE_TIME + "<constructor>(..)");
    private static final MethodMatcher ANY_LOCAL_DATE_TIME = new MethodMatcher(JODA_LOCAL_DATE_TIME + " *(..)");
    private static final MethodMatcher ANY_NEW_LOCAL_TIME = new MethodMatcher(JODA_LOCAL_TIME + "<constructor>(..)");
    private static final MethodMatcher ANY_LOCAL_TIME = new MethodMatcher(JODA_LOCAL_TIME + " *(..)");
    private static final MethodMatcher ANY_SECONDS = new MethodMatcher(JODA_SECONDS + " *(..)");
    private static final MethodMatcher ANY_HOURS = new MethodMatcher(JODA_HOURS + " *(..)");
    private static final MethodMatcher ANY_DAYS = new MethodMatcher(JODA_DAYS + " *(..)");
    private static final MethodMatcher ANY_WEEKS = new MethodMatcher(JODA_WEEKS + " *(..)");
    private static final MethodMatcher ANY_MONTHS = new MethodMatcher(JODA_MONTHS + " *(..)");
    private static final MethodMatcher ANY_YEARS = new MethodMatcher(JODA_YEARS + " *(..)");
    private static final MethodMatcher ANY_JODA_DATE_TIME_UTILS = new MethodMatcher(JODA_DATE_TIME_UTILS + " *(..)");

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
            add(new MatcherAndTemplates(ANY_NEW_DATE_MIDNIGHT, new DateTimeTemplates()));
            add(new MatcherAndTemplates(ANY_DATE_MIDNIGHT, new DateTimeTemplates()));
            add(new MatcherAndTemplates(ANY_NEW_DURATION, new DurationTemplates()));
            add(new MatcherAndTemplates(ANY_DURATION, new DurationTemplates()));
            add(new MatcherAndTemplates(ANY_BASE_DURATION, new BaseDurationTemplates()));
            add(new MatcherAndTemplates(ANY_DATE_TIMEZONE, new DateTimeZoneTemplates()));
            add(new MatcherAndTemplates(ANY_INSTANT, new InstantTemplates()));
            add(new MatcherAndTemplates(ANY_NEW_INSTANT, new InstantTemplates()));
            add(new MatcherAndTemplates(ANY_NEW_INTERVAL, new IntervalTemplates()));
            add(new MatcherAndTemplates(ANY_INTERVAL, new IntervalTemplates()));
            add(new MatcherAndTemplates(ANY_ABSTRACT_INTERVAL, new AbstractIntervalTemplates()));
            add(new MatcherAndTemplates(ANY_ABSTRACT_PARTIAL, new AbstractPartialTemplates()));
            add(new MatcherAndTemplates(ANY_BASE_INTERVAL, new BaseIntervalTemplates()));
            add(new MatcherAndTemplates(ANY_NEW_LOCAL_DATE, new LocatDateTemplates()));
            add(new MatcherAndTemplates(ANY_LOCAL_DATE, new LocatDateTemplates()));
            add(new MatcherAndTemplates(ANY_NEW_LOCAL_DATE_TIME, new LocatDateTimeTemplates()));
            add(new MatcherAndTemplates(ANY_LOCAL_DATE_TIME, new LocatDateTimeTemplates()));
            add(new MatcherAndTemplates(ANY_NEW_LOCAL_TIME, new LocatTimeTemplates()));
            add(new MatcherAndTemplates(ANY_LOCAL_TIME, new LocatTimeTemplates()));
            add(new MatcherAndTemplates(ANY_SECONDS, new SecondsTemplates()));
            add(new MatcherAndTemplates(ANY_HOURS, new HoursTemplates()));
            add(new MatcherAndTemplates(ANY_DAYS, new DaysTemplates()));
            add(new MatcherAndTemplates(ANY_WEEKS, new WeeksTemplates()));
            add(new MatcherAndTemplates(ANY_MONTHS, new MonthsTemplates()));
            add(new MatcherAndTemplates(ANY_YEARS, new YearsTemplates()));
            add(new MatcherAndTemplates(ANY_JODA_DATE_TIME_UTILS, new DateTimeUtilsTemplates()));
        }
    };

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
