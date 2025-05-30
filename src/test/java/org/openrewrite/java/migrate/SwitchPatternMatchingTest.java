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
package org.openrewrite.java.migrate;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class SwitchPatternMatchingTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.migrate.SwitchPatternMatching");
    }

    @DocumentExample
    @Test
    void chainedRecipeEffects() {
        rewriteRun(
          //language=java
          java(
            // We want to see the separate `null` check converted to a case, which needs two recipes
            """
              class Test {
                  static String formatter(Object obj) {
                      String formatted;
                      if (obj == null) {
                          formatted = "null";
                      }
                      if (obj instanceof Integer i)
                          formatted = String.format("int %d", i);
                      else if (obj instanceof Long l) {
                          formatted = String.format("long %d", l);
                      } else if (obj instanceof Double d) {
                          formatted = String.format("double %f", d);
                      } else if (obj instanceof String s) {
                          formatted = String.format("string %s", s);
                      } else {
                          formatted = "unknown";
                      }
                      return formatted;
                  }
              }
              """,
            """
              class Test {
                  static String formatter(Object obj) {
                      String formatted;
                      switch (obj) {
                          case null -> formatted = "null";
                          case Integer i -> formatted = String.format("int %d", i);
                          case Long l -> formatted = String.format("long %d", l);
                          case Double d -> formatted = String.format("double %f", d);
                          case String s -> formatted = String.format("string %s", s);
                          default -> formatted = "unknown";
                      }
                      return formatted;
                  }
              }
              """
          )
        );
    }
}
