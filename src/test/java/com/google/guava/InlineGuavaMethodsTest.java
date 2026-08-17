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
}
