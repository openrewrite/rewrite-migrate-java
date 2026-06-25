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
                  Map<String, String> m = Map.of(
                      "stru", "menta",
                      "mod", "erne");
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
                      return Map.ofEntries(
                          Map.entry("a", 1),
                          Map.entry("b", 2),
                          Map.entry("c", 3),
                          Map.entry("d", 4),
                          Map.entry("e", 5),
                          Map.entry("f", 6),
                          Map.entry("g", 7),
                          Map.entry("h", 8),
                          Map.entry("i", 9),
                          Map.entry("j", 10),
                          Map.entry("k", 11));
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
                      return Map.of(
                          "a", 1,
                          "b", 2,
                          "c", 3,
                          "d", 4,
                          "e", 5,
                          "f", 6,
                          "g", 7,
                          "h", 8,
                          "i", 9,
                          "j", 10);
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/1092")
    @Test
    void doNotChangeWhenNullValue() {
        //language=java
        rewriteRun(
          java(
            """
              import java.util.HashMap;
              import java.util.Map;

              class Test {
                  Map<String, String> m = new HashMap<>() {{
                      put("key", "value");
                      put("nullable", null);
                  }};
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/1092")
    @Test
    void doNotChangeWhenNullKey() {
        //language=java
        rewriteRun(
          java(
            """
              import java.util.HashMap;
              import java.util.Map;

              class Test {
                  Map<String, String> m = new HashMap<>() {{
                      put(null, "value");
                      put("key", "other");
                  }};
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/1148")
    @Test
    void doNotChangeConcreteHashMapField() {
        //language=java
        rewriteRun(
          spec -> spec.allSources(s -> s.markers(javaVersion(25))),
          java(
            """
              import java.util.HashMap;

              class Main {
                  private static final HashMap<String, String> VALUES = new HashMap<String, String>() {{
                      put("key", "value");
                  }};
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/1148")
    @Test
    void doNotChangeConcreteHashMapLocalVariable() {
        //language=java
        rewriteRun(
          java(
            """
              import java.util.HashMap;

              class Main {
                  void m() {
                      HashMap<String, Integer> ages = new HashMap<>() {{
                          put("Bob", 42);
                          put("alice", 30);
                      }};
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/1112")
    @Test
    void doNotChangeLinkedHashMap() {
        //language=java
        rewriteRun(
          java(
            """
              import java.util.LinkedHashMap;
              import java.util.Map;

              class Test {
                  static final Map<String, String> ORDERED = new LinkedHashMap<>() {{
                      put("a", "1");
                      put("b", "2");
                      put("c", "3");
                  }};
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/1112")
    @Test
    void doNotChangeTreeMap() {
        //language=java
        rewriteRun(
          java(
            """
              import java.util.Map;
              import java.util.TreeMap;

              class Test {
                  static final Map<String, String> SORTED = new TreeMap<>() {{
                      put("a", "1");
                      put("b", "2");
                  }};
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

    @Test
    void proseChainCollapsedIntoHashMapConstructorMapOf() {
        //language=java
        rewriteRun(
          java(
            """
              import java.util.HashMap;
              import java.util.Map;

              class Test {
                  void m() {
                      Map<String, Integer> ages = new HashMap<>();
                      ages.put("Bob", 42);
                      ages.put("alice", 30);
                      ages.put("Charlie", 51);
                  }
              }
              """,
            """
              import java.util.HashMap;
              import java.util.Map;

              class Test {
                  void m() {
                      Map<String, Integer> ages = new HashMap<>(Map.of(
                              "Bob", 42,
                              "alice", 30,
                              "Charlie", 51));
                  }
              }
              """
          )
        );
    }

    @Test
    void prosePreservesCommentsOnPutStatements() {
        //language=java
        rewriteRun(
          java(
            """
              import java.util.HashMap;
              import java.util.Map;

              class Test {
                  void m() {
                      Map<String, Integer> ages = new HashMap<>();
                      // Bob is the boss
                      ages.put("Bob", 42);
                      ages.put("alice", 30);
                  }
              }
              """,
            """
              import java.util.HashMap;
              import java.util.Map;

              class Test {
                  void m() {
                      Map<String, Integer> ages = new HashMap<>(Map.of(
                              // Bob is the boss
                              "Bob", 42,
                              "alice", 30));
                  }
              }
              """
          )
        );
    }

    @Test
    void proseChainOverTenPairsUsesMapOfEntries() {
        //language=java
        rewriteRun(
          java(
            """
              import java.util.HashMap;
              import java.util.Map;

              class Test {
                  void m() {
                      Map<String, Integer> codes = new HashMap<>();
                      codes.put("a", 1);
                      codes.put("b", 2);
                      codes.put("c", 3);
                      codes.put("d", 4);
                      codes.put("e", 5);
                      codes.put("f", 6);
                      codes.put("g", 7);
                      codes.put("h", 8);
                      codes.put("i", 9);
                      codes.put("j", 10);
                      codes.put("k", 11);
                  }
              }
              """,
            """
              import java.util.HashMap;
              import java.util.Map;

              class Test {
                  void m() {
                      Map<String, Integer> codes = new HashMap<>(Map.ofEntries(
                              Map.entry("a", 1),
                              Map.entry("b", 2),
                              Map.entry("c", 3),
                              Map.entry("d", 4),
                              Map.entry("e", 5),
                              Map.entry("f", 6),
                              Map.entry("g", 7),
                              Map.entry("h", 8),
                              Map.entry("i", 9),
                              Map.entry("j", 10),
                              Map.entry("k", 11)));
                  }
              }
              """
          )
        );
    }

    @Test
    void proseSinglePutBelowThresholdLeftAlone() {
        //language=java
        rewriteRun(
          java(
            """
              import java.util.HashMap;
              import java.util.Map;

              class Test {
                  void m() {
                      Map<String, Integer> ages = new HashMap<>();
                      ages.put("Bob", 42);
                  }
              }
              """
          )
        );
    }

    @Test
    void proseInterveningStatementStopsAccumulation() {
        //language=java
        rewriteRun(
          java(
            """
              import java.util.HashMap;
              import java.util.Map;

              class Test {
                  void m() {
                      Map<String, Integer> ages = new HashMap<>();
                      ages.put("Bob", 42);
                      System.out.println("debug");
                      ages.put("alice", 30);
                  }
              }
              """
          )
        );
    }

    @Test
    void proseValueReferencingTargetIsBail() {
        //language=java
        rewriteRun(
          java(
            """
              import java.util.HashMap;
              import java.util.Map;

              class Test {
                  void m() {
                      Map<String, Integer> counts = new HashMap<>();
                      counts.put("first", 1);
                      counts.put("size", counts.size());
                  }
              }
              """
          )
        );
    }

    @Test
    void proseNullKeyOrValueIsBail() {
        //language=java
        rewriteRun(
          java(
            """
              import java.util.HashMap;
              import java.util.Map;

              class Test {
                  void m() {
                      Map<String, Integer> ages = new HashMap<>();
                      ages.put("Bob", 42);
                      ages.put("alice", null);
                  }
              }
              """
          )
        );
    }

    @Test
    void proseRawMapLeftAlone() {
        //language=java
        rewriteRun(
          java(
            """
              import java.util.HashMap;
              import java.util.Map;

              class Test {
                  void m() {
                      Map ages = new HashMap();
                      ages.put("Bob", 42);
                      ages.put("alice", 30);
                  }
              }
              """
          )
        );
    }
}
