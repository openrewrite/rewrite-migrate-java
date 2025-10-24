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
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class NoGuavaSetsFilterTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipe(new NoGuavaSetsFilter())
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
              import java.util.stream.Collectors;

              import com.google.common.base.Predicate;

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
              import java.util.stream.Collectors;

              import com.google.common.base.Predicate;

              class Test {
                  public static Set<Object> test(SortedSet<Object> set, Predicate<Object> isNotNull) {
                      return set.stream().filter(isNotNull).collect(Collectors.toCollection(TreeSet::new));
                  }
              }
              """
          )
        );
    }
}
