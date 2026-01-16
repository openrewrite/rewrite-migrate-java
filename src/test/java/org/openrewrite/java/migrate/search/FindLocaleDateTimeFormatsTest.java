/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.java.migrate.search;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class FindLocaleDateTimeFormatsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FindLocaleDateTimeFormats());
    }

    @DocumentExample
    @Test
    void findDateFormatGetTimeInstance() {
        rewriteRun(
          //language=java
          java(
            """
              import java.text.DateFormat;
              import java.util.Date;

              class Test {
                  void test() {
                      DateFormat df = DateFormat.getTimeInstance();
                      String formatted = df.format(new Date());
                  }
              }
              """,
            """
              import java.text.DateFormat;
              import java.util.Date;

              class Test {
                  void test() {
                      DateFormat df = /*~~(JDK 20+ CLDR: may use NNBSP before AM/PM)~~>*/DateFormat.getTimeInstance();
                      String formatted = df.format(new Date());
                  }
              }
              """
          )
        );
    }

    @CsvSource(textBlock = """
      DateFormat.getTimeInstance(DateFormat.SHORT)
      DateFormat.getDateTimeInstance()
      DateFormat.getInstance()
      """)
    @ParameterizedTest
    void findDateFormatMethods(String methodCall) {
        rewriteRun(
          java(
            """
              import java.text.DateFormat;
              import java.util.Date;

              class Test {
                  void test() {
                      DateFormat df = %s;
                      String formatted = df.format(new Date());
                  }
              }
              """.formatted(methodCall),
            """
              import java.text.DateFormat;
              import java.util.Date;

              class Test {
                  void test() {
                      DateFormat df = /*~~(JDK 20+ CLDR: may use NNBSP before AM/PM)~~>*/%s;
                      String formatted = df.format(new Date());
                  }
              }
              """.formatted(methodCall)
          )
        );
    }

    @CsvSource(textBlock = """
      'DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM)', 'LocalTime.now()'
      'DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)', 'LocalDateTime.now()'
      'DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)', 'LocalDateTime.now()'
      """)
    @ParameterizedTest
    void findDateTimeFormatterMethods(String methodCall, String formatArg) {
        rewriteRun(
          java(
            """
              import java.time.LocalDateTime;
              import java.time.LocalTime;
              import java.time.format.DateTimeFormatter;
              import java.time.format.FormatStyle;

              class Test {
                  void test() {
                      DateTimeFormatter dtf = %s;
                      String formatted = dtf.format(%s);
                  }
              }
              """.formatted(methodCall, formatArg),
            """
              import java.time.LocalDateTime;
              import java.time.LocalTime;
              import java.time.format.DateTimeFormatter;
              import java.time.format.FormatStyle;

              class Test {
                  void test() {
                      DateTimeFormatter dtf = /*~~(JDK 20+ CLDR: may use NNBSP before AM/PM)~~>*/%s;
                      String formatted = dtf.format(%s);
                  }
              }
              """.formatted(methodCall, formatArg)
          )
        );
    }

    @Test
    void findMultipleUsages() {
        rewriteRun(
          //language=java
          java(
            """
              import java.text.DateFormat;
              import java.time.LocalTime;
              import java.time.format.DateTimeFormatter;
              import java.time.format.FormatStyle;
              import java.util.Date;

              class Test {
                  void test() {
                      DateFormat df1 = DateFormat.getTimeInstance();
                      DateFormat df2 = DateFormat.getDateTimeInstance();
                      DateTimeFormatter dtf = DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM);
                  }
              }
              """,
            """
              import java.text.DateFormat;
              import java.time.LocalTime;
              import java.time.format.DateTimeFormatter;
              import java.time.format.FormatStyle;
              import java.util.Date;

              class Test {
                  void test() {
                      DateFormat df1 = /*~~(JDK 20+ CLDR: may use NNBSP before AM/PM)~~>*/DateFormat.getTimeInstance();
                      DateFormat df2 = /*~~(JDK 20+ CLDR: may use NNBSP before AM/PM)~~>*/DateFormat.getDateTimeInstance();
                      DateTimeFormatter dtf = /*~~(JDK 20+ CLDR: may use NNBSP before AM/PM)~~>*/DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM);
                  }
              }
              """
          )
        );
    }

    @Nested
    class NoChange {
        @Test
        void noMatchForExplicitPattern() {
            rewriteRun(
              //language=java
              java(
                """
                  import java.time.LocalDateTime;
                  import java.time.format.DateTimeFormatter;

                  class Test {
                      void test() {
                          // Explicit patterns are not affected by CLDR changes
                          DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss a");
                          String formatted = dtf.format(LocalDateTime.now());
                      }
                  }
                  """
              )
            );
        }

        @Test
        void noMatchForDateFormatGetDateInstance() {
            rewriteRun(
              //language=java
              java(
                """
                  import java.text.DateFormat;
                  import java.util.Date;

                  class Test {
                      void test() {
                          // Date-only formatting doesn't include AM/PM
                          DateFormat df = DateFormat.getDateInstance();
                          String formatted = df.format(new Date());
                      }
                  }
                  """
              )
            );
        }
    }
}
