package org.openrewrite.java.migrate.joda.templates;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;

import java.util.ArrayList;
import java.util.List;

import static org.openrewrite.java.migrate.joda.templates.TimeClassNames.*;

@NoArgsConstructor
public class PeriodTemplates implements Templates{
    final MethodMatcher newPeriod = new MethodMatcher(JODA_PERIOD + "<constructor>(Object)");
    final MethodMatcher newPeriodOfHmSMillis = new MethodMatcher(JODA_PERIOD + "<constructor>(int,int,int,int)");
    final MethodMatcher newPeriodOfYMDHySMillis = new MethodMatcher(JODA_PERIOD + "<constructor>(int,int,int,int,int,int,int,int)");
    final MethodMatcher minus = new MethodMatcher(JODA_PERIOD + " minus(org.joda.time.ReadablePeriod)");
    final MethodMatcher plus = new MethodMatcher(JODA_PERIOD + " plus(org.joda.time.ReadablePeriod)");
    final MethodMatcher getDays = new MethodMatcher(JODA_PERIOD + " getDays()");

    final JavaTemplate.Builder getDaysTemplate = JavaTemplate.builder("#{any(java.time.Period)}.getDays()");
    final JavaTemplate.Builder minusTemplate = JavaTemplate.builder("#{any(java.time.Period)}.minus(#{any(java.time.Period)})");
    final JavaTemplate.Builder plusTemplate = JavaTemplate.builder("#{any(java.time.Period)}.plus(#{any(java.time.Period)})");
    final JavaTemplate.Builder periodOfHMSMillisTemplate = JavaTemplate.builder("Duration.ofHours(#{any(int)}).plusMinutes(#{any(int)}).plusSeconds(#{any(int)}).plusMillis(#{any(int)})");

    @Getter
    private final List<MethodTemplate> templates = new ArrayList<MethodTemplate>() {
        {
            add(new MethodTemplate(newPeriodOfHmSMillis, build(periodOfHMSMillisTemplate)));
            add(new MethodTemplate(minus, build(minusTemplate)));
            add(new MethodTemplate(plus, build(plusTemplate)));
            add(new MethodTemplate(getDays, build(getDaysTemplate)));

            add(new MethodTemplate(newPeriodOfYMDHySMillis, JODA_NO_AUTOMATIC_MAPPING_POSSIBLE_TEMPLATE));
            add(new MethodTemplate(newPeriod, JODA_MULTIPLE_MAPPING_POSSIBLE_TEMPLATE));
        }
    };

    private JavaTemplate build(JavaTemplate.Builder builder) {
        return buildWithImport(builder, JAVA_PERIOD);
    }

}
