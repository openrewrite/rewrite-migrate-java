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

class NoGuavaImmutableListCopyOfTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipe(new NoGuavaImmutableListCopyOf())
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
                import java.util.ArrayList;
                import java.util.List;
                import com.google.common.collect.ImmutableList;

                class Test {
                    List<String> m = ImmutableList.copyOf(new ArrayList<>());
                }
                """,
              """
                import java.util.ArrayList;
                import java.util.List;

                class Test {
                    List<String> m = List.copyOf(new ArrayList<>());
                }
                """
            ),
            10
          )
        );
    }

    @Test
    void doNotChangeReturnsImmutableList() {
        rewriteRun(
          version(
            //language=java
            java(
              """
                import java.util.ArrayList;
                import com.google.common.collect.ImmutableList;

                class Test {
                    ImmutableList<String> getList() {
                        return ImmutableList.copyOf(new ArrayList<>());
                    }
                }
                """
            ),
            10
          )
        );
    }

    @Test
    void doNotChangeFieldAssignmentToImmutableList() {
        rewriteRun(
          version(
            //language=java
            java(
              """
                import java.util.ArrayList;
                import com.google.common.collect.ImmutableList;

                class Test {
                    ImmutableList<String> m;

                    {
                        this.m = ImmutableList.copyOf(new ArrayList<>());
                    }
                }
                """
            ),
            10
          )
        );
    }

    @Test
    void doNotChangeAssignsToImmutableList() {
        rewriteRun(
          version(
            //language=java
            java(
              """
                import java.util.ArrayList;
                import com.google.common.collect.ImmutableList;

                class Test {
                    ImmutableList<String> m;

                    void init() {
                        m = ImmutableList.copyOf(new ArrayList<>());
                    }
                }
                """
            ),
            10
          )
        );
    }

    @Test
    void fieldAssignmentToList() {
        rewriteRun(
          version(
            //language=java
            java(
              """
                import java.util.ArrayList;
                import java.util.List;
                import com.google.common.collect.ImmutableList;

                class Test {
                    List<String> m;
                    {
                        this.m = ImmutableList.copyOf(new ArrayList<>());
                    }
                }
                """,
              """
                import java.util.ArrayList;
                import java.util.List;

                class Test {
                    List<String> m;
                    {
                        this.m = List.copyOf(new ArrayList<>());
                    }
                }
                """
            ),
            10
          )
        );
    }

    @Test
    void returnsList() {
        rewriteRun(
          version(
            //language=java
            java(
              """
                import java.util.ArrayList;
                import java.util.List;
                import com.google.common.collect.ImmutableList;

                class Test {
                    List<String> list() {
                        return ImmutableList.copyOf(new ArrayList<>());
                    }
                }
                """,
              """
                import java.util.ArrayList;
                import java.util.List;

                class Test {
                    List<String> list() {
                        return List.copyOf(new ArrayList<>());
                    }
                }
                """
            ),
            10
          )
        );
    }

    @Test
    void newClassWithListArgument() {
        //language=java
        rewriteRun(
          java(
            """
              import java.util.List;

              public class A {
                  List<String> list;
                  public A(List<String> list) {
                      this.list = list;
                  }
              }
              """
          ),
          version(
            //language=java
            java(
              """
                import java.util.ArrayList;
                import com.google.common.collect.ImmutableList;

                class Test {
                    A a = new A(ImmutableList.copyOf(new ArrayList<>()));
                }
                """,
              """
                import java.util.ArrayList;
                import java.util.List;

                class Test {
                    A a = new A(List.copyOf(new ArrayList<>()));
                }
                """
            ),
            10
          )
        );
    }

    @Test
    void methodInvocationWithListArgument() {
        //language=java
        rewriteRun(
          java(
            """
              import java.util.List;

              public class A {
                  List<String> list;
                  public void method(List<String> list) {
                      this.list = list;
                  }
              }
              """
          ),
          version(
            //language=java
            java(
              """
                import java.util.ArrayList;
                import com.google.common.collect.ImmutableList;

                class Test {
                    void method() {
                        A a = new A();
                        a.method(ImmutableList.copyOf(new ArrayList<>()));
                    }
                }
                """,
              """
                import java.util.ArrayList;
                import java.util.List;

                class Test {
                    void method() {
                        A a = new A();
                        a.method(List.copyOf(new ArrayList<>()));
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
                import java.util.ArrayList;
                import com.google.common.collect.ImmutableList;

                class A {
                    Object o = ImmutableList.copyOf(new ArrayList<>());
                }
                """,
              """
                import java.util.ArrayList;
                import java.util.List;

                class A {
                    Object o = List.copyOf(new ArrayList<>());
                }
                """
            ),
            11
          )
        );
    }

    @Test
    void doChangeAssignFromImmutableListToList() {
        rewriteRun(
          spec -> spec.recipe(new NoGuavaImmutableListCopyOf(true)),
          version(
            //language=java
            java(
              """
                import java.util.ArrayList;
                import com.google.common.collect.ImmutableList;

                class A {
                    void test() {
                        ImmutableList<String> o = ImmutableList.copyOf(new ArrayList<>());
                    }
                }
                """,
              """
                import java.util.ArrayList;
                import java.util.List;

                class A {
                    void test() {
                        List<String> o = List.copyOf(new ArrayList<>());
                    }
                }
                """
            ),
            11
          )
        );
    }
}
