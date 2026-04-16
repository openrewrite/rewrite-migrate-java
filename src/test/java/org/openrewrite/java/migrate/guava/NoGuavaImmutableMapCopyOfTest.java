/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.java.migrate.guava;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
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
          .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "guava"));
    }

    @DocumentExample
    @Test
    void replaceAssignment() {
        rewriteRun(
          version(
            //language=java
            java(
              """
                import java.util.HashMap;
                import java.util.Map;
                import com.google.common.collect.ImmutableMap;

                class Test {
                    Map<String, String> m = ImmutableMap.copyOf(new HashMap<>());
                }
                """,
              """
                import java.util.HashMap;
                import java.util.Map;

                class Test {
                    Map<String, String> m = Map.copyOf(new HashMap<>());
                }
                """
            ),
            10
          )
        );
    }

    @Test
    void doNotChangeReturnsImmutableMap() {
        rewriteRun(
          version(
            //language=java
            java(
              """
                import java.util.HashMap;
                import com.google.common.collect.ImmutableMap;

                class Test {
                    ImmutableMap<String, String> getMap() {
                        return ImmutableMap.copyOf(new HashMap<>());
                    }
                }
                """
            ),
            10
          )
        );
    }

    @Test
    void doNotChangeFieldAssignmentToImmutableMap() {
        rewriteRun(
          version(
            //language=java
            java(
              """
                import java.util.HashMap;
                import com.google.common.collect.ImmutableMap;

                class Test {
                    ImmutableMap<String, String> m;

                    {
                        this.m = ImmutableMap.copyOf(new HashMap<>());
                    }
                }
                """
            ),
            10
          )
        );
    }

    @Test
    void doNotChangeAssignsToImmutableMap() {
        rewriteRun(
          version(
            //language=java
            java(
              """
                import java.util.HashMap;
                import com.google.common.collect.ImmutableMap;

                class Test {
                    ImmutableMap<String, String> m;

                    void init() {
                        m = ImmutableMap.copyOf(new HashMap<>());
                    }
                }
                """
            ),
            10
          )
        );
    }

    @Test
    void fieldAssignmentToMap() {
        rewriteRun(
          version(
            //language=java
            java(
              """
                import java.util.HashMap;
                import java.util.Map;
                import com.google.common.collect.ImmutableMap;

                class Test {
                    Map<String, String> m;
                    {
                        this.m = ImmutableMap.copyOf(new HashMap<>());
                    }
                }
                """,
              """
                import java.util.HashMap;
                import java.util.Map;

                class Test {
                    Map<String, String> m;
                    {
                        this.m = Map.copyOf(new HashMap<>());
                    }
                }
                """
            ),
            10
          )
        );
    }

    @Test
    void returnsMap() {
        rewriteRun(
          version(
            //language=java
            java(
              """
                import java.util.HashMap;
                import java.util.Map;
                import com.google.common.collect.ImmutableMap;

                class Test {
                    Map<String, String> map() {
                        return ImmutableMap.copyOf(new HashMap<>());
                    }
                }
                """,
              """
                import java.util.HashMap;
                import java.util.Map;

                class Test {
                    Map<String, String> map() {
                        return Map.copyOf(new HashMap<>());
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
            //language=java
            java(
              """
                import java.util.HashMap;
                import com.google.common.collect.ImmutableMap;

                class Test {
                    A a = new A(ImmutableMap.copyOf(new HashMap<>()));
                }
                """,
              """
                import java.util.HashMap;
                import java.util.Map;

                class Test {
                    A a = new A(Map.copyOf(new HashMap<>()));
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
            //language=java
            java(
              """
                import java.util.HashMap;
                import com.google.common.collect.ImmutableMap;

                class Test {
                    void method() {
                        A a = new A();
                        a.method(ImmutableMap.copyOf(new HashMap<>()));
                    }
                }
                """,
              """
                import java.util.HashMap;
                import java.util.Map;

                class Test {
                    void method() {
                        A a = new A();
                        a.method(Map.copyOf(new HashMap<>()));
                    }
                }
                """
            ),
            11
          )
        );
    }

    @Test
    void assignToMoreGeneralType() {
        rewriteRun(
          version(
            //language=java
            java(
              """
                import java.util.HashMap;
                import com.google.common.collect.ImmutableMap;

                class A {
                    Object o = ImmutableMap.copyOf(new HashMap<>());
                }
                """,
              """
                import java.util.HashMap;
                import java.util.Map;

                class A {
                    Object o = Map.copyOf(new HashMap<>());
                }
                """
            ),
            11
          )
        );
    }

    @Test
    void doChangeAssignFromImmutableMapToMap() {
        rewriteRun(
          spec -> spec.recipe(new NoGuavaImmutableMapCopyOf(true)),
          version(
            //language=java
            java(
              """
                import java.util.HashMap;
                import com.google.common.collect.ImmutableMap;

                class A {
                    void test() {
                        ImmutableMap<String, String> o = ImmutableMap.copyOf(new HashMap<>());
                    }
                }
                """,
              """
                import java.util.HashMap;
                import java.util.Map;

                class A {
                    void test() {
                        Map<String, String> o = Map.copyOf(new HashMap<>());
                    }
                }
                """
            ),
            11
          )
        );
    }
}
