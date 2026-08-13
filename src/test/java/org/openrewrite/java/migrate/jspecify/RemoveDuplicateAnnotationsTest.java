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
import org.openrewrite.Issue;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

@Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/1199")
class RemoveDuplicateAnnotationsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipe(new RemoveDuplicateAnnotations("org.jspecify.annotations.*"))
          .parser(JavaParser.fromJavaVersion().classpath("jspecify"));
    }

    @DocumentExample
    @Test
    void removeDuplicateAnnotationOnMethodParameter() {
        rewriteRun(
          //language=java
          java(
            """
              import org.jspecify.annotations.Nullable;

              class Foo {
                  public void add(@Nullable @org.jspecify.annotations.Nullable final String bar) {
                  }
              }
              """,
            """
              import org.jspecify.annotations.Nullable;

              class Foo {
                  public void add(@Nullable final String bar) {
                  }
              }
              """
          )
        );
    }

    @Test
    void removeDuplicateAnnotationOnField() {
        rewriteRun(
          //language=java
          java(
            """
              import org.jspecify.annotations.Nullable;

              class Foo {
                  @Nullable
                  @Nullable
                  private String bar;
              }
              """,
            """
              import org.jspecify.annotations.Nullable;

              class Foo {
                  @Nullable
                  private String bar;
              }
              """
          )
        );
    }

    @Test
    void removeDuplicateAnnotationOnMethodReturnType() {
        rewriteRun(
          //language=java
          java(
            """
              import org.jspecify.annotations.Nullable;

              class Foo {
                  @Nullable
                  @Nullable
                  public String bar() {
                      return null;
                  }
              }
              """,
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
    void removeDuplicateAnnotationOnClass() {
        rewriteRun(
          //language=java
          java(
            """
              import org.jspecify.annotations.NullMarked;

              @NullMarked
              @NullMarked
              class Foo {
              }
              """,
            """
              import org.jspecify.annotations.NullMarked;

              @NullMarked
              class Foo {
              }
              """
          )
        );
    }

    @Test
    void removeDuplicateTypeUseAnnotation() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.List;
              import org.jspecify.annotations.Nullable;

              class Foo {
                  private List<@Nullable @Nullable String> bar;
              }
              """,
            """
              import java.util.List;
              import org.jspecify.annotations.Nullable;

              class Foo {
                  private List<@Nullable String> bar;
              }
              """
          )
        );
    }

    @Test
    void retainSingleAnnotation() {
        rewriteRun(
          //language=java
          java(
            """
              import org.jspecify.annotations.NonNull;
              import org.jspecify.annotations.Nullable;

              class Foo {
                  public @Nullable String bar(@NonNull String baz) {
                      return null;
                  }
              }
              """
          )
        );
    }

    @Test
    void retainDistinctAnnotations() {
        rewriteRun(
          //language=java
          java(
            """
              import org.jspecify.annotations.NonNull;
              import org.jspecify.annotations.Nullable;

              class Foo {
                  public void bar(@Nullable @NonNull String baz) {
                  }
              }
              """
          )
        );
    }

    @Test
    void retainAnnotationsOfOtherTypes() {
        rewriteRun(
          //language=java
          java(
            """
              class Foo {
                  @SuppressWarnings("all")
                  public void bar() {
                  }
              }
              """
          )
        );
    }
}
