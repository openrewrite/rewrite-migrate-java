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

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/1163")
    @Test
    void doNotChangeLinkedHashMapBuiltWithPutStatements() {
        //language=java
        rewriteRun(
          java(
            """
              import java.util.LinkedHashMap;
              import java.util.Map;

              class Test {
                  static Map<String, String> ordered() {
                      Map<String, String> m = new LinkedHashMap<>();
                      m.put("a", "1");
                      m.put("b", "2");
                      m.put("c", "3");
                      return m;
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

    @Test
    void proseMapsWithProvablyNonNullValuesAreCollapsed() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.HashMap;
              import java.util.Map;

              class PrimitiveValues {
                  Map<String, Integer> values(int first, int second) {
                      Map<String, Integer> values = new HashMap<>();
                      values.put("first", first);
                      values.put("second", second);
                      return values;
                  }
              }
              """,
            """
              import java.util.HashMap;
              import java.util.Map;

              class PrimitiveValues {
                  Map<String, Integer> values(int first, int second) {
                      Map<String, Integer> values = new HashMap<>(Map.of(
                              "first", first,
                              "second", second));
                      return values;
                  }
              }
              """
          ),
          //language=java
          java(
            """
              import java.util.HashMap;
              import java.util.Map;

              class ParenthesizedValues {
                  Map<String, Integer> values(int first, int second) {
                      Map<String, Integer> values = new HashMap<>();
                      values.put(("first"), first);
                      values.put(("second"), second);
                      return values;
                  }
              }
              """,
            """
              import java.util.HashMap;
              import java.util.Map;

              class ParenthesizedValues {
                  Map<String, Integer> values(int first, int second) {
                      Map<String, Integer> values = new HashMap<>(Map.of(
                              "first", first,
                              "second", second));
                      return values;
                  }
              }
              """
          ),
          //language=java
          java(
            """
              import java.util.HashMap;
              import java.util.Map;

              class NewClassValues {
                  Map<String, Object> values() {
                      Map<String, Object> values = new HashMap<>();
                      values.put("first", new Object());
                      values.put("second", new Object());
                      return values;
                  }
              }
              """,
            """
              import java.util.HashMap;
              import java.util.Map;

              class NewClassValues {
                  Map<String, Object> values() {
                      Map<String, Object> values = new HashMap<>(Map.of(
                              "first", new Object(),
                              "second", new Object()));
                      return values;
                  }
              }
              """
          )
        );
    }

    @Test
    void proseMapWithUnknownNullableValueIsLeftAlone() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.HashMap;
              import java.util.Map;

              class Test {
                  Map<String, Object> prompt(Object card, Object attempt) {
                      Map<String, Object> values = new HashMap<>();
                      values.put("card", card);
                      values.put("attempt", attempt);
                      return values;
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/1310")
    @Test
    void doNotRefactorAnonymousClassWithMoreThanMaxEntries() {
        //language=java
        rewriteRun(
          java(
            """
              import java.util.HashMap;
              import java.util.Map;

              class Test {
                  Map<String, Integer> values() {
                      return new HashMap<>() {{
                      put("k1", 1);
                      put("k2", 2);
                      put("k3", 3);
                      put("k4", 4);
                      put("k5", 5);
                      put("k6", 6);
                      put("k7", 7);
                      put("k8", 8);
                      put("k9", 9);
                      put("k10", 10);
                      put("k11", 11);
                      put("k12", 12);
                      put("k13", 13);
                      put("k14", 14);
                      put("k15", 15);
                      put("k16", 16);
                      put("k17", 17);
                      put("k18", 18);
                      put("k19", 19);
                      put("k20", 20);
                      put("k21", 21);
                      put("k22", 22);
                      put("k23", 23);
                      put("k24", 24);
                      put("k25", 25);
                      put("k26", 26);
                      put("k27", 27);
                      put("k28", 28);
                      put("k29", 29);
                      put("k30", 30);
                      put("k31", 31);
                      put("k32", 32);
                      put("k33", 33);
                      put("k34", 34);
                      put("k35", 35);
                      put("k36", 36);
                      put("k37", 37);
                      put("k38", 38);
                      put("k39", 39);
                      put("k40", 40);
                      put("k41", 41);
                      put("k42", 42);
                      put("k43", 43);
                      put("k44", 44);
                      put("k45", 45);
                      put("k46", 46);
                      put("k47", 47);
                      put("k48", 48);
                      put("k49", 49);
                      put("k50", 50);
                      put("k51", 51);
                      }};
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/1310")
    @Test
    void doNotRefactorProseChainWithMoreThanMaxEntries() {
        //language=java
        rewriteRun(
          java(
            """
              import java.util.HashMap;
              import java.util.Map;

              class Test {
                  void m() {
                      Map<String, Integer> codes = new HashMap<>();
                      codes.put("k1", 1);
                      codes.put("k2", 2);
                      codes.put("k3", 3);
                      codes.put("k4", 4);
                      codes.put("k5", 5);
                      codes.put("k6", 6);
                      codes.put("k7", 7);
                      codes.put("k8", 8);
                      codes.put("k9", 9);
                      codes.put("k10", 10);
                      codes.put("k11", 11);
                      codes.put("k12", 12);
                      codes.put("k13", 13);
                      codes.put("k14", 14);
                      codes.put("k15", 15);
                      codes.put("k16", 16);
                      codes.put("k17", 17);
                      codes.put("k18", 18);
                      codes.put("k19", 19);
                      codes.put("k20", 20);
                      codes.put("k21", 21);
                      codes.put("k22", 22);
                      codes.put("k23", 23);
                      codes.put("k24", 24);
                      codes.put("k25", 25);
                      codes.put("k26", 26);
                      codes.put("k27", 27);
                      codes.put("k28", 28);
                      codes.put("k29", 29);
                      codes.put("k30", 30);
                      codes.put("k31", 31);
                      codes.put("k32", 32);
                      codes.put("k33", 33);
                      codes.put("k34", 34);
                      codes.put("k35", 35);
                      codes.put("k36", 36);
                      codes.put("k37", 37);
                      codes.put("k38", 38);
                      codes.put("k39", 39);
                      codes.put("k40", 40);
                      codes.put("k41", 41);
                      codes.put("k42", 42);
                      codes.put("k43", 43);
                      codes.put("k44", 44);
                      codes.put("k45", 45);
                      codes.put("k46", 46);
                      codes.put("k47", 47);
                      codes.put("k48", 48);
                      codes.put("k49", 49);
                      codes.put("k50", 50);
                      codes.put("k51", 51);
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/1310")
    @Test
    void maxEntriesOptionCanBeRaised() {
        rewriteRun(
          spec -> spec.recipe(new UseMapOf(60)),
          //language=java
          java(
            """
              import java.util.HashMap;
              import java.util.Map;

              class Test {
                  void m() {
                      Map<String, Integer> codes = new HashMap<>();
                      codes.put("k1", 1);
                      codes.put("k2", 2);
                      codes.put("k3", 3);
                      codes.put("k4", 4);
                      codes.put("k5", 5);
                      codes.put("k6", 6);
                      codes.put("k7", 7);
                      codes.put("k8", 8);
                      codes.put("k9", 9);
                      codes.put("k10", 10);
                      codes.put("k11", 11);
                      codes.put("k12", 12);
                      codes.put("k13", 13);
                      codes.put("k14", 14);
                      codes.put("k15", 15);
                      codes.put("k16", 16);
                      codes.put("k17", 17);
                      codes.put("k18", 18);
                      codes.put("k19", 19);
                      codes.put("k20", 20);
                      codes.put("k21", 21);
                      codes.put("k22", 22);
                      codes.put("k23", 23);
                      codes.put("k24", 24);
                      codes.put("k25", 25);
                      codes.put("k26", 26);
                      codes.put("k27", 27);
                      codes.put("k28", 28);
                      codes.put("k29", 29);
                      codes.put("k30", 30);
                      codes.put("k31", 31);
                      codes.put("k32", 32);
                      codes.put("k33", 33);
                      codes.put("k34", 34);
                      codes.put("k35", 35);
                      codes.put("k36", 36);
                      codes.put("k37", 37);
                      codes.put("k38", 38);
                      codes.put("k39", 39);
                      codes.put("k40", 40);
                      codes.put("k41", 41);
                      codes.put("k42", 42);
                      codes.put("k43", 43);
                      codes.put("k44", 44);
                      codes.put("k45", 45);
                      codes.put("k46", 46);
                      codes.put("k47", 47);
                      codes.put("k48", 48);
                      codes.put("k49", 49);
                      codes.put("k50", 50);
                      codes.put("k51", 51);
                  }
              }
              """,
            """
              import java.util.HashMap;
              import java.util.Map;

              class Test {
                  void m() {
                      Map<String, Integer> codes = new HashMap<>(Map.ofEntries(
                              Map.entry("k1", 1),
                              Map.entry("k2", 2),
                              Map.entry("k3", 3),
                              Map.entry("k4", 4),
                              Map.entry("k5", 5),
                              Map.entry("k6", 6),
                              Map.entry("k7", 7),
                              Map.entry("k8", 8),
                              Map.entry("k9", 9),
                              Map.entry("k10", 10),
                              Map.entry("k11", 11),
                              Map.entry("k12", 12),
                              Map.entry("k13", 13),
                              Map.entry("k14", 14),
                              Map.entry("k15", 15),
                              Map.entry("k16", 16),
                              Map.entry("k17", 17),
                              Map.entry("k18", 18),
                              Map.entry("k19", 19),
                              Map.entry("k20", 20),
                              Map.entry("k21", 21),
                              Map.entry("k22", 22),
                              Map.entry("k23", 23),
                              Map.entry("k24", 24),
                              Map.entry("k25", 25),
                              Map.entry("k26", 26),
                              Map.entry("k27", 27),
                              Map.entry("k28", 28),
                              Map.entry("k29", 29),
                              Map.entry("k30", 30),
                              Map.entry("k31", 31),
                              Map.entry("k32", 32),
                              Map.entry("k33", 33),
                              Map.entry("k34", 34),
                              Map.entry("k35", 35),
                              Map.entry("k36", 36),
                              Map.entry("k37", 37),
                              Map.entry("k38", 38),
                              Map.entry("k39", 39),
                              Map.entry("k40", 40),
                              Map.entry("k41", 41),
                              Map.entry("k42", 42),
                              Map.entry("k43", 43),
                              Map.entry("k44", 44),
                              Map.entry("k45", 45),
                              Map.entry("k46", 46),
                              Map.entry("k47", 47),
                              Map.entry("k48", 48),
                              Map.entry("k49", 49),
                              Map.entry("k50", 50),
                              Map.entry("k51", 51)));
                  }
              }
              """
          )
        );
    }

    @Test
    void maxEntriesOptionCanBeLowered() {
        rewriteRun(
          spec -> spec.recipe(new UseMapOf(3)),
          //language=java
          java(
            """
              import java.util.HashMap;
              import java.util.Map;

              class FourEntries {
                  void m() {
                      Map<String, Integer> codes = new HashMap<>();
                      codes.put("a", 1);
                      codes.put("b", 2);
                      codes.put("c", 3);
                      codes.put("d", 4);
                  }
              }
              """
          ),
          //language=java
          java(
            """
              import java.util.HashMap;
              import java.util.Map;

              class ThreeEntries {
                  void m() {
                      Map<String, Integer> codes = new HashMap<>();
                      codes.put("a", 1);
                      codes.put("b", 2);
                      codes.put("c", 3);
                  }
              }
              """,
            """
              import java.util.HashMap;
              import java.util.Map;

              class ThreeEntries {
                  void m() {
                      Map<String, Integer> codes = new HashMap<>(Map.of(
                              "a", 1,
                              "b", 2,
                              "c", 3));
                  }
              }
              """
          )
        );
    }

}
