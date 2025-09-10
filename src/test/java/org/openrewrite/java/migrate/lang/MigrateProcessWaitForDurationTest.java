/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.java.migrate.lang;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.junit.jupiter.api.condition.JRE.JAVA_25;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.javaVersion;

class MigrateProcessWaitForDurationTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .allSources(src -> src.markers(javaVersion(25)))
          .recipe(new MigrateProcessWaitForDuration());
    }

    @DocumentExample
    @Test
    void migrateProcessWaitForWithStaticImport() {
        rewriteRun(
          //language=java
          java(
            """
              import static java.util.concurrent.TimeUnit.SECONDS;
              import static java.util.concurrent.TimeUnit.MILLISECONDS;

              class Test {
                  void test(Process process) throws Exception {
                      process.waitFor(5, SECONDS);
                      process.waitFor(100, MILLISECONDS);
                  }
              }
              """,
            """
              import java.time.Duration;

              class Test {
                  void test(Process process) throws Exception {
                      process.waitFor(Duration.ofSeconds(5));
                      process.waitFor(Duration.ofMillis(100));
                  }
              }
              """
          )
        );
    }

    @CsvSource(textBlock = """
      SECONDS, 5, ofSeconds
      MINUTES, 2, ofMinutes
      HOURS, 23, ofHours
      DAYS, 7, ofDays
      MILLISECONDS, 1001, ofMillis
      NANOSECONDS, 1000000, ofNanos
      """)
    @ParameterizedTest
    void migrateProcessWaitForWithExpressiveMethods(String timeUnit, String value, String durationMethod) {
        rewriteRun(
          java(
            """
              import java.util.concurrent.TimeUnit;

              class Test {
                  void test(Process process) throws Exception {
                      process.waitFor(%s, TimeUnit.%s);
                  }
              }
              """.formatted(value, timeUnit),
            """
              import java.time.Duration;

              class Test {
                  void test(Process process) throws Exception {
                      process.waitFor(Duration.%s(%s));
                  }
              }
              """.formatted(durationMethod, value)
          )
        );
    }

    @Test
    void migrateProcessWaitForWithMicroseconds() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.concurrent.TimeUnit;

              class Test {
                  void test(Process process) throws Exception {
                      process.waitFor(999, TimeUnit.MICROSECONDS);
                  }
              }
              """,
            """
              import java.time.Duration;
              import java.time.temporal.ChronoUnit;

              class Test {
                  void test(Process process) throws Exception {
                      process.waitFor(Duration.of(999, ChronoUnit.MICROS));
                  }
              }
              """
          )
        );
    }

    @Test
    void migrateProcessWaitForWithVariables() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.concurrent.TimeUnit;

              class Test {
                  void test(Process process) throws Exception {
                      long timeout = 5;
                      TimeUnit unit = TimeUnit.SECONDS;
                      process.waitFor(timeout, unit);
                  }
              }
              """,
            """
              import java.time.Duration;
              import java.util.concurrent.TimeUnit;

              class Test {
                  void test(Process process) throws Exception {
                      long timeout = 5;
                      TimeUnit unit = TimeUnit.SECONDS;
                      process.waitFor(Duration.of(timeout, unit.toChronoUnit()));
                  }
              }
              """
          )
        );
    }

    @Test
    void migrateProcessWaitForWithMethodCall() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.concurrent.TimeUnit;

              class Test {
                  private long getTimeout() {
                      return 10;
                  }

                  void test(Process process) throws Exception {
                      process.waitFor(getTimeout(), TimeUnit.SECONDS);
                  }
              }
              """,
            """
              import java.time.Duration;
              import java.util.concurrent.TimeUnit;

              class Test {
                  private long getTimeout() {
                      return 10;
                  }

                  void test(Process process) throws Exception {
                      process.waitFor(Duration.of(getTimeout(), TimeUnit.SECONDS.toChronoUnit()));
                  }
              }
              """
          )
        );
    }

    @Test
    void migrateProcessWaitForWithExpression() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.concurrent.TimeUnit;

              class Test {
                  void test(Process process) throws Exception {
                      long timeout = 5;
                      process.waitFor(timeout * 2, TimeUnit.MINUTES);
                  }
              }
              """,
            """
              import java.time.Duration;
              import java.util.concurrent.TimeUnit;

              class Test {
                  void test(Process process) throws Exception {
                      long timeout = 5;
                      process.waitFor(Duration.of(timeout * 2, TimeUnit.MINUTES.toChronoUnit()));
                  }
              }
              """
          )
        );
    }

    @Test
    void migrateProcessWaitForWithReturnValue() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.concurrent.TimeUnit;

              class Test {
                  void test(Process process) throws Exception {
                      if (process.waitFor(5, TimeUnit.SECONDS)) {
                          System.out.println("Process completed within timeout");
                      }
                  }
              }
              """,
            """
              import java.time.Duration;

              class Test {
                  void test(Process process) throws Exception {
                      if (process.waitFor(Duration.ofSeconds(5))) {
                          System.out.println("Process completed within timeout");
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void migrateProcessWaitForWithChainedCalls() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.concurrent.TimeUnit;

              class Test {
                  void test() throws Exception {
                      new ProcessBuilder("echo", "hello").start().waitFor(3, TimeUnit.SECONDS);
                  }
              }
              """,
            """
              import java.time.Duration;

              class Test {
                  void test() throws Exception {
                      new ProcessBuilder("echo", "hello").start().waitFor(Duration.ofSeconds(3));
                  }
              }
              """
          )
        );
    }

    @EnabledForJreRange(min = JAVA_25)
    @Test
    void noChangeForWaitForDuration() {
        rewriteRun(
          //language=java
          java(
            """
              import java.time.Duration;

              class Test {
                  void test(Process process) throws Exception {
                      process.waitFor(Duration.ofSeconds(5));
                  }
              }
              """
          )
        );
    }

    @Test
    void noChangeForParameterlessWaitFor() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  void test(Process process) throws Exception {
                      process.waitFor();
                  }
              }
              """
          )
        );
    }
}
