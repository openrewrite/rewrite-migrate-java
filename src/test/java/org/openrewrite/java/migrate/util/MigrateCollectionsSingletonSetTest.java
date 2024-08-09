/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.java.migrate.util;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.version;

class MigrateCollectionsSingletonSetTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigrateCollectionsSingletonSet());
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/72")
    @Test
    void singleTonSet() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                import java.util.*;

                class Test {
                    Set<String> set = Collections.singleton("Hello");
                }
                """,
              """
                import java.util.Set;

                class Test {
                    Set<String> set = Set.of("Hello");
                }
                """
            ),
            9
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/72")
    @Test
    void singletonSetCustomType() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                import java.util.*;
                import java.time.LocalDate;

                class Test {
                    Set<LocalDate> set = Collections.singleton(LocalDate.now());
                }
                """,
              """
                import java.util.Set;
                import java.time.LocalDate;

                class Test {
                    Set<LocalDate> set = Set.of(LocalDate.now());
                }
                """
            ),
            9
          )
        );
    }
}
