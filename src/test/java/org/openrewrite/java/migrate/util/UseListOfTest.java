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

class UseListOfTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UseListOf())
          .allSources(s -> s.markers(javaVersion(10)));
    }

    @DocumentExample
    @Test
    void anonymousArrayListWithAdd() {
        //language=java
        rewriteRun(
          java(
            """
              import java.util.ArrayList;
              import java.util.List;

              class Test {
                  List<String> l = new ArrayList<>() {{
                      add("a");
                      add("b");
                  }};
              }
              """,
            """
              import java.util.List;

              class Test {
                  List<String> l = List.of("a", "b");
              }
              """
          )
        );
    }

    @Test
    void anonymousArrayListInMethod() {
        //language=java
        rewriteRun(
          java(
            """
              import java.util.ArrayList;
              import java.util.List;

              class Test {
                  void foo() {
                      List<String> l = new ArrayList<>() {{
                          add("x");
                          add("y");
                          add("z");
                      }};
                  }
              }
              """,
            """
              import java.util.List;

              class Test {
                  void foo() {
                      List<String> l = List.of("x", "y", "z");
                  }
              }
              """
          )
        );
    }

    @Test
    void anonymousArrayListWithNonStringTypes() {
        //language=java
        rewriteRun(
          java(
            """
              import java.util.ArrayList;
              import java.util.List;

              class Test {
                  private static final String CONST = "constant";

                  void foo() {
                      new ArrayList<>() {{
                          add(CONST);
                          add("literal");
                      }};
                  }
              }
              """,
            """
              import java.util.List;

              class Test {
                  private static final String CONST = "constant";

                  void foo() {
                      List.of(CONST, "literal");
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
              import java.util.ArrayList;
              import java.util.List;

              class Test {
                  List<Integer> l = new ArrayList<>() {{
                      add(1);
                  }};
              }
              """,
            """
              import java.util.List;

              class Test {
                  List<Integer> l = List.of(1);
              }
              """
          )
        );
    }

    @Test
    void doesNotChangeArrayListWithConstructorArgs() {
        //language=java
        rewriteRun(
          java(
            """
              import java.util.ArrayList;
              import java.util.List;

              class Test {
                  List<String> existing = List.of("x");
                  List<String> l = new ArrayList<>(existing) {{
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
              import java.util.ArrayList;
              import java.util.List;

              class Test {
                  List<String> l = new ArrayList<>() {{
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
              import java.util.ArrayList;
              import java.util.List;

              class Test {
                  void process(List<String> list) {}

                  void foo() {
                      process(new ArrayList<>() {{
                          add("a");
                      }});
                  }
              }
              """,
            """
              import java.util.List;

              class Test {
                  void process(List<String> list) {}

                  void foo() {
                      process(List.of("a"));
                  }
              }
              """
          )
        );
    }
}
