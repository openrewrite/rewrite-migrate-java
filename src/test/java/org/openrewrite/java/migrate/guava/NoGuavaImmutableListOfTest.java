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

class NoGuavaImmutableListOfTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipe(new NoGuavaImmutableListOf())
          .parser(JavaParser.fromJavaVersion().classpath("guava"));
    }

    @Test
    void doNotChangeReturnsImmutableList() {
        rewriteRun(
          version(
            //language=java
            java(
              """
                import com.google.common.collect.ImmutableList;

                class Test {
                    ImmutableList<String> getList() {
                        return ImmutableList.of();
                    }
                }
                """
            )
            , 9)
        );

    }

    @Test
    void doNotChangeFieldAssignmentToImmutableList() {
        rewriteRun(
          version(
            //language=java
            java(
              """
                import com.google.common.collect.ImmutableList;

                class Test {
                    ImmutableList<String> m;

                    {
                        this.m = ImmutableList.of();
                    }
                }
                """
            )
            , 9)
        );
    }

    @Test
    void doNotChangeAssignsToImmutableList() {
        rewriteRun(
          version(
            //language=java
            java(
              """
                import com.google.common.collect.ImmutableList;

                class Test {
                    ImmutableList<String> m;

                    void init() {
                        m = ImmutableList.of();
                    }
                }
                """
            )
            , 9)
        );
    }

    @Test
    void doNotChangeNewClass() {
        //language=java
        rewriteRun(
          java(
            """
              import com.google.common.collect.ImmutableList;

              public class A {
                  ImmutableList<String> immutableList;
                  public A(ImmutableList<String> immutableList) {
                      this.immutableList = immutableList;
                  }
              }
              """
          ),
          version(
            //language=java
            java(
              """
                import com.google.common.collect.ImmutableList;

                class Test {
                    A a = new A(ImmutableList.of());
                }
                """)
            , 9)
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
                  import com.google.common.collect.ImmutableList;

                  public class A {
                      ImmutableList<String> immutableList;
                      public void method(ImmutableList<String> immutableList) {
                          this.immutableList = immutableList;
                      }
                  }
                  """
              )
          ),
          version(
            //language=java
            java(
              """
                import com.google.common.collect.ImmutableList;

                class Test {
                    void method() {
                        A a = new A();
                        a.method(ImmutableList.of());
                    }
                }
                """
            )
            , 9)
        );
    }

    @Test
    void replaceArguments() {
        rewriteRun(
          version(
            //language=java
            java(
              """
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
                """
            ),
            9
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
                """
            ),
            9
          )
        );
    }

    @Test
    void assignmentToList() {
        rewriteRun(
          version(
            //language=java
            java(
              """
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
            ),
            9
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
                """
            ),
            9
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
                """
            ),
            11
          )
        );
    }

    @Test
    void doChangeMethodInvocationWithSelect() {
        //language=java
        rewriteRun(
          java(
            """
              import java.util.List;

              public class A {
                  Object[] list;
                  public void method(Object[] list ) {
                      this.list = list;
                  }
              }
              """
          ),
          version(
            //language=java
            java(
              """
                import com.google.common.collect.ImmutableList;

                class Test {
                    void method() {
                        A a = new A();
                        a.method(ImmutableList.of().toArray());
                    }
                }
                """,
              """
                import java.util.List;

                class Test {
                    void method() {
                        A a = new A();
                        a.method(List.of().toArray());
                    }
                }
                """
            ),
            11
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
                """
            ),
            11
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/137")
    @Test
    void listOfInts() {
        rewriteRun(
          version(
            //language=java
            java(
              """
                import java.util.List;
                import com.google.common.collect.ImmutableList;

                class Test {
                    List<Integer> list() {
                        return ImmutableList.of(1, 2, 3);
                    }
                }
                """,
              """
                import java.util.List;

                class Test {
                    List<Integer> list() {
                        return List.of(1, 2, 3);
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
        rewriteRun(
          version(
            //language=java
            java(
              """
                import com.google.common.collect.ImmutableList;

                class A {
                    Object[] o = new Object[] {
                            ImmutableList.of(1, 2, 3)
                    };
                }
                """,
              """
                import java.util.List;

                class A {
                    Object[] o = new Object[] {
                            List.of(1, 2, 3)
                    };
                }
                """
            ),
            9
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/136")
    @Test
    void assignToMoreGeneralType() {
        rewriteRun(
          version(
            //language=java
            java(
              """
                import com.google.common.collect.ImmutableList;

                class A {
                    Object o = ImmutableList.of(1, 2, 3);
                }
                """,
              """
                import java.util.List;

                class A {
                    Object o = List.of(1, 2, 3);
                }
                """
            ),
            11
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/256")
    @Test
    void doChangeAssignToImmutableMap() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                import com.google.common.collect.ImmutableList;
                import java.util.List;

                class A {
                    Object o = List.of(ImmutableList.of(1, 2), ImmutableList.of(2, 3));
                }
                """
            ),
            9
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/489")
    @Test
    void doChangeWithStreamOperation() {
        rewriteRun(
          version(
            //language=java
            java(
              """
                import com.google.common.collect.ImmutableList;

                class A {
                    void test() {
                        final List<Integer> list = ImmutableList.of(1, 2).stream().toList();
                    }
                }
                """,
              """
                import java.util.List;

                class A {
                    void test() {
                        final List<Integer> list = List.of(1, 2).stream().toList();
                    }
                }
                """
            ),
            11
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/489")
    @Test
    void doNotChangeWithImmutableListMethod() {
        rewriteRun(
          version(
            //language=java
            java(
              """
                import com.google.common.collect.ImmutableList;

                class A {
                    void test() {
                        ImmutableList.of(1, 2).asList();
                    }
                }
                """
            ),
            11
          )
        );
    }

    @Test
    void doChangeAllAssignToImmutableMap() {
        //language=java
        rewriteRun(
          spec -> spec.recipe(new NoGuavaImmutableListOf(true)),
          version(
            java(
              """
                import com.google.common.collect.ImmutableList;
                import java.util.List;

                class A {
                    Object o = List.of(ImmutableList.of(1, 2), ImmutableList.of(2, 3));
                }
                """,
              """
                import java.util.List;

                class A {
                    Object o = List.of(List.of(1, 2), List.of(2, 3));
                }
                """
            ),
            9
          )
        );
    }
    @Test
    void doChangeNestedListsWithVariableType() {
        //language=java
        rewriteRun(
          spec -> spec.recipe(new NoGuavaImmutableListOf(true)),
          version(
            java(
              """
                import com.google.common.collect.ImmutableList;

                class A {
                    ImmutableList<ImmutableList<Integer>> o = ImmutableList.of(ImmutableList.of(1, 2), ImmutableList.of(2, 3));
                }
                """,
              """
                import java.util.List;

                class A {
                    List<List<Integer>> o = List.of(List.of(1, 2), List.of(2, 3));
                }
                """
            ),
            9
          )
        );
    }

    @Test
    void doChangeAssignFromImmutableListToList() {
        rewriteRun(
          spec -> spec.recipe(new NoGuavaImmutableListOf(true)),
          version(
            //language=java
            java(
              """
                import com.google.common.collect.ImmutableList;

                class A {
                    void test() {
                        ImmutableList<Integer> o = ImmutableList.of(1, 2);
                    }
                }
                """,
              """
                import java.util.List;

                class A {
                    void test() {
                        List<Integer> o = List.of(1, 2);
                    }
                }
                """
            ),
            11
          )
        );
    }
}
