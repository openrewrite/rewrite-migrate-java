package org.openrewrite.java.migrate.lang.var;

import static org.openrewrite.java.Assertions.*;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

public class UseVarForGenericsTest implements RewriteTest {
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UseVarForGenerics())
          .allSources(s -> s.markers(javaVersion(10)));
    }

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
            //todo check if this may be possible!, We could transform ArrayList into ArrayList<String>
            //language=java
            rewriteRun(
              version(
                java("""
                  package com.example.app;
                  
                  import java.util.List;
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
                              
                  import java.util.List;
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
            // this one fails for generics because it's covered by UseVarForObjects
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
                                    
                  import hava.util.List;
                  import java.util.ArrayList;
                                    
                  class A {
                    void m() {
                        List<String> strs = new ArrayList<>();
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
