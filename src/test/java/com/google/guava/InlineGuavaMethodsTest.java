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
package com.google.guava;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class InlineGuavaMethodsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("com.google.guava.InlineGuavaMethods");
    }

    @DocumentExample
    @Test
    void stringsRegular() {
        rewriteRun(
          java(
            """
              import com.google.common.base.Strings;
              class Regular {
                  String repeatString(String s, int n) {
                      return Strings.repeat(s, n);
                  }
              }
              """,
            """
              class Regular {
                  String repeatString(String s, int n) {
                      return s.repeat(n);
                  }
              }
              """
          )
        );
    }

    @Test
    void stringsStaticImport() {
        rewriteRun(
          java(
            """
              import static com.google.common.base.Strings.repeat;
              class StaticImport {
                  String repeatString(String s, int n) {
                      return repeat(s, n);
                  }
              }
              """,
            """
              class StaticImport {
                  String repeatString(String s, int n) {
                      return s.repeat(n);
                  }
              }
              """
          )
        );
    }

    @Test
    void atomics() {
        rewriteRun(
          java(
            """
              import com.google.common.util.concurrent.Atomics;

              import java.util.concurrent.atomic.AtomicReference;
              import java.util.concurrent.atomic.AtomicReferenceArray;

              class Atomic {
                  AtomicReference<String> empty() {
                      return Atomics.newReference();
                  }

                  AtomicReference<String> initial(String value) {
                      return Atomics.newReference(value);
                  }

                  AtomicReferenceArray<String> sized(int length) {
                      return Atomics.newReferenceArray(length);
                  }

                  AtomicReferenceArray<String> fromArray(String[] array) {
                      return Atomics.newReferenceArray(array);
                  }
              }
              """,
            """
              import java.util.concurrent.atomic.AtomicReference;
              import java.util.concurrent.atomic.AtomicReferenceArray;

              class Atomic {
                  AtomicReference<String> empty() {
                      return new AtomicReference<>();
                  }

                  AtomicReference<String> initial(String value) {
                      return new AtomicReference<>(value);
                  }

                  AtomicReferenceArray<String> sized(int length) {
                      return new AtomicReferenceArray<>(length);
                  }

                  AtomicReferenceArray<String> fromArray(String[] array) {
                      return new AtomicReferenceArray<>(array);
                  }
              }
              """
          )
        );
    }

    @Test
    void range() {
        rewriteRun(
          java(
            """
              import com.google.common.collect.Range;

              class R {
                  boolean applied(Range<Integer> range, Integer value) {
                      return range.apply(value);
                  }

                  boolean tested(Range<Integer> range, Integer value) {
                      return range.test(value);
                  }
              }
              """,
            """
              import com.google.common.collect.Range;

              class R {
                  boolean applied(Range<Integer> range, Integer value) {
                      return range.contains(value);
                  }

                  boolean tested(Range<Integer> range, Integer value) {
                      return range.contains(value);
                  }
              }
              """
          )
        );
    }

    @Test
    void bloomFilter() {
        rewriteRun(
          java(
            """
              import com.google.common.hash.BloomFilter;

              class B {
                  boolean applied(BloomFilter<String> filter, String value) {
                      return filter.apply(value);
                  }

                  boolean tested(BloomFilter<String> filter, String value) {
                      return filter.test(value);
                  }
              }
              """,
            """
              import com.google.common.hash.BloomFilter;

              class B {
                  boolean applied(BloomFilter<String> filter, String value) {
                      return filter.mightContain(value);
                  }

                  boolean tested(BloomFilter<String> filter, String value) {
                      return filter.mightContain(value);
                  }
              }
              """
          )
        );
    }

    @Test
    void converter() {
        rewriteRun(
          java(
            """
              import com.google.common.base.Converter;

              class C {
                  Integer applied(Converter<String, Integer> converter, String value) {
                      return converter.apply(value);
                  }
              }
              """,
            """
              import com.google.common.base.Converter;

              class C {
                  Integer applied(Converter<String, Integer> converter, String value) {
                      return converter.convert(value);
                  }
              }
              """
          )
        );
    }

    @Test
    void equivalence() {
        rewriteRun(
          java(
            """
              import com.google.common.base.Equivalence;

              class E {
                  boolean tested(Equivalence<String> equivalence, String left, String right) {
                      return equivalence.test(left, right);
                  }
              }
              """,
            """
              import com.google.common.base.Equivalence;

              class E {
                  boolean tested(Equivalence<String> equivalence, String left, String right) {
                      return equivalence.equivalent(left, right);
                  }
              }
              """
          )
        );
    }

    @Test
    void orderingBinarySearch() {
        rewriteRun(
          java(
            """
              import com.google.common.collect.Ordering;

              import java.util.List;

              class O {
                  int search(Ordering<String> ordering, List<String> list, String key) {
                      return ordering.binarySearch(list, key);
                  }
              }
              """,
            """
              import com.google.common.collect.Ordering;

              import java.util.Collections;
              import java.util.List;

              class O {
                  int search(Ordering<String> ordering, List<String> list, String key) {
                      return Collections.binarySearch(list, key, ordering);
                  }
              }
              """
          )
        );
    }
}
