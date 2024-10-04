package org.openrewrite.java.migrate.joda.templates;

import lombok.NoArgsConstructor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;

import java.util.ArrayList;
import java.util.List;

import static org.openrewrite.java.migrate.joda.templates.TimeClassNames.*;

@NoArgsConstructor
public class DurationTemplates {
  private final MethodMatcher parse = new MethodMatcher(JODA_DURATION + " parse(String)");
  private final MethodMatcher standardDays = new MethodMatcher(JODA_DURATION + " standardDays(long)");
  private final MethodMatcher standardHours = new MethodMatcher(JODA_DURATION + " standardHours(long)");
  private final MethodMatcher standardMinutes = new MethodMatcher(JODA_DURATION + " standardMinutes(long)");
  private final MethodMatcher standardSeconds = new MethodMatcher(JODA_DURATION + " standardSeconds(long)");
  private final MethodMatcher millis = new MethodMatcher(JODA_DURATION + " millis(long)");

  private final MethodMatcher newDuration = new MethodMatcher(JODA_DURATION + "<constructor>(long)");
  private final MethodMatcher newDurationWithInstants = new MethodMatcher(JODA_DURATION + "<constructor>(long,long)");

  private final MethodMatcher getStandardDays = new MethodMatcher(JODA_DURATION + " getStandardDays()");
  private final MethodMatcher getStandardHours = new MethodMatcher(JODA_DURATION + " getStandardHours()");
  private final MethodMatcher getStandardMinutes = new MethodMatcher(JODA_DURATION + " getStandardMinutes()");
  private final MethodMatcher getStandardSeconds = new MethodMatcher(JODA_DURATION + " getStandardSeconds()");

  private final MethodMatcher toDuration = new MethodMatcher(JODA_DURATION + " toDuration()");

  private final MethodMatcher toStandardDays = new MethodMatcher(JODA_DURATION + " toStandardDays()");
  private final MethodMatcher toStandardHours = new MethodMatcher(JODA_DURATION + " toStandardHours()");
  private final MethodMatcher toStandardMinutes = new MethodMatcher(JODA_DURATION + " toStandardMinutes()");
  private final MethodMatcher toStandardSeconds = new MethodMatcher(JODA_DURATION + " toStandardSeconds()");

  private final MethodMatcher withMillis = new MethodMatcher(JODA_DURATION + " withMillis(long)");
  private final MethodMatcher withDurationAdded = new MethodMatcher(JODA_DURATION + " withDurationAdded(long,int)");
  private final MethodMatcher withDurationAddedReadable = new MethodMatcher(JODA_DURATION + " withDurationAdded(" + JODA_READABLE_DURATION + ",int)");

  private final MethodMatcher plusLong = new MethodMatcher(JODA_DURATION + " plus(long)");
  private final MethodMatcher plusReadable = new MethodMatcher(JODA_DURATION + " plus(" + JODA_READABLE_DURATION + ")");
  private final MethodMatcher minusLong = new MethodMatcher(JODA_DURATION + " minus(long)");
  private final MethodMatcher minusReadable = new MethodMatcher(JODA_DURATION + " minus(" + JODA_READABLE_DURATION + ")");

  private final MethodMatcher multipliedBy = new MethodMatcher(JODA_DURATION + " multipliedBy(long)");
  private final MethodMatcher dividedBy = new MethodMatcher(JODA_DURATION + " dividedBy(long)");

  private final MethodMatcher negated = new MethodMatcher(JODA_DURATION + " negated()");
  private final MethodMatcher abs = new MethodMatcher(JODA_DURATION + " abs()");

  private final JavaTemplate parseTemplate = JavaTemplate.builder("Duration.parse(#{any(String)})")
      .imports(JAVA_DURATION)
      .build();
  private final JavaTemplate standardDaysTemplate = JavaTemplate.builder("Duration.ofDays(#{any(long)})")
      .imports(JAVA_DURATION)
      .build();
  private final JavaTemplate standardHoursTemplate = JavaTemplate.builder("Duration.ofHours(#{any(long)})")
      .imports(JAVA_DURATION)
      .build();
  private final JavaTemplate standardMinutesTemplate = JavaTemplate.builder("Duration.ofMinutes(#{any(long)})")
      .imports(JAVA_DURATION)
      .build();
  private final JavaTemplate standardSecondsTemplate = JavaTemplate.builder("Duration.ofSeconds(#{any(long)})")
      .imports(JAVA_DURATION)
      .build();
  private final JavaTemplate millisTemplate = JavaTemplate.builder("Duration.ofMillis(#{any(long)})")
      .imports(JAVA_DURATION)
      .build();

