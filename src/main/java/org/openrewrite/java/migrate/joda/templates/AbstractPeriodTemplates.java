package org.openrewrite.java.migrate.joda.templates;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;

import java.util.ArrayList;
import java.util.List;

import static org.openrewrite.java.migrate.joda.templates.TimeClassNames.*;

@NoArgsConstructor
public class AbstractPeriodTemplates implements Templates{
    final MethodMatcher toString = new MethodMatcher(JODA_PERIOD + " toString()");

    final JavaTemplate toStringTemplate = JavaTemplate.builder("#{any(java.time.Period)}.getDays()").build();

    @Getter
    private final List<MethodTemplate> templates = new ArrayList<MethodTemplate>() {
        {
            add(new MethodTemplate(toString, toStringTemplate));
        }
    };
}
