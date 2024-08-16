/*
 * Copyright 2023 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.migrate.util;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;


@Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/243")
@EnabledForJreRange(min = JRE.JAVA_21)
class SequencedCollectionTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResource("/META-INF/rewrite/java-version-21.yml", "org.openrewrite.java.migrate.util.SequencedCollection");
    }

    @Nested
    class SortedSetFirstLast {
        @Test
        void firstToGetFirst() {
            rewriteRun(
              //language=java
              java(
                """
                  import java.util.*;

                  class Foo {
                      void bar(SortedSet<String> collection) {
                          String first = collection.first();
                          String last = collection.last();
                      }
                  }
                  """,
                """
                  import java.util.*;

                  class Foo {
                      void bar(SortedSet<String> collection) {
                          String first = collection.getFirst();
                          String last = collection.getLast();
                      }
                  }
                  """
              )
            );
        }
    }

    @Nested
    class NavigableSetDescendingSet {
        @Test
        void descendingSetToReversed() {
            rewriteRun(
              //language=java
              java(
                """
                  import java.util.*;

                  class Foo {
                      void bar(NavigableSet<String> collection) {
                          NavigableSet<String> reversed = collection.descendingSet();
                      }
                  }
                  """,
                """
                  import java.util.*;

                  class Foo {
                      void bar(NavigableSet<String> collection) {
                          NavigableSet<String> reversed = collection.reversed();
                      }
                  }
                  """
              )
            );
        }
    }
}
