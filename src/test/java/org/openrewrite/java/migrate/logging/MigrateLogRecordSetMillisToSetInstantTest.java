/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.java.migrate.logging;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.*;

class MigrateLogRecordSetMillisToSetInstantTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigrateLogRecordSetMillisToSetInstant())
          .allSources(s -> s.markers(javaVersion(9)));
    }

    @DocumentExample
    @Test
    void setMillisToSetInstantWhenParameterIsIdentifier() {
        //language=java
        rewriteRun(
          java(
            """
              package org.openrewrite.example;

              import java.util.logging.Level;
              import java.util.logging.LogRecord;

              public class Test {
                  public static void method(long millis) {
                      LogRecord logRecord = new LogRecord(Level.parse("0"), "msg");
                      logRecord.setMillis(millis);
                  }
              }
              """,
            """
              package org.openrewrite.example;

              import java.time.Instant;
              import java.util.logging.Level;
              import java.util.logging.LogRecord;

              public class Test {
                  public static void method(long millis) {
                      LogRecord logRecord = new LogRecord(Level.parse("0"), "msg");
                      logRecord.setInstant(Instant.ofEpochMilli(millis));
                  }
              }
              """
          )
        );
    }

    @Test
    void setMillisToSetInstantWhenParameterIsMethodCall() {
        //language=java
        rewriteRun(
          java(
            """
              package org.openrewrite.example;

              import java.util.logging.Level;
              import java.util.logging.LogRecord;

              public class Test {
                  private static long getLong() {
                      return 1L;
                  }

                  public static void method() {
                      LogRecord logRecord = new LogRecord(Level.parse("0"), "msg");
                      logRecord.setMillis(getLong());
                  }
              }
              """,
            """
              package org.openrewrite.example;

              import java.time.Instant;
              import java.util.logging.Level;
              import java.util.logging.LogRecord;

              public class Test {
                  private static long getLong() {
                      return 1L;
                  }

                  public static void method() {
                      LogRecord logRecord = new LogRecord(Level.parse("0"), "msg");
                      logRecord.setInstant(Instant.ofEpochMilli(getLong()));
                  }
              }
              """
          )
        );
    }

    @Test
    void setMillisToSetInstantWhenParameterIsLiteral() {
        //language=java
        rewriteRun(
          java(
            """
              package org.openrewrite.example;

              import java.util.logging.Level;
              import java.util.logging.LogRecord;

              public class Test {
                  public static void method() {
                      LogRecord logRecord = new LogRecord(Level.parse("0"), "msg");
                      logRecord.setMillis(1L);
                  }
              }
              """,
            """
              package org.openrewrite.example;

              import java.time.Instant;
              import java.util.logging.Level;
              import java.util.logging.LogRecord;

              public class Test {
                  public static void method() {
                      LogRecord logRecord = new LogRecord(Level.parse("0"), "msg");
                      logRecord.setInstant(Instant.ofEpochMilli(1L));
                  }
              }
              """
          )
        );
    }
}
