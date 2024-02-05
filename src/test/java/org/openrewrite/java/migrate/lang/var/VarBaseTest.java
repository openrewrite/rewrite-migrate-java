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
package org.openrewrite.java.migrate.lang.var;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.version;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

abstract class VarBaseTest implements RewriteTest {
    @Nested
    class GeneralNotApplicable {

        @Test
        void recordField() {
            //language=java
            rewriteRun(
              version(
                java("""
                  package com.example.app;

                  record A(int i, Object o) {
                  }
                  """
                ), 17)
            );
        }

        @Test
        void fieldsInInnerClass() {
            //language=java
            rewriteRun(
              java(
                    """
                package com.example.app;

                class A {
                  void m() {
                    class Inner {
                      final String str = "test";
                      final int i = 0;
                    }
                  }
                }
                """
              )
            );
        }

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
                      int i;
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
                    int i1, i2 = 1;
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
                      int i;
                      i = 1;
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

                import java.util.Date;

                class A {
                  void m() {
                      var str1 = "Hello World!";
                      var i = 1;
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
                      String o = true ? "isTrue" : "isFalse";
                      int i = true ? 1 : 0;
                  }
                }
                """
              )
            );
        }

        @Nested
        class Generics {
            @Test
            void inDefinition() {
                //language=java
                rewriteRun(
                  java(
                        """
                    package com.example.app;

                    import java.util.List;
                    import java.util.ArrayList;
                    
                    class A {
                      void m() {
                          List<Object> os = new ArrayList<>();
                      }
                    }
                    """
                  )
                );
            }

            @Test
            void inInitializer() {
                //language=java
                rewriteRun(
                  java(
                        """
                    package com.example.app;
                    
                    import java.util.ArrayList;
                    import java.util.List;

                    class A {
                      void m() {
                          List os = new ArrayList<String>();
                      }
                    }
                    """
                  )
                );
            }
        }
    }
}
