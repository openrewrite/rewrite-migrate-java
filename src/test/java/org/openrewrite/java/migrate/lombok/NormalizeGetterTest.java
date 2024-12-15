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
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class NormalizeGetterTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new NormalizeGetter());
    }

    @DocumentExample
    @Test
    void renameInSingleClass() {
        rewriteRun(// language=java
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
    void renamePrimitiveBooleanInSingleClass() {
        rewriteRun(// language=java
          java(
            """
              package com.yourorg.whatever;
              class A {
                  boolean foo;
                  boolean giveFoo() { return foo; }
              }
              """,
            """
              package com.yourorg.whatever;
              class A {
                  boolean foo;
                  boolean isFoo() { return foo; }
              }
              """
          )
        );
    }

    /**
     * Verifies that the correct method name is chosen for a boolean field with an 'is' prefix.
     * The corresponding method name in this case should not be `isIsFoo` but just `isFoo`.
     */
    @Test
    void renamePrimitiveBooleanWithPrefixInSingleClass() {
        rewriteRun(// language=java
          java(
            """
              package com.yourorg.whatever;
              class A {
                  boolean isFoo;
                  boolean giveFoo() { return isFoo; }
              }
              """,
            """
              package com.yourorg.whatever;
              class A {
                  boolean isFoo;
                  boolean isFoo() { return isFoo; }
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
                  Boolean Foo;
                  Boolean giveFoo() { return Foo; }
              }
              """,
            """
              package com.yourorg.whatever;
              class A {
                  Boolean Foo;
                  Boolean getFoo() { return Foo; }
              }
              """
          )
        );
    }

    /**
     * Verifies that the correct method name is chosen for a boolean field with an 'is' prefix.
     * The corresponding method name in this case should not be `isFoo` as in the primitive case,
     * but prefix a `get` as for any other field.
     */
    @Test
    void renameClassBooleanWithPrefixInSingleClass() {
        rewriteRun(// language=java
          java(
            """
              package com.yourorg.whatever;
              class A {
                  Boolean isFoo;
                  Boolean giveFoo() { return isFoo; }
              }
              """,
            """
              package com.yourorg.whatever;
              class A {
                  Boolean isFoo;
                  Boolean getIsFoo() { return isFoo; }
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
          ),// language=java
          java(
            """
              package com.yourorg.whatever;
              class B {
                  void useIt() {
                      var a = new A();
                      a.giveFoo();
                  }
              }
              """,
            """
              package com.yourorg.whatever;
              class B {
                  void useIt() {
                      var a = new A();
                      a.getFoo();
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
                  public long getTime() {
                      return foo;
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

                  public long getTime() {
                      return foo;
                  }
              }
              """,
            """
              class A {

                  private long foo;

                  public long getFoo() {
                      return foo;
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

                  public long getTime() {
                      return foo;
                  }
              }
              """,
            """
              class A {

                  private long foo;

                  public long getFoo() {
                      return foo;
                  }
              }
              """
          ),// language=java
          java(
            """
              class B extends A {

                  @Override
                  public long getTime() {
                      return 0;
                  }
              }
              """,
            """
              class B extends A {

                  @Override
                  public long getFoo() {
                      return 0;
                  }
              }
              """
          )
        );
    }


    //@Disabled("For now, if it happens, user has to manually resolve...")
    @Test
    void shouldNotRenameToExistingMethods() {
        rewriteRun(// language=java
          java(
            """
              package com.yourorg.whatever;

              class A {

                  private long foo;

                  public long getTime() {
                      return foo;
                  }

                  public long getFoo() {
                      return 8;
                  }
              }
              """
          )
        );
    }

    /**
     * If two methods are effectively the same getter then only one can be renamed.
     * Renaming both would result in a duplicate method definition, so we cannot do this.
     * Ideally the other effective getter would have their usages renamed but be themselves deleted...
     * TODO: create a second cleanup recipe that identifies redundant getters (isEffectiveGetter + field already has the getter annotation)
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

                  public long firstToBeRenamed() {
                      return foo;
                  }

                  public long secondToBeRenamed() {
                      return foo;
                  }
              }
              """,
            """
              package com.yourorg.whatever;

              class A {

                  private long foo;

                  public long getFoo() {
                      return foo;
                  }

                  public long secondToBeRenamed() {
                      return foo;
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

                      public long giveFoo() {
                          return foo;
                      }
                  }
              }
              """,
            """
              package com.yourorg.whatever;

              class A {

                  class B {

                      private long foo;

                      public long getFoo() {
                          return foo;
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

                      public long giveFoo() {
                          return foo;
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

                      public long getFoo() {
                          return foo;
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

                  public long giveFoo() {
                      return foo;
                  }

                  class B {

                      private long foo;

                      public long giveFoo() {
                          return foo;
                      }
                  }
              }
              """,
            """
              package com.yourorg.whatever;

              class A {

                  private long foo;

                  public long getFoo() {
                      return foo;
                  }

                  class B {

                      private long foo;

                      public long getFoo() {
                          return foo;
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

                  public long giveFoo() {
                      return foo;
                  }

                  class B {

                      private long ba;

                      public long giveBa() {
                          return ba;
                      }
                  }
              }
              """,
            """
              package com.yourorg.whatever;

              class A {

                  private long foo;

                  public long getFoo() {
                      return foo;
                  }

                  class B {

                      private long ba;

                      public long getBa() {
                          return ba;
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

                  public int getBar() {
                      return foo;
                  }

                  public int getFoo() {
                      return bar;
                  }

              }
              """,
            """
              package com.yourorg.whatever;

              class A {

                  int foo;

                  int bar;

                  public int getFoo() {
                      return foo;
                  }

                  public int getBar() {
                      return bar;
                  }

              }
              """
          )
        );
    }

}