  private final JavaTemplate newDurationTemplate = JavaTemplate.builder("Duration.ofMillis(#{any(long)})")
      .imports(JAVA_DURATION)
      .build();
  private final JavaTemplate newDurationWithInstantsTemplate = JavaTemplate.builder("Duration.between(Instant.ofEpochMilli(#{any(long)}), Instant.ofEpochMilli(#{any(long)}))")
      .imports(JAVA_DURATION, JAVA_INSTANT)
      .build();
  private final JavaTemplate toDurationTemplate = JavaTemplate.builder("#{any(java.time.Duration)}")
      .build();
  private final JavaTemplate toDaysTemplate = JavaTemplate.builder("#{any(java.time.Duration)}.toDays()")
      .build();
  private final JavaTemplate toHoursTemplate = JavaTemplate.builder("#{any(java.time.Duration)}.toHours()")
      .build();
  private final JavaTemplate toMinutesTemplate = JavaTemplate.builder("#{any(java.time.Duration)}.toMinutes()")
      .build();
  private final JavaTemplate getSecondsTemplate = JavaTemplate.builder("#{any(java.time.Duration)}.getSeconds()")
      .build();
  private final JavaTemplate ofMillisTemplate = JavaTemplate.builder("Duration.ofMillis(#{any(long)})")
      .imports(JAVA_DURATION)
      .build();
  private final JavaTemplate withDurationAddedTemplate = JavaTemplate.builder("#{any(java.time.Duration)}.plusMillis(#{any(long)} * #{any(int)})")
      .build();
  private final JavaTemplate withDurationAddedReadableTemplate = JavaTemplate.builder("#{any(java.time.Duration)}.plus(#{any(java.time.Duration)}.multipliedBy(#{any(int)}))")
      .build();
  private final JavaTemplate plusLongTemplate = JavaTemplate.builder("#{any(java.time.Duration)}.plusMillis(#{any(long)})")
      .build();
  private final JavaTemplate plusReadableTemplate = JavaTemplate.builder("#{any(java.time.Duration)}.plus(#{any(java.time.Duration)})")
      .build();
  private final JavaTemplate minusLongTemplate = JavaTemplate.builder("#{any(java.time.Duration)}.minusMillis(#{any(long)})")
      .build();
  private final JavaTemplate minusReadableTemplate = JavaTemplate.builder("#{any(java.time.Duration)}.minus(#{any(java.time.Duration)})")
      .build();
  private final JavaTemplate multipliedByTemplate = JavaTemplate.builder("#{any(java.time.Duration)}.multipliedBy(#{any(long)})")
      .build();
  private final JavaTemplate dividedByTemplate = JavaTemplate.builder("#{any(java.time.Duration)}.dividedBy(#{any(long)})")
      .build();
  private final JavaTemplate negatedTemplate = JavaTemplate.builder("#{any(java.time.Duration)}.negated()")
      .build();
  private final JavaTemplate absTemplate = JavaTemplate.builder("#{any(java.time.Duration)}.abs()")
      .build();

  private final List<MethodTemplate> templates = new ArrayList<MethodTemplate>() {
    {
      add(new MethodTemplate(parse, parseTemplate));
      add(new MethodTemplate(standardDays, standardDaysTemplate));
      add(new MethodTemplate(standardHours, standardHoursTemplate));
      add(new MethodTemplate(standardMinutes, standardMinutesTemplate));
      add(new MethodTemplate(standardSeconds, standardSecondsTemplate));
      add(new MethodTemplate(millis, millisTemplate));

      add(new MethodTemplate(newDuration, newDurationTemplate));
      add(new MethodTemplate(newDurationWithInstants, newDurationWithInstantsTemplate));

      add(new MethodTemplate(getStandardDays, toDaysTemplate));
      add(new MethodTemplate(getStandardHours, toHoursTemplate));
      add(new MethodTemplate(getStandardMinutes, toMinutesTemplate));
      add(new MethodTemplate(getStandardSeconds, getSecondsTemplate));

      add(new MethodTemplate(toDuration, toDurationTemplate));

      add(new MethodTemplate(toStandardDays, toDaysTemplate));
      add(new MethodTemplate(toStandardHours, toHoursTemplate));
      add(new MethodTemplate(toStandardMinutes, toMinutesTemplate));
      add(new MethodTemplate(toStandardSeconds, getSecondsTemplate));

      add(new MethodTemplate(withMillis, ofMillisTemplate, m -> new Expression[]{m.getArguments().get(0)}));
      add(new MethodTemplate(withDurationAdded, withDurationAddedTemplate));
      add(new MethodTemplate(withDurationAddedReadable, withDurationAddedReadableTemplate));

      add(new MethodTemplate(plusLong, plusLongTemplate));
      add(new MethodTemplate(plusReadable, plusReadableTemplate));
      add(new MethodTemplate(minusLong, minusLongTemplate));
      add(new MethodTemplate(minusReadable, minusReadableTemplate));

      add(new MethodTemplate(multipliedBy, multipliedByTemplate));
      add(new MethodTemplate(dividedBy, dividedByTemplate));

      add(new MethodTemplate(negated, negatedTemplate));
      add(new MethodTemplate(abs, absTemplate));
    }
  };

  public static List<MethodTemplate> getTemplates() {
    return new DurationTemplates().templates;
  }
}
