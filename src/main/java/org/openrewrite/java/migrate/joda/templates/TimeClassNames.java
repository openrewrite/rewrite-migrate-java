package org.openrewrite.java.migrate.joda.templates;

import java.util.regex.Pattern;

public class TimeClassNames {
  public static final Pattern JODA_CLASS_PATTERN = Pattern.compile("org\\.joda\\.time\\..*");
  // Joda-Time classes
  public static final String JODA_TIME_PKG = "org.joda.time";
  public static final String JODA_DATE_TIME = JODA_TIME_PKG + ".DateTime";
  public static final String JODA_DATE_TIME_ZONE = JODA_TIME_PKG + ".DateTimeZone";
  public static final String JODA_TIME_FORMAT = JODA_TIME_PKG + ".format.DateTimeFormat";
  public static final String JODA_TIME_FORMATTER = JODA_TIME_PKG + ".format.DateTimeFormatter";
  public static final String JODA_LOCAL_DATE = JODA_TIME_PKG + ".LocalDate";
  public static final String JODA_LOCAL_TIME = JODA_TIME_PKG + ".LocalTime";
  public static final String JODA_DATE_TIME_FIELD_TYPE = JODA_TIME_PKG + ".DateTimeFieldType";
  public static final String JODA_DURATION_FIELD_TYPE = JODA_TIME_PKG + ".DurationFieldType";
  public static final String JODA_DURATION = JODA_TIME_PKG + ".Duration";
  public static final String JODA_READABLE_DURATION = JODA_TIME_PKG + ".ReadableDuration";

  // Java Time classes
  public static final String JAVA_TIME_PKG = "java.time";
  public static final String JAVA_DATE_TIME = JAVA_TIME_PKG + ".ZonedDateTime";
  public static final String JAVA_DURATION = JAVA_TIME_PKG + ".Duration";
  public static final String JAVA_ZONE_OFFSET = JAVA_TIME_PKG + ".ZoneOffset";
  public static final String JAVA_ZONE_ID = JAVA_TIME_PKG + ".ZoneId";
  public static final String JAVA_INSTANT = JAVA_TIME_PKG + ".Instant";
  public static final String JAVA_TIME_FORMATTER = JAVA_TIME_PKG + ".format.DateTimeFormatter";
  public static final String JAVA_TIME_FORMAT_STYLE = JAVA_TIME_PKG + ".format.FormatStyle";
  public static final String JAVA_TEMPORAL_ADJUSTER = JAVA_TIME_PKG + ".temporal.TemporalAdjuster";
  public static final String JAVA_LOCAL_DATE = JAVA_TIME_PKG + ".LocalDate";
  public static final String JAVA_LOCAL_TIME = JAVA_TIME_PKG + ".LocalTime";
  public static final String JAVA_TEMPORAL_ISO_FIELDS = JAVA_TIME_PKG + ".temporal.IsoFields";
  public static final String JAVA_CHRONO_FIELD = JAVA_TIME_PKG + ".temporal.ChronoField";
}
