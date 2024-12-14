/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.java.migrate.logging;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.config.Environment;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

@SuppressWarnings("deprecation")
class JavaLoggingAPIsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(Environment.builder()
          .scanRuntimeClasspath("org.openrewrite.java.migrate.logging")
          .build()
          .activateRecipes("org.openrewrite.java.migrate.logging.JavaLoggingAPIs"));
    }

    @DocumentExample
    @Test
    void loggingMXBeanToPlatformLoggingMXBean() {
        //language=java
        rewriteRun(
          java(
            """
              import java.util.logging.LoggingMXBean;

              class Test {
                  static void method() {
                      LoggingMXBean loggingBean = null;
                  }
              }
              """,
            """
              import java.lang.management.PlatformLoggingMXBean;

              class Test {
                  static void method() {
                      PlatformLoggingMXBean loggingBean = null;
                  }
              }
              """
          )
        );
    }
}
