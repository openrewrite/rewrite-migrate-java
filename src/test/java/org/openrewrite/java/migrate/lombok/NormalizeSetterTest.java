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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.ChangeMethodName;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class NormalizeSetterTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new NormalizeSetter())
          .parser(JavaParser.fromJavaVersion().logCompilationWarningsAndErrors(true));
    }

    @DocumentExample

    @Test
    void applyDirectly() {//TODO remove again
        rewriteRun(
          spec -> spec
            .recipe(new ChangeMethodName("com.yourorg.whatever.A giveFoo()", "getFoo", null, null))
            .parser(JavaParser.fromJavaVersion().logCompilationWarningsAndErrors(true)),
          // language=java
          java(
            """
              package com.yourorg.whatever;
              class A {
                  int foo = 9;
                  int giveFoo() { return foo; }
              }
              """,
            """
              package com.yourorg.whatever;
              class A {
                  int foo = 9;
                  int getFoo() { return foo; }
              }
              """
          )
        );
    }

    @Test
    void applyDirectlyOnSetter() {//TODO remove again
        rewriteRun(
          spec -> spec
            .recipe(new ChangeMethodName("com.yourorg.whatever.A storeFoo(int)", "setFoo", null, null))
            .parser(JavaParser.fromJavaVersion().logCompilationWarningsAndErrors(true)),
          // language=java
          java(
            """
              package com.yourorg.whatever;
              class A {
                  int foo = 9;
                  public void storeFoo(int foo) {
                      this.foo = foo;
                  }
              }
              """,
            """
              package com.yourorg.whatever;
              class A {
                  int foo = 9;
                  public void setFoo(int foo) {
                      this.foo = foo;
                  }
              }
              """
          )
        );
    }



    @Test
    void renameInSingleClass() {
        rewriteRun(// language=java
          java(
            """
              package com.yourorg.whatever;
              class A {
                  int foo = 9;
                  public void storeFoo(int foo) {
                      this.foo = foo;
                  }
              }
              """,
            """
              package com.yourorg.whatever;
              class A {
                  int foo = 9;
                  public void setFoo(int foo) {
                      this.foo = foo;
                  }
              }
              """
          )
        );
    }

    @Test
    void renameInSingleClassWhitespace() {
        rewriteRun(// language=java
          java(
            """
              package com.yourorg.whatever;
              class A {
                  int foo = 9;
                  public void storeFoo( int  foo ) {
                      this .foo  =  foo;
                  }
              }
              """,
            """
              package com.yourorg.whatever;
              class A {
                  int foo = 9;
                  public void setFoo( int  foo ) {
                      this .foo  =  foo;
                  }
              }
              """
          )
        );
    }

    @Test
    void renamePrimitiveBooleanInSingleClass() {
        rewriteRun(// language=java
          java(
            """
              package com.yourorg.whatever;
              class A {
                  boolean foo;
                  void storeFoo(boolean foo) { this.foo = foo; }
              }
              """,
            """
              package com.yourorg.whatever;
              class A {
                  boolean foo;
                  void setFoo(boolean foo) { this.foo = foo; }
              }
              """
          )
        );
    }

    @Test
    void renameClassBooleanInSingleClass() {
        rewriteRun(// language=java
          java(
            """
              package com.yourorg.whatever;
              class A {
                  Boolean foo;
                  void storeFoo(Boolean foo) { this.foo = foo; }
              }
              """,
            """
              package com.yourorg.whatever;
              class A {
                  Boolean foo;
                  void setFoo(Boolean foo) { this.foo = foo; }
              }
              """
          )
        );
    }

    @Test
    void noBoxing1() {
        rewriteRun(// language=java
          java(
            """
              package com.yourorg.whatever;
              class A {
                  Boolean Foo;
                  void storeFoo(boolean foo) { this.foo = foo; }
              }
              """
          )
        );
    }

    @Test
    void noBoxing2() {
        rewriteRun(// language=java
          java(
            """
              package com.yourorg.whatever;
              class A {
                  boolean Foo;
                  void storeFoo(Boolean foo) { this.foo = foo; }
              }
              """
          )
        );
    }

    @Test
    void renameAcrossClasses() {
        rewriteRun(// language=java
          java(
            """
              package com.yourorg.whatever;
              class A {
                  int foo = 9;
                  void storeFoo(int foo) { this.foo = foo; }
              }
              """,
            """
              package com.yourorg.whatever;
              class A {
                  int foo = 9;
                  void setFoo(int foo) { this.foo = foo; }
              }
              """
          ),// language=java
          java(
            """
              package com.yourorg.whatever;
              class B {
                  void useIt() {
                      var a = new A();
                      a.storeFoo(4);
                  }
              }
              """,
            """
              package com.yourorg.whatever;
              class B {
                  void useIt() {
                      var a = new A();
                      a.setFoo(4);
                  }
              }
              """
          )
        );
    }

    @Test
    void shouldNotChangeOverridesOfExternalMethods() {
        rewriteRun(// language=java
          java(
            """
              package com.yourorg.whatever;

              import java.util.Date;

              class A extends Date {

                  private long foo;

                  @Override
                  public long setTime(long time) {
                      this.foo = time;
                  }
              }
              """
          )
        );
    }

    @Test
    void withoutPackage() {
        rewriteRun(// language=java
          java(
            """
              class A {

                  private long foo;

                  public void setTime(long foo) {
                      this.foo = foo;
                  }
              }
              """,
            """
              class A {

                  private long foo;

                  public void setFoo(long foo) {
                      this.foo = foo;
                  }
              }
              """
          )
        );
    }

    @Test
    void shouldChangeOverridesOfInternalMethods() {
        rewriteRun(// language=java
          java(
            """
              class A {

                  private long foo;

                  public void setTime(long foo) {
                      this.foo = foo;
                  }
              }
              """,
            """
              class A {

                  private long foo;

                  public void setFoo(long foo) {
                      this.foo = foo;
                  }
              }
              """
          ),// language=java
          java(
            """
              class B extends A {

                  @Override
                  public void setTime(long foo) {
                  }
              }
              """,
            """
              class B extends A {

                  @Override
                  public void setFoo(long foo) {
                  }
              }
              """
          )
        );
    }

    @Test
    void shouldNotRenameToExistingMethods() {
        rewriteRun(// language=java
          java(
            """
              package com.yourorg.whatever;

              class A {

                  private long foo;

                  public void setTime(long foo) {
                      this.foo = foo;
                  }

                  public void setFoo(long foo) {
                  }
              }
              """
          )
        );
    }

    /**
     * If two methods are effectively the same setter then only one can be renamed.
     * Renaming both would result in a duplicate method definition, so we cannot do this.
     * Ideally the other effective setter would have their usages renamed but be themselves deleted...
     * TODO: create a second cleanup recipe that identifies redundant Setters (isEffectiveSetter + field already has the setter annotation)
     *  and redirects their usage (ChangeMethodName with both flags true) and then deletes them.
     */
    @Test
    void shouldNotRenameTwoToTheSame() {
        rewriteRun(// language=java
          java(
            """
              package com.yourorg.whatever;

              class A {

                  private long foo;

                  public void firstToBeRenamed(long foo) {
                      this.foo = foo;
                  }

                  public void secondToBeRenamed(long foo) {
                      this.foo = foo;
                  }
              }
              """,
            """
              package com.yourorg.whatever;

              class A {

                  private long foo;

                  public void setFoo(long foo) {
                      this.foo = foo;
                  }

                  public void secondToBeRenamed(long foo) {
                      this.foo = foo;
                  }
              }
              """
          )
        );
    }

    /**
     * Methods in inner classes should be renamed as well.
     */
    @Test
    void shouldWorkOnInnerClasses() {
        rewriteRun(// language=java
          java(
            """
              package com.yourorg.whatever;

              class A {

                  class B {

                      private long foo;

                      public void storeFoo(long foo) {
                          this.foo = foo;
                      }
                  }
              }
              """,
            """
              package com.yourorg.whatever;

              class A {

                  class B {

                      private long foo;

                      public void setFoo(long foo) {
                          this.foo = foo;
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void shouldWorkOnInnerClasses2() {
        rewriteRun(// language=java
          java(
            """
              package com.yourorg.whatever;

              class A {

                  class B {

                  class C {

                      private long foo;

                      public void giveFoo(long foo) {
                          this.foo = foo;
                      }
                  }}
              }
              """,
            """
              package com.yourorg.whatever;

              class A {

                  class B {

                  class C {

                      private long foo;

                      public void setFoo(long foo) {
                          this.foo = foo;
                      }
                  }}
              }
              """
          )
        );
    }

    /**
     * Methods on top level should be renamed just as well when there is an inner class.
     */
    @Test
    void shouldWorkDespiteInnerClassesSameNameMethods() {
        rewriteRun(// language=java
          java(
            """
              package com.yourorg.whatever;

              class A {

                  private long foo;

                  public void storeFoo(long foo) {
                      this.foo = foo;
                  }

                  class B {

                      private long foo;

                      public void storeFoo(long foo) {
                          this.foo = foo;
                      }
                  }
              }
              """,
            """
              package com.yourorg.whatever;

              class A {

                  private long foo;

                  public void setFoo(long foo) {
                      this.foo = foo;
                  }

                  class B {

                      private long foo;

                      public void setFoo(long foo) {
                          this.foo = foo;
                      }
                  }
              }
              """
          )
        );
    }

    /**
     * Methods on top level should be renamed just as well when there is an inner class.
     */
    @Test
    void shouldWorkDespiteInnerClassesDifferentNameMethods() {
        rewriteRun(// language=java
          java(
            """
              package com.yourorg.whatever;

              class A {

                  private long foo;

                  public void storeFoo(long foo) {
                      this.foo = foo;
                  }

                  class B {

                      private long ba;

                      public void storeBa(long ba) {
                          this.ba = ba;
                      }
                  }
              }
              """,
            """
              package com.yourorg.whatever;

              class A {

                  private long foo;

                  public void setFoo(long foo) {
                      this.foo = foo;
                  }

                  class B {

                      private long ba;

                      public void setBa(long ba) {
                          this.ba = ba;
                      }
                  }
              }
              """
          )
        );
    }

    /**
     * If existing method names need to be rotated in a loop the recipe should still work.
     * For now this is not planned.
     */
    @Disabled("Not planned to fix but listed here for completeness")
    @Test
    void shouldWorkOnCircleCases() {
        rewriteRun(// language=java
          java(
            """
              package com.yourorg.whatever;

              class A {

                  int foo;

                  int bar;

                  public void setBar(long bar) {
                      this.foo = bar;
                  }

                  public void getFoo(long foo) {
                      this.bar = foo;
                  }

              }
              """,
            """
              package com.yourorg.whatever;

              class A {

                  int foo;

                  int bar;

                  public void getFoo(long foo) {
                      this.foo = foo;
                  }

                  public void setBar(long bar) {
                      this.bar = bar;
                  }

              }
              """
          )
        );
    }

}
