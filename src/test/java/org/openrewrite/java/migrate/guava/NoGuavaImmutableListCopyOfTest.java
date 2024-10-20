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

class NoGuavaImmutableListCopyOfTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipe(new NoGuavaImmutableListCopyOf())
          .parser(JavaParser.fromJavaVersion().classpath("guava"));
    }

    @Test
    void doNotChangeReturnsImmutableList() {
        rewriteRun(
          //language=java
          java(
            """
              import com.google.common.collect.ImmutableList;

              class Test {
                  ImmutableList<String> getList() {
                      return ImmutableList.copyOf(new String[]{"A", "B", "C"});
                  }
              }
              """
          )
        );

    }

    @Test
    void doNotChangeFieldAssignmentToImmutableList() {
        rewriteRun(
          //language=java
          java(
            """
              import com.google.common.collect.ImmutableList;

              class Test {
                  ImmutableList<String> m;

                  {
                      this.m = ImmutableList.copyOf(new String[]{"A", "B", "C"});
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotChangeAssignsToImmutableList() {
        rewriteRun(
          //language=java
          java(
            """
              import com.google.common.collect.ImmutableList;

              class Test {
                  ImmutableList<String> m;

                  void init() {
                      m = ImmutableList.copyOf(new String[]{"A", "B", "C"});
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
              import com.google.common.collect.ImmutableList;

              public class A {
                  ImmutableList<String> immutableList;
                  public A(ImmutableList<String> immutableList) {
                      this.immutableList = immutableList;
                  }
              }
              """
          ),
          //language=java
          java(
            """
              import com.google.common.collect.ImmutableList;

              class Test {
                  A a = new A(ImmutableList.copyOf(new String[]{"A", "B", "C"}));
              }
              """)
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
          //language=java
          java(
            """
              import com.google.common.collect.ImmutableList;

              class Test {
                  void method() {
                      A a = new A();
                      a.method(ImmutableList.copyOf(new String[]{"A", "B", "C"}));
                  }
              }
              """
          )
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
                    List<String> m = ImmutableList.copyOf(new String[]{"A", "B", "C"});
                }
                """,
              """
                import java.util.List;

                class Test {
                    List<String> m = List.copyOf(new String[]{"A", "B", "C"});
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
                import java.util.List;
                import com.google.common.collect.ImmutableList;

                class Test {
                    List<String> m;
                    {
                        this.m = ImmutableList.copyOf(new String[]{"A", "B", "C"});
                    }
                }
                """,
              """
                import java.util.List;

                class Test {
                    List<String> m;
                    {
                        this.m = List.copyOf(new String[]{"A", "B", "C"});
                    }
                }
                """
            ),
            10
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
                    List<String> m = ImmutableList.copyOf(new String[]{"A", "B", "C"});
                }
                """,
              """
                import java.util.List;

                class Test {
                    List<String> m = List.copyOf(new String[]{"A", "B", "C"});
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
                import java.util.List;
                import com.google.common.collect.ImmutableList;

                class Test {
                    List<String> list() {
                        return ImmutableList.copyOf(new String[]{"A", "B", "C"});
                    }
                }
                """,
              """
                import java.util.List;

                class Test {
                    List<String> list() {
                        return List.copyOf(new String[]{"A", "B", "C"});
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
                  import com.google.common.collect.ImmutableList;

                  class Test {
                      A a = new A(ImmutableList.copyOf(new String[]{"A", "B", "C"}));
                  }
                """,
              """
                  import java.util.List;

                  class Test {
                      A a = new A(List.copyOf(new String[]{"A", "B", "C"}));
                  }
                """
            ),
            10
          )
        );
    }

    @Test
    void doNotChangeMethodInvocationWithSelect() {
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
                        a.method(ImmutableList.copyOf(new String[]{"A", "B", "C"}).toArray());
                    }
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
                import com.google.common.collect.ImmutableList;

                class Test {
                    void method() {
                        A a = new A();
                        a.method(ImmutableList.copyOf(new String[]{"A", "B", "C"}));
                    }
                }
                """,
              """
                import java.util.List;

                class Test {
                    void method() {
                        A a = new A();
                        a.method(List.copyOf(new String[]{"A", "B", "C"}));
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
                        return ImmutableList.copyOf(new Integer[]{1, 2, 3});
                    }
                }
                """,
              """
                import java.util.List;

                class Test {
                    List<Integer> list() {
                        return List.copyOf(new Integer[]{1, 2, 3});
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
        rewriteRun(
          version(
            //language=java
            java(
              """
                import com.google.common.collect.ImmutableList;

                class A {
                    Object[] o = new Object[] {
                            ImmutableList.copyOf(new Integer[]{1, 2, 3})
                    };
                }
                """,
              """
                import java.util.List;

                class A {
                    Object[] o = new Object[] {
                            List.copyOf(new Integer[]{1, 2, 3})
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
        rewriteRun(
          version(
            //language=java
            java(
              """
                import com.google.common.collect.ImmutableList;

                class A {
                    Object o = ImmutableList.copyOf(new Integer[]{1, 2, 3});
                }
                """,
              """
                import java.util.List;

                class A {
                    Object o = List.copyOf(new Integer[]{1, 2, 3});
                }
                """
            ),
            10
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/256")
    @Test
    void doNotChangeNestedLists() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                import com.google.common.collect.ImmutableList;
                import java.util.List;

                class A {
                    Object o = List.of(ImmutableList.copyOf(new Integer[]{1, 2}), ImmutableList.copyOf(new Integer[]{2, 3}));
                }
                """
            ),
            10
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/256")
    @Test
    void doNotchangeAssignToImmutableList() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                import com.google.common.collect.ImmutableList;

                class Test {
                    ImmutableList<String> l = ImmutableList.copyOf(new String[]{"A", "B", "C"});
                }
                """
            ),
            10
          )
        );
    }
}
