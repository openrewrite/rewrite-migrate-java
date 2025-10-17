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
package org.openrewrite.java.migrate.search.threadlocal;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class FindThreadLocalsMutableFromOutsideTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FindThreadLocalsMutableFromOutside());
    }

    @DocumentExample
    @Test
    void identifyNonPrivateThreadLocal() {
        rewriteRun(
          java(
            """
              class Example {
                  public static final ThreadLocal<String> PUBLIC_TL = new ThreadLocal<>();

                  public String getValue() {
                      return PUBLIC_TL.get();
                  }
              }
              """,
            """
              class Example {
                  /*~~(ThreadLocal is static non-private and can potentially be mutated from outside)~~>*/public static final ThreadLocal<String> PUBLIC_TL = new ThreadLocal<>();

                  public String getValue() {
                      return PUBLIC_TL.get();
                  }
              }
              """
          )
        );
    }

    @Test
    void identifyPackagePrivateThreadLocal() {
        rewriteRun(
          java(
            """
              class Example {
                  static final ThreadLocal<String> PACKAGE_TL = new ThreadLocal<>();

                  public String getValue() {
                      return PACKAGE_TL.get();
                  }
              }
              """,
            """
              class Example {
                  /*~~(ThreadLocal is static non-private and can potentially be mutated from outside)~~>*/static final ThreadLocal<String> PACKAGE_TL = new ThreadLocal<>();

                  public String getValue() {
                      return PACKAGE_TL.get();
                  }
              }
              """
          )
        );
    }

    @Test
    void identifyProtectedThreadLocal() {
        rewriteRun(
          java(
            """
              class Example {
                  protected static final ThreadLocal<String> PROTECTED_TL = new ThreadLocal<>();

                  public String getValue() {
                      return PROTECTED_TL.get();
                  }
              }
              """,
            """
              class Example {
                  /*~~(ThreadLocal is static non-private and can potentially be mutated from outside)~~>*/protected static final ThreadLocal<String> PROTECTED_TL = new ThreadLocal<>();

                  public String getValue() {
                      return PROTECTED_TL.get();
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotMarkPrivateThreadLocal() {
        rewriteRun(
          java(
            """
              class Example {
                  private static final ThreadLocal<String> PRIVATE_TL = new ThreadLocal<>();

                  public String getValue() {
                      return PRIVATE_TL.get();
                  }
              }
              """
          )
        );
    }

    @Test
    void identifyThreadLocalMutatedFromOutside() {
        rewriteRun(
          java(
            """
              package com.example;

              public class ThreadLocalHolder {
                  public static final ThreadLocal<String> SHARED_TL = new ThreadLocal<>();

                  public String getValue() {
                      return SHARED_TL.get();
                  }
              }
              """,
            """
              package com.example;

              public class ThreadLocalHolder {
                  /*~~(ThreadLocal is mutated from outside its defining class)~~>*/public static final ThreadLocal<String> SHARED_TL = new ThreadLocal<>();

                  public String getValue() {
                      return SHARED_TL.get();
                  }
              }
              """
          ),
          java(
            """
              package com.example;

              class ThreadLocalMutator {
                  public void mutate() {
                      ThreadLocalHolder.SHARED_TL.set("mutated");
                  }
              }
              """
          )
        );
    }

    @Test
    void identifyInstanceThreadLocalNonPrivate() {
        rewriteRun(
          java(
            """
              class Example {
                  public final ThreadLocal<String> instanceTL = new ThreadLocal<>();

                  public String getValue() {
                      return instanceTL.get();
                  }
              }
              """,
            """
              class Example {
                  /*~~(ThreadLocal is non-private and can potentially be mutated from outside)~~>*/public final ThreadLocal<String> instanceTL = new ThreadLocal<>();

                  public String getValue() {
                      return instanceTL.get();
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotMarkPrivateInstanceThreadLocal() {
        rewriteRun(
          java(
            """
              class Example {
                  private final ThreadLocal<String> instanceTL = new ThreadLocal<>();

                  public String getValue() {
                      return instanceTL.get();
                  }
              }
              """
          )
        );
    }

    @Test
    void identifyProtectedMutatedFromSubclass() {
        rewriteRun(
          java(
            """
              package com.example;

              public class BaseClass {
                  protected static final ThreadLocal<String> PROTECTED_TL = new ThreadLocal<>();

                  public String getValue() {
                      return PROTECTED_TL.get();
                  }
              }
              """,
            """
              package com.example;

              public class BaseClass {
                  /*~~(ThreadLocal is mutated from outside its defining class)~~>*/protected static final ThreadLocal<String> PROTECTED_TL = new ThreadLocal<>();

                  public String getValue() {
                      return PROTECTED_TL.get();
                  }
              }
              """
          ),
          java(
            """
              package com.example.sub;

              import com.example.BaseClass;

              public class SubClass extends BaseClass {
                  public void modifyThreadLocal() {
                      PROTECTED_TL.set("modified by subclass");
                  }
              }
              """
          )
        );
    }
}
