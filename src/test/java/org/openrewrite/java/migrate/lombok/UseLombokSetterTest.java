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
package org.openrewrite.java.migrate.lombok;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

// This is a test for the ConvertToNoArgsConstructor recipe, as an example of how to write a test for an imperative recipe.
class UseLombokSetterTest implements RewriteTest {

    // Note, you can define defaults for the RecipeSpec and these defaults will be used for all tests.
    // In this case, the recipe and the parser are common. See below, on how the defaults can be overridden
    // per test.
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UseLombokSetter())
          .parser(JavaParser.fromJavaVersion()
            .logCompilationWarningsAndErrors(true));
    }

    int foo;

    @DocumentExample
    @Test
    void replaceSetter() {
        rewriteRun(// language=java
          java(
            """
              class A {

                  int foo = 9;

                  public void setFoo(int foo) {
                      this.foo = foo;
                  }
              }
              """,
            """
              import lombok.Setter;

              class A {

                  @Setter
                  int foo = 9;
              }
              """
          )
        );
    }

    @Test
    void replaceSetterWhenArgNameWithUnderscore() {
        rewriteRun(// language=java
          java(
            """
              class A {

                  int foo = 9;

                  public void setFoo(int foo_) {
                      this.foo = foo_;
                  }
              }
              """,
            """
              import lombok.Setter;

              class A {

                  @Setter
                  int foo = 9;
              }
              """
          )
        );
    }

    @Test
    void replaceSetterWhenArgNameWithUnderscoreAndUnqualifiedFieldAccess() {
        rewriteRun(// language=java
          java(
            """
              class A {

                  int foo = 9;

                  public void setFoo(int foo_) {
                      foo = foo_;
                  }
              }
              """,
            """
              import lombok.Setter;

              class A {

                  @Setter
                  int foo = 9;
              }
              """
          )
        );
    }

    @Test
    void tolerantToNonstandardParameterNames() {
        rewriteRun(// language=java
          java(
            """
              class A {

                  int foo = 9;

                  public void setFoo(int fub) {
                      this.foo = fub;
                  }
              }
              """,
            """
              import lombok.Setter;

              class A {

                  @Setter
                  int foo = 9;
              }
              """
          )
        );
    }

    @Test
    void replacePackageSetter() {
        rewriteRun(// language=java
          java(
            """
              class A {

                  int foo = 9;

                  void setFoo(int foo) {
                      this.foo = foo;
                  }
              }
              """,
            """
              import lombok.AccessLevel;
              import lombok.Setter;

              class A {

                  @Setter(AccessLevel.PACKAGE)
                  int foo = 9;
              }
              """
          )
        );
    }

    @Test
    void replaceProtectedSetter() {
        rewriteRun(// language=java
          java(
            """
              class A {

                  int foo = 9;

                  protected void setFoo(int foo) {
                      this.foo = foo;
                  }
              }
              """,
            """
              import lombok.AccessLevel;
              import lombok.Setter;

              class A {

                  @Setter(AccessLevel.PROTECTED)
                  int foo = 9;
              }
              """
          )
        );
    }

    @Test
    void replacePrivateSetter() {
        rewriteRun(// language=java
          java(
            """
              class A {

                  int foo = 9;

                  private void setFoo(int foo) {
                      this.foo = foo;
                  }
              }
              """,
            """
              import lombok.AccessLevel;
              import lombok.Setter;

              class A {

                  @Setter(AccessLevel.PRIVATE)
                  int foo = 9;
              }
              """
          )
        );
    }

	@Test
	void replacePrivateSetterAnnotated() {
		rewriteRun(// language=java
		  java(
			"""
			  class A {

				  int foo = 9;

				  @Deprecated
				  private void setFoo(int foo) {
					  this.foo = foo;
				  }
			  }
			  """,
			"""
              import lombok.AccessLevel;
              import lombok.Setter;

              class A {

                  @Setter(value = AccessLevel.PRIVATE, onMethod_ = {@Deprecated})
                  int foo = 9;
              }
              """
		  )
		);
	}

    @Test
    void replaceJustTheMatchingSetter() {
        rewriteRun(// language=java
          java(
            """
              class A {

                  int foo = 9;

                  int ba;

                  public A() {
                      ba = 1;
                  }

                  public void setFoo(int foo) {
                      this.foo = foo;
                  }

                  public void setMoo(int foo) {//method name wrong
                      this.foo = foo;
                  }
              }
              """,
            """
              import lombok.Setter;

              class A {

                  @Setter
                  int foo = 9;

                  int ba;

                  public A() {
                      ba = 1;
                  }

                  public void setMoo(int foo) {//method name wrong
                      this.foo = foo;
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

                  public void setfoo(int foo) {//method name wrong
                      this.foo = foo;
                  }
              }
              """
          )
        );
    }

    @Test
    void noChangeWhenParameterTypeDoesntMatch() {
        rewriteRun(// language=java
          java(
            """
              class A {

                  int foo = 9;

                  public A() {
                  }

                  public void setFoo(long foo) {//parameter type wrong
                      this.foo = (int) foo;
                  }
              }
              """
          )
        );
    }

    @Test
    void noChangeWhenFieldIsNotAssigned() {
        rewriteRun(// language=java
          java(
            """
              class A {

                  int foo = 9;
                  int ba = 10;

                  public A() {
                  }

                  public void setFoo(int foo) {
                      int foo = foo;
                  }
              }
              """
          )
        );
    }

    @Test
    void noChangeWhenDifferentFieldIsAssigned() {
        rewriteRun(// language=java
          java(
            """
              class A {

                  int foo = 9;
                  int ba = 10;

                  public A() {
                  }

                  public void setFoo(int foo) {
                      this.ba = foo; //assigns wrong variable
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

                  public void setFoo(int foo) {
                      foo++;//does extra stuff
                      this.foo = foo;
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

                  public void setFoo(boolean foo) {
                      this.foo =  foo;
                  }
              }
              """,
            """
              import lombok.Setter;

              class A {

                  @Setter
                  boolean foo = true;
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

                  public void setFoo(Boolean foo) {
                      this.foo =  foo;
                  }
              }
              """,
            """
              import lombok.Setter;

              class A {

                  @Setter
                  Boolean foo = true;
              }
              """
          )
        );
    }

    @Test
    void annotateOnlyFields() {
        rewriteRun(// language=java
          java(
            """
              class A {

                  int foo = 9;

                  public void setFoo(int foo) {
                      this.foo = foo;
                  }

                  public void unrelated1(int foo) {
                  }
                  public void unrelated2() {
                      int foo = 10;
                  }
              }
              """,
            """
              import lombok.Setter;

              class A {

                  @Setter
                  int foo = 9;

                  public void unrelated1(int foo) {
                  }
                  public void unrelated2() {
                      int foo = 10;
                  }
              }
              """
          )
        );
    }

    @Test
    void annotateOnlyFields2() {
        rewriteRun(// language=java
          java(
            """
              class A {

                  public void setFoo(int foo) {
                      this.foo = foo;
                  }

                  public void unrelated1(int foo) {
                  }
                  public void unrelated2() {
                      int foo = 10;
                  }

                  int foo = 9;
              }
              """,
            """
              import lombok.Setter;

              class A {

                  public void unrelated1(int foo) {
                  }
                  public void unrelated2() {
                      int foo = 10;
                  }

                  @Setter
                  int foo = 9;
              }
              """
          )
        );
    }

    @Test
    void noChangeNestedClassSetter() {
        rewriteRun(// language=java
          java(
            """
              class Outer {
                  int foo = 9;

                  class Inner {
                      public void setFoo(int foo) {
                          Outer.this.foo = foo;
                      }
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/5015")
    @Test
    void addOnMethodArgIfAnnotated() {
        rewriteRun(// language=java
          java(
            """
              class A {

                  int foo = 9;

                  @Deprecated
                  @SuppressWarnings("deprecation")
                  public void setFoo(int fub) {
                      this.foo = fub;
                  }
              }
              """,
			"""
              import lombok.Setter;

              class A {

                  @Setter(onMethod_ = {@Deprecated, @SuppressWarnings("deprecation")})
                  int foo = 9;
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/5015")
    @Test
    void replaceSetterIfOverwrite() {
        rewriteRun(// language=java
          java(
            """
              class A {

                  int foo = 9;

                  @Override
                  public void setFoo(int foo) {
                      this.foo = foo;
                  }
              }
              """,
            """
              import lombok.Setter;

              class A {

                  @Setter
                  int foo = 9;
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/870")
    @Test
    void noChangeWhenInstanceMethodAccessesStaticField() {
        rewriteRun(// language=java
          java(
            """
              class A {
                  static int field;
                  void setField(int field) {
                      A.field = field;
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/870")
    @Test
    void noChangeWhenInstanceMethodAccessesStaticFieldWithThis() {
        rewriteRun(// language=java
          java(
            """
              class A {
                  static int field;
                  void setField(int field) {
                      this.field = field;
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/872")
    @Test
    void noChangeWhenMethodSetsFieldOfAnotherObject() {
        rewriteRun(// language=java
          java(
            """
              public class A {

                  private class Sample {
                      public Long number;
                  }

                  private class Inner {
                      static Sample sample = null;

                      private void setNumber(Long value) {
                          sample.number = value;
                      }
                  }
              }
              """
          )
        );
    }
}
