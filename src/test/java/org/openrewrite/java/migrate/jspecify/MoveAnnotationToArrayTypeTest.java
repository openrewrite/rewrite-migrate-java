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
package org.openrewrite.java.migrate.jspecify;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

@Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/934")
class MoveAnnotationToArrayTypeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipe(new MoveAnnotationToArrayType("org.jspecify.annotations.*"))
          .parser(JavaParser.fromJavaVersion().classpath("jspecify"));
    }

    @DocumentExample
    @Test
    void moveNullableToArrayReturnType() {
        rewriteRun(
          //language=java
          java(
            """
              import org.jspecify.annotations.Nullable;

              class Foo {
                  @Nullable
                  public byte[] bar() {
                      return null;
                  }
              }
              """,
            """
              import org.jspecify.annotations.Nullable;

              class Foo {
                  public byte @Nullable[] bar() {
                      return null;
                  }
              }
              """
          )
        );
    }

    @Test
    void moveNullableToArrayParameter() {
        rewriteRun(
          //language=java
          java(
            """
              import org.jspecify.annotations.Nullable;

              class Foo {
                  public void baz(@Nullable byte[] a) {
                  }
              }
              """,
            """
              import org.jspecify.annotations.Nullable;

              class Foo {
                  public void baz(byte @Nullable[] a) {
                  }
              }
              """
          )
        );
    }

    @Test
    void moveNullableToArrayField() {
        rewriteRun(
          //language=java
          java(
            """
              import org.jspecify.annotations.Nullable;

              class Foo {
                  @Nullable
                  public byte[] data;
              }
              """,
            """
              import org.jspecify.annotations.Nullable;

              class Foo {
                  public byte @Nullable[] data;
              }
              """
          )
        );
    }

    @Test
    void noChangeForNonArrayReturnType() {
        rewriteRun(
          //language=java
          java(
            """
              import org.jspecify.annotations.Nullable;

              class Foo {
                  @Nullable
                  public String bar() {
                      return null;
                  }
              }
              """
          )
        );
    }

    @Test
    void noChangeForNonArrayParameter() {
        rewriteRun(
          //language=java
          java(
            """
              import org.jspecify.annotations.Nullable;

              class Foo {
                  public void baz(@Nullable String a) {
                  }
              }
              """
          )
        );
    }

    @Test
    void multiDimensionalArray() {
        rewriteRun(
          //language=java
          java(
            """
              import org.jspecify.annotations.Nullable;

              class Foo {
                  @Nullable
                  public String[][] bar() {
                      return null;
                  }
              }
              """,
            """
              import org.jspecify.annotations.Nullable;

              class Foo {
                  public String @Nullable[][] bar() {
                      return null;
                  }
              }
              """
          )
        );
    }
}
