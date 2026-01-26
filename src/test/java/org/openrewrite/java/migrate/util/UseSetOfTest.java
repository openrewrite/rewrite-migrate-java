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
package org.openrewrite.java.migrate.util;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.javaVersion;

class UseSetOfTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UseSetOf())
          .allSources(s -> s.markers(javaVersion(10)));
    }

    @DocumentExample
    @Test
    void anonymousHashSetWithAdd() {
        //language=java
        rewriteRun(
          java(
            """
              import java.util.HashSet;
              import java.util.Set;

              class Test {
                  Set<String> s = new HashSet<>() {{
                      add("a");
                      add("b");
                  }};
              }
              """,
            """
              import java.util.Set;

              class Test {
                  Set<String> s = Set.of("a", "b");
              }
              """
          )
        );
    }

    @Test
    void anonymousHashSetInMethod() {
        //language=java
        rewriteRun(
          java(
            """
              import java.util.HashSet;
              import java.util.Set;

              class Test {
                  void foo() {
                      Set<String> s = new HashSet<>() {{
                          add("x");
                          add("y");
                          add("z");
                      }};
                  }
              }
              """,
            """
              import java.util.Set;

              class Test {
                  void foo() {
                      Set<String> s = Set.of("x", "y", "z");
                  }
              }
              """
          )
        );
    }

    @Test
    void anonymousHashSetWithNonStringTypes() {
        //language=java
        rewriteRun(
          java(
            """
              import java.util.HashSet;
              import java.util.Set;

              class Test {
                  private static final String CONST = "constant";

                  void foo() {
                      new HashSet<>() {{
                          add(CONST);
                          add("literal");
                      }};
                  }
              }
              """,
            """
              import java.util.Set;

              class Test {
                  private static final String CONST = "constant";

                  void foo() {
                      Set.of(CONST, "literal");
                  }
              }
              """
          )
        );
    }

    @Test
    void singleElement() {
        //language=java
        rewriteRun(
          java(
            """
              import java.util.HashSet;
              import java.util.Set;

              class Test {
                  Set<Integer> s = new HashSet<>() {{
                      add(1);
                  }};
              }
              """,
            """
              import java.util.Set;

              class Test {
                  Set<Integer> s = Set.of(1);
              }
              """
          )
        );
    }

    @Test
    void doesNotChangeHashSetWithConstructorArgs() {
        //language=java
        rewriteRun(
          java(
            """
              import java.util.HashSet;
              import java.util.Set;

              class Test {
                  Set<String> existing = Set.of("x");
                  Set<String> s = new HashSet<>(existing) {{
                      add("a");
                  }};
              }
              """
          )
        );
    }

    @Test
    void doesNotChangeWhenContainsNonAddStatements() {
        //language=java
        rewriteRun(
          java(
            """
              import java.util.HashSet;
              import java.util.Set;

              class Test {
                  Set<String> s = new HashSet<>() {{
                      add("a");
                      System.out.println("debug");
                      add("b");
                  }};
              }
              """
          )
        );
    }

    @Test
    void transformsWhenUsedAsMethodArgument() {
        //language=java
        rewriteRun(
          java(
            """
              import java.util.HashSet;
              import java.util.Set;

              class Test {
                  void process(Set<String> set) {}

                  void foo() {
                      process(new HashSet<>() {{
                          add("a");
                      }});
                  }
              }
              """,
            """
              import java.util.Set;

              class Test {
                  void process(Set<String> set) {}

                  void foo() {
                      process(Set.of("a"));
                  }
              }
              """
          )
        );
    }
}
