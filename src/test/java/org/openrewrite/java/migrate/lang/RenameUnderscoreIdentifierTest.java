/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.java.migrate.lang;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.RenameVariable;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.javaVersion;

class RenameUnderscoreIdentifierTest implements RewriteTest {

    /**
     * Setup recipe that renames {@code UNDERSCORE} to {@code _} in the LST,
     * simulating Java 8 source code that uses {@code _} as an identifier.
     * This is necessary because the Java 9+ parser cannot parse {@code _} as a regular identifier.
     */
    private static Recipe renameToUnderscore() {
        return new Recipe() {
            @Override
            public String getDisplayName() {
                return "Rename UNDERSCORE to _";
            }

            @Override
            public String getDescription() {
                return "Test setup recipe.";
            }

            @Override
            public TreeVisitor<?, ExecutionContext> getVisitor() {
                return new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.VariableDeclarations visitVariableDeclarations(
                            J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                        for (J.VariableDeclarations.NamedVariable v : multiVariable.getVariables()) {
                            if ("UNDERSCORE".equals(v.getSimpleName())) {
                                doAfterVisit(new RenameVariable<>(v, "_"));
                            }
                        }
                        return super.visitVariableDeclarations(multiVariable, ctx);
                    }

                    @Override
                    public J.MethodDeclaration visitMethodDeclaration(
                            J.MethodDeclaration method, ExecutionContext ctx) {
                        method = super.visitMethodDeclaration(method, ctx);
                        if ("UNDERSCORE".equals(method.getSimpleName())) {
                            JavaType.Method type = method.getMethodType();
                            if (type != null) {
                                type = type.withName("_");
                            }
                            method = method.withName(method.getName().withSimpleName("_")
                                            .withType(type))
                                    .withMethodType(type);
                        }
                        return method;
                    }

                    @Override
                    public J.MethodInvocation visitMethodInvocation(
                            J.MethodInvocation method, ExecutionContext ctx) {
                        method = super.visitMethodInvocation(method, ctx);
                        if ("UNDERSCORE".equals(method.getSimpleName())) {
                            JavaType.Method type = method.getMethodType();
                            if (type != null) {
                                type = type.withName("_");
                            }
                            method = method.withName(method.getName().withSimpleName("_")
                                            .withType(type))
                                    .withMethodType(type);
                        }
                        return method;
                    }
                };
            }
        };
    }

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipes(renameToUnderscore(), new RenameUnderscoreIdentifier())
          .allSources(s -> s.markers(javaVersion(8)));
    }

    @DocumentExample
    @Test
    void localVariable() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  int test() {
                      int UNDERSCORE = 1;
                      return UNDERSCORE;
                  }
              }
              """,
            """
              class Test {
                  int test() {
                      int __ = 1;
                      return __;
                  }
              }
              """
          )
        );
    }

    @Test
    void methodParameter() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  void foo(int UNDERSCORE) {
                      System.out.println(UNDERSCORE);
                  }
              }
              """,
            """
              class Test {
                  void foo(int __) {
                      System.out.println(__);
                  }
              }
              """
          )
        );
    }

    @Test
    void instanceField() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  private int UNDERSCORE;
                  void set(int value) {
                      this.UNDERSCORE = value;
                  }
                  int get() {
                      return UNDERSCORE;
                  }
              }
              """,
            """
              class Test {
                  private int __;
                  void set(int value) {
                      this.__ = value;
                  }
                  int get() {
                      return __;
                  }
              }
              """
          )
        );
    }

    @Test
    void methodName() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  void UNDERSCORE() {
                  }
                  void caller() {
                      UNDERSCORE();
                  }
              }
              """,
            """
              class Test {
                  void __() {
                  }
                  void caller() {
                      __();
                  }
              }
              """
          )
        );
    }

    @Test
    void noChangeOnJava9() {
        rewriteRun(
          spec -> spec.recipe(new RenameUnderscoreIdentifier())
            .allSources(s -> s.markers(javaVersion(9))),
          //language=java
          java(
            """
              class Test {
                  int _foo = 1;
              }
              """
          )
        );
    }

    @Test
    void noChangeForUnrelatedIdentifiers() {
        rewriteRun(
          spec -> spec.recipe(new RenameUnderscoreIdentifier()),
          //language=java
          java(
            """
              class Test {
                  int _foo = 1;
                  int foo_ = 2;
                  int my_value = 3;
              }
              """
          )
        );
    }

    @Test
    void noChangeForDoubleUnderscore() {
        rewriteRun(
          spec -> spec.recipe(new RenameUnderscoreIdentifier()),
          //language=java
          java(
            """
              class Test {
                  int __ = 1;
              }
              """
          )
        );
    }

    @Test
    void catchParameter() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  void test() {
                      try {
                          Integer.parseInt("x");
                      } catch (NumberFormatException UNDERSCORE) {
                          System.out.println("error");
                      }
                  }
              }
              """,
            """
              class Test {
                  void test() {
                      try {
                          Integer.parseInt("x");
                      } catch (NumberFormatException __) {
                          System.out.println("error");
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void lambdaParameter() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.stream.Stream;
              import java.util.stream.Collectors;

              class Test {
                  void test() {
                      Stream.of("a", "b")
                          .collect(Collectors.toMap(String::toUpperCase, UNDERSCORE -> "val"));
                  }
              }
              """,
            """
              import java.util.stream.Stream;
              import java.util.stream.Collectors;

              class Test {
                  void test() {
                      Stream.of("a", "b")
                          .collect(Collectors.toMap(String::toUpperCase, __ -> "val"));
                  }
              }
              """
          )
        );
    }

    @Test
    void forEachLoopVariable() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.List;

              class Test {
                  int count(List<String> items) {
                      int total = 0;
                      for (String UNDERSCORE : items) {
                          total++;
                      }
                      return total;
                  }
              }
              """,
            """
              import java.util.List;

              class Test {
                  int count(List<String> items) {
                      int total = 0;
                      for (String __ : items) {
                          total++;
                      }
                      return total;
                  }
              }
              """
          )
        );
    }
}
