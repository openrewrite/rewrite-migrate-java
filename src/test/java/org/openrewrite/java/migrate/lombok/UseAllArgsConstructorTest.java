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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class UseAllArgsConstructorTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UseAllArgsConstructor());
    }

    @DocumentExample
    @Test
    void allNonStaticFields() {
        rewriteRun(
          //language=java
          java(
            """
              class A {
                  private final String name;
                  private int age;

                  public A(String name, int age) {
                      this.name = name;
                      this.age = age;
                  }
              }
              """,
            """
              import lombok.AllArgsConstructor;

              @AllArgsConstructor
              class A {
                  private final String name;
                  private int age;
              }
              """
          )
        );
    }

    @Test
    void allNonFinalFields() {
        rewriteRun(
          //language=java
          java(
            """
              class A {
                  private String name;
                  private int age;

                  public A(String name, int age) {
                      this.name = name;
                      this.age = age;
                  }
              }
              """,
            """
              import lombok.AllArgsConstructor;

              @AllArgsConstructor
              class A {
                  private String name;
                  private int age;
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
                  private String name;

                  protected A(String name) {
                      this.name = name;
                  }
              }
              """,
            """
              import lombok.AccessLevel;
              import lombok.AllArgsConstructor;

              @AllArgsConstructor(access = AccessLevel.PROTECTED)
              class A {
                  private String name;
              }
              """
          )
        );
    }

    @Test
    void staticFieldsExcluded() {
        rewriteRun(
          //language=java
          java(
            """
              class A {
                  private static final String DEFAULT = "x";
                  private String name;

                  public A(String name) {
                      this.name = name;
                  }
              }
              """,
            """
              import lombok.AllArgsConstructor;

              @AllArgsConstructor
              class A {
                  private static final String DEFAULT = "x";
                  private String name;
              }
              """
          )
        );
    }

    @Test
    void keepWhenAlreadyAnnotatedWithAllArgs() {
        rewriteRun(
          //language=java
          java(
            """
              import lombok.AllArgsConstructor;

              @AllArgsConstructor
              class A {
                  private String name;

                  public A(String name) {
                      this.name = name;
                  }
              }
              """
          )
        );
    }

    @Test
    void keepWhenAlreadyAnnotatedWithRequiredArgs() {
        rewriteRun(
          //language=java
          java(
            """
              import lombok.RequiredArgsConstructor;

              @RequiredArgsConstructor
              class A {
                  private final String name;

                  public A(String name) {
                      this.name = name;
                  }
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
                  private String name;

                  public A(String name) {
                      this.name = name;
                      System.out.println("created");
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
                  private String name;
                  private int age;

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
    void keepConstructorNotAssigningAllFields() {
        rewriteRun(
          //language=java
          java(
            """
              class A {
                  private String name;
                  private int age;

                  public A(String name) {
                      this.name = name;
                  }
              }
              """
          )
        );
    }
}
