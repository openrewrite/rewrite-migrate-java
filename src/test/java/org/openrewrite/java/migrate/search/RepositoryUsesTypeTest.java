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
package org.openrewrite.java.migrate.search;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.test.SourceSpecs.text;

class RepositoryUsesTypeTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RepositoryUsesType("java.util.BitSet"));
    }

    @DocumentExample
    @Test
    void marksEveryFileWhenOneModuleUsesType() {
        rewriteRun(
          mavenProject("uses",
            //language=java
            java(
              """
                import java.util.BitSet;

                class Uses {
                    BitSet bits;
                }
                """,
              """
                /*~~(Repository uses type: java.util.BitSet)~~>*/import java.util.BitSet;

                class Uses {
                    BitSet bits;
                }
                """
            )
          ),
          mavenProject("other",
            //language=java
            java(
              """
                class Other {
                }
                """,
              """
                /*~~(Repository uses type: java.util.BitSet)~~>*/class Other {
                }
                """
            )
          ),
          text("readme", "~~(Repository uses type: java.util.BitSet)~~>readme")
        );
    }

    @Nested
    class NoChange {
        @Test
        void marksNothingWhenTypeIsUnused() {
            rewriteRun(
              //language=java
              java(
                """
                  class Other {
                  }
                  """
              ),
              text("readme")
            );
        }
    }
}
