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

class NoGuavaImmutableSetCopyOfTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipe(new NoGuavaImmutableSetCopyOf())
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
                import java.util.HashSet;
                import java.util.Set;
                import com.google.common.collect.ImmutableSet;

                class Test {
                    Set<String> m = ImmutableSet.copyOf(new HashSet<>());
                }
                """,
              """
                import java.util.HashSet;
                import java.util.Set;

                class Test {
                    Set<String> m = Set.copyOf(new HashSet<>());
                }
                """
            ),
            10
          )
        );
    }

    @Test
    void doNotChangeReturnsImmutableSet() {
        rewriteRun(
          version(
            //language=java
            java(
              """
                import java.util.HashSet;
                import com.google.common.collect.ImmutableSet;

                class Test {
                    ImmutableSet<String> getSet() {
                        return ImmutableSet.copyOf(new HashSet<>());
                    }
                }
                """
            ),
            10
          )
        );
    }

    @Test
    void doNotChangeFieldAssignmentToImmutableSet() {
        rewriteRun(
          version(
            //language=java
            java(
              """
                import java.util.HashSet;
                import com.google.common.collect.ImmutableSet;

                class Test {
                    ImmutableSet<String> m;

                    {
                        this.m = ImmutableSet.copyOf(new HashSet<>());
                    }
                }
                """
            ),
            10
          )
        );
    }

    @Test
    void doNotChangeAssignsToImmutableSet() {
        rewriteRun(
          version(
            //language=java
            java(
              """
                import java.util.HashSet;
                import com.google.common.collect.ImmutableSet;

                class Test {
                    ImmutableSet<String> m;

                    void init() {
                        m = ImmutableSet.copyOf(new HashSet<>());
                    }
                }
                """
            ),
            10
          )
        );
    }

    @Test
    void fieldAssignmentToSet() {
        rewriteRun(
          version(
            //language=java
            java(
              """
                import java.util.HashSet;
                import java.util.Set;
                import com.google.common.collect.ImmutableSet;

                class Test {
                    Set<String> m;
                    {
                        this.m = ImmutableSet.copyOf(new HashSet<>());
                    }
                }
                """,
              """
                import java.util.HashSet;
                import java.util.Set;

                class Test {
                    Set<String> m;
                    {
                        this.m = Set.copyOf(new HashSet<>());
                    }
                }
                """
            ),
            10
          )
        );
    }

    @Test
    void returnsSet() {
        rewriteRun(
          version(
            //language=java
            java(
              """
                import java.util.HashSet;
                import java.util.Set;
                import com.google.common.collect.ImmutableSet;

                class Test {
                    Set<String> set() {
                        return ImmutableSet.copyOf(new HashSet<>());
                    }
                }
                """,
              """
                import java.util.HashSet;
                import java.util.Set;

                class Test {
                    Set<String> set() {
                        return Set.copyOf(new HashSet<>());
                    }
                }
                """
            ),
            10
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
                import java.util.HashSet;
                import com.google.common.collect.ImmutableSet;

                class Test {
                    A a = new A(ImmutableSet.copyOf(new HashSet<>()));
                }
                """,
              """
                import java.util.HashSet;
                import java.util.Set;

                class Test {
                    A a = new A(Set.copyOf(new HashSet<>()));
                }
                """
            ),
            10
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
            //language=java
            java(
              """
                import java.util.HashSet;
                import com.google.common.collect.ImmutableSet;

                class Test {
                    void method() {
                        A a = new A();
                        a.method(ImmutableSet.copyOf(new HashSet<>()));
                    }
                }
                """,
              """
                import java.util.HashSet;
                import java.util.Set;

                class Test {
                    void method() {
                        A a = new A();
                        a.method(Set.copyOf(new HashSet<>()));
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
                import java.util.HashSet;
                import com.google.common.collect.ImmutableSet;

                class A {
                    Object o = ImmutableSet.copyOf(new HashSet<>());
                }
                """,
              """
                import java.util.HashSet;
                import java.util.Set;

                class A {
                    Object o = Set.copyOf(new HashSet<>());
                }
                """
            ),
            11
          )
        );
    }

    @Test
    void doChangeAssignFromImmutableSetToSet() {
        rewriteRun(
          spec -> spec.recipe(new NoGuavaImmutableSetCopyOf(true)),
          version(
            //language=java
            java(
              """
                import java.util.HashSet;
                import com.google.common.collect.ImmutableSet;

                class A {
                    void test() {
                        ImmutableSet<String> o = ImmutableSet.copyOf(new HashSet<>());
                    }
                }
                """,
              """
                import java.util.HashSet;
                import java.util.Set;

                class A {
                    void test() {
                        Set<String> o = Set.copyOf(new HashSet<>());
                    }
                }
                """
            ),
            11
          )
        );
    }
}
