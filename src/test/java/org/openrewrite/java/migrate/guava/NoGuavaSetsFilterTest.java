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
import org.junitpioneer.jupiter.ExpectedToFail;
import org.junitpioneer.jupiter.Issue;
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class NoGuavaSetsFilterTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipeFromResources("org.openrewrite.java.migrate.guava.NoGuava")
          .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "guava"));
    }

    @DocumentExample
    @Test
    void replaceSetsFilter() {
        //language=java
        rewriteRun(
          java(
            """
              import java.util.Set;

              import com.google.common.base.Predicate;
              import com.google.common.collect.Sets;

              class Test {
                  public static Set<Object> test(Set<Object> set, Predicate<Object> isNotNull) {
                      return Sets.filter(set, isNotNull);
                  }
              }
              """,
            """
              import java.util.Set;
              import java.util.function.Predicate;
              import java.util.stream.Collectors;

              class Test {
                  public static Set<Object> test(Set<Object> set, Predicate<Object> isNotNull) {
                      return set.stream().filter(isNotNull).collect(Collectors.toSet());
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceSetsFilterUsingSortedSet() {
        //language=java
        rewriteRun(
          java(
            """
              import java.util.Set;
              import java.util.SortedSet;

              import com.google.common.base.Predicate;
              import com.google.common.collect.Sets;

              class Test {
                  public static Set<Object> test(SortedSet<Object> set, Predicate<Object> isNotNull) {
                      return Sets.filter(set, isNotNull);
                  }
              }
              """,
            """
              import java.util.Set;
              import java.util.SortedSet;
              import java.util.TreeSet;
              import java.util.function.Predicate;
              import java.util.stream.Collectors;

              class Test {
                  public static Set<Object> test(SortedSet<Object> set, Predicate<Object> isNotNull) {
                      return set.stream().filter(isNotNull).collect(Collectors.toCollection(TreeSet::new));
                  }
              }
              """
          )
        );
    }

    @ExpectedToFail("Sets.filter has special handling for sorted sets which now gets lost")
    @Issue("https://github.com/openrewrite/rewrite-migrate-java/pull/898#discussion_r2461706208")
    @Test
    void replaceSetsFilterIndirectlyUsingSortedSet() {
        //language=java
        rewriteRun(
          java(
            """
              import java.util.Set;
              import java.util.SortedSet;

              import com.google.common.base.Predicate;
              import com.google.common.collect.Sets;

              class Test {
                  public static Set<Object> test(SortedSet<Object> set, Predicate<Object> isNotNull) {
                      Set<Object> indirectSet = set;
                      return Sets.filter(indirectSet, isNotNull);
                  }
              }
              """
            // This now gets converted, but skips past a `instanceof SortedSet` check that keeps it a sorted set
          )
        );
    }
}
