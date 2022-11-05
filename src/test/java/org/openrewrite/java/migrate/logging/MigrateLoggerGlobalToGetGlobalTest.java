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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;

@SuppressWarnings("deprecation")
class MigrateLoggerGlobalToGetGlobalTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigrateLoggerGlobalToGetGlobal());
    }

    @Test
    void globalToGetGlobal() {
        //language=java
        rewriteRun(
          spec -> spec.typeValidationOptions(new TypeValidation(true, true, false, true)),
          java(
            """
              package org.openrewrite.example;

              import java.util.logging.Logger;

              public class Test {
                  public static void method() {
                      Logger logger = Logger.global;
                  }
              }
              """,
            """
              package org.openrewrite.example;

              import java.util.logging.Logger;

              public class Test {
                  public static void method() {
                      Logger logger = Logger.getGlobal();
                  }
              }
              """
          )
        );
    }
}
