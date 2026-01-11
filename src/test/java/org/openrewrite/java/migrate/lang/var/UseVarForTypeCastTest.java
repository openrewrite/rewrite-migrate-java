/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.java.migrate.lang.var;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.javaVersion;

class UseVarForTypeCastTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UseVarForTypeCast())
          .allSources(s -> s.markers(javaVersion(10)));
    }

    @DocumentExample
    @Test
    void simpleCast() {
        rewriteRun(
          java(
            """
              class A {
                  void m(Object obj) {
                      String s = (String) obj;
                  }
              }
              """,
            """
              class A {
                  void m(Object obj) {
                      var s = (String) obj;
                  }
              }
              """
          )
        );
    }

    @Test
    void castWithFinalModifier() {
        rewriteRun(
          java(
            """
              class A {
                  void m(Object obj) {
                      final String s = (String) obj;
                  }
              }
              """,
            """
              class A {
                  void m(Object obj) {
                      final var s = (String) obj;
                  }
              }
              """
          )
        );
    }

    @Test
    void castToFullyQualifiedType() {
        rewriteRun(
          java(
            """
              import java.util.List;

              class A {
                  void m(Object obj) {
                      List list = (List) obj;
                  }
              }
              """,
            """
              import java.util.List;

              class A {
                  void m(Object obj) {
                      var list = (List) obj;
                  }
              }
              """
          )
        );
    }

    @Test
    void primitiveCast() {
        rewriteRun(
          java(
            """
              class A {
                  void m(double d) {
                      int i = (int) d;
                  }
              }
              """,
            """
              class A {
                  void m(double d) {
                      var i = (int) d;
                  }
              }
              """
          )
        );
    }

    @Test
    void nullInitializerWithCast() {
        rewriteRun(
          java(
            """
              class A {
                  void m() {
                      String s = (String) null;
                  }
              }
              """,
            """
              class A {
                  void m() {
                      var s = (String) null;
                  }
              }
              """
          )
        );
    }

    @Nested
    class NoChange {
        @Test
        void fieldDeclaration() {
            rewriteRun(
              java(
                """
                  class A {
                      static Object obj = new Object();
                      String s = (String) obj;
                  }
                  """
              )
            );
        }

        @Test
        void castTypeDiffersFromDeclaredType() {
            rewriteRun(
              java(
                """
                  class A {
                      void m(Object obj) {
                          Object o = (String) obj;
                      }
                  }
                  """
              )
            );
        }

        @Test
        void alreadyUsesVar() {
            rewriteRun(
              java(
                """
                  class A {
                      void m(Object obj) {
                          var s = (String) obj;
                      }
                  }
                  """
              )
            );
        }

        @Test
        void notACast() {
            rewriteRun(
              java(
                """
                  class A {
                      void m() {
                          String s = "hello";
                      }
                  }
                  """
              )
            );
        }

        @Test
        void noInitializer() {
            rewriteRun(
              java(
                """
                  class A {
                      void m() {
                          String s;
                      }
                  }
                  """
              )
            );
        }

        @Test
        void nullInitializer() {
            rewriteRun(
              java(
                """
                  class A {
                      void m() {
                          String s = null;
                      }
                  }
                  """
              )
            );
        }

        @Test
        void multipleVariables() {
            rewriteRun(
              java(
                """
                  class A {
                      void m(Object obj1, Object obj2) {
                          String s1, s2 = (String) obj2;
                      }
                  }
                  """
              )
            );
        }

        @Test
        void java9NotSupported() {
            rewriteRun(
              spec -> spec.allSources(s -> s.markers(javaVersion(9))),
              java(
                """
                  class A {
                      void m(Object obj) {
                          String s = (String) obj;
                      }
                  }
                  """
              )
            );
        }
    }
}
