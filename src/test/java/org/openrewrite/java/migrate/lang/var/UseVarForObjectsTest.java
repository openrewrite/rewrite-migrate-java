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
import org.openrewrite.test.RecipeSpec;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.javaVersion;

class UseVarForObjectsTest extends VarBaseTest {

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
              java("""
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
              java("""
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
        void withModifier() {
            //language=java
            rewriteRun(
              java("""
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
              java("""
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
              java("""
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
              java("""
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
    }

    @Nested
    class NotApplicable {
        @Test
        void fieldInAnonymousSubclass() {
            //language=java
            rewriteRun(
              java("""
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
              java("""
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
              java("""
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
