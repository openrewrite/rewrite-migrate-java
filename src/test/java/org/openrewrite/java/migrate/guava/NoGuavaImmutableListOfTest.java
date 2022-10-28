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
package org.openrewrite.java.migrate.guava;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.marker.JavaVersion;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.UUID;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.version;

class NoGuavaImmutableListOfTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipe(new NoGuavaImmutableListOf())
          .parser(JavaParser.fromJavaVersion()
            .classpath("guava"));
    }

    @Test
    void doNotChangeReturnsImmutableList() {
        rewriteRun(java("""
              import com.google.common.collect.ImmutableList;

              class Test {
                  ImmutableList<String> getList() {
                      return ImmutableList.of();
                  }
              }
          """));

    }

    @Test
    void doNotChangeFieldAssignmentToImmutableList() {
        rewriteRun(java("""
              import com.google.common.collect.ImmutableList;
              
              class Test {
                  ImmutableList<String> m;
              
                  {
                      this.m = ImmutableList.of();
                  }
              }
          """));
    }

    @Test
    void doNotChangeAssignsToImmutableList() {
        rewriteRun(java("""
              import com.google.common.collect.ImmutableList;
              
              class Test {
                  ImmutableList<String> m;
              
                  void init() {
                      m = ImmutableList.of();
                  }
              }
          """));
    }

    @Test
    void doNotChangeNewClass() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(
            """
                  import com.google.common.collect.ImmutableList;
                      
                  public class A {
                      ImmutableList<String> immutableList;
                      public A(ImmutableList<String> immutableList) {
                          this.immutableList = immutableList;
                      }
                  }
              """
          )),
          java("""
                import com.google.common.collect.ImmutableList;

                class Test {
                    A a = new A(ImmutableList.of());
                }
            """));
    }

    @Test
    void doNotChangeMethodInvocation() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(
            """
                  import com.google.common.collect.ImmutableList;
                      
                  public class A {
                      ImmutableList<String> immutableList;
                      public void method(ImmutableList<String> immutableList) {
                          this.immutableList = immutableList;
                      }
                  }
              """
          )),
          java("""
                import com.google.common.collect.ImmutableList;

                class Test {
                    void method() {
                        A a = new A();
                        a.method(ImmutableList.of());
                    }
                }
            """));
    }

    @Test
    void replaceArguments() {
        rewriteRun(version(
          java("""
                  import java.util.List;
                  import com.google.common.collect.ImmutableList;
                  
                  class Test {
                      List<String> m = ImmutableList.of("A", "B", "C", "D");
                  }
              """,
            """
                  import java.util.List;
                  
                  class Test {
                      List<String> m = List.of("A", "B", "C", "D");
                  }
              """), 9
        ));
    }

    @Test
    void fieldAssignmentToList() {
        rewriteRun(version(
          java("""
                  import java.util.List;
                  import com.google.common.collect.ImmutableList;
                  
                  class Test {
                      List<String> m;
                      {
                          this.m = ImmutableList.of();
                      }
                  }
              """,
            """
                  import java.util.List;
                  
                  class Test {
                      List<String> m;
                      {
                          this.m = List.of();
                      }
                  }
              """), 9));
    }

    @Test
    void assigmentToList() {
        rewriteRun(version(
          java("""
                  import java.util.List;
                  import com.google.common.collect.ImmutableList;
                  
                  class Test {
                      List<String> m = ImmutableList.of();
                  }
              """,
            """
                  import java.util.List;
                  
                  class Test {
                      List<String> m = List.of();
                  }
              """
          ), 9));
    }

    @Test
    void returnsList() {
        rewriteRun(version(
          java("""
                  import java.util.List;
                  import com.google.common.collect.ImmutableList;
                  
                  class Test {
                      List<String> list() {
                          return ImmutableList.of();
                      }
                  }
              """,
            """
                  import java.util.List;
                  
                  class Test {
                      List<String> list() {
                          return List.of();
                      }
                  }
              """), 9));
    }

    @Test
    void newClassWithListArgument() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().classpath("guava").dependsOn(
            """
                  import java.util.List;
                      
                  public class A {
                      List<String> list;
                      public A(List<String> list) {
                          this.list = list;
                      }
                  }
              """
          )),
          version(java("""
                  import com.google.common.collect.ImmutableList;

                  class Test {
                      A a = new A(ImmutableList.of());
                  }
              """,
            """
                  import java.util.List;

                  class Test {
                      A a = new A(List.of());
                  }
              """), 11));
    }

    @Test
    void methodInvocationWithListArgument() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().classpath("guava").dependsOn(
            """
                    import java.util.List;
                        
                    public class A {
                        List<String> list;
                        public void method(List<String> list) {
                            this.list = list;
                        }
                    }
              """
          )),
          version(
            java("""
                    import com.google.common.collect.ImmutableList;

                    class Test {
                        void method() {
                            A a = new A();
                            a.method(ImmutableList.of());
                        }
                    }
                """,
              """
                    import java.util.List;

                    class Test {
                        void method() {
                            A a = new A();
                            a.method(List.of());
                        }
                    }
                """), 11));
    }

    @Disabled
    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/136")
    @Test
    void insideAnonymousArrayInitializer() {
        rewriteRun(
          version(
            //language=java
            java("""
                    import com.google.common.collect.ImmutableList;
                    
                    class A {
                        {
                            Object[] o = new Object[] {
                                    ImmutableList.of(1, 2, 3)
                            };
                        }
                    }
              """,
              """
                    import java.util.List;
                    
                    class A {
                        {
                            Object[] o = new Object[] {
                                    List.of(1, 2, 3)
                            };
                        }
                    }
              """),
            11
          )
        );
    }

    @Disabled
    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/136")
    @Test
    void assignToMoreGeneralType() {
        rewriteRun(
          version(
            //language=java
            java("""
                    import com.google.common.collect.ImmutableList;
                    
                    class A {
                        {
                            Object o = ImmutableList.of(1, 2, 3);
                        }
                    }
              """,
              """
                    import java.util.List;
                    
                    class A {
                        {
                            Object o = List.of(1, 2, 3);
                        }
                    }
              """),
            11
          )
        );
    }
}
