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
package org.openrewrite.java.migrate.guava;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;


class NoGuavaMapsNewTreeMapTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipe(new NoGuavaMapsNewTreeMap())
          .parser(JavaParser.fromJavaVersion().classpath("guava"));
    }

    @DocumentExample
    @Test
    void replaceWithNewTreeMap() {
        //language=java
        rewriteRun(
          java(
            """
              import com.google.common.collect.*;
              import java.util.Map;

              class Test {
                  Map<Integer, Integer> cardinalsWorldSeries = Maps.newTreeMap();
              }
              """,
            """
              import java.util.Map;
              import java.util.TreeMap;

              class Test {
                  Map<Integer, Integer> cardinalsWorldSeries = new TreeMap<>();
              }
              """
          )
        );
    }

    @Test
    void replaceWithNewTreeMapWithComparator() {
        //language=java
        rewriteRun(
          java(
            """
              import java.util.Comparator;
              import com.google.common.collect.*;
              import java.util.Map;

              class Test {
                  Comparator<Integer> comparator = (o1, o2) -> 0;
                  Map<Integer, Integer> cardinalsWorldSeries = Maps.newTreeMap(comparator);
              }
              """,
            """
              import java.util.Comparator;
              import java.util.Map;
              import java.util.TreeMap;

              class Test {
                  Comparator<Integer> comparator = (o1, o2) -> 0;
                  Map<Integer, Integer> cardinalsWorldSeries = new TreeMap<>(comparator);
              }
              """
          )
        );
    }

    @Test
    void replaceWithNewTreeMapWithMap() {
        //language=java
        rewriteRun(
          java(
            """
              import com.google.common.collect.Maps;

              import java.util.Collections;
              import java.util.Map;
              import java.util.SortedMap;

              class Test {
                  SortedMap<Integer, Integer> m = Collections.emptySortedMap();
                  Map<Integer, Integer> cardinalsWorldSeries = Maps.newTreeMap(m);
              }
              """,
            """
              import java.util.Collections;
              import java.util.Map;
              import java.util.SortedMap;
              import java.util.TreeMap;

              class Test {
                  SortedMap<Integer, Integer> m = Collections.emptySortedMap();
                  Map<Integer, Integer> cardinalsWorldSeries = new TreeMap<>(m);
              }
              """
          )
        );
    }
}
