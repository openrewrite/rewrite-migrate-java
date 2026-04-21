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

class MigrateCollectionsEmptyListTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigrateCollectionsEmptyList())
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
                  List<String> list = Collections.emptyList();
              }
              """,
            """
              import java.util.List;

              class Test {
                  List<String> list = List.of();
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
              import static java.util.Collections.emptyList;

              class Test {
                  List<String> list = emptyList();
              }
              """,
            """
              import java.util.List;

              class Test {
                  List<String> list = List.of();
              }
              """
          )
        );
    }

    @Test
    void emptyListAsMethodArgument() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.*;

              class Test {
                  void use(List<String> l) {}
                  void call() {
                      use(Collections.emptyList());
                  }
              }
              """,
            """
              import java.util.List;

              class Test {
                  void use(List<String> l) {}
                  void call() {
                      use(List.of());
                  }
              }
              """
          )
        );
    }

    @Test
    void emptyListInTernary() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.*;

              class Test {
                  List<String> get(boolean flag, List<String> other) {
                      return flag ? Collections.emptyList() : other;
                  }
              }
              """,
            """
              import java.util.List;

              class Test {
                  List<String> get(boolean flag, List<String> other) {
                      return flag ? List.of() : other;
                  }
              }
              """
          )
        );
    }

    @Test
    void emptyListInSwitchExpression() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.*;

              class Test {
                  List<String> get(int i) {
                      return switch (i) {
                          case 0 -> Collections.emptyList();
                          default -> null;
                      };
                  }
              }
              """,
            """
              import java.util.List;

              class Test {
                  List<String> get(int i) {
                      return switch (i) {
                          case 0 -> List.of();
                          default -> null;
                      };
                  }
              }
              """
          )
        );
    }
}
