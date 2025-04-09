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
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class NoGuavaRefasterTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new NoGuavaRefasterRecipes())
          .parser(JavaParser.fromJavaVersion().classpath("rewrite-java", "guava"));
    }

    @DocumentExample
    @Test
    void preconditionsCheckNotNullToObjectsRequireNonNull() {
        rewriteRun(
          //language=java
          java(
            """
              import com.google.common.base.Preconditions;

              class A {
                  Object foo(Object obj) {
                      return Preconditions.checkNotNull(obj);
                  }
              }
              """,
            """
              import java.util.Objects;

              class A {
                  Object foo(Object obj) {
                      return Objects.requireNonNull(obj);
                  }
              }
              """
          )
        );
    }

    @Test
    void preconditionsCheckNotNullToObjectsRequireNonNullStringArgument() {
        rewriteRun(
          //language=java
          java(
            """
              import com.google.common.base.Preconditions;

              class A {
                  String foo(String str) {
                      return Preconditions.checkNotNull(str);
                  }
              }
              """,
            """
              import java.util.Objects;

              class A {
                  String foo(String str) {
                      return Objects.requireNonNull(str);
                  }
              }
              """
          )
        );
    }

    @Test
    void preconditionsCheckNotNullToObjectsRequireNonNullTwoArguments() {
        rewriteRun(
          //language=java
          java(
            """
              import com.google.common.base.Preconditions;

              class A {
                  Object foo(Object obj) {
                      return Preconditions.checkNotNull(obj, "foo");
                  }
              }
              """,
            """
              import java.util.Objects;

              class A {
                  Object foo(Object obj) {
                      return Objects.requireNonNull(obj, "foo");
                  }
              }
              """
          )
        );
    }

    @Test
    void preconditionsCheckNotNullToObjectsRequireNonNullTwoArgumentsSecondObject() {
        rewriteRun(
          //language=java
          java(
            """
              import com.google.common.base.Preconditions;

              class A {
                  Object foo(Object obj, StringBuilder description) {
                      return Preconditions.checkNotNull(obj, description);
                  }
              }
              """,
            """
              import java.util.Objects;

              class A {
                  Object foo(Object obj, StringBuilder description) {
                      return Objects.requireNonNull(obj, String.valueOf(description));
                  }
              }
              """
          )
        );
    }

    @Test
    void preconditionsCheckNotNullToObjectsRequireNonNullStatic() {
        rewriteRun(
          //language=java
          java(
            """
              import static com.google.common.base.Preconditions.checkNotNull;

              class A {
                  Object foo(Object obj) {
                      return checkNotNull(obj);
                  }
              }
              """,
            """
              import java.util.Objects;

              class A {
                  Object foo(Object obj) {
                      return Objects.requireNonNull(obj);
                  }
              }
              """
          )
        );
    }

    @Test
    void preconditionsCheckNotNullWithTemplateArgument() {
        // There's no direct replacement for this three arg lenient format variant
        rewriteRun(
          //language=java
          java(
            """
              import com.google.common.base.Preconditions;

              class A {
                  Object foo(Object obj) {
                      return Preconditions.checkNotNull(obj, "%s", "foo");
                  }
              }
              """
          )
        );
    }
}
