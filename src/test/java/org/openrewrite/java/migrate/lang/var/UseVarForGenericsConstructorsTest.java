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
import org.openrewrite.Issue;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.migrate.UseJavaUtilBase64;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.List;

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
            //language=java
            rewriteRun(
              java("""
                import java.util.List;
                import java.util.ArrayList;

                class A {
                    void generic() {
                        List<? extends String> lst = new ArrayList<>();
                    }
                }
                """
              )
            );
        }

        @Test
        void twoParamsWithBounds() {
            //language=java
            rewriteRun(
              java("""
                import java.util.Map;
                import java.util.LinkedHashMap;

                class AbstractOAuth2Configurer {}

                class A {
                    void twoParams() {
                        Map<Class<? extends AbstractOAuth2Configurer>, AbstractOAuth2Configurer> configurers = new LinkedHashMap<>();
                    }
                }
                """
              )
            );
        }

        @Test
        void forEmptyFactoryMethod() {
            //language=java
            rewriteRun(
              java(
                """
                  import java.util.List;

                  class A {
                    void m() {
                        List<String> strs = List.of();
                    }
                  }
                  """
              )
            );
        }

        @Test
        void withFactoryMethods() {
            // this one is handled by UseVarForMethodInvocations
            //language=java
            rewriteRun(
              java("""
                import java.util.List;

                class A {
                  void m() {
                      List<String> strs = List.of("one", "two");
                  }
                }
                """
              )
            );
        }

        @Test
        void forEmptyDiamondOperators() {
            //language=java
            rewriteRun(
              java(
                """
                  import java.util.List;
                  import java.util.ArrayList;

                  class A {
                    void m() {
                        List strs = new ArrayList<>();
                    }
                  }
                  """
              )
            );
        }

        @Test
        void withDiamondOperatorOnRaw() {
            //todo check if this may be possible!, We could transform ArrayList into ArrayList<String>
            //language=java
            rewriteRun(
              java("""
                import java.util.List;
                import java.util.ArrayList;

                class A {
                  void m() {
                      List<String> strs = new ArrayList();
                  }
                }
                """
              )
            );
        }

        @Test
        void forNoDiamondOperators() {
            // this one fails for generics because it's covered by UseVarForObjects
            //language=java
            rewriteRun(
              java(
                """
                  import java.util.List;
                  import java.util.ArrayList;

                  class A {
                    void m() {
                        List strs = new ArrayList();
                    }
                  }
                  """
              )
            );
        }
    }

    @Nested
    class Applicable {

        @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/257")
        @Nested
        class AdvancedGenerics {
            @Test
            void genericMethod() {
                //language=java
                rewriteRun(
                  java("""
                      import java.util.List;
                      import java.util.ArrayList;

                      class A {
                          static <T> void generic() {
                              List<T> lst = new ArrayList<>();
                          }
                      }
                      """,
                    """
                      import java.util.ArrayList;

                      class A {
                          static <T> void generic() {
                              var lst = new ArrayList<T>();
                          }
                      }
                      """
                  )
                );
            }

            @Test
            void unboundedGenerics() {
                //language=java
                rewriteRun(
                  java("""
                      import java.util.List;
                      import java.util.ArrayList;

                      class A {
                          void generic() {
                              List<?> lst = new ArrayList<>();
                          }
                      }
                      """,
                    """
                      import java.util.ArrayList;

                      class A {
                          void generic() {
                              var lst = new ArrayList<?>();
                          }
                      }
                      """
                  )
                );
            }

            @Test
            void inceptionGenerics() {
                //language=java
                rewriteRun(
                  java("""
                      import java.util.List;
                      import java.util.ArrayList;

                      class A {
                          void generic() {
                              List<List<Object>> lst = new ArrayList<>();
                          }
                      }
                      """,
                    """
                      import java.util.List;
                      import java.util.ArrayList;

                      class A {
                          void generic() {
                              var lst = new ArrayList<List<Object>>();
                          }
                      }
                      """
                  )
                );
            }

            @Test
            void twoParams() {
                //language=java
                rewriteRun(
                  java("""
                      import java.util.Map;
                      import java.util.HashMap;

                      class A {
                          void twoParams() {
                              Map<String, Object> map = new HashMap<>();
                          }
                      }
                      """,
                    """
                      import java.util.HashMap;

                      class A {
                          void twoParams() {
                              var map = new HashMap<String, Object>();
                          }
                      }
                      """
                  )
                );
            }
        }

        @DocumentExample
        @Test
        void withTypeParameterInDefinitionOnly() {
            //language=java
            rewriteRun(
              java("""
                  import java.util.List;
                  import java.util.ArrayList;

                  class A {
                      void m() {
                          List<String> strs = new ArrayList<>();
                      }
                  }
                  """,
                """
                  import java.util.ArrayList;

                  class A {
                      void m() {
                          var strs = new ArrayList<String>();
                      }
                  }
                  """
              )
            );
        }

        @Test
        void diamondOperatorIsNotUsed() {
            //language=java
            rewriteRun(
              java("""
                  import java.util.List;
                  import java.util.ArrayList;

                  class A {
                    void m() {
                        List<String> strs = new ArrayList<String>();
                    }
                  }
                  """,
                """
                  import java.util.ArrayList;

                  class A {
                    void m() {
                        var strs = new ArrayList<String>();
                    }
                  }
                  """
              )
            );
        }

        @Test
        void arrayAsType() {
            //language=java
            rewriteRun(
              java("""
                  import java.util.List;
                  import java.util.ArrayList;

                  class A {
                    void m() {
                        List<char[]> strs = new ArrayList<>();
                    }
                  }
                  """,
                """
                  import java.util.ArrayList;

                  class A {
                    void m() {
                        var strs = new ArrayList<char[]>();
                    }
                  }
                  """
              )
            );
        }

        @Test
        void ownObject() {
            //language=java
            rewriteRun(
              java("""
                package com.test;
                public class Option<T> {}
                """),
              java("""
                  import com.test.Option;
                  import java.util.HashSet;
                  import java.util.Set;

                  class A {
                      void m() {
                          Set<Option<Long>> ids = new HashSet<>();
                      }
                  }
                  """,
                """
                  import com.test.Option;
                  import java.util.HashSet;

                  class A {
                      void m() {
                          var ids = new HashSet<Option<Long>>();
                      }
                  }
                  """
              )
            );
        }
    }
}

