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
import org.openrewrite.Issue;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class NoGuavaPredicateTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipe(new NoGuavaPredicate())
          .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "guava"));
    }

    @DocumentExample
    @Test
    void changeGuavaPredicateToJavaUtilPredicate() {
        rewriteRun(
          //language=java
          java(
            """
              import com.google.common.base.Predicate;

              class Test {
                  Predicate<String> predicate = input -> input != null && !input.isEmpty();
              }
              """,
            """
              import java.util.function.Predicate;

              class Test {
                  Predicate<String> predicate = input -> input != null && !input.isEmpty();
              }
              """
          )
        );
    }

    @Test
    void changeWhenMethodOnlyReturnsGuavaPredicate() {
        // Recipe only blocks when Predicate is used as a parameter, not return type
        rewriteRun(
          //language=java
          java(
            """
              import com.google.common.base.Predicate;

              class Test {
                  Predicate<String> predicate = input -> input != null;

                  public Predicate<String> createPredicate() {
                      return s -> s.isEmpty();
                  }
              }
              """,
            """
              import java.util.function.Predicate;

              class Test {
                  Predicate<String> predicate = input -> input != null;

                  public Predicate<String> createPredicate() {
                      return s -> s.isEmpty();
                  }
              }
              """
          )
        );
    }

    // Unfortunately also not changed now that we exclude all methods that take a Guava Predicate as precondition
    @Test
    void doNotChangePredicatesNot() {
        rewriteRun(
          //language=java
          java(
            """
              import com.google.common.base.Predicate;
              import com.google.common.base.Predicates;

              class A {
                  public static Predicate<String> notEmptyPredicate() {
                      Predicate<String> isEmpty = String::isEmpty;
                      return Predicates.not(isEmpty);
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/897")
    @Test
    void doNotChangeWhenUsingSetsFilter() {
        // Sets.filter requires Guava Predicate as last parameter
        rewriteRun(
          //language=java
          java(
            """
              import com.google.common.base.Predicate;
              import com.google.common.collect.Sets;
              import java.util.Set;

              class Test {
                  Predicate<String> notEmpty = s -> !s.isEmpty();

                  public Set<String> filterSet(Set<String> input) {
                      return Sets.filter(input, notEmpty);
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/883")
    @Test
    void doNotChangeWhenUsingIterablesFilter() {
        // Iterables.filter requires Guava Predicate as last parameter
        rewriteRun(
          //language=java
          java(
            """
              import com.google.common.base.Predicate;
              import com.google.common.collect.Iterables;
              import java.util.List;

              class Test {
                  Predicate<Integer> isPositive = n -> n > 0;

                  public Iterable<Integer> filterPositive(List<Integer> numbers) {
                      return Iterables.filter(numbers, isPositive);
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/899")
    @Test
    void doNotChangeWhenUsingCollectionsFilter() {
        // Collections2.filter requires Guava Predicate as last parameter
        rewriteRun(
          //language=java
          java(
            """
              import com.google.common.base.Predicate;
              import com.google.common.collect.Collections2;
              import java.util.Collection;

              class Test {
                  Predicate<String> notEmpty = s -> !s.isEmpty();

                  public Collection<String> filterCollection(Collection<String> input) {
                      return Collections2.filter(input, notEmpty);
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotChangeWhenUsingIteratorsFilter() {
        // Iterators.filter requires Guava Predicate as last parameter
        rewriteRun(
          //language=java
          java(
            """
              import com.google.common.base.Predicate;
              import com.google.common.collect.Iterators;

              import java.util.Iterator;

              class Test {
                  Predicate<Integer> isPositive = n -> n > 0;

                  public Iterator<Integer> filterPositive(Iterator<Integer> numbers) {
                      return Iterators.filter(numbers, isPositive);
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotChangeWhenUsingIterablesAny() {
        // Iterables.any requires Guava Predicate as last parameter
        rewriteRun(
          //language=java
          java(
            """
              import com.google.common.base.Predicate;
              import com.google.common.collect.Iterables;
              import java.util.List;

              class Test {
                  public boolean any(List<Integer> input, Predicate<Integer> aPredicate) {
                      return Iterables.any(input, aPredicate);
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotChangeWhenUsingMapsFilterEntries() {
        // Maps.filterEntries requires Guava Predicate as last parameter
        rewriteRun(
          //language=java
          java(
            """
              import com.google.common.base.Predicate;
              import com.google.common.collect.Maps;
              import java.util.Map;

              class Test {
                  public Map<String, String> filterMap(Map<String, String> input, Predicate<Map.Entry<String,String>> aPredicate) {
                      return Maps.filterEntries(input, aPredicate);
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotChangeWhenUsingMapsFilterValues() {
        // Maps.filterValues requires Guava Predicate as last parameter
        rewriteRun(
          //language=java
          java(
            """
              import com.google.common.base.Predicate;
              import com.google.common.collect.Maps;
              import java.util.Map;

              class Test {
                  public Map<String, String> filterMap(Map<String, String> input, Predicate<String> aPredicate) {
                      return Maps.filterValues(input, aPredicate);
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotChangeWhenUsingMapsFilterKeys() {
        // Maps.filterKeys requires Guava Predicate as last parameter
        rewriteRun(
          //language=java
          java(
            """
              import com.google.common.base.Predicate;
              import com.google.common.collect.Maps;
              import java.util.Map;

              class Test {
                  public Map<String, String> filterMap(Map<String, String> input, Predicate<String> aPredicate) {
                      return Maps.filterKeys(input, aPredicate);
                  }
              }
              """
          )
        );
    }
}
