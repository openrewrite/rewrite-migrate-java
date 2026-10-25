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
import org.junitpioneer.jupiter.ExpectedToFail;
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.javaVersion;
import static org.openrewrite.maven.Assertions.pomXml;

class NoGuavaTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipeFromResources("org.openrewrite.java.migrate.guava.NoGuava")
          .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "guava"));
    }

    @DocumentExample
    @Test
    void moreObjectsFirstNonNullToObjectsRequireNonNullElse() {
        rewriteRun(
          //language=java
          java(
            """
              import com.google.common.base.MoreObjects;

              class A {
                  Object foo(Object obj) {
                      return MoreObjects.firstNonNull(obj, "default");
                  }
              }
              """,
            """
              import java.util.Objects;

              class A {
                  Object foo(Object obj) {
                      return Objects.requireNonNullElse(obj, "default");
                  }
              }
              """,
            spec -> spec.markers(javaVersion(11))
          )
        );
    }

    @Test
    void upgradeSpringfoxThatStillUsesGuavaOptional() {
        rewriteRun(
          //language=xml
          pomXml(
            """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>demo</artifactId>
                  <version>1.0.0</version>
                  <dependencies>
                      <dependency>
                          <groupId>io.springfox</groupId>
                          <artifactId>springfox-swagger2</artifactId>
                          <version>2.9.2</version>
                      </dependency>
                  </dependencies>
              </project>
              """,
            """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>demo</artifactId>
                  <version>1.0.0</version>
                  <dependencies>
                      <dependency>
                          <groupId>io.springfox</groupId>
                          <artifactId>springfox-swagger2</artifactId>
                          <version>2.10.5</version>
                      </dependency>
                  </dependencies>
              </project>
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/39#issuecomment-910673213")
    @Test
    void preferJavaUtilObjectsHashCode() {
        //language=java
        rewriteRun(
          java(
            """
              package com.acme.product;

              import com.google.common.base.Objects;

              class MyPojo {
                  String x;

                  @Override
                  public int hashCode() {
                      return Objects.hashCode(x);
                  }
              }
              """,
            """
              package com.acme.product;

              import java.util.Objects;

              class MyPojo {
                  String x;

                  @Override
                  public int hashCode() {
                      return Objects.hash(x);
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/39#issuecomment-910673213")
    @Test
    void preferJavaUtilObjectsEquals() {
        //language=java
        rewriteRun(
          java(
            """
              import com.google.common.base.Objects;

              class Test {
                  static boolean isEqual(Object obj0, Object obj1) {
                      return Objects.equal(obj0, obj1);
                  }
              }
              """,
            """
              import java.util.Objects;

              class Test {
                  static boolean isEqual(Object obj0, Object obj1) {
                      return Objects.equals(obj0, obj1);
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/920")
    @Test
    void iterablesAllMigration() {
        //language=java
        rewriteRun(
          java(
            """
              import java.util.Collection;

              import com.google.common.base.Predicate;
              import com.google.common.collect.Iterables;

              class Test {
                  boolean test(Collection<String> collection, Predicate<String> aPredicate) {
                      return Iterables.all(collection, aPredicate);
                  }
              }
              """,
            """
              import java.util.Collection;
              import java.util.function.Predicate;

              class Test {
                  boolean test(Collection<String> collection, Predicate<String> aPredicate) {
                      return collection.stream().allMatch(aPredicate);
                  }
              }
              """
          )
        );
    }

    @Test
    void predicatesEqualToToPredicateIsEqual() {
        rewriteRun(
          //language=java
          java(
            """
              import com.google.common.base.Predicate;
              import com.google.common.base.Predicates;

              class A {
                  public static Predicate<String> isHelloPredicate() {
                      return Predicates.equalTo("hello");
                  }
              }
              """,
            """
              import java.util.function.Predicate;

              class A {
                  public static Predicate<String> isHelloPredicate() {
                      return Predicate.<String>isEqual("hello");
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/903")
    @Test
    void inlinedPredicatesEqualToWithParameterizedType() {
        rewriteRun(
          //language=java
          java(
            """
              import com.google.common.base.Predicates;
              import com.google.common.base.Predicate;

              import java.util.Collection;

              class Test {
                  public static void test(Collection<String> aCollection, Predicate<Collection<String>> anotherPredicate) {
                      Predicate<Collection<String>> combined = Predicates.and(Predicates.equalTo(aCollection), anotherPredicate);
                  }
              }
              """,
            """
              import java.util.Collection;
              import java.util.function.Predicate;

              class Test {
                  public static void test(Collection<String> aCollection, Predicate<Collection<String>> anotherPredicate) {
                      Predicate<Collection<String>> combined = Predicate.<Collection<String>>isEqual(aCollection).and(anotherPredicate);
                  }
              }
              """
          )
        );
    }

    @Test
    void inlinedPredicatesEqualToWithStandardType() {
        rewriteRun(
          //language=java
          java(
            """
              import com.google.common.base.Predicates;
              import com.google.common.base.Predicate;

              class Test {
                  static Predicate<String> getMaxLengthPredicate() {
                          return s -> s.length() < 10;
                  }

                  public static void test() {
                      Predicate<String> combined = Predicates.and(Predicates.equalTo("MyTest"), getMaxLengthPredicate());
                  }
              }
              """,
            """
              import java.util.function.Predicate;

              class Test {
                  static Predicate<String> getMaxLengthPredicate() {
                          return s -> s.length() < 10;
                  }

                  public static void test() {
                      Predicate<String> combined = Predicate.<String>isEqual("MyTest").and(getMaxLengthPredicate());
                  }
              }
              """
          )
        );
    }

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
