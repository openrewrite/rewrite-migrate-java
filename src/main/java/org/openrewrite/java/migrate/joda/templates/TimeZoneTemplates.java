package org.openrewrite.java.migrate.joda.templates;

import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;

import java.util.ArrayList;
import java.util.List;

import static org.openrewrite.java.migrate.joda.templates.TimeClassNames.JAVA_ZONE_ID;
import static org.openrewrite.java.migrate.joda.templates.TimeClassNames.JAVA_ZONE_OFFSET;
import static org.openrewrite.java.migrate.joda.templates.TimeClassNames.JODA_DATE_TIME_ZONE;

public class TimeZoneTemplates {
  private final MethodMatcher zoneForID = new MethodMatcher(JODA_DATE_TIME_ZONE + " forID(String)");
  private final MethodMatcher zoneForOffsetHours = new MethodMatcher(JODA_DATE_TIME_ZONE + " forOffsetHours(int)");
  private final MethodMatcher zoneForOffsetHoursMinutes = new MethodMatcher(JODA_DATE_TIME_ZONE + " forOffsetHoursMinutes(int, int)");
  private final MethodMatcher zoneForTimeZone = new MethodMatcher(JODA_DATE_TIME_ZONE + " forTimeZone(java.util.TimeZone)");

  private final JavaTemplate zoneIdOfTemplate = JavaTemplate.builder("ZoneId.of(#{any(String)})")
      .imports(JAVA_ZONE_ID)
      .build();
  private final JavaTemplate zoneOffsetHoursTemplate = JavaTemplate.builder("ZoneOffset.ofHours(#{any(int)})")
      .imports(JAVA_ZONE_OFFSET)
      .build();
  private final JavaTemplate zoneOffsetHoursMinutesTemplate = JavaTemplate.builder("ZoneOffset.ofHoursMinutes(#{any(int)}, #{any(int)})")
      .imports(JAVA_ZONE_OFFSET)
      .build();
  private final JavaTemplate timeZoneToZoneIdTemplate = JavaTemplate.builder("#{any(java.util.TimeZone)}.toZoneId()")
      .build();

  private final List<MethodTemplate> templates = new ArrayList<MethodTemplate>() {
    {
      add(new MethodTemplate(zoneForID, zoneIdOfTemplate));
      add(new MethodTemplate(zoneForOffsetHours, zoneOffsetHoursTemplate));
      add(new MethodTemplate(zoneForOffsetHoursMinutes, zoneOffsetHoursMinutesTemplate));
      add(new MethodTemplate(zoneForTimeZone, timeZoneToZoneIdTemplate));
    }
  };

  public static List<MethodTemplate>  getTemplates() {
    return new TimeZoneTemplates().templates;
  }
}
