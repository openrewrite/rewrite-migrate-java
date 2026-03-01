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
package org.openrewrite.java.migrate.lombok;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class UseRequiredArgsConstructorTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UseRequiredArgsConstructor());
    }

    @DocumentExample
    @Test
    void singleFinalField() {
        rewriteRun(
          //language=java
          java(
            """
              class A {
                  private final int foo;

                  public A(int foo) {
                      this.foo = foo;
                  }
              }
              """,
            """
              import lombok.RequiredArgsConstructor;

              @RequiredArgsConstructor
              class A {
                  private final int foo;
              }
              """
          )
        );
    }

    @Test
    void multipleFinalFields() {
        rewriteRun(
          //language=java
          java(
            """
              class A {
                  private final String name;
                  private final int age;

                  public A(String name, int age) {
                      this.name = name;
                      this.age = age;
                  }
              }
              """,
            """
              import lombok.RequiredArgsConstructor;

              @RequiredArgsConstructor
              class A {
                  private final String name;
                  private final int age;
              }
              """
          )
        );
    }

    @Test
    void protectedAccessLevel() {
        rewriteRun(
          //language=java
          java(
            """
              class A {
                  private final int foo;

                  protected A(int foo) {
                      this.foo = foo;
                  }
              }
              """,
            """
              import lombok.AccessLevel;
              import lombok.RequiredArgsConstructor;

              @RequiredArgsConstructor(access = AccessLevel.PROTECTED)
              class A {
                  private final int foo;
              }
              """
          )
        );
    }

    @Test
    void privateAccessLevel() {
        rewriteRun(
          //language=java
          java(
            """
              class A {
                  private final int foo;

                  private A(int foo) {
                      this.foo = foo;
                  }
              }
              """,
            """
              import lombok.AccessLevel;
              import lombok.RequiredArgsConstructor;

              @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
              class A {
                  private final int foo;
              }
              """
          )
        );
    }

    @Test
    void packagePrivateAccessLevel() {
        rewriteRun(
          //language=java
          java(
            """
              class A {
                  private final int foo;

                  A(int foo) {
                      this.foo = foo;
                  }
              }
              """,
            """
              import lombok.AccessLevel;
              import lombok.RequiredArgsConstructor;

              @RequiredArgsConstructor(access = AccessLevel.PACKAGE)
              class A {
                  private final int foo;
              }
              """
          )
        );
    }

    @Test
    void assignmentWithoutThis() {
        rewriteRun(
          //language=java
          java(
            """
              class A {
                  private final int foo;

                  public A(int value) {
                      foo = value;
                  }
              }
              """,
            """
              import lombok.RequiredArgsConstructor;

              @RequiredArgsConstructor
              class A {
                  private final int foo;
              }
              """
          )
        );
    }

    @Test
    void staticFinalFieldExcluded() {
        rewriteRun(
          //language=java
          java(
            """
              class A {
                  private static final String DEFAULT = "x";
                  private final String name;

                  public A(String name) {
                      this.name = name;
                  }
              }
              """,
            """
              import lombok.RequiredArgsConstructor;

              @RequiredArgsConstructor
              class A {
                  private static final String DEFAULT = "x";
                  private final String name;
              }
              """
          )
        );
    }

    @Test
    void finalFieldWithInitializerExcluded() {
        rewriteRun(
          //language=java
          java(
            """
              class A {
                  private final String defaultName = "default";
                  private final String name;

                  public A(String name) {
                      this.name = name;
                  }
              }
              """,
            """
              import lombok.RequiredArgsConstructor;

              @RequiredArgsConstructor
              class A {
                  private final String defaultName = "default";
                  private final String name;
              }
              """
          )
        );
    }

    @Test
    void mixOfFinalAndNonFinalFields() {
        rewriteRun(
          //language=java
          java(
            """
              class A {
                  private final String name;
                  private int age;

                  public A(String name) {
                      this.name = name;
                  }
              }
              """,
            """
              import lombok.RequiredArgsConstructor;

              @RequiredArgsConstructor
              class A {
                  private final String name;
                  private int age;
              }
              """
          )
        );
    }

    @Test
    void keepConstructorWithExtraStatements() {
        rewriteRun(
          //language=java
          java(
            """
              class A {
                  private final int foo;

                  public A(int foo) {
                      this.foo = foo;
                      System.out.println("created");
                  }
              }
              """
          )
        );
    }

    @Test
    void keepConstructorWithValidation() {
        rewriteRun(
          //language=java
          java(
            """
              class A {
                  private final int foo;

                  public A(int foo) {
                      if (foo < 0) throw new IllegalArgumentException();
                      this.foo = foo;
                  }
              }
              """
          )
        );
    }

    @Test
    void keepConstructorWithWrongParameterOrder() {
        rewriteRun(
          //language=java
          java(
            """
              class A {
                  private final String name;
                  private final int age;

                  public A(int age, String name) {
                      this.name = name;
                      this.age = age;
                  }
              }
              """
          )
        );
    }

    @Test
    void keepWhenAlreadyAnnotated() {
        rewriteRun(
          //language=java
          java(
            """
              import lombok.RequiredArgsConstructor;

              @RequiredArgsConstructor
              class A {
                  private final int foo;

                  public A(int foo) {
                      this.foo = foo;
                  }
              }
              """
          )
        );
    }

    @Test
    void keepWhenNoFinalFields() {
        rewriteRun(
          //language=java
          java(
            """
              class A {
                  private int foo;

                  public A(int foo) {
                      this.foo = foo;
                  }
              }
              """
          )
        );
    }

    @Test
    void keepCompactConstructor() {
        rewriteRun(
          //language=java
          java(
            """
              public record Foo(String id) {
                  public Foo {
                      if (id == null || id.isBlank()) {
                          throw new IllegalArgumentException("ID cannot be null or blank");
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void keepConstructorWithExtraParameters() {
        rewriteRun(
          //language=java
          java(
            """
              class A {
                  private final int foo;

                  public A(int foo, int bar) {
                      this.foo = foo;
                  }
              }
              """
          )
        );
    }
}
