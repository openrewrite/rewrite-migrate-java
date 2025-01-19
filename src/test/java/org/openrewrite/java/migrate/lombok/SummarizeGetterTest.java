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

class SummarizeGetterTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new SummarizeGetter());
    }

    @DocumentExample
    @Test
    void replaceOneFieldGetter() {
        rewriteRun(// language=java
          java(
            """
              import lombok.Getter;

              class A {

                  @Getter
                  int foo;

              }
              """,
            """
              import lombok.Getter;

              @Getter
              class A {

                  int foo;

              }
              """
          )
        );
    }

    @Test
    void replaceOneFieldGetterWhenTheresUnrelatedVariableDeclarations() {
        rewriteRun(// language=java
          java(
            """
              import lombok.Getter;

              class A {

                  @Getter
                  int foo;

                  void bar() {
                      int x = 0;
                  }

              }
              """,
            """
              import lombok.Getter;

              @Getter
              class A {

                  int foo;

                  void bar() {
                      int x = 0;
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceOneFieldGetterWhenInFront() {
        rewriteRun(// language=java
          java(
            """
              import lombok.Getter;

              class A {

                  @Getter int foo = 9;

              }
              """,
            """
              import lombok.Getter;

              @Getter
              class A {

                  int foo = 9;

              }
              """
          )
        );
    }

    @Test
    void otherAnnotationAbove() {
        rewriteRun(// language=java
          java(
            """
              import lombok.Getter;
              import lombok.Setter;

              class A {

                  @Setter
                  @Getter
                  int foo;

              }
              """,
            """
              import lombok.Getter;
              import lombok.Setter;

              @Getter
              class A {

                  @Setter
                  int foo;

              }
              """
          )
        );
    }

    @Test
    void otherAnnotationBelow() {
        rewriteRun(// language=java
          java(
            """
              import lombok.Getter;
              import lombok.Setter;

              class A {

                  @Getter
                  @Setter
                  int foo;

              }
              """,
            """
              import lombok.Getter;
              import lombok.Setter;

              @Getter
              class A {

                  @Setter
                  int foo;

              }
              """
          )
        );
    }

    @Test
    void otherAnnotationsAround() {
        rewriteRun(// language=java
          java(
            """
              import lombok.Getter;
              import lombok.Setter;
              import lombok.Singular;

              class A {

                  @Singular
                  @Getter
                  @Setter
                  int foo;

              }
              """,
            """
              import lombok.Getter;
              import lombok.Setter;
              import lombok.Singular;

              @Getter
              class A {

                  @Singular
                  @Setter
                  int foo;

              }
              """
          )
        );
    }

    @Test
    void doNothingWhenNotEveryFieldIsAnnotated() {
        rewriteRun(// language=java
          java(
            """
              import lombok.Getter;

              class A {

                  @Getter
                  int foo;

                  int bar;

              }
              """
          )
        );
    }

    @Test
    void doNothingWhenAFieldHasSpecialConfig() {
        rewriteRun(// language=java
          java(
            """
              import lombok.AccessLevel;
              import lombok.Getter;

              class A {

                  @Getter
                  int foo;

                  @Getter(AccessLevel.PACKAGE)
                  int bar;

              }
              """
          )
        );
    }

    @Test
    void manyFields() {
        rewriteRun(// language=java
          java(
            """
              import lombok.AccessLevel;
              import lombok.Getter;

              class A {

                  @Getter
                  int foo;

                  @Getter
                  int bar;

                  @Getter
                  int foobar;

                  @Getter
                  int barfoo;

              }
              """,
            """
              import lombok.AccessLevel;
              import lombok.Getter;

              @Getter
              class A {

                  int foo;

                  int bar;

                  int foobar;

                  int barfoo;

              }
              """
          )
        );
    }



}
