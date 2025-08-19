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
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;


class NoGuavaMapsNewHashMapTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipe(new NoGuavaMapsNewHashMap())
          .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "guava"));
    }

    @DocumentExample
    @Test
    void replaceWithNewHashMap() {
        //language=java
        rewriteRun(
          java(
            """
              import com.google.common.collect.*;
              import java.util.Map;

              class Test {
                  Map<Integer, Integer> cardinalsWorldSeries = Maps.newHashMap();
              }
              """,
            """
              import java.util.HashMap;
              import java.util.Map;

              class Test {
                  Map<Integer, Integer> cardinalsWorldSeries = new HashMap<>();
              }
              """
          )
        );
    }

    @Test
    void replaceWithNewHashMapWithMap() {
        //language=java
        rewriteRun(
          java(
            """
              import com.google.common.collect.*;
              import java.util.Collections;
              import java.util.Map;

              class Test {
                  Map<Integer, Integer> m = Collections.emptyMap();
                  Map<Integer, Integer> cardinalsWorldSeries = Maps.newHashMap(m);
              }
              """,
            """
              import java.util.Collections;
              import java.util.HashMap;
              import java.util.Map;

              class Test {
                  Map<Integer, Integer> m = Collections.emptyMap();
                  Map<Integer, Integer> cardinalsWorldSeries = new HashMap<>(m);
              }
              """
          )
        );
    }
}
