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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

@Disabled("Not yet implemented")
public class UseVarForMethodInvocationsTest implements RewriteTest {
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UseVarForMethodInvocations())
          .allSources(s -> s.markers(javaVersion(10)));
    }

    @Nested
    class NotApplicable {
        @Test
        void forEmptyJDKFactoryMethod() {
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

        @Test
        void withEmptyOnwFactoryMethods() {
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
    }

    @Nested
    class Applicable {
        @Test
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
                ""","""
                package com.example.app;
                                    
                import java.util.List;
                                    
                class A {
                  static List<String> myList() {
                      return List.of("one", "two");
                  }
                  void m() {
                      var strs = myList();
                  }
                }
                """),
                10
              )
            );
        }

        @Test
        void withOnwFactoryMethods() {
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
