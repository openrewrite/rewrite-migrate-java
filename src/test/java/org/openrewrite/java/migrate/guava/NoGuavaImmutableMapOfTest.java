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

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.version;

class NoGuavaImmutableMapOfTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipe(new NoGuavaImmutableMapOf())
          .parser(JavaParser.fromJavaVersion().classpath("guava"));
    }

    @Test
    void doNotChangeReturnsImmutableMap() {
        //language=java
        rewriteRun(
          java(
            """
              import com.google.common.collect.ImmutableMap;

              class Test {
                  ImmutableMap<String, String> getMap() {
                      return ImmutableMap.of();
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotChangeFieldAssignmentToImmutableMap() {
        rewriteRun(
          //language=java
          java(
            """
              import com.google.common.collect.ImmutableMap;
                            
              class Test {
                  ImmutableMap<String, String> m;
                            
                  {
                      this.m = ImmutableMap.of();
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotChangeAssignsToImmutableMap() {
        //language=java
        rewriteRun(
          java(
            """
              import com.google.common.collect.ImmutableMap;
                            
              class Test {
                  ImmutableMap<String, String> m;
                            
                  void init() {
                      m = ImmutableMap.of();
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotChangeNewClass() {
        //language=java
        rewriteRun(
          java(
            """
              import com.google.common.collect.ImmutableMap;
                              
              public class A {
                  ImmutableMap<String, String> immutableMap;
                  public A(ImmutableMap<String, String> immutableMap) {
                      this.immutableMap = immutableMap;
                  }
              }
              """
          ),
          java(
            """
              import com.google.common.collect.ImmutableMap;
                            
              class Test {
                  A a = new A(ImmutableMap.of());
              }
              """
          )
        );
    }

    @Test
    void doNotChangeMethodInvocation() {
        //language=java
        rewriteRun(
          java(
            """
              import com.google.common.collect.ImmutableMap;
                              
              public class A {
                  ImmutableMap<String, String> immutableMap;
                  public void method(ImmutableMap<String, String> immutableMap) {
                      this.immutableMap = immutableMap;
                  }
              }
              """
          ),
          java(
            """
              import com.google.common.collect.ImmutableMap;

              class Test {
                  void method() {
                      A a = new A();
                      a.method(ImmutableMap.of());
                  }
              }
              """
          )
        );
    }

    @Test
    void replaceArguments() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                import java.util.Map;
                import com.google.common.collect.ImmutableMap;
                                
                class Test {
                    Map<String, String> m = ImmutableMap.of("A", "B", "C", "D");
                }
                """,
              """
                import java.util.Map;
                                
                class Test {
                    Map<String, String> m = Map.of("A", "B", "C", "D");
                }
                """
            ),
            9
          )
        );
    }

    @Test
    void fieldAssignmentToMap() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                import java.util.Map;
                import com.google.common.collect.ImmutableMap;
                                
                class Test {
                    Map<String, String> m;
                    {
                        this.m = ImmutableMap.of();
                    }
                }
                """,
              """
                import java.util.Map;
                                
                class Test {
                    Map<String, String> m;
                    {
                        this.m = Map.of();
                    }
                }
                """
            ),
            9
          )
        );
    }

    @Test
    void assignmentToMap() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                import java.util.Map;
                import com.google.common.collect.ImmutableMap;
                                
                class Test {
                    Map<String, String> m = ImmutableMap.of();
                }
                """,
              """
                import java.util.Map;
                                
                class Test {
                    Map<String, String> m = Map.of();
                }
                """
            ),
            9
          )
        );
    }

    @Test
    void returnsMap() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                import java.util.Map;
                import com.google.common.collect.ImmutableMap;
                                
                class Test {
                    Map<String, String> map() {
                        return ImmutableMap.of();
                    }
                }
                """,
              """
                import java.util.Map;
                                
                class Test {
                    Map<String, String> map() {
                        return Map.of();
                    }
                }
                """
            ),
            9
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/137")
    @Test
    void mapOfInts() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                import java.util.Map;
                import com.google.common.collect.ImmutableMap;
                                
                class Test {
                    Map<Integer, Integer> map() {
                        return ImmutableMap.of(1, 1, 2, 2, 3, 3);
                    }
                }
                """,
              """
                import java.util.Map;
                                
                class Test {
                    Map<Integer, Integer> map() {
                        return Map.of(1, 1, 2, 2, 3, 3);
                    }
                }
                """
            ),
            9
          )
        );
    }

    @Test
    void newClassWithMapArgument() {
        //language=java
        rewriteRun(
          java(
            """
              import java.util.Map;
                  
              public class A {
                  Map<String, String> map;
                  public A(Map<String, String> map) {
                      this.map = map;
                  }
              }
              """
          ),
          version(
            java(
              """
                import com.google.common.collect.ImmutableMap;
                                
                class Test {
                    A a = new A(ImmutableMap.of());
                }
                """,
              """
                import java.util.Map;
                                
                class Test {
                    A a = new A(Map.of());
                }
                """
            ),
            9
          )
        );
    }

    @Test
    void methodInvocationWithMapArgument() {
        //language=java
        rewriteRun(
          java(
            """
              import java.util.Map;
                  
              public class A {
                  Map<String, String> map;
                  public void method(Map<String, String> map) {
                      this.map = map;
                  }
              }
              """
          ),
          version(
            java(
              """
                import com.google.common.collect.ImmutableMap;
                                
                class Test {
                    void method() {
                        A a = new A();
                        a.method(ImmutableMap.of());
                    }
                }
                """,
              """
                import java.util.Map;
                                
                class Test {
                    void method() {
                        A a = new A();
                        a.method(Map.of());
                    }
                }
                """
            ),
            9
          )
        );
    }

    @Test
    void variableIsMap() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                import java.util.HashMap;
                import java.util.Map;
                import com.google.common.collect.ImmutableMap;
                                
                class Test {
                    Map<Integer, Map<String, String>> map = new HashMap<>();
                    void setMap(String value) {
                        for (int i = 0; i < 10; i++) {
                            map.getOrDefault(i, ImmutableMap.of());
                        }
                    }
                }
                """,
              """
                import java.util.HashMap;
                import java.util.Map;
                                
                class Test {
                    Map<Integer, Map<String, String>> map = new HashMap<>();
                    void setMap(String value) {
                        for (int i = 0; i < 10; i++) {
                            map.getOrDefault(i, Map.of());
                        }
                    }
                }
                """
            ),
            9
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/138")
    @Test
    void insideAnonymousArrayInitializer() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                import com.google.common.collect.ImmutableMap;
                                
                class A {
                    Object[] o = new Object[] {
                            ImmutableMap.of(1, 1, 2, 2, 3, 3)
                    };
                }
                """,
              """
                import java.util.Map;
                                
                class A {
                    Object[] o = new Object[] {
                            Map.of(1, 1, 2, 2, 3, 3)
                    };
                }
                """
            ),
            11
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/136")
    @Test
    void assignToMoreGeneralType() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                import com.google.common.collect.ImmutableMap;
                                
                class A {
                    Object o = ImmutableMap.of(1, 1, 2, 2, 3, 3);
                }
                """,
              """
                import java.util.Map;
                                
                class A {
                    Object o = Map.of(1, 1, 2, 2, 3, 3);
                }
                """
            ),
            11
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/205")
    @Test
    void nestedMaps() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                import com.google.common.collect.ImmutableMap;
                                
                class A {
                    Object o = ImmutableMap.of(1, ImmutableMap.of(2, 3));
                }
                """,
              """
                import java.util.Map;
                                
                class A {
                    Object o = Map.of(1, Map.of(2, 3));
                }
                """
            ),
            11
          )
        );
    }
}
