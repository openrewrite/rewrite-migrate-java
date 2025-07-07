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
package org.openrewrite.java.migrate.lang;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.config.Environment;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.version;

class UseVarKeywordTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(
            Environment.builder()
              .scanRuntimeClasspath("org.openrewrite.java.migrate.lang")
              .build()
              .activateRecipes("org.openrewrite.java.migrate.lang.UseVar"))
          .allSources(sources -> version(sources, 10))
        ;
    }

    @Nested
    class GeneralNotApplicable {

        @Test
        void assignNull() {
            //language=java
            rewriteRun(
              java(
                """
                  package com.example.app;

                  class A {
                    void m() {
                      String str = null;
                    }
                  }
                  """
              )
            );
        }

        @Test
        void assignNothing() {
            //language=java
            rewriteRun(
              java(
                """
                  package com.example.app;

                  class A {
                    void m() {
                        String str;
                    }
                  }
                  """
              )
            );
        }

        @Test
        void multipleVariables() {
            //language=java
            rewriteRun(
              java(
                """
                  package com.example.app;

                  class A {
                    void m() {
                      String str1, str2 = "Hello World!";
                    }
                  }
                  """
              )
            );
        }

        @Test
        void simpleAssigment() {
            //language=java
            rewriteRun(
              java(
                """
                  package com.example.app;

                  class A {
                    void m() {
                        String str1;
                        str1 = "Hello World!";
                    }
                  }
                  """
              )
            );
        }

        @Test
        void varUsage() {
            //language=java
            rewriteRun(
              java(
                """
                  package com.example.app;

                  class A {
                    void m() {
                        var str1 = "Hello World!";
                    }
                  }
                  """
              )
            );
        }

        @Test
        void withTernary() {
            //language=java
            rewriteRun(
              java(
                """
                  package com.example.app;

                  class A {
                    void m() {
                        String o = true ? "isTrue" : "Test";
                    }
                  }
                  """
              )
            );
        }
    }

    @Nested
    class Objects {

        @Nested
        class Applicable {
            @Test
            void inMethodBody() {
                //language=java
                rewriteRun(
                  java(
                    """
                      package com.example.app;

                      class A {
                        void m() {
                            Object o = new Object();
                        }
                      }
                      """,
                    """
                      package com.example.app;

                      class A {
                        void m() {
                            var o = new Object();
                        }
                      }
                      """
                  )
                );
            }

            @Test
            void reassignment() {
                //language=java
                rewriteRun(
                  java(
                    """
                      package com.example.app;

                      class A {
                        Object o = new Object();
                        void m() {
                            Object innerO = o;
                        }
                      }
                      """,
                    """
                      package com.example.app;

                      class A {
                        Object o = new Object();
                        void m() {
                            var innerO = o;
                        }
                      }
                      """
                  )
                );
            }

            @Disabled("this should be possible, but it needs very hard type inference")
            @Test
            void withTernary() {
                //language=java
                rewriteRun(
                  java(
                    """
                      package com.example.app;

                      class A {
                        void m() {
                            String o = true ? "isTrue" : "Test";
                        }
                      }
                      """,
                    """
                      package com.example.app;

                      class A {
                        void m() {
                            var o = true ? "isTrue" : "Test";
                        }
                      }
                      """
                  )
                );
            }

            @Test
            void inStaticInitializer() {
                //language=java
                rewriteRun(
                  java(
                    """
                      package com.example.app;

                      class A {
                        static {
                            Object o = new Object();
                        }
                      }
                      """,
                    """
                      package com.example.app;

                      class A {
                        static {
                            var o = new Object();
                        }
                      }
                      """
                  )
                );
            }

            @Test
            void inInstanceInitializer() {
                //language=java
                rewriteRun(
                  java(
                    """
                      package com.example.app;

                      class A {
                        {
                            Object o = new Object();
                        }
                      }
                      """,
                    """
                      package com.example.app;

                      class A {
                        {
                            var o = new Object();
                        }
                      }
                      """
                  )
                );
            }
        }

        @Nested
        class NotApplicable {
            @Test
            void asParameter() {
                //language=java
                rewriteRun(
                  java(
                    """
                      package com.example.app;

                      class A {
                        Object m(Object o) {
                            return o;
                        }
                      }
                      """
                  )
                );
            }

            @Test
            void asField() {
                //language=java
                rewriteRun(
                  java(
                    """
                      package com.example.app;

                      class A {
                        Object o = new Object();
                        Object m() {
                            return o;
                        }
                      }
                      """
                  )
                );
            }
        }
    }

    @Nested
    class Primitives {
        @Nested
        class NotApplicable {
            @Test
            void forShort() {
                //language=java
                rewriteRun(
                  java(
                    """
                      package com.example.app;

                      class A {
                        void m() {
                            short mask = 0x7fff;
                        }
                      }
                      """
                  )
                );
            }

            @Test
            void forByte() {
                //language=java
                rewriteRun(
                  java(
                    """
                      package com.example.app;

                      class A {
                        void m() {
                            byte flags = 0;
                        }
                      }
                      """
                  )
                );
            }
        }

        @Nested
        class Applicable {
            @DocumentExample
            @Test
            void forString() {
                //language=java
                rewriteRun(
                  java(
                    """
                      package com.example.app;

                      class A {
                        void m() {
                            String str = "I am a value";
                        }
                      }
                      """,
                    """
                      package com.example.app;

                      class A {
                        void m() {
                            var str = "I am a value";
                        }
                      }
                      """
                  )
                );
            }

            @Test
            void forBoolean() {
                //language=java
                rewriteRun(
                  java(
                    """
                      package com.example.app;

                      class A {
                        void m() {
                            boolean b = true;
                        }
                      }
                      """,
                    """
                      package com.example.app;

                      class A {
                        void m() {
                            var b = true;
                        }
                      }
                      """
                  )
                );
            }

            @Test
            void forChar() {
                //language=java
                rewriteRun(
                  java(
                    """
                      package com.example.app;

                      class A {
                        void m() {
                            char ch = '\ufffd';
                        }
                      }
                      """,
                    """
                      package com.example.app;

                      class A {
                        void m() {
                            var ch = '\ufffd';
                        }
                      }
                      """
                  )
                );
            }

            @Test
            void forDouble() {
                //language=java
                rewriteRun(
                  java(
                    """
                      package com.example.app;

                      class A {
                        void m() {
                            double d = 2.0;
                        }
                      }
                      """,
                    """
                      package com.example.app;

                      class A {
                        void m() {
                            var d = 2.0;
                        }
                      }
                      """
                  )
                );
            }

            @Test
            void forFloat() {
                //language=java
                rewriteRun(
                  java(
                    """
                      package com.example.app;

                      class A {
                        void m() {
                            float f = 2.0F;
                        }
                      }
                      """,
                    """
                      package com.example.app;

                      class A {
                        void m() {
                            var f = 2.0F;
                        }
                      }
                      """
                  )
                );
            }

            @Test
            void forLong() {
                //language=java
                rewriteRun(
                  version(
                    java("""
                        package com.example.app;

                        class A {
                          void m() {
                              long l = 2;
                          }
                        }
                        """,
                      """
                        package com.example.app;

                        class A {
                          void m() {
                              var l = 2L;
                          }
                        }
                        """),
                    10
                  )
                );
            }

            @Test
            void forDoubleWithTypNotation() {
                //language=java
                rewriteRun(
                  java(
                    """
                      package com.example.app;

                      class A {
                        void m() {
                            double d = 2.0D;
                        }
                      }
                      """,
                    """
                      package com.example.app;

                      class A {
                        void m() {
                            var d = 2.0D;
                        }
                      }
                      """
                  )
                );
            }

            @Test
            void forFloatWithTypNotation() {
                //language=java
                rewriteRun(
                  java(
                    """
                      package com.example.app;

                      class A {
                        void m() {
                            float f = 2.0F;
                        }
                      }
                      """,
                    """
                      package com.example.app;

                      class A {
                        void m() {
                            var f = 2.0F;
                        }
                      }
                      """
                  )
                );
            }

            @Test
            void forLongWithTypNotation() {
                //language=java
                rewriteRun(
                  java(
                    """
                      package com.example.app;

                      class A {
                        void m() {
                            long l = 2L;
                        }
                      }
                      """,
                    """
                      package com.example.app;

                      class A {
                        void m() {
                            var l = 2L;
                        }
                      }
                      """
                  )
                );
            }
        }
    }

    @Nested
    class Generics {
        @Nested
        class NotApplicable {
            @Test
            void forEmptyFactoryMethod() {
                //language=java
                rewriteRun(
                  java(
                    """
                      package com.example.app;

                      import java.util.List;

                      class A {
                        void m() {
                            List<String> strs = List.of();
                        }
                      }
                      """
                  )
                );
            }

            @Test
            void forEmptyDiamondOperators() {
                //language=java
                rewriteRun(
                  java(
                    """
                      package com.example.app;

                      import java.util.ArrayList;
                      import java.util.List;

                      class A {
                        void m() {
                            List strs = new ArrayList<>();
                        }
                      }
                      """
                  )
                );
            }

            @Test
            void withDiamondOperatorOnRaw() {
                //language=java
                rewriteRun(
                  java(
                    """
                      package com.example.app;

                      import java.util.List;
                      import java.util.ArrayList;

                      class A {
                        void m() {
                            List<String> strs = new ArrayList();
                        }
                      }
                      """
                  )
                );
            }
        }

        @Nested
        class Applicable {

            @DocumentExample
            @Test
            void withDiamondOperator() {
                //language=java
                rewriteRun(
                  java(
                    """
                      package com.example.app;

                      import java.util.List;
                      import java.util.ArrayList;

                      class A {
                        void m() {
                            List<String> strs = new ArrayList<>();
                        }
                      }
                      """,
                    """
                      package com.example.app;

                      import java.util.ArrayList;

                      class A {
                        void m() {
                            var strs = new ArrayList<String>();
                        }
                      }
                      """
                  )
                );
            }
            @Test
            void ifWelldefined() {
                //language=java
                rewriteRun(
                  java(
                    """
                      package com.example.app;

                      import java.util.List;
                      import java.util.ArrayList;

                      class A {
                        void m() {
                            List<String> strs = new ArrayList<String>();
                        }
                      }
                      """,
                    """
                      package com.example.app;

                      import java.util.ArrayList;

                      class A {
                        void m() {
                            var strs = new ArrayList<String>();
                        }
                      }
                      """
                  )
                );
            }

            @Test
            void forNoDiamondOperators() {
                //language=java
                rewriteRun(
                  java(
                    """
                      package com.example.app;

                      import java.util.ArrayList;
                      import java.util.List;

                      class A {
                        void m() {
                            List strs = new ArrayList();
                        }
                      }
                      """,
                    """
                      package com.example.app;

                      import java.util.ArrayList;

                      class A {
                        void m() {
                            var strs = new ArrayList();
                        }
                      }
                      """
                  )
                );
            }

            @Test
            void withFactoryMethods() {
                //language=java
                rewriteRun(
                  java(
                    """
                      package com.example.app;

                      import java.util.List;

                      class A {
                        void m() {
                            List<String> strs = List.of("one", "two");
                        }
                      }
                      """,
                    """
                      package com.example.app;

                      import java.util.List;

                      class A {
                        void m() {
                            var strs = List.of("one", "two");
                        }
                      }
                      """
                  )
                );
            }
        }
    }
}
