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
package org.openrewrite.java.migrate.guava;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.*;

class NoGuavaImmutableSetOfTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipe(new NoGuavaImmutableSetOf())
          .parser(JavaParser.fromJavaVersion().classpath("guava"));
    }

    @DocumentExample
    @Test
    void replaceArguments() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                import java.util.Set;
                import com.google.common.collect.ImmutableSet;

                class Test {
                    Set<String> m = ImmutableSet.of("A", "B", "C", "D");
                }
                """,
              """
                import java.util.Set;

                class Test {
                    Set<String> m = Set.of("A", "B", "C", "D");
                }
                """
            ),
            9
          )
        );
    }

    @Test
    void doNotChangeReturnsImmutableSet() {
        //language=java
        rewriteRun(
          java(
            """
              import com.google.common.collect.ImmutableSet;

              class Test {
                  ImmutableSet<String> getSet() {
                      return ImmutableSet.of();
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotChangeFieldAssignmentToImmutableSet() {
        //language=java
        rewriteRun(
          java(
            """
              import com.google.common.collect.ImmutableSet;

              class Test {
                  ImmutableSet<String> m;

                  {
                      this.m = ImmutableSet.of();
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotChangeAssignsToImmutableSet() {
        //language=java
        rewriteRun(
          java(
            """
              import com.google.common.collect.ImmutableSet;

              class Test {
                  ImmutableSet<String> m;

                  void init() {
                      m = ImmutableSet.of();
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
                  import com.google.common.collect.ImmutableSet;

                  public class A {
                      ImmutableSet<String> immutableSet;
                      public A(ImmutableSet<String> immutableSet) {
                          this.immutableSet = immutableSet;
                      }
                  }
                  """
              )
          ),
          java(
            """
              import com.google.common.collect.ImmutableSet;

              class Test {
                  A a = new A(ImmutableSet.of());
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
                  import com.google.common.collect.ImmutableSet;

                  public class A {
                      ImmutableSet<String> immutableSet;
                      public void method(ImmutableSet<String> immutableSet) {
                          this.immutableSet = immutableSet;
                      }
                  }
                  """
              )
          ),
          java(
            """
              import com.google.common.collect.ImmutableSet;

              class Test {
                  void method() {
                      A a = new A();
                      a.method(ImmutableSet.of());
                  }
              }
              """
          )
        );
    }

    @Test
    void fieldAssignmentToSet() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                import java.util.Set;
                import com.google.common.collect.ImmutableSet;

                class Test {
                    Set<String> m;
                    {
                        this.m = ImmutableSet.of();
                    }
                }
                """,
              """
                import java.util.Set;

                class Test {
                    Set<String> m;
                    {
                        this.m = Set.of();
                    }
                }
                """
            ),
            9
          )
        );
    }

    @Test
    void assignmentToSet() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                import java.util.Set;
                import com.google.common.collect.ImmutableSet;

                class Test {
                    Set<String> m = ImmutableSet.of();
                }
                """,
              """
                import java.util.Set;

                class Test {
                    Set<String> m = Set.of();
                }
                """
            ),
            9
          )
        );
    }

    @Test
    void returnsSet() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                import java.util.Set;
                import com.google.common.collect.ImmutableSet;

                class Test {
                    Set<String> set() {
                        return ImmutableSet.of();
                    }
                }
                """,
              """
                import java.util.Set;

                class Test {
                    Set<String> set() {
                        return Set.of();
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
    void setOfInts() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                import java.util.Set;
                import com.google.common.collect.ImmutableSet;

                class Test {
                    Set<Integer> set() {
                        return ImmutableSet.of(1, 2, 3);
                    }
                }
                """,
              """
                import java.util.Set;

                class Test {
                    Set<Integer> set() {
                        return Set.of(1, 2, 3);
                    }
                }
                """
            ),
            9
          )
        );
    }

    @Test
    void newClassWithSetArgument() {
        //language=java
        rewriteRun(
          java(
            """
              import java.util.Set;

              public class A {
                  Set<String> set;
                  public A(Set<String> set) {
                      this.set = set;
                  }
              }
              """
          ),
          version(
            //language=java
            java(
              """
                import com.google.common.collect.ImmutableSet;

                class Test {
                    A a = new A(ImmutableSet.of());
                }
                """,
              """
                import java.util.Set;

                class Test {
                    A a = new A(Set.of());
                }
                """
            ),
            9
          )
        );
    }

    @Test
    void methodInvocationWithSetArgument() {
        //language=java
        rewriteRun(
          java(
            """
              import java.util.Set;

              public class A {
                  Set<String> set;
                  public void method(Set<String> set) {
                      this.set = set;
                  }
              }
              """
          ),
          version(
            java(
              """
                import com.google.common.collect.ImmutableSet;

                class Test {
                    void method() {
                        A a = new A();
                        a.method(ImmutableSet.of());
                    }
                }
                """,
              """
                import java.util.Set;

                class Test {
                    void method() {
                        A a = new A();
                        a.method(Set.of());
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
                import com.google.common.collect.ImmutableSet;

                class A {
                    Object[] o = new Object[] {
                            ImmutableSet.of(1, 2, 3)
                    };
                }
                """,
              """
                import java.util.Set;

                class A {
                    Object[] o = new Object[] {
                            Set.of(1, 2, 3)
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
                import com.google.common.collect.ImmutableSet;

                class A {
                    Object o = ImmutableSet.of(1, 2, 3);
                }
                """,
              """
                import java.util.Set;

                class A {
                    Object o = Set.of(1, 2, 3);
                }
                """
            ),
            11
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/256")
    @Test
    void doNotChangeNestedSets() {
        //language=java
        rewriteRun(
          version(
            java(
              """
                import com.google.common.collect.ImmutableSet;
                import java.util.Set;

                class A {
                    Object o = Set.of(ImmutableSet.of(1, 2));
                }
                """
            ),
            9
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/256")
    @Test
    void doNotChangeAssignToImmutableSet() {
        //language=java
        rewriteRun(
          spec -> spec.allSources(all -> all.markers(javaVersion(9))),
          java(
            """
              import com.google.common.collect.ImmutableSet;

              class Test {
                  ImmutableSet<String> m = ImmutableSet.of();
              }
              """
          )
        );
    }

    @Test
    void multiLine() {
        //language=java
        rewriteRun(
          spec -> spec.allSources(all -> all.markers(javaVersion(11))),
          java(
            """
              import com.google.common.collect.ImmutableSet;
              import java.util.Set;

              class Test {
                  Set<String> m = ImmutableSet.of(
                    "foo",
                    "bar"
                  );
              }
              """,
            """
              import java.util.Set;

              class Test {
                  Set<String> m = Set.of(
                    "foo",
                    "bar"
                  );
              }
              """
          )
        );
    }

    @Test
    void doChangeNestedSets() {
        //language=java
        rewriteRun(
          spec -> spec.recipe(new NoGuavaImmutableSetOf(true)),
          version(
            java(
              """
                import com.google.common.collect.ImmutableSet;
                import java.util.Set;

                class A {
                    Object o = Set.of(ImmutableSet.of(1, 2));
                }
                """,
              """
                import java.util.Set;

                class A {
                    Object o = Set.of(Set.of(1, 2));
                }
                """
            ),
            9
          )
        );
    }

    @Test
    void doChangeAssignToImmutableSet() {
        //language=java
        rewriteRun(
          spec -> spec.recipe(new NoGuavaImmutableSetOf(true)),
          version(
            java(
              """
                import com.google.common.collect.ImmutableSet;

                class Test {
                    ImmutableSet<String> m = ImmutableSet.of();
                }
                """,
              """
                import java.util.Set;

                class Test {
                    Set<String> m = Set.of();
                }
                """
            ),
            11)
        );
    }

}
