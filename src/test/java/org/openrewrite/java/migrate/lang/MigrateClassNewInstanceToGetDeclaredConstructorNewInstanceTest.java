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
package org.openrewrite.java.migrate.lang;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.javaVersion;

@SuppressWarnings("deprecation")
class MigrateClassNewInstanceToGetDeclaredConstructorNewInstanceTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigrateClassNewInstanceToGetDeclaredConstructorNewInstance())
          .allSources(s -> s.markers(javaVersion(9)));
    }

    @Test
    void doesNotThrowExceptionOrThrowable() {
        //language=java
        rewriteRun(
          java(
            """
              package com.abc;

              class A {
                 public void test() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
                     Class<?> class_ = Class.forName("org.openrewrite.Test");
                     class_.newInstance();
                 }
              }
              """
          )
        );
    }

    @DocumentExample
    @Test
    void methodThrowsThrowable() {
        //language=java
        rewriteRun(
          java(
            """
              package com.abc;

              class A {
                 public void test() throws Throwable {
                     Class<?> class_ = Class.forName("org.openrewrite.Test");
                     class_.newInstance();
                 }
              }
              """,
            """
              package com.abc;

              class A {
                 public void test() throws Throwable {
                     Class<?> class_ = Class.forName("org.openrewrite.Test");
                     class_.getDeclaredConstructor().newInstance();
                 }
              }
              """
          )
        );
    }

    @Test
    void tryBlockCatchesException() {
        //language=java
        rewriteRun(
          java(
            """
              package com.abc;

              class A {
                  public void test() {
                      try {
                          Class<?> class_ = Class.forName("org.openrewrite.Test");
                          class_.newInstance();
                      } catch (Exception ex) {
                          System.out.println(ex.getMessage());
                      }
                  }
              }
              """,
            """
              package com.abc;

              class A {
                  public void test() {
                      try {
                          Class<?> class_ = Class.forName("org.openrewrite.Test");
                          class_.getDeclaredConstructor().newInstance();
                      } catch (Exception ex) {
                          System.out.println(ex.getMessage());
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void newInstanceInCatch() {
        //language=java
        rewriteRun(
          java(
            """
              package com.abc;

              class A {
                  public void test() throws IllegalAccessException, InstantiationException {
                      try {
                          System.out.println();
                      } catch (Exception ex) {
                          Class<?> class_ = Class.forName("org.openrewrite.Test");
                          class_.newInstance();
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void newInstanceInCatchMethodDeclarationThrowsException() {
        //language=java
        rewriteRun(
          java(
            """
              package com.abc;

              class A {
                  public void test() throws Exception {
                      try {
                          System.out.println();
                      } catch (Exception ex) {
                          Class<?> class_ = Class.forName("org.openrewrite.Test");
                          class_.newInstance();
                      }
                  }
              }
              """,
            """
              package com.abc;

              class A {
                  public void test() throws Exception {
                      try {
                          System.out.println();
                      } catch (Exception ex) {
                          Class<?> class_ = Class.forName("org.openrewrite.Test");
                          class_.getDeclaredConstructor().newInstance();
                      }
                  }
              }
              """
          )
        );
    }
}
