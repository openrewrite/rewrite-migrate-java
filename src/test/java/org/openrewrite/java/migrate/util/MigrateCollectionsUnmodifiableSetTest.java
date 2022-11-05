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

class MigrateCollectionsUnmodifiableSetTest implements RewriteTest {

    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigrateCollectionsUnmodifiableSet());
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/67")
    @Test
    void unmodifiableSet() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                import java.util.*;
                                
                class Test {
                    Set<Integer> s = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(1, 2, 3)));
                }
                """,
              """
                import java.util.Set;
                                
                class Test {
                    Set<Integer> s = Set.of(1, 2, 3);
                }
                """
            ),
            9
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/67")
    @Test
    void unmodifiableSetTyped() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                import java.util.*;
                import java.time.LocalDate;
                                
                class Test {
                    Set<LocalDate> s = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(LocalDate.of(2010,1,1),LocalDate.now())));
                }
                """,
              """
                import java.util.Set;
                import java.time.LocalDate;
                                
                class Test {
                    Set<LocalDate> s = Set.of(LocalDate.of(2010, 1, 1), LocalDate.now());
                }
                """
            ),
            9
          )
        );
    }
}
