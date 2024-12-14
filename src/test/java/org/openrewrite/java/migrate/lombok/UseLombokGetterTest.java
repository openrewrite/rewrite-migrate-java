/*
 * Copyright 2024 the original author or authors.
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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class UseLombokGetterTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UseLombokGetter());
    }

    @DocumentExample
    @Test
    void replaceGetter() {
        rewriteRun(// language=java
          java(
            """
              class A {

                  int foo = 9;

                  public int getFoo() {
                      return foo;
                  }
              }
              """,
            """
              import lombok.Getter;

              class A {

                  @Getter
                  int foo = 9;
              }
              """
          )
        );
    }

    @Test
    void replaceGetterWithFieldAccess() {
        rewriteRun(// language=java
          java(
            """
              class A {

                  int foo = 9;

                  public int getFoo() {
                      return this.foo;
                  }
              }
              """,
            """
              import lombok.Getter;

              class A {

                  @Getter
                  int foo = 9;
              }
              """
          )
        );
    }

    @Test
    void replaceGetterWithMultiVariable() {
        // Technically this adds a new public getter not there previously, but we'll tolerate it
        rewriteRun(// language=java
          java(
            """
              class A {

                  int foo = 9, bar = 10;

                  public int getFoo() {
                      return foo;
                  }
              }
              """,
            """
              import lombok.Getter;

              class A {

                  @Getter
                  int foo = 9, bar = 10;
              }
              """
          )
        );
    }

    @Test
    void replacePackageGetter() {
        rewriteRun(// language=java
          java(
            """
              class A {

                  int foo = 9;

                  int getFoo() {
                      return foo;
                  }
              }
              """,
            """
              import lombok.AccessLevel;
              import lombok.Getter;

              class A {

                  @Getter(AccessLevel.PACKAGE)
                  int foo = 9;
              }
              """
          )
        );
    }

    @Test
    void replaceProtectedGetter() {
        rewriteRun(// language=java
          java(
            """
              class A {

                  int foo = 9;

                  protected int getFoo() {
                      return foo;
                  }
              }
              """,
            """
              import lombok.AccessLevel;
              import lombok.Getter;

              class A {

                  @Getter(AccessLevel.PROTECTED)
                  int foo = 9;
              }
              """
          )
        );
    }

    @Test
    void replacePrivateGetter() {
        rewriteRun(// language=java
          java(
            """
              class A {

                  int foo = 9;

                  private int getFoo() {
                      return foo;
                  }
              }
              """,
            """
              import lombok.AccessLevel;
              import lombok.Getter;

              class A {

                  @Getter(AccessLevel.PRIVATE)
                  int foo = 9;
              }
              """
          )
        );
    }

    @Test
    void replaceJustTheMatchingGetter() {
        rewriteRun(// language=java
          java(
            """
              class A {

                  int foo = 9;

                  int ba;

                  public A() {
                      ba = 1;
                  }

                  public int getFoo() {
                      return foo;
                  }

                  public int getMoo() {//method name wrong
                      return ba;
                  }
              }
              """,
            """
              import lombok.Getter;

              class A {

                  @Getter
                  int foo = 9;

                  int ba;

                  public A() {
                      ba = 1;
                  }

                  public int getMoo() {//method name wrong
                      return ba;
                  }
              }
              """
          )
        );
    }

    @Test
    void noChangeWhenMethodNameDoesntMatch() {
        rewriteRun(// language=java
          java(
            """
              class A {

                  int foo = 9;

                  public A() {
                  }

                  int getfoo() {//method name wrong
                      return foo;
                  }
              }
              """
          )
        );
    }

    @Test
    void noChangeWhenReturnTypeDoesntMatch() {
        rewriteRun(// language=java
          java(
            """
              class A {

                  int foo = 9;

                  public A() {
                  }

                  long getFoo() { //return type wrong
                      return foo;
                  }
              }
              """
          )
        );
    }

    @Test
    void noChangeWhenFieldIsNotReturned() {
        rewriteRun(// language=java
          java(
            """
              class A {

                  int foo = 9;
                  int ba = 10;

                  public A() {
                  }

                  int getFoo() {
                      return 5;//does not return variable
                  }
              }
              """
          )
        );
    }

    @Test
    void noChangeWhenDifferentFieldIsReturned() {
        rewriteRun(// language=java
          java(
            """
              class A {

                  int foo = 9;
                  int ba = 10;

                  public A() {
                  }

                  int getFoo() {
                      return ba;//returns wrong variable
                  }
              }
              """
          )
        );
    }

    @Test
    void noChangeWhenSideEffects1() {
        rewriteRun(// language=java
          java(
            """
              class A {

                  int foo = 9;
                  int ba = 10;

                  public A() {
                  }

                  int getfoo() {
                      foo++;//does extra stuff
                      return foo;
                  }
              }
              """
          )
        );
    }

    @Test
    void noChangeWhenSideEffects2() {
        rewriteRun(// language=java
          java(
            """
              class A {

                  int foo = 9;
                  int ba = 10;

                  public A() {
                  }

                  int getFoo() {
                      ba++;//does unrelated extra stuff
                      return foo;
                  }
              }
              """
          )
        );
    }

    @Test
    void replacePrimitiveBoolean() {
        rewriteRun(// language=java
          java(
            """
              class A {

                  boolean foo = true;

                  public boolean isFoo() {
                      return foo;
                  }
              }
              """,
            """
              import lombok.Getter;

              class A {

                  @Getter
                  boolean foo = true;
              }
              """
          )
        );
    }

    @Test
    void replacePrimitiveBooleanStartingWithIs() {
        rewriteRun(// language=java
          java(
            """
              class A {

                  boolean isFoo = true;

                  public boolean isFoo() {
                      return isFoo;
                  }
              }
              """,
            """
              import lombok.Getter;

              class A {

                  @Getter
                  boolean isFoo = true;
              }
              """
          )
        );
    }

    @Test
    void noChangeWhenPrimitiveBooleanUsesGet() {
        rewriteRun(// language=java
          java(
            """
              class A {

                  boolean foo = true;

                  boolean getFoo() {
                      return foo;
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceBoolean() {
        rewriteRun(// language=java
          java(
            """
              class A {

                  Boolean foo = true;

                  public Boolean getFoo() {
                      return foo;
                  }
              }
              """,
            """
              import lombok.Getter;

              class A {

                  @Getter
                  Boolean foo = true;
              }
              """
          )
        );
    }

    @Test
    void noChangeWhenBooleanUsesIs() {
        rewriteRun(// language=java
          java(
            """
              class A {

                  Boolean foo = true;

                  Boolean isFoo() {
                      return foo;
                  }
              }
              """
          )
        );
    }

    @Test
    void noChangeWhenBooleanUsesIs2() {
        rewriteRun(// language=java
          java(
            """
              class A {

                  Boolean isfoo = true;

                  Boolean isFoo() {
                      return isfoo;
                  }
              }
              """
          )
        );
    }

    @Test
    void noChangeNestedClassGetter() {
        rewriteRun(// language=java
          java(
            """
              class Outer {
                  int foo = 9;

                  class Inner {
                      public int getFoo() {
                          return foo;
                      }
                  }
              }
              """
          )
        );
    }
}
