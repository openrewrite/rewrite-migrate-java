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
        void fieldsInInnerClass() {
            //language=java
            rewriteRun(
              version(
                java("""
                  package com.example.app;

                  class A {
                    void m() {
                      class Inner {
                        final String str = "test";
                      }
                    }
                  }
                  """),
                10
              )
            );
        }

        @Test
        void assignNull() {
            //language=java
            rewriteRun(
              version(
                java("""
                  package com.example.app;

                  class A {
                    void m() {
                      String str = null;
                    }
                  }
                  """),
                10
              )
            );
        }

        @Test
        void assignNothing() {
            //language=java
            rewriteRun(
              version(
                java("""
                  package com.example.app;

                  class A {
                    void m() {
                        String str;
                        int i;
                    }
                  }
                  """),
                10
              )
            );
        }

        @Test
        void multipleVariables() {
            //language=java
            rewriteRun(
              version(
                java("""
                  package com.example.app;

                  class A {
                    void m() {
                      String str1, str2 = "Hello World!";
                      int i1, i2 = 1;
                    }
                  }
                  """),
                10
              )
            );
        }

        @Test
        void simpleAssigment() {
            //language=java
            rewriteRun(
              version(
                java("""
                  package com.example.app;

                  class A {
                    void m() {
                        String str1;
                        str1 = "Hello World!";
                        int i;
                        i = 1;
                    }
                  }
                  """),
                10
              )
            );
        }

        @Test
        void varUsage() {
            //language=java
            rewriteRun(
              version(
                java("""
                  package com.example.app;

                  import java.util.Date;

                  class A {
                    void m() {
                        var str1 = "Hello World!";
                        var i = 1;
                    }
                  }
                  """),
                10
              )
            );
        }

        @Test
        void withTernary() {
            //language=java
            rewriteRun(
              version(
                java("""
                  package com.example.app;

                  class A {
                    void m() {
                        String o = "isTrue";
                        var i = 1;
                    }
                  }
                  """),
                10
              )
            );
        }

        @Nested
        class Generics {
            @Test
            void inDefinition() {
                //language=java
                rewriteRun(
                  version(
                    java("""
                      package com.example.app;

                      class A {
                        void m() {
                            List<Object> os = new List();
                        }
                      }
                      """),
                    10
                  )
                );
            }

            @Test
            void inInitializer() {
                //language=java
                rewriteRun(
                  version(
                    java("""
                      package com.example.app;

                      class A {
                        void m() {
                            List os = new List<Object>();
                        }
                      }
                      """),
                    10
                  )
                );
            }
        }
    }
}
