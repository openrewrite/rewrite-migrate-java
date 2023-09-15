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

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.version;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

class ReplaceCollectWithStreamToListTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ReplaceCollectWithStreamToList(false));
    }

    @Test
    @DocumentExample
    void replacesToUnmodifiableList() {
        rewriteRun(
          version(
            //language=java
            java(
              """
              import java.util.stream.Collectors;
              import java.util.stream.Stream;
              import java.util.List;

              class Example {
                  public List<String> test(Stream<String> stream) {
                      return stream.collect(Collectors.toUnmodifiableList());
                  }
              }
              """,
              """
              import java.util.stream.Stream;
              import java.util.List;

              class Example {
                  public List<String> test(Stream<String> stream) {
                      return stream.toList();
                  }
              }
              """),
            16));
    }

    @Test
    void doesNotReplaceToList() {
        rewriteRun(
          version(
            //language=java
            java(
              """
              package com.example;

              import java.util.stream.Collectors;
              import java.util.stream.Stream;

              class Example {
                  public void test() {
                      Stream.of().collect(Collectors.toList());
                  }
              }
              """),
            16));
    }

    @Test
    void doesReplaceToList() {
        rewriteRun(
          recipeSpec -> recipeSpec.recipe(new ReplaceCollectWithStreamToList(true)),
          version(
            //language=java
            java(
              """
              package com.example;

              import java.util.stream.Collectors;
              import java.util.stream.Stream;

              class Example {
                  public void test() {
                      Stream.of().collect(Collectors.toList());
                  }
              }
              """,
              """
              package com.example;

              import java.util.stream.Stream;

              class Example {
                  public void test() {
                      Stream.of().toList();
                  }
              }
              """),
            16));
    }

    @Test
    void formatting() {
        rewriteRun(
          version(
            //language=java
            java(
              """
              package com.example;

              import java.util.stream.Collectors;
              import java.util.stream.Stream;

              class Example {
                  public void test() {
                      Stream.of()
                          .collect(Collectors.toUnmodifiableList());
                  }
              }
              """,
              """
              package com.example;

              import java.util.stream.Stream;

              class Example {
                  public void test() {
                      Stream.of()
                          .toList();
                  }
              }
              """),
            16));
    }

    @Test
    void comment() {
        rewriteRun(
          version(
            //language=java
            java(
              """
              package com.example;

              import java.util.stream.Collectors;
              import java.util.stream.Stream;

              class Example {
                  public void test() {
                      Stream.of()
                          // Convert to list
                          .collect(Collectors.toUnmodifiableList());
                  }
              }
              """,
              """
              package com.example;

              import java.util.stream.Stream;

              class Example {
                  public void test() {
                      Stream.of()
                          // Convert to list
                          .toList();
                  }
              }
              """),
            16));
    }

}
