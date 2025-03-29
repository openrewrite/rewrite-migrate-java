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

import org.openrewrite.java.JavaTemplate;

import java.util.regex.Pattern;

public class TimeClassNames {
    public static final Pattern JODA_CLASS_PATTERN = Pattern.compile("org\\.joda\\.time\\..*");
    public static final String JODA_MULTIPLE_MAPPING_POSSIBLE = "Multiple mapping is possible.Update manually";
    public static final String JODA_NO_AUTOMATIC_MAPPING_POSSIBLE = "Not possible to migrate with recipe.Update manually";
    public static final JavaTemplate JODA_MULTIPLE_MAPPING_POSSIBLE_TEMPLATE = JavaTemplate.builder(JODA_MULTIPLE_MAPPING_POSSIBLE).build();
    public static final JavaTemplate JODA_NO_AUTOMATIC_MAPPING_POSSIBLE_TEMPLATE = JavaTemplate.builder(JODA_NO_AUTOMATIC_MAPPING_POSSIBLE).build();

    // java util
    public static final String JAVA_UTIL_DATE = "java.util.Date";
    public static final String JAVA_UTIL_LOCALE = "java.util.Locale";

    // Joda-Time classes
    public static final String JODA_TIME_PKG = "org.joda.time";
    public static final String JODA_ABSTRACT_DATE_TIME = JODA_TIME_PKG + ".base.AbstractDateTime";
    public static final String JODA_ABSTRACT_DURATION = JODA_TIME_PKG + ".base.AbstractDuration";
    public static final String JODA_ABSTRACT_INTERVAL = JODA_TIME_PKG + ".base.AbstractInterval";
    public static final String JODA_ABSTRACT_PARTIAL = JODA_TIME_PKG + ".base.AbstractPartial";
    public static final String JODA_BASE_DATE_TIME = JODA_TIME_PKG + ".base.BaseDateTime";
    public static final String JODA_DATE_TIME = JODA_TIME_PKG + ".DateTime";
    public static final String JODA_DATE_MIDNIGHT = JODA_TIME_PKG + ".DateMidnight";
    public static final String JODA_DATE_TIME_ZONE = JODA_TIME_PKG + ".DateTimeZone";
    public static final String JODA_TIME_FORMAT = JODA_TIME_PKG + ".format.DateTimeFormat";
    public static final String JODA_TIME_FORMATTER = JODA_TIME_PKG + ".format.DateTimeFormatter";
    public static final String JODA_LOCAL_DATE = JODA_TIME_PKG + ".LocalDate";
    public static final String JODA_LOCAL_DATE_PROPERTY = JODA_TIME_PKG + ".LocalDate.Property";
    public static final String JODA_LOCAL_TIME = JODA_TIME_PKG + ".LocalTime";
    public static final String JODA_LOCAL_DATE_TIME = JODA_TIME_PKG + ".LocalDateTime";
    public static final String JODA_DATE_TIME_FIELD_TYPE = JODA_TIME_PKG + ".DateTimeFieldType";
    public static final String JODA_DURATION_FIELD_TYPE = JODA_TIME_PKG + ".DurationFieldType";
    public static final String JODA_DURATION = JODA_TIME_PKG + ".Duration";
    public static final String JODA_PERIOD = JODA_TIME_PKG + ".Period";
    public static final String JODA_READABLE_DURATION = JODA_TIME_PKG + ".ReadableDuration";
    public static final String JODA_BASE_DURATION = JODA_TIME_PKG + ".base.BaseDuration";
    public static final String JODA_ABSTRACT_INSTANT = JODA_TIME_PKG + ".base.AbstractInstant";
    public static final String JODA_READABLE_INSTANT = JODA_TIME_PKG + ".ReadableInstant";
    public static final String JODA_INSTANT = JODA_TIME_PKG + ".Instant";
    public static final String JODA_INTERVAL = JODA_TIME_PKG + ".Interval";
    public static final String JODA_BASE_INTERVAL = JODA_TIME_PKG + ".base.BaseInterval";
    public static final String JODA_SECONDS = JODA_TIME_PKG + ".Seconds";
    public static final String JODA_HOURS = JODA_TIME_PKG + ".Hours";
    public static final String JODA_DAYS = JODA_TIME_PKG + ".Days";
    public static final String JODA_WEEKS = JODA_TIME_PKG + ".Weeks";
    public static final String JODA_MONTHS = JODA_TIME_PKG + ".Months";
    public static final String JODA_YEARS = JODA_TIME_PKG + ".Years";
    public static final String JODA_DATE_TIME_UTILS = JODA_TIME_PKG + ".DateTimeUtils";

    // Java Time classes
    public static final String JAVA_TIME_PKG = "java.time";
    public static final String JAVA_DATE_TIME = JAVA_TIME_PKG + ".ZonedDateTime";
    public static final String JAVA_DURATION = JAVA_TIME_PKG + ".Duration";
    public static final String JAVA_PERIOD = JAVA_TIME_PKG + ".Period";
    public static final String JAVA_ZONE_OFFSET = JAVA_TIME_PKG + ".ZoneOffset";
    public static final String JAVA_ZONE_ID = JAVA_TIME_PKG + ".ZoneId";
    public static final String JAVA_INSTANT = JAVA_TIME_PKG + ".Instant";
    public static final String JAVA_TIME_FORMATTER = JAVA_TIME_PKG + ".format.DateTimeFormatter";
    public static final String JAVA_TIME_FORMAT_STYLE = JAVA_TIME_PKG + ".format.FormatStyle";
    public static final String JAVA_TEMPORAL_ADJUSTER = JAVA_TIME_PKG + ".temporal.TemporalAdjuster";
    public static final String JAVA_LOCAL_DATE = JAVA_TIME_PKG + ".LocalDate";
    public static final String JAVA_LOCAL_TIME = JAVA_TIME_PKG + ".LocalTime";
    public static final String JAVA_LOCAL_DATE_TIME = JAVA_TIME_PKG + ".LocalDateTime";
    public static final String JAVA_TEMPORAL_ISO_FIELDS = JAVA_TIME_PKG + ".temporal.IsoFields";
    public static final String JAVA_CHRONO_FIELD = JAVA_TIME_PKG + ".temporal.ChronoField";
    public static final String JAVA_CHRONO_UNIT = JAVA_TIME_PKG + ".temporal.ChronoUnit";

    // ThreeTen-Extra classes
    public static final String THREE_TEN_EXTRA_PKG = "org.threeten.extra";
    public static final String THREE_TEN_EXTRA_INTERVAL = THREE_TEN_EXTRA_PKG + ".Interval";
    public static final String THREE_TEN_EXTRA_DAYS = THREE_TEN_EXTRA_PKG + ".Days";
    public static final String THREE_TEN_EXTRA_WEEKS = THREE_TEN_EXTRA_PKG + ".Weeks";
    public static final String THREE_TEN_EXTRA_MONTHS = THREE_TEN_EXTRA_PKG + ".Months";
    public static final String THREE_TEN_EXTRA_YEARS = THREE_TEN_EXTRA_PKG + ".Years";
}
