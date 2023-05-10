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

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.version;

class UseVarKeywordTest implements RewriteTest {
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UseVarKeyword());
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
        void compoundDeclaration() {
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
    }

    @Nested
    class Objects {
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
    }

    @Nested
    class Generics {
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
                      """, """
                      package com.example.app;
                                        
                      import java.util.ArrayList;
                                        
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
            void withDiamondOperator() {
                //language=java
                rewriteRun(
                  version(
                    java("""
                      package com.example.app;
                                        
                      import java.util.ArrayList;
                                        
                      class A {
                        void m() {
                            List<String> strs = new ArrayList<>();
                        }
                      }
                      """, """
                      package com.example.app;
                                        
                      import java.util.ArrayList;
                                        
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
            void withFactoryMethods() {
                //language=java
                rewriteRun(
                  version(
                    java(
                      """
                        package com.example.app;
                                            
                        import java.util.List;
                                            
                        class A {
                          void m() {
                              List<String> strs = List.of("one","two");
                          }
                        }
                        """, """
                        package com.example.app;
                                            
                        import java.util.List;
                                            
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
        }

        @Nested
        class NotApplicable {
            @Test
            void forEmptyFactoryMethode() {
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
}
