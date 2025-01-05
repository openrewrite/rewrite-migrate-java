/*
 * Copyright 2025 the original author or authors.
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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.javaVersion;

class NoMapsAndSetsWithExpectedSizeTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new NoMapsAndSetsWithExpectedSize());
    }

    @DocumentExample
    @Test
    void noMapSetWithExpectedSize() {
        rewriteRun(
          //language=java
          java(
            """
              import com.google.common.collect.Maps;
              import com.google.common.collect.Sets;
              import java.util.Map;
              import java.util.Set;

              class A {
                  void method() {
                      Map<String, String> a = Maps.newHashMapWithExpectedSize(1);
                      Map<String, String> b = Maps.newLinkedHashMapWithExpectedSize(1);
                      Set<String> c = Sets.newHashSetWithExpectedSize(1);
                      Set<String> d = Sets.newLinkedHashSetWithExpectedSize(1);
                  }
              }
              """,
            """
              import java.util.*;

              class A {
                  void method() {
                      Map<String, String> a = new HashMap<>(1);
                      Map<String, String> b = new LinkedHashMap<>(1);
                      Set<String> c = new HashSet<>(1);
                      Set<String> d = new LinkedHashSet<>(1);
                  }
              }
              """,
            spec -> spec.markers(javaVersion(21))
          )
        );
    }
}
