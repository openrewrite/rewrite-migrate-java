package org.openrewrite.java.migrate.lang.var;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.version;

abstract class VarBaseTest implements RewriteTest {
    @Nested
    class GeneralNotApplicable {

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
                        String o = true ? "isTrue" : "Test";
                        var i = true ? 1 : 0;
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
