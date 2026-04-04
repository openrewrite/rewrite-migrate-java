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
package org.openrewrite.java.migrate.util;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.javaVersion;

class MigrateCollectionsEmptyMapTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigrateCollectionsEmptyMap())
          .allSources(s -> s.markers(javaVersion(9)));
    }

    @DocumentExample
    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/1044")
    @Test
    void emptyList() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.*;

              class Test {
                  Map<String, String> map = Collections.emptyMap();
              }
              """,
            """
              import java.util.Map;

              class Test {
                  Map<String, String> map = Map.of();
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/1044")
    @Test
    void emptyListStaticImport() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.*;
              import static java.util.Collections.emptyMap;

              class Test {
                  Map<String, String> map = emptyMap();
              }
              """,
            """
              import java.util.Map;

              class Test {
                  Map<String, String> map = Map.of();
              }
              """
          )
        );
    }
}
