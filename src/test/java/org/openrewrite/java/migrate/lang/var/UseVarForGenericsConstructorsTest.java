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
import org.openrewrite.Example;
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.*;

class UseVarForGenericsConstructorsTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UseVarForGenericsConstructors())
          .allSources(s -> s.markers(javaVersion(10)));
    }

    @Nested
    class NotApplicable {

        @Test
        void boundedGenerics() {
            // could be var lst = new ArrayList<? extends String>();
            //language=java
            rewriteRun(
              version(
                java("""
                      package com.example.app;
                                  
                      import java.util.List;
                      import java.util.ArrayList;
                                        
                      class A {
                          void generic() {
                              List<? extends String> lst = new ArrayList<>();
                          }
                      }
                      """),
                10
              )
            );
        }
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
        void withFactoryMethods() {
            // this one is handled by UseVarForMethodInvocations
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
                      import java.util.ArrayList;
                      
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
        @Test
        void forNoDiamondOperators() {
            // this one fails for generics because it's covered by UseVarForObjects
            //language=java
            rewriteRun(
              version(
                java(
                  """
                      package com.example.app;
                      
                      import java.util.List;
                      import java.util.ArrayList;
                      
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
    }

    @Nested
    class Applicable {

        @Nested
        @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/257")
        class AdvancedGenerics {
            @Test
            void genericMethod() {
                //language=java
                rewriteRun(
                  version(
                    java("""
                  package com.example.app;
                              
                  import java.util.List;
                  import java.util.ArrayList;
                                    
                  class A {
                      static <T> void generic() {
                          List<T> lst = new ArrayList<>();
                      }
                  }
                  """, """
                  package com.example.app;

                  import java.util.ArrayList;
                                    
                  class A {
                      static <T> void generic() {
                          var lst = new ArrayList<T>();
                      }
                  }
                  """),
                10
              )
            );
        }

        @Test
        void unboundedGenerics() {
            //language=java
            rewriteRun(
              version(
                java("""
                  package com.example.app;
                              
                  import java.util.List;
                  import java.util.ArrayList;
                                    
                  class A {
                      void generic() {
                          List<?> lst = new ArrayList<>();
                      }
                  }
                  """, """
                  package com.example.app;
                                                
                  import java.util.ArrayList;
                                    
                  class A {
                      void generic() {
                          var lst = new ArrayList<?>();
                      }
                  }
                  """),
                10
              )
            );
        }

            @Test
            void boundedGenerics() {
                // could be var lst = new ArrayList<? extends String>();
                //language=java
                rewriteRun(
                  version(
                    java("""
                  package com.example.app;
                              
                  import java.util.List;
                  import java.util.ArrayList;
                                    
                  class A {
                      void generic() {
                          List<? extends String> lst = new ArrayList<>();
                      }
                  }
                  """),
                    10
                  )
                );
            }

            @Test
            void inceptionGenerics() {
                //language=java
                rewriteRun(
                  version(
                    java("""
                      package com.example.app;
                                  
                      import java.util.List;
                      import java.util.ArrayList;
                                        
                      class A {
                          void generic() {
                              List<List<Object>> lst = new ArrayList<>();
                          }
                      }
                      """, """
                      package com.example.app;

                      import java.util.List;
                      import java.util.ArrayList;
                                        
                      class A {
                          void generic() {
                              var lst = new ArrayList<List<Object>>();
                          }
                      }
                      """),
                    10
                  )
                );
            }

            @Test
            void twoParams() {
                //language=java
                rewriteRun(
                  version(
                    java("""
                  package com.example.app;
                              
                  import java.util.Map;
                  import java.util.HashMap;
                                    
                  class A {
                      void twoParams() {
                          Map<String, Object> map = new HashMap<>();
                      }
                  }
                  """, """
                  package com.example.app;

                  import java.util.HashMap;
                                    
                  class A {
                      void twoParams() {
                          var map = new HashMap<String, Object>();
                      }
                  }
                  """),
                    10
                  )
                );
            }
        }

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
                  """, """
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
        void arrayAsType() {
            //language=java
            rewriteRun(
              version(
                java("""
                  package com.example.app;
                              
                  import java.util.List;
                  import java.util.ArrayList;
                                    
                  class A {
                    void m() {
                        List<char[]> strs = new ArrayList<>();
                    }
                  }
                  """, """
                  package com.example.app;

                  import java.util.ArrayList;
                                    
                  class A {
                    void m() {
                        var strs = new ArrayList<char[]>();
                    }
                  }
                  """),
                10
              )
            );
        }

        @Test
        void twoParamsWithBounds() {
            //language=java
            rewriteRun(
              version(
                java("""
                  package com.example.app;
                              
                  import java.util.Map;
                  import java.util.LinkedHashMap;
                                    
                  class AbstractOAuth2Configurer {}
                                    
                  class A {
                      void twoParams() {
                          Map<Class<? extends AbstractOAuth2Configurer>, AbstractOAuth2Configurer> configurers = new LinkedHashMap<>();
                      }
                  }
                  """),
                10
              )
            );
        }

        @Test
        @Example
        void withTypeParameterInDefinitionOnly() {
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
                  """, """
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
    }
}
