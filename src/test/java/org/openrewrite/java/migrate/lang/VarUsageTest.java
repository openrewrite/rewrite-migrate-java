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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.version;

class VarUsageTest implements RewriteTest {
    public void defaults(RecipeSpec spec) {
        spec.recipe(new VarKeyword())
          .typeValidationOptions(new TypeValidation().methodInvocations(false));
    }

    @Test
    void donotTransformNull() {
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

    @Nested
    class TransformObject {
        @Test
        void transformObjectInMethodBody() {
            //language=java
            rewriteRun(
              version(
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
                  """),
                10
              )
            );
        }

        @Test
        void donotTransformObjectInParameter() {
            //language=java
            rewriteRun(
              version(
                java("""
                  package com.example.app;
                                    
                  class A {
                    Object m(Object o) {
                        return o;
                    }
                  }
                  """),
                10
              )
            );
        }

        @Test
        void donotTransformObjectInClass() {
            //language=java
            rewriteRun(
              version(
                java("""
                  package com.example.app;
                                    
                  class A {
                    Object o;
                    Object m() {
                        return o;
                    }
                  }
                  """),
                10
              )
            );
        }
    }

    @Nested
    class Generics {
        @Test
        void transformFullGenerics() {
            //language=java
            rewriteRun(
              version(
                java("""
                  package com.example.app;
                                    
                  class A {
                    void m() {
                        List<String> strs = new ArrayList<String>();
                    }
                  }
                  """, """
                  package com.example.app;
                                    
                  class A {
                    void m() {
                      var str = new ArrayList<String>();
                    }
                  }
                  """),
                10
              )
            );
        }

        @Test
        void transformGenericsWithDiamond() {
            //language=java
            rewriteRun(
              version(
                java("""
                  package com.example.app;
                                    
                  class A {
                    void m() {
                        List<String> strs = new ArrayList<>();
                    }
                  }
                  """, """
                  package com.example.app;
                    
                  class A {
                    void m() {
                      var str = new ArrayList<String>();
                    }
                  }
                  """),
                10
              )
            );
        }

        @Test
        void transformGenericsOnFactory() {
            //language=java
            rewriteRun(
              version(
                java(
                  """
                    package com.example.app;
                    
                    import java.util.ArrayList;class A {
                      void m() {
                          List<String> strs = List.of("one","two");
                      }
                    }
                    """, """
                    package com.example.app;
                    
                    class A {
                      void m() {
                        var str = List.of("one","two");
                      }
                    }
                    """),
                10
              )
            );
        }

        @Test
        void donotTransformGenericsOnEmptyFactory() {
            //language=java
            rewriteRun(
              version(
                java(
                  """
                      package com.example.app;
                      
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
