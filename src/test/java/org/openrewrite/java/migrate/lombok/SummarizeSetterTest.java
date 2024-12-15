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
package org.openrewrite.java.migrate.lombok;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

// This is a test for the ConvertToNoArgsConstructor recipe, as an example of how to write a test for an imperative recipe.
class SummarizeSetterTest implements RewriteTest {

    // Note, you can define defaults for the RecipeSpec and these defaults will be used for all tests.
    // In this case, the recipe and the parser are common. See below, on how the defaults can be overridden
    // per test.
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new SummarizeSetter())
          .parser(JavaParser.fromJavaVersion().logCompilationWarningsAndErrors(true).classpath("lombok"));
    }

    @DocumentExample
    @Test
    void replaceOneFieldGetter() {
        rewriteRun(// language=java
          java(
            """
              import lombok.Setter;

              class A {

                  @Setter
                  int foo;

              }
              """,
            """
              import lombok.Setter;

              @Setter
              class A {

                  int foo;

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
              import lombok.Setter;

              class A {

                  @Setter int foo = 9;

              }
              """,
            """
              import lombok.Setter;

              @Setter
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

                  @Getter
                  @Setter
                  int foo;

              }
              """,
            """
              import lombok.Getter;
              import lombok.Setter;

              @Setter
              class A {

                  @Getter
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

                  @Setter
                  @Getter
                  int foo;

              }
              """,
            """
              import lombok.Getter;
              import lombok.Setter;

              @Setter
              class A {

                  @Getter
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
                  @Setter
                  @Getter
                  int foo;

              }
              """,
            """
              import lombok.Getter;
              import lombok.Setter;
              import lombok.Singular;

              @Setter
              class A {

                  @Singular
                  @Getter
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
              import lombok.Setter;

              class A {

                  @Setter
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
              import lombok.Setter;

              class A {

                  @Setter
                  int foo;

                  @Setter(AccessLevel.PACKAGE)
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
              import lombok.Setter;

              class A {

                  @Setter
                  int foo;

                  @Setter
                  int bar;

                  @Setter
                  int foobar;

                  @Setter
                  int barfoo;

              }
              """,
            """
              import lombok.AccessLevel;
              import lombok.Setter;

              @Setter
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

    /**
     * The occurrence of final fields does not stand in the way of a class level @Setter.
     * Lombok won't create Setters for them, but it does compile.
     */
    @Test
    void finalField() {
        rewriteRun(// language=java
          java(
            """
              import lombok.AccessLevel;
              import lombok.Setter;

              class A {

                  @Setter
                  int foo;

                  final int bar;

                  @Setter
                  final int foobar;

                  @Setter
                  int barfoo;

              }
              """,
            """
              import lombok.AccessLevel;
              import lombok.Setter;

              @Setter
              class A {

                  int foo;

                  final int bar;

                  final int foobar;

                  int barfoo;

              }
              """
          )
        );
    }

    /**
     * The occurrence of static fields does not stand in the way of a class level @Setter.
     * Lombok won't create Setters for them, but it does compile.
     */
    @Test
    void staticField() {
        rewriteRun(// language=java
          java(
            """
              import lombok.AccessLevel;
              import lombok.Setter;

              class A {

                  @Setter
                  int foo;

                  static int bar;

                  @Setter
                  static int foobar;

                  @Setter
                  int barfoo;

              }
              """,
            """
              import lombok.AccessLevel;
              import lombok.Setter;

              @Setter
              class A {

                  int foo;

                  static int bar;

                  static int foobar;

                  int barfoo;

              }
              """
          )
        );
    }

    /**
     * The occurrence of final static fields does not stand in the way of a class level @Setter.
     * Lombok won't create Setters for them, but it does compile.
     */
    @Test
    void staticFinalField() {
        rewriteRun(// language=java
          java(
            """
              import lombok.AccessLevel;
              import lombok.Setter;

              class A {

                  @Setter
                  int foo;

                  static final int bar;

                  @Setter
                  static final int foobar;

                  @Setter
                  int barfoo;

              }
              """,
            """
              import lombok.AccessLevel;
              import lombok.Setter;

              @Setter
              class A {

                  int foo;

                  static final int bar;

                  static final int foobar;

                  int barfoo;

              }
              """
          )
        );
    }


}
