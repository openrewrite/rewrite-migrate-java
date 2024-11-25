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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;

import static org.openrewrite.java.Assertions.*;

class UseVarForObjectsTest extends VarBaseTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UseVarForObject())
          .allSources(s -> s.markers(javaVersion(10)));
    }

    @Nested
    class Applicable {
        @DocumentExample
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
                  """, """
                  package com.example.app;

                  class A {
                    void m() {
                        var o = new Object();
                    }
                  }
                  """)
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
                  """, """
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

        @Test
        @DocumentExample
        void withModifier() {
            //language=java
            rewriteRun(
              java(
                """
                  class A {
                    void m() {
                        final Object o = new Object();
                    }
                  }
                  """, """
                  class A {
                    void m() {
                        final var o = new Object();
                    }
                  }
                  """
              )
            );
        }

        @Test
        @Disabled("this should be possible, but it needs very hard type inference")
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
                  """, """
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
                  """, """
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
                  """, """
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

        @Nested
        class InitilizedByMethod {
            @Test
            void sameType() {
                //language=java
                rewriteRun(
                  version(
                    java("""
                      package com.example.app;

                      class A {
                        String getHello() {
                            return "Hello";
                        }
                        void m() {
                            String phrase = getHello();
                        }
                      }
                      """, """
                      package com.example.app;

                      class A {
                        String getHello() {
                            return "Hello";
                        }
                        void m() {
                            var phrase = getHello();
                        }
                      }
                      """),
                    10
                  )
                );
            }

            @Test
            void subType() {
                //language=java
                rewriteRun(
                  version(
                    java("""
                      package com.example.app;

                      class A {
                        class CustomTokenizer extends java.util.StringTokenizer {
                            CustomTokenizer() {
                                super("");
                            }
                        }
                        CustomTokenizer getHello() {
                            return new CustomTokenizer();
                        }
                        void m() {
                            CustomTokenizer phrase = getHello();
                        }
                      }
                      """, """
                      package com.example.app;

                      class A {
                        class CustomTokenizer extends java.util.StringTokenizer {
                            CustomTokenizer() {
                                super("");
                            }
                        }
                        CustomTokenizer getHello() {
                            return new CustomTokenizer();
                        }
                        void m() {
                            var phrase = getHello();
                        }
                      }
                      """),
                    10
                  )
                );
            }

            @Test
            @Disabled("in favor to https://github.com/openrewrite/rewrite-migrate-java/issues/608 we skip all static methods ATM")
            void staticMethods() {
                //language=java
                rewriteRun(
                  version(
                    java("""
                      package com.example.app;

                      class A {
                        static class B {
                            private B() {}
                            static B newInstance() {
                                return new B();
                            }
                        }
                        void m() {
                            B b = B.newInstance();
                        }
                      }
                      """, """
                      package com.example.app;

                      class A {
                        static class B {
                            private B() {}
                            static B newInstance() {
                                return new B();
                            }
                        }
                        void m() {
                            var b = B.newInstance();
                        }
                      }
                      """),
                    10
                  )
                );
            }

            @Test
            @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/550")
            void genericType() {
                rewriteRun(
                  //language=java
                  java(
                    """
                      import java.io.Serializable;

                      abstract class Outer<T extends Serializable> {
                          abstract T doIt();
                          void trigger() {
                              T x = doIt();
                          }
                      }
                      """,
                    """
                      import java.io.Serializable;

                      abstract class Outer<T extends Serializable> {
                          abstract T doIt();
                          void trigger() {
                              var x = doIt();
                          }
                      }
                      """
                  )
                );
            }
        }
    }

    @Nested
    class NotApplicable {

        @Test
        @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/608")
        void genericTypeInStaticMethod() {
            // ATM the recipe skips all static method initialized variables
            rewriteRun(
              //language=java
              java(
                """
                  package example;

                  class Global {
                      static <T> T cast(Object o) {
                          return (T) o;
                      }
                  }
                  class User {
                      public String test() {
                          Object o = "Hello";
                          String string = Global.cast(o); // static method unchanged
                          return string;
                      }
                  }
                  """,
                """
                  package example;

                  class Global {
                      static <T> T cast(Object o) {
                          return (T) o;
                      }
                  }
                  class User {
                      public String test() {
                          var o = "Hello";
                          String string = Global.cast(o); // static method unchanged
                          return string;
                      }
                  }
                  """
              )
            );
        }

        @Test
        @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/551")
        void arrayInitializer() {
            //language=java
            rewriteRun(
              java(
                """
                  package com.example.app;

                  class A {
                    void m() {
                        String[] dictionary = {"aa", "b", "aba", "ba"};
                    }
                  }
                  """)
            );
        }

        @Test
        void fieldInAnonymousSubclass() {
            //language=java
            rewriteRun(
              java(
                """
                  class A {
                      void m() {
                          new Object() {
                              private final Object o1 = new Object();
                          };
                      }
                  }
                  """
              )
            );
        }

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
