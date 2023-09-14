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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.Example;
import org.openrewrite.config.Environment;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.version;

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
    class BugFixing {

        @Test
        void anonymousClass() {
            // spring-projects/spring-data-commons @ main: src/test/java/org/springframework/data/domain/ManagedTypesUnitTests.java
            // solving: Expected a template that would generate exactly one statement to replace one statement, but generated 2. Template:
            //var typesSupplier = __P__.<org.springframework.data.domain.ManagedTypesUnitTests.1>/*__p1__*/p()
            //language=java
            rewriteRun(
              version(
                java("""
                      package com.example.app;
                                    
                      import java.util.Collections;
                      import java.util.function.Supplier;
                                    
                      class ManagedTypesUnitTests {
                          void supplierBasedManagedTypesAreEvaluatedLazily() {
                              Supplier<Iterable<Class<?>>> typesSupplier = spy(new Supplier<Iterable<Class<?>>>() {
                                  @Override
                                  public Iterable<Class<?>> get() {
                                      return Collections.singleton(Object.class);
                                  }
                              });
                          }
                                    
                          // mock for mockito method
                          private Supplier<Iterable<Class<?>>> spy(Supplier<Iterable<Class<?>>> supplier) {
                              return null;
                          }
                      }
                  """, """
                      package com.example.app;
                                    
                      import java.util.Collections;
                      import java.util.function.Supplier;
                                    
                      class ManagedTypesUnitTests {
                          void supplierBasedManagedTypesAreEvaluatedLazily() {
                              var typesSupplier = spy(new Supplier<Iterable<Class<?>>>() {
                                  @Override
                                  public Iterable<Class<?>> get() {
                                      return Collections.singleton(Object.class);
                                  }
                              });
                          }
                                    
                          // mock for mockito method
                          private Supplier<Iterable<Class<?>>> spy(Supplier<Iterable<Class<?>>> supplier) {
                              return null;
                          }
                      }
                  """),
                10
              )
            );
        }

        @Test
        void multiGenerics() {
            // spring-cloud/spring-cloud-contract @ main: spring-cloud-contract-verifier/src/test/resources/contractsToCompile/contract_multipart.java
            // solving java.lang.IllegalArgumentException: Unable to parse expression from JavaType Unknown
            //language=java
            rewriteRun(
              version(
                java("""
                  import java.util.Collection;
                  import java.util.HashMap;
                  import java.util.Map;
                  import java.util.function.Supplier;
                                    
                  class contract_multipart implements Supplier<Collection<Contract>> {                                      \s
                        private static Map<String, DslProperty> namedProps(HttpSender.Request r) {
                            Map<String, DslProperty> map = new HashMap<>();
                            return map;
                        }
                                    
                      @Override
                      public Collection<Contract> get() { return null; }
                  }
                  // replacements
                  class Contract{}
                  class DslProperty{}
                  class HttpSender {
                      static class Request {}
                  }
                  """, """
                  import java.util.Collection;
                  import java.util.HashMap;
                  import java.util.Map;
                  import java.util.function.Supplier;
                                    
                  class contract_multipart implements Supplier<Collection<Contract>> {                                      \s
                        private static Map<String, DslProperty> namedProps(HttpSender.Request r) {
                            var map = new HashMap<String, DslProperty>();
                            return map;
                        }
                                    
                      @Override
                      public Collection<Contract> get() { return null; }
                  }
                  // replacements
                  class Contract{}
                  class DslProperty{}
                  class HttpSender {
                      static class Request {}
                  }
                  """),
                10
              )
            );
        }

        @Test
        void duplicateTemplate() {
            // spring-projects/spring-hateoas @ main src/test/java/org/springframework/hateoas/mediatype/html/HtmlInputTypeUnitTests.java
            // solving Expected a template that would generate exactly one statement to replace one statement, but generated 2. Template:
            //var numbers = __P__.<java.util.stream.Stream<org.springframework.hateoas.mediatype.html.HtmlInputTypeUnitTests..>>/*__p1__*/p()
            //language=java
            rewriteRun(
              version(
                java("""
                  import java.math.BigDecimal;
                  import java.util.Arrays;
                  import java.util.Collection;
                  import java.util.stream.Stream;
                                    
                  class HtmlInputTypeUnitTests {
                        Stream<DynamicTest> derivesInputTypesFromType() {
                            Stream<$> numbers = HtmlInputType.NUMERIC_TYPES.stream() //
                                        .map(it -> $.of(it, HtmlInputType.NUMBER));
                            return null;
                        }
                     
                        static class HtmlInputType {
                            static final Collection<Class<?>> NUMERIC_TYPES = Arrays.asList(int.class, long.class, float.class,
                                    double.class, short.class, Integer.class, Long.class, Float.class, Double.class, Short.class, BigDecimal.class);
                                    
                            public static final HtmlInputType NUMBER = new HtmlInputType();
                         
                            public static HtmlInputType from(Class<?> type) { return null; }
                        }
                     
                        static class $ {
                                    
                            Class<?> type;
                            HtmlInputType expected;
                                    
                            static $ of(Class<?> it, HtmlInputType number){ return null; }\s
                         
                            public void verify() {
                                assertThat(HtmlInputType.from(type)).isEqualTo(expected);
                            }
                                    
                                    
                            @Override
                            public String toString() {
                                return String.format("Derives %s from %s.", expected, type);
                            }
                            //mocking
                            private <SELF extends AbstractBigDecimalAssert<SELF>> AbstractBigDecimalAssert assertThat(HtmlInputType from) {
                                return null;
                            }
                        }
                  }
                  // replacement
                  class DynamicTest {}
                  class AbstractBigDecimalAssert<T> {
                      public void isEqualTo(Object expected) {}
                  }
                  """, """
                  import java.math.BigDecimal;
                  import java.util.Arrays;
                  import java.util.Collection;
                  import java.util.stream.Stream;
                                    
                  import static org.assertj.core.api.Assertions.assertThat;
                                    
                  class HtmlInputTypeUnitTests {
                        Stream<DynamicTest> derivesInputTypesFromType() {
                            var numbers = HtmlInputType.NUMERIC_TYPES.stream() //
                                        .map(it -> $.of(it, HtmlInputType.NUMBER));
                            return null;
                        }
                      
                        static class HtmlInputType {
                            static final Collection<Class<?>> NUMERIC_TYPES = Arrays.asList(int.class, long.class, float.class,
                                    double.class, short.class, Integer.class, Long.class, Float.class, Double.class, Short.class, BigDecimal.class);
                                    
                            public static final HtmlInputType NUMBER = new HtmlInputType();
                          
                            public static HtmlInputType from(Class<?> type) { return null; }
                        }
                      
                        static class $ {
                                    
                            Class<?> type;
                            HtmlInputType expected;
                                    
                            static $ of(Class<?> it, HtmlInputType number){ return null; }\s
                          
                            public void verify() {
                                assertThat(HtmlInputType.from(type)).isEqualTo(expected);
                            }
                                    
                            @Override
                            public String toString() {
                                return String.format("Derives %s from %s.", expected, type);
                            }
                        }
                  }
                  // replacement
                  class DynamicTest {}
                  """),
                10
              )
            );
        }
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
            @Example
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
            @Example
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
