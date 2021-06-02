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
package org.openrewrite.java.migrate.logging

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.java.JavaRecipeTest

class MigrateLogRecordSetMillisToSetInstantTest : JavaRecipeTest {
    override val recipe: Recipe
        get() = MigrateLogRecordSetMillisToSetInstant()

    @Test
    fun setMillisToSetInstantWhenParameterIsIdentifier() = assertChanged(
        before = """
            package org.openrewrite.example;

            import java.util.logging.Level;
            import java.util.logging.LogRecord;

            public class B {
                public static void method(long millis) {
                    LogRecord logRecord = new LogRecord(Level.parse("0"), "msg");
                    logRecord.setMillis(millis);
                }
            }
        """,
        after = """
            package org.openrewrite.example;

            import java.time.Instant;
            import java.util.logging.Level;
            import java.util.logging.LogRecord;

            public class B {
                public static void method(long millis) {
                    LogRecord logRecord = new LogRecord(Level.parse("0"), "msg");
                    logRecord.setInstant(Instant.ofEpochMilli(millis));
                }
            }
        """
    )

    @Test
    fun setMillisToSetInstantWhenParameterIsMethodCall() = assertChanged(
        before = """
            package org.openrewrite.example;

            import java.util.logging.Level;
            import java.util.logging.LogRecord;

            public class B {
                private static long getLong() {
                    return 1L;
                }

                public static void method() {
                    LogRecord logRecord = new LogRecord(Level.parse("0"), "msg");
                    logRecord.setMillis(getLong());
                }
            }
        """,
        after = """
            package org.openrewrite.example;

            import java.time.Instant;
            import java.util.logging.Level;
            import java.util.logging.LogRecord;

            public class B {
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

    @Test
    fun setMillisToSetInstantWhenParameterIsLiteral() = assertChanged(
        before = """
            package org.openrewrite.example;

            import java.util.logging.Level;
            import java.util.logging.LogRecord;

            public class B {
                public static void method() {
                    LogRecord logRecord = new LogRecord(Level.parse("0"), "msg");
                    logRecord.setMillis(1L);
                }
            }
        """,
        after = """
            package org.openrewrite.example;

            import java.time.Instant;
            import java.util.logging.Level;
            import java.util.logging.LogRecord;

            public class B {
                public static void method() {
                    LogRecord logRecord = new LogRecord(Level.parse("0"), "msg");
                    logRecord.setInstant(Instant.ofEpochMilli(1L));
                }
            }
        """
    )

}
