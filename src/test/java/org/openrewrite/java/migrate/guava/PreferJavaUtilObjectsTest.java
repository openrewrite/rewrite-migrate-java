/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.java.migrate.guava;

import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.ExpectedToFail;
import org.openrewrite.DocumentExample;
import org.openrewrite.config.Environment;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class PreferJavaUtilObjectsTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(
            Environment.builder()
              .scanRuntimeClasspath("org.openrewrite.java.migrate.guava")
              .build()
              .activateRecipes("org.openrewrite.java.migrate.guava.NoGuava")
          )
          .parser(JavaParser.fromJavaVersion().classpath("rewrite-java", "guava"));
    }

    @DocumentExample
    @Test
    void preconditionsCheckNotNullToObjectsRequireNonNull() {
        //language=java
        rewriteRun(java("""
          import com.google.common.base.Preconditions;

          class A {
              Object foo(Object obj) {
                  return Preconditions.checkNotNull(obj);
              }
          }
          """, """
          import java.util.Objects;

          class A {
              Object foo(Object obj) {
                  return Objects.requireNonNull(obj);
              }
          }
          """));
    }

    @Test
    @ExpectedToFail("""
        Preconditions has both `checkNotNull(Object, Object)` and `checkNotNull(Object, String, Object...)`,
        meaning we can't exactly match only the `(Object, String)` case which Objects.requireNonNull supports.
      """)
    void preconditionsCheckNotNullToObjectsRequireNonNullTwoArguments() {
        //language=java
        rewriteRun(java("""
          import com.google.common.base.Preconditions;

          class A {
              Object foo(Object obj) {
                  return Preconditions.checkNotNull(obj, "foo");
              }
          }
          """, """
          import java.util.Objects;

          class A {
              Object foo(Object obj) {
                  return Objects.requireNonNull(obj, "foo");
              }
          }
          """));
    }

    @Test
    void preconditionsCheckNotNullToObjectsRequireNonNullStatic() {
        //language=java
        rewriteRun(java("""
          import static com.google.common.base.Preconditions.checkNotNull;

          class A {
              Object foo(Object obj) {
                  return checkNotNull(obj);
              }
          }
          """, """
          import static java.util.Objects.requireNonNull;

          class A {
              Object foo(Object obj) {
                  return requireNonNull(obj);
              }
          }
          """));
    }

    @Test
    void moreObjectsFirstNonNullToObjectsRequireNonNullElse() {
        //language=java
        rewriteRun((spec) -> {spec.recipes(Environment.builder()
            .scanRuntimeClasspath("org.openrewrite.java.migrate.guava")
            .build()
            .activateRecipes("org.openrewrite.java.migrate.guava.NoGuavaJava11"));
            },
          java("""
          import com.google.common.base.MoreObjects;

          class A {
              Object foo(Object obj) {
                  return MoreObjects.firstNonNull(obj, "default");
              }
          }
          """, """
          import java.util.Objects;

          class A {
              Object foo(Object obj) {
                  return Objects.requireNonNullElse(obj, "default");
              }
          }
          """));
    }
}
