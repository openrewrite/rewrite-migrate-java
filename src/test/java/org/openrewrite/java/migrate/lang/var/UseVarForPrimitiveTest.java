/*
 * Copyright 2024 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
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
import static org.openrewrite.java.Assertions.javaVersion;

class UseVarForPrimitiveTest extends VarBaseTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UseVarForPrimitive())
          .allSources(s -> s.markers(javaVersion(10)));
    }

    @Nested
    class NotApplicable {
        @Test
        void forShort() {
            //language=java
            rewriteRun(
              java(
                """
                  package com.example.app;

                  class A {
                    void m() {
                        short mask = 0x7fff;
                    }
                  }
                  """
              )
            );
        }

        @Test
        void forByte() {
            //language=java
            rewriteRun(
              java(
                """
                  package com.example.app;

                  class A {
                    void m() {
                        byte flags = 0;
                    }
                  }
                  """
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
              java(
                """
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
                  """
              )
            );
        }

        @Test
        @DocumentExample
        void forChar() {
            //language=java
            rewriteRun(
              java(
                """
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
                  """
              )
            );
        }

        @Test
        void forDouble() {
            //language=java
            rewriteRun(
              java(
                """
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
                  """
              )
            );
        }

        @Test
        void forFloat() {
            //language=java
            rewriteRun(
              java(
                """
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
                  """
              )
            );
        }

        @Test
        void forLong() {
            //language=java
            rewriteRun(
              java(
                """
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
                  """
              )
            );
        }

        @Test
        void forDoubleWithTypNotation() {
            //language=java
            rewriteRun(
              java(
                """
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
                  """
              )
            );
        }

        @Test
        void forFloatWithTypNotation() {
            //language=java
            rewriteRun(
              java(
                """
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
                  """
              )
            );
        }

        @Test
        void forLongWithTypNotation() {
            //language=java
            rewriteRun(
              java(
                """
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
                  """
              )
            );
        }

        @Test
        void withModifier() {
            //language=java
            rewriteRun(
              java(
                """
                  class A {
                    void m() {
                        final int i = 42;
                    }
                  }
                  """, """
                  class A {
                    void m() {
                        final var i = 42;
                    }
                  }
                  """
              )
            );
        }
    }
}
