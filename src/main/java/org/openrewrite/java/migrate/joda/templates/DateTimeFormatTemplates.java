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

import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;

import java.util.ArrayList;
import java.util.List;

import static org.openrewrite.java.migrate.joda.templates.TimeClassNames.*;

public class DateTimeFormatTemplates {
  private final MethodMatcher forPattern = new MethodMatcher(JODA_TIME_FORMAT + " forPattern(String)");
  private final MethodMatcher forStyle = new MethodMatcher(JODA_TIME_FORMAT + " forStyle(String)");
  private final MethodMatcher patternForStyle = new MethodMatcher(JODA_TIME_FORMAT + " patternForStyle(String, java.util.Locale)");
  private final MethodMatcher shortDate = new MethodMatcher(JODA_TIME_FORMAT + " shortDate()");
  private final MethodMatcher mediumDate = new MethodMatcher(JODA_TIME_FORMAT + " mediumDate()");
  private final MethodMatcher longDate = new MethodMatcher(JODA_TIME_FORMAT + " longDate()");
  private final MethodMatcher fullDate = new MethodMatcher(JODA_TIME_FORMAT + " fullDate()");
  private final MethodMatcher shortTime = new MethodMatcher(JODA_TIME_FORMAT + " shortTime()");
  private final MethodMatcher mediumTime = new MethodMatcher(JODA_TIME_FORMAT + " mediumTime()");
  private final MethodMatcher longTime = new MethodMatcher(JODA_TIME_FORMAT + " longTime()");
  private final MethodMatcher fullTime = new MethodMatcher(JODA_TIME_FORMAT + " fullTime()");
  private final MethodMatcher shortDateTime = new MethodMatcher(JODA_TIME_FORMAT + " shortDateTime()");
  private final MethodMatcher mediumDateTime = new MethodMatcher(JODA_TIME_FORMAT + " mediumDateTime()");
  private final MethodMatcher longDateTime = new MethodMatcher(JODA_TIME_FORMAT + " longDateTime()");
  private final MethodMatcher fullDateTime = new MethodMatcher(JODA_TIME_FORMAT + " fullDateTime()");

  private final JavaTemplate ofPatternTemplate = JavaTemplate.builder("DateTimeFormatter.ofPattern(#{any(String)})")
      .imports("java.time.format.DateTimeFormatter")
      .build();
  private final JavaTemplate shortDateTemplate = JavaTemplate.builder("DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)")
      .imports(JAVA_TIME_FORMATTER, JAVA_TIME_FORMAT_STYLE)
      .build();
  private final JavaTemplate mediumDateTemplate = JavaTemplate.builder("DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)")
      .imports(JAVA_TIME_FORMATTER, JAVA_TIME_FORMAT_STYLE)
      .build();
  private final JavaTemplate longDateTemplate = JavaTemplate.builder("DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)")
      .imports(JAVA_TIME_FORMATTER, JAVA_TIME_FORMAT_STYLE)
      .build();
  private final JavaTemplate fullDateTemplate = JavaTemplate.builder("DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)")
      .imports(JAVA_TIME_FORMATTER, JAVA_TIME_FORMAT_STYLE)
      .build();
  private final JavaTemplate shortTimeTemplate = JavaTemplate.builder("DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)")
      .imports(JAVA_TIME_FORMATTER, JAVA_TIME_FORMAT_STYLE)
      .build();
  private final JavaTemplate mediumTimeTemplate = JavaTemplate.builder("DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM)")
      .imports(JAVA_TIME_FORMATTER, JAVA_TIME_FORMAT_STYLE)
      .build();
  private final JavaTemplate longTimeTemplate = JavaTemplate.builder("DateTimeFormatter.ofLocalizedTime(FormatStyle.LONG)")
      .imports(JAVA_TIME_FORMATTER, JAVA_TIME_FORMAT_STYLE)
      .build();
  private final JavaTemplate fullTimeTemplate = JavaTemplate.builder("DateTimeFormatter.ofLocalizedTime(FormatStyle.FULL)")
      .imports(JAVA_TIME_FORMATTER, JAVA_TIME_FORMAT_STYLE)
      .build();
  private final JavaTemplate shortDateTimeTemplate = JavaTemplate.builder("DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.SHORT)")
      .imports(JAVA_TIME_FORMATTER, JAVA_TIME_FORMAT_STYLE)
      .build();
  private final JavaTemplate mediumDateTimeTemplate = JavaTemplate.builder("DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.MEDIUM)")
      .imports(JAVA_TIME_FORMATTER, JAVA_TIME_FORMAT_STYLE)
      .build();
  private final JavaTemplate longDateTimeTemplate = JavaTemplate.builder("DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG, FormatStyle.LONG)")
      .imports(JAVA_TIME_FORMATTER, JAVA_TIME_FORMAT_STYLE)
      .build();
  private final JavaTemplate fullDateTimeTemplate = JavaTemplate.builder("DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.FULL)")
      .imports(JAVA_TIME_FORMATTER, JAVA_TIME_FORMAT_STYLE)
      .build();
  private final List<MethodTemplate> templates = new ArrayList<MethodTemplate>() {
    {
      add(new MethodTemplate(forPattern, ofPatternTemplate));
      add(new MethodTemplate(shortDate, shortDateTemplate));
      add(new MethodTemplate(mediumDate, mediumDateTemplate));
      add(new MethodTemplate(longDate, longDateTemplate));
      add(new MethodTemplate(fullDate, fullDateTemplate));
      add(new MethodTemplate(shortTime, shortTimeTemplate));
      add(new MethodTemplate(mediumTime, mediumTimeTemplate));
      add(new MethodTemplate(longTime, longTimeTemplate));
      add(new MethodTemplate(fullTime, fullTimeTemplate));
      add(new MethodTemplate(shortDateTime, shortDateTimeTemplate));
      add(new MethodTemplate(mediumDateTime, mediumDateTimeTemplate));
      add(new MethodTemplate(longDateTime, longDateTimeTemplate));
      add(new MethodTemplate(fullDateTime, fullDateTimeTemplate));
    }
  };

  public static List<MethodTemplate> getTemplates() {
    return new DateTimeFormatTemplates().templates;
  }
}
