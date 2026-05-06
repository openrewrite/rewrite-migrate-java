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
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.javaVersion;

class UseMapOfTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UseMapOf())
          .allSources(s -> s.markers(javaVersion(10)));
    }

    @DocumentExample
    @Test
    void anonymousClass() {
        //language=java
        rewriteRun(
          java(
            """
              import java.util.HashMap;
              import java.util.Map;

              class Test {
                  Map<String, String> m = new HashMap<>() {{
                      put("stru", "menta");
                      put("mod", "erne");
                  }};
              }
              """,
            """
              import java.util.Map;

              class Test {
                  Map<String, String> m = Map.of("stru", "menta", "mod", "erne");
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/1087")
    @Test
    void useMapOfEntriesForMoreThanTenEntries() {
        //language=java
        rewriteRun(
          java(
            """
              import java.util.HashMap;
              import java.util.Map;

              class Test {
                  Map<String, Integer> values() {
                      return new HashMap<>() {{
                          put("a", 1);
                          put("b", 2);
                          put("c", 3);
                          put("d", 4);
                          put("e", 5);
                          put("f", 6);
                          put("g", 7);
                          put("h", 8);
                          put("i", 9);
                          put("j", 10);
                          put("k", 11);
                      }};
                  }
              }
              """,
            """
              import java.util.Map;

              class Test {
                  Map<String, Integer> values() {
                      return Map.ofEntries(Map.entry("a", 1), Map.entry("b", 2), Map.entry("c", 3), Map.entry("d", 4), Map.entry("e", 5), Map.entry("f", 6), Map.entry("g", 7), Map.entry("h", 8), Map.entry("i", 9), Map.entry("j", 10), Map.entry("k", 11));
                  }
              }
              """
          )
        );
    }

    @Test
    void useMapOfForExactlyTenEntries() {
        //language=java
        rewriteRun(
          java(
            """
              import java.util.HashMap;
              import java.util.Map;

              class Test {
                  Map<String, Integer> values() {
                      return new HashMap<>() {{
                          put("a", 1);
                          put("b", 2);
                          put("c", 3);
                          put("d", 4);
                          put("e", 5);
                          put("f", 6);
                          put("g", 7);
                          put("h", 8);
                          put("i", 9);
                          put("j", 10);
                      }};
                  }
              }
              """,
            """
              import java.util.Map;

              class Test {
                  Map<String, Integer> values() {
                      return Map.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7, "h", 8, "i", 9, "j", 10);
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/566")
    @Test
    void changeDoubleBraceInitForNonStringTypes() {
        //language=java
        rewriteRun(
          java(
            """
              import java.util.HashMap;
              import java.util.Map;

              class Test {
              private static final String BLAH ="ss";
              
              void foo() {
                        new HashMap<>() {{
                          put(BLAH, "foo");
                      }};
                  }
              }
              """,
            """
              import java.util.Map;

              class Test {
              private static final String BLAH ="ss";
              
              void foo() {
                  Map.of(BLAH, "foo");
                  }
              }
              """
          )
        );
    }
}
