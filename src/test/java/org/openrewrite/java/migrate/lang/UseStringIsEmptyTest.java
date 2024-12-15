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
package org.openrewrite.java.migrate.lang;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class UseStringIsEmptyTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UseStringIsEmptyRecipe());
    }

    @DocumentExample
    @Test
    void lengthGreaterZero() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  void m(String s) {
                      boolean b = s.length() > 0;
                      boolean c = 0 < s.length();
                      boolean d = 0 != s.length();
                      boolean e = s.length() != 0;
                  }
              }
              """,
            """
              class Test {
                  void m(String s) {
                      boolean b = !s.isEmpty();
                      boolean c = !s.isEmpty();
                      boolean d = !s.isEmpty();
                      boolean e = !s.isEmpty();
                  }
              }
              """
          )
        );
    }

    @Test
    void lengthGreaterOne() {
        rewriteRun(
          java(
            """
              class Test {
                  void m(String s) {
                      boolean b = s.length() > 1;
                  }
              }
              """
          )
        );
    }
}
