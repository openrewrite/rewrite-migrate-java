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
package org.openrewrite.java.migrate.util;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.version;

class UseLocaleOfTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UseLocaleOf());
    }

    @DocumentExample
    @Test
    void localeOf() {
        //language=java
        rewriteRun(
          // FIXME type validation is disabled because the current JDK is not a Java 19 JDK
          spec -> spec.typeValidationOptions(TypeValidation.none()),
          version(
            java(
              """
                import java.util.Locale;

                class Test {
                    Locale locale1 = new Locale("english");
                    Locale locale2 = new Locale("english", "us");
                    Locale locale3 = new Locale("english", "us", "");
                }
                """,
              """
                import java.util.Locale;

                class Test {
                    Locale locale1 = Locale.of("english");
                    Locale locale2 = Locale.of("english", "us");
                    Locale locale3 = Locale.of("english", "us", "");
                }
                """
            ), 19)
        );
    }
}
