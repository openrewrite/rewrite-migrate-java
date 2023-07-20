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

import static org.openrewrite.java.Assertions.*;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

public class UseVarForGenericMethodInvocationsTest implements RewriteTest {
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UseVarForGenericMethodInvocations())
          .allSources(s -> s.markers(javaVersion(10)));
    }

    @Nested
    class NotApplicable {
        @Test
        void forNonGenericMethod() {
            // this one is handled/covered by UseVarForObjects/Test#staticMethods
            //language=java
            rewriteRun(
              version(
                java("""
                package com.example.app;
                                    
                class A {
                  static String myString(String ... values) {
                      return String.join("",values);
                  }
                  void m() {
                      String strs = myString();
                  }
                }
                """),
                10
              )
            );
        }

        @Nested
        class NotSupportedByOpenRewrite {
            // this is possible because `myList()`s type is fixed to `List<String>` but it is not distinguishable from
            // a generic method with generic var args like `List.of()`
            @Test
            void withStaticMethods() {
                //language=java
                rewriteRun(
                  version(
                    java("""
                package com.example.app;
                                    
                import java.util.List;
                                    
                class A {
                  static List<String> myList() {
                      return List.of("one", "two");
                  }
                  void m() {
                      List<String> strs = myList();
                  }
                }
                """),
                    10
                  )
                );
            }
            @Test
            void withEmptyOnwNonStaticFactoryMethods() {
                //if detectable this could be `var strs = this.<String>myList();`
                //language=java
                rewriteRun(
                  version(
                    java("""
                package com.example.app;
                                    
                import java.util.List;
                                    
                class A {
                  <T> List<T> myList(T ... values) {
                      return List.of(values);
                  }
                  void m() {
                      List<String> strs = myList();
                  }
                }
                """),
                    10
                  )
                );
            }
            @Test
            void withEmptyOnwFactoryMethods() {
                // if detectable this could be `var strs = A.<String>myList();`
                //language=java
                rewriteRun(
                  version(
                    java("""
                package com.example.app;
                                    
                import java.util.List;
                                    
                class A {
                  static <T> List<T> myList(T ... values) {
                      return List.of(values);
                  }
                  void m() {
                      List<String> strs = myList();
                  }
                }
                """),
                    10
                  )
                );
            }
            @Test
            void forEmptyJDKFactoryMethod() {
                // if detectable this could be `var strs = List.<String>of();`
                //language=java
                rewriteRun(
                  version(
                    java(
                      """
                          package com.example.app;
                          
                          import java.util.List;
                          
                          class A {
                            void m() {
                                List<String> strs = List.of();
                            }
                          }
                        """),
                    10
                  )
                );
            }
        }
    }

    @Nested
    class Applicable {
        @Test
        @DocumentExample
        void withJDKFactoryMethods() {
            //language=java
            rewriteRun(
              version(
                java("""
                package com.example.app;
                                    
                import java.util.List;
                                    
                class A {
                  void m() {
                      List<String> strs = List.of("one", "two");
                  }
                }
                ""","""
                package com.example.app;
                                    
                import java.util.List;
                                    
                class A {
                  void m() {
                      var strs = List.of("one", "two");
                  }
                }
                """),
                10
              )
            );
        }

        @Test
        void withJDKFactoryMethodsAndBounds() {
            //language=java
            rewriteRun(
              version(
                java("""
                package com.example.app;
                                    
                import java.util.List;
                                    
                class A {
                  void m() {
                      List<? extends String> lst = List.of("Test");
                  }
                }
                ""","""
                package com.example.app;
                                    
                import java.util.List;
                                    
                class A {
                  void m() {
                      var lst = List.of("Test");
                  }
                }
                """),
                10
              )
            );
        }

        @Test
        void withOwnFactoryMethods() {
            //language=java
            rewriteRun(
              version(
                java("""
                package com.example.app;
                                    
                import java.util.List;
                                    
                class A {
                  static <T> List<T> myList(T ... values) {
                      return List.of(values);
                  }
                  void m() {
                      List<String> strs = myList("one", "two");
                  }
                }
                ""","""
                package com.example.app;
                                    
                import java.util.List;
                                    
                class A {
                  static <T> List<T> myList(T ... values) {
                      return List.of(values);
                  }
                  void m() {
                      var strs = myList("one", "two");
                  }
                }
                """),
                10
              )
            );
        }
    }
}
