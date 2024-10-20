/*
 * Copyright 2023 the original author or authors.
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

class NoGuavaImmutableMapCopyOfTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipe(new NoGuavaImmutableMapCopyOf())
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
                      return ImmutableMap.copyOf(new String[]{"A", "B", "C"});
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotChangeFieldAssignmentToImmutableMap() {
        //language=java
        rewriteRun(
          java(
            """
              import com.google.common.collect.ImmutableMap;

              class Test {
                  ImmutableMap<String, String> m;

                  {
                      this.m = ImmutableMap.copyOf(new String[]{"A", "B", "C"});
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
                      m = ImmutableMap.copyOf(new String[]{"A", "B", "C"});
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotChangeNewClass() {
        rewriteRun(
          spec -> spec.parser(
            JavaParser.fromJavaVersion()
              .classpath("guava")
              .dependsOn(
                //language=java
                """
                  import com.google.common.collect.ImmutableMap;

                  public class A {
                      ImmutableMap<String, String> immutableMap;
                      public A(ImmutableMap<String, String> immutableMap) {
                          this.immutableMap = immutableMap;
                      }
                  }
                  """
              )
          ),
          java(
            """
              import com.google.common.collect.ImmutableMap;

              class Test {
                  A a = new A(ImmutableMap.copyOf(new String[]{"A", "B", "C"}));
              }
              """
          )
        );
    }

    @Test
    void doNotChangeMethodInvocation() {
        rewriteRun(
          spec -> spec.parser(
            JavaParser.fromJavaVersion()
              .classpath("guava")
              .dependsOn(
                //language=java
                """
                  import com.google.common.collect.ImmutableMap;

                  public class A {
                      ImmutableMap<String, String> immutableMap;
                      public void method(ImmutableMap<String, String> immutableMap) {
                          this.immutableMap = immutableMap;
                      }
                  }
                  """
              )
          ),
          java(
            """
              import com.google.common.collect.ImmutableMap;

              class Test {
                  void method() {
                      A a = new A();
                      a.method(ImmutableMap.copyOf(new String[]{"A", "B", "C"}));
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
                    Map<String, String> m = ImmutableMap.copyOf(new String[]{"A", "B", "C"});
                }
                """,
              """
                import java.util.Map;

                class Test {
                    Map<String, String> m = Map.copyOf(new String[]{"A", "B", "C"});
                }
                """
            ),
            10
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
                        this.m = ImmutableMap.copyOf(new String[]{"A", "B", "C"});
                    }
                }
                """,
              """
                import java.util.Map;

                class Test {
                    Map<String, String> m;
                    {
                        this.m = Map.copyOf(new String[]{"A", "B", "C"});
                    }
                }
                """
            ),
            10
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
                    Map<String, String> m = ImmutableMap.copyOf(new String[]{"A", "B", "C"});
                }
                """,
              """
                import java.util.Map;

                class Test {
                    Map<String, String> m = Map.copyOf(new String[]{"A", "B", "C"});
                }
                """
            ),
            10
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
                        return ImmutableMap.copyOf(new String[]{"A", "B", "C"});
                    }
                }
                """,
              """
                import java.util.Map;

                class Test {
                    Map<String, String> map() {
                        return Map.copyOf(new String[]{"A", "B", "C"});
                    }
                }
                """
            ),
            10
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
                        return ImmutableMap.copyOf(new Integer[]{1, 2, 3});
                    }
                }
                """,
              """
                import java.util.Map;

                class Test {
                    Map<Integer, Integer> map() {
                        return Map.copyOf(new Integer[]{1, 2, 3});
                    }
                }
                """
            ),
            10
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
                    A a = new A(ImmutableMap.copyOf(new String[]{"A", "B", "C"}));
                }
                """,
              """
                import java.util.Map;

                class Test {
                    A a = new A(Map.copyOf(new String[]{"A", "B", "C"}));
                }
                """
            ),
            10
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
                        a.method(ImmutableMap.copyOf(new String[]{"A", "B", "C"}));
                    }
                }
                """,
              """
                import java.util.Map;

                class Test {
                    void method() {
                        A a = new A();
                        a.method(Map.copyOf(new String[]{"A", "B", "C"}));
                    }
                }
                """
            ),
            10
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
                            ImmutableMap.copyOf(new Integer[]{1, 2, 3})
                    };
                }
                """,
              """
                import java.util.Map;

                class A {
                    Object[] o = new Object[] {
                            Map.copyOf(new Integer[]{1, 2, 3})
                    };
                }
                """
            ),
            10
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
                    Object o = ImmutableMap.copyOf(new Integer[]{1, 2, 3});
                }
                """,
              """
                import java.util.Map;

                class A {
                    Object o = Map.copyOf(new Integer[]{1, 2, 3});
                }
                """
            ),
            10
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/256")
    @Test
    void doNotChangeNestedMaps() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                import com.google.common.collect.ImmutableMap;
                import java.util.Map;

                class A {
                    Object o = Map.of(1, ImmutableMap.copyOf(new Integer[]{1, 2}));
                }
                """
            ),
            10
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/256")
    @Test
    void doNotchangeAssignToImmutableMap() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                import com.google.common.collect.ImmutableMap;

                class Test {
                    ImmutableMap<String, String> m = ImmutableMap.copyOf(new String[]{"A", "B", "C"});
                }
                """
            ),
            10
          )
        );
    }
}
