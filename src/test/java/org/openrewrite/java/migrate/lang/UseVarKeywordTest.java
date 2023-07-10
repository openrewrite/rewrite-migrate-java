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

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.version;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.config.Environment;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

class UseVarKeywordTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(
          Environment.builder()
            .scanRuntimeClasspath("org.openrewrite.java.migrate.lang")
            .build()
            .activateRecipes("org.openrewrite.java.migrate.lang.UseVar"));
    }

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
                        }
                      }
                      """),
                10
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
            void reassignment() {
                //language=java
                rewriteRun(
                  version(
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
                      """),
                    10
                  )
                );
            }

            @Test
            @Disabled("this should be possible, but it needs very hard type inference")
            void withTernary() {
                //language=java
                rewriteRun(
                  version(
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
                      """),
                    10
                  )
                );
            }

            @Test
            void inStaticInitializer() {
                //language=java
                rewriteRun(
                  version(
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
                      """),
                    10
                  )
                );
            }

            @Test
            void inInstanceInitializer() {
                //language=java
                rewriteRun(
                  version(
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
                      """),
                    10
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
            void asField() {
                //language=java
                rewriteRun(
                  version(
                    java("""
                      package com.example.app;
                                        
                      class A {
                        Object o = new Object();
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
    }

    @Nested
    class Primitives {
        @Nested
        class NotApplicable {
            @Test
            void forShort() {
                //language=java
                rewriteRun(
                  version(
                    java("""
                      package com.example.app;
                                
                      class A {
                        void m() {
                            short mask = 0x7fff;
                        }
                      }
                      """),
                    10
                  )
                );
            }

            @Test
            void forByte() {
                //language=java
                rewriteRun(
                  version(
                    java("""
                      package com.example.app;
                                
                      class A {
                        void m() {
                            byte flags = 0;
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
            void forString() {
                //language=java
                rewriteRun(
                  version(
                    java("""
                      package com.example.app;
                                
                      class A {
                        void m() {
                            String str = "I am a value";
                        }
                      }
                      """, """
                      package com.example.app;
                                
                      class A {
                        void m() {
                            var str = "I am a value";
                        }
                      }
                      """),
                    10
                  )
                );
            }

            @Test
            void forBoolean() {
                //language=java
                rewriteRun(
                  version(
                    java("""
                      package com.example.app;
                                
                      class A {
                        void m() {
                            boolean b = true;
                        }
                      }
                      """, """
                      package com.example.app;
                                
                      class A {
                        void m() {
                            var b = true;
                        }
                      }
                      """),
                    10
                  )
                );
            }

            @Test
            void forChar() {
                //language=java
                rewriteRun(
                  version(
                    java("""
                      package com.example.app;
                                
                      class A {
                        void m() {
                            char ch = '\ufffd';
                        }
                      }
                      """, """
                      package com.example.app;
                                
                      class A {
                        void m() {
                            var ch = '\ufffd';
                        }
                      }
                      """),
                    10
                  )
                );
            }

            @Test
            void forDouble() {
                //language=java
                rewriteRun(
                  version(
                    java("""
                      package com.example.app;
                                
                      class A {
                        void m() {
                            double d = 2.0;
                        }
                      }
                      """, """
                      package com.example.app;
                                
                      class A {
                        void m() {
                            var d = 2.0;
                        }
                      }
                      """),
                    10
                  )
                );
            }

            @Test
            void forFloat() {
                //language=java
                rewriteRun(
                  version(
                    java("""
                      package com.example.app;
                                
                      class A {
                        void m() {
                            float f = 2.0;
                        }
                      }
                      """, """
                      package com.example.app;
                                
                      class A {                      
                        void m() {
                            var f = 2.0F;
                        }
                      }
                      """),
                    10
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
                      """, """
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
                  version(
                    java("""
                      package com.example.app;
                                
                      class A {
                        void m() {
                            double d = 2.0D;
                        }
                      }
                      """, """
                      package com.example.app;
                                
                      class A {
                        void m() {
                            var d = 2.0D;
                        }
                      }
                      """),
                    10
                  )
                );
            }

            @Test
            void forFloatWithTypNotation() {
                //language=java
                rewriteRun(
                  version(
                    java("""
                      package com.example.app;
                                
                      class A {
                        void m() {
                            float f = 2.0F;
                        }
                      }
                      """, """
                      package com.example.app;
                                
                      class A {
                        void m() {
                            var f = 2.0F;
                        }
                      }
                      """),
                    10
                  )
                );
            }

            @Test
            void forLongWithTypNotation() {
                //language=java
                rewriteRun(
                  version(
                    java("""
                      package com.example.app;
                                
                      class A {
                        void m() {
                            long l = 2L;
                        }
                      }
                      """, """
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
            void forEmptyDiamondOperators() {
                //language=java
                rewriteRun(
                  version(
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
                        """),
                    10
                  )
                );
            }
            @Test
            void withDiamondOperatorOnRaw() {
                //language=java
                rewriteRun(
                  version(
                    java("""
                  package com.example.app;
                                    
                  import java.util.ArrayList;
                                    
                  class A {
                    void m() {
                        List<String> strs = new ArrayList();
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
            void ifWelldefined() {
                //language=java
                rewriteRun(
                  version(
                    java("""
                  package com.example.app;
                                    
                  import java.util.ArrayList;
                                    
                  class A {
                    void m() {
                        List<String> strs = new ArrayList<String>();
                    }
                  }
                  ""","""
                  package com.example.app;
                                    
                  import java.util.ArrayList;
                                    
                  class A {
                    void m() {
                        var strs = new ArrayList<String>();
                    }
                  }
                  """),
                    10
                  )
                );
            }
            @Test
            void forNoDiamondOperators() {
                //language=java
                rewriteRun(
                  version(
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
                        ""","""
                          package com.example.app;
                          
                          import java.util.ArrayList;
                          import java.util.List;
                          
                          class A {
                            void m() {
                                var strs = new ArrayList();
                            }
                          }
                        """),
                    10
                  )
                );
            }
            @Test
            void withDiamondOperator() {
                //language=java
                rewriteRun(
                  version(
                    java("""
                  package com.example.app;
                  
                  import java.util.List;
                  import java.util.ArrayList;
                                    
                  class A {
                    void m() {
                        List<String> strs = new ArrayList<>();
                    }
                  }
                  ""","""
                  package com.example.app;
                  
                  import java.util.List;                 
                  import java.util.ArrayList;
                                    
                  class A {
                    void m() {
                        var strs = new ArrayList<String>();
                    }
                  }
                  """),
                    10
                  )
                );
            }

            @Test
            @Disabled("not yet implemented by UseVarForMethodInvocations") // todo mboegers in PR #249
            void withFactoryMethods() {
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
                      List<String> strs = List.of("one", "two");
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
