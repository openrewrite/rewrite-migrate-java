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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.version;

class UseVarForPrimitiveTest extends VarBaseTest {
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UseVarForPrimitive());
    }

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
        @DocumentExample
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
