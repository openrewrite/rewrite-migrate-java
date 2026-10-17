/*
 * Copyright 2026 the original author or authors.
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
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class RemoveAnnotationFromVoidMethodTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipe(new RemoveAnnotationFromVoidMethod("org.jetbrains.annotations.*ull*"))
          .parser(JavaParser.fromJavaVersion().classpath("annotations"));
    }

    @DocumentExample
    @Test
    void removeNullableFromVoidMethod() {
        rewriteRun(
          //language=java
          java(
            """
              import org.jetbrains.annotations.Nullable;

              class Foo {
                  @Nullable
                  public void bar() {
                  }
              }
              """,
            """
              class Foo {
                  public void bar() {
                  }
              }
              """
          )
        );
    }

    @Test
    void removeAllMatchingAnnotations() {
        rewriteRun(
          //language=java
          java(
            """
              import org.jetbrains.annotations.NotNull;
              import org.jetbrains.annotations.Nullable;

              class Foo {
                  @NotNull
                  @Nullable
                  public void bar() {
                  }
              }
              """,
            """
              class Foo {
                  public void bar() {
                  }
              }
              """
          )
        );
    }

    @Test
    void keepOtherAnnotations() {
        rewriteRun(
          //language=java
          java(
            """
              import org.jetbrains.annotations.Nullable;

              class Foo {
                  @Nullable
                  @Deprecated
                  public void bar() {
                  }

                  @Deprecated
                  @Nullable
                  public void baz() {
                  }
              }
              """,
            """
              class Foo {
                  @Deprecated
                  public void bar() {
                  }

                  @Deprecated
                  public void baz() {
                  }
              }
              """
          )
        );
    }

    @Test
    void voidMethodWithoutModifiers() {
        rewriteRun(
          //language=java
          java(
            """
              class Foo {
                  @org.jetbrains.annotations.Nullable
                  void bar() {
                  }

                  @org.jetbrains.annotations.Nullable
                  <T> void baz(T t) {
                  }
              }
              """,
            """
              class Foo {
                  void bar() {
                  }

                  <T> void baz(T t) {
                  }
              }
              """
          )
        );
    }

    @Test
    void removeAnnotationsFollowingModifiers() {
        rewriteRun(
          //language=java
          java(
            """
              import org.jetbrains.annotations.Nullable;

              class Foo {
                  public @Nullable void a() {
                  }

                  public @Nullable static void b() {
                  }

                  public static @Nullable <T> void c(T t) {
                  }

                  public @Deprecated @Nullable void d() {
                  }

                  public @Nullable @Deprecated void e() {
                  }
              }
              """,
            """
              class Foo {
                  public void a() {
                  }

                  public static void b() {
                  }

                  public static <T> void c(T t) {
                  }

                  public @Deprecated void d() {
                  }

                  public @Deprecated void e() {
                  }
              }
              """
          )
        );
    }

    @Test
    void keepAnnotationOnNonVoidMethod() {
        rewriteRun(
          //language=java
          java(
            """
              import org.jetbrains.annotations.Nullable;

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
    void keepAnnotationOnParameterOfVoidMethod() {
        rewriteRun(
          //language=java
          java(
            """
              import org.jetbrains.annotations.Nullable;

              class Foo {
                  public void bar(@Nullable String baz) {
                  }
              }
              """
          )
        );
    }

    @Test
    void keepAnnotationOnBoxedVoidMethod() {
        rewriteRun(
          //language=java
          java(
            """
              import org.jetbrains.annotations.Nullable;

              class Foo {
                  @Nullable
                  public Void bar() {
                      return null;
                  }
              }
              """
          )
        );
    }
}
