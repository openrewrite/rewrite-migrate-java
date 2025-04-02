package org.openrewrite.java.migrate.joda.templates;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;

import java.util.ArrayList;
import java.util.List;

import static org.openrewrite.java.migrate.joda.templates.TimeClassNames.*;

@NoArgsConstructor
public class ChronologyTemplates implements Templates{
    final MethodMatcher getInstanceUTC = new MethodMatcher(JODA_GEORGIAN_CHRONOLOGY + " getInstanceUTC()");
    final MethodMatcher getDateTimeMillis = new MethodMatcher(JODA_BASIC_CHRONOLOGY + " getDateTimeMillis(int,int,int,int)");

    final JavaTemplate.Builder instanceTemplate = JavaTemplate.builder("IsoChronology.INSTANCE");
    final JavaTemplate.Builder epochSecondTemplate = JavaTemplate.builder("#{any(java.time.Duration)}.epochSecond(#{any(int)},#{any(int)},#{any(int)},#{any(int)},0,0,0,ZoneOffset.ofTotalSeconds(0))");

    @Getter
    private final List<MethodTemplate> templates = new ArrayList<MethodTemplate>() {
        {
            add(new MethodTemplate(getInstanceUTC, build(instanceTemplate)));
            add(new MethodTemplate(getDateTimeMillis, build(epochSecondTemplate)));
        }
    };

    private JavaTemplate build(JavaTemplate.Builder builder) {
        return buildWithImport(builder, JAVA_ISA_CHRONOLOGY);
    }
}
