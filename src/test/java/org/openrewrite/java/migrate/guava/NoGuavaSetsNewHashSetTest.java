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

class NoGuavaSetsNewHashSetTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipe(new NoGuavaSetsNewHashSet())
          .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "guava"));
    }

    @DocumentExample
    @Test
    void replaceWithNewHashSet() {
        //language=java
        rewriteRun(
          java(
            """
              import com.google.common.collect.*;

              import java.util.Set;

              class Test {
                  Set<Integer> cardinalsWorldSeries = Sets.newHashSet();
              }
              """,
            """
              import java.util.HashSet;
              import java.util.Set;

              class Test {
                  Set<Integer> cardinalsWorldSeries = new HashSet<>();
              }
              """
          )
        );
    }

    @Test
    void replaceWithNewHashSetCollection() {
        //language=java
        rewriteRun(
          java(
            """
              import com.google.common.collect.*;

              import java.util.Collections;
              import java.util.List;
              import java.util.Set;

              class Test {
                  List<Integer> l = Collections.emptyList();
                  Set<Integer> cardinalsWorldSeries = Sets.newHashSet(l);
              }
              """,
            """
              import java.util.Collections;
              import java.util.HashSet;
              import java.util.List;
              import java.util.Set;

              class Test {
                  List<Integer> l = Collections.emptyList();
                  Set<Integer> cardinalsWorldSeries = new HashSet<>(l);
              }
              """
          )
        );
    }

    @Test
    void replaceWithNewHashSetVarargs() {
        //language=java
        rewriteRun(
          java(
            """
              import com.google.common.collect.*;

              import java.util.Set;

              class Test {
                  Set<Integer> cardinalsWorldSeries = Sets.newHashSet(2006, 2011);
              }
              """,
            """
              import java.util.Arrays;
              import java.util.HashSet;
              import java.util.Set;

              class Test {
                  Set<Integer> cardinalsWorldSeries = new HashSet<>(Arrays.asList(2006, 2011));
              }
              """
          )
        );
    }

    @Test
    void setsNewHashSetWithIterablesFilter() {
        //language=java
        rewriteRun(
          java(
            """
              import java.util.ArrayList;
              import java.util.List;

              import com.google.common.collect.Iterables;
              import com.google.common.collect.Sets;

              class Test {
                  void test() {
                      final List<ClassCastException> result = new ArrayList<ClassCastException>();
                      List<Exception> myExceptions = new ArrayList<Exception>();
                      result.addAll(Sets.newHashSet(Iterables.filter(myExceptions, ClassCastException.class)));
                  }
              }
              """
          )
        );
    }

    @Test
    void setsNewHashSetWithIteratorsFilter() {
        //language=java
        rewriteRun(
          java(
            """
              import java.util.Collection;
              import java.util.Iterator;

              import com.google.common.collect.Iterators;
              import com.google.common.collect.Sets;

              class Test {
                  public Collection<String> collectExistingRepresentations(Iterator<Object> iterator) {
                      return Sets.newHashSet(Iterators.filter(iterator, String.class));
                  }
              }
              """
          )
        );
    }

    @Test
    void setsNewHashSetWithCustomIterable() {
        //language=java
        rewriteRun(
          java(
            """
              import com.google.common.collect.Sets;

              class Test {
                  void test(Iterable<String> myIterable) {
                      var result = Sets.newHashSet(myIterable);
                  }
              }
              """
          )
        );
    }

    @Test
    void setsNewHashSetWithCollectionStillWorks() {
        //language=java
        rewriteRun(
          java(
            """
              import com.google.common.collect.Sets;

              import java.util.List;
              import java.util.Set;

              class Test {
                  public static void test(List<String> myList) {
                      Set<String> result = Sets.newHashSet(myList);
                  }
              }
              """,
            """
              import java.util.HashSet;
              import java.util.List;
              import java.util.Set;

              class Test {
                  public static void test(List<String> myList) {
                      Set<String> result = new HashSet<>(myList);
                  }
              }
              """
          )
        );
    }
}
