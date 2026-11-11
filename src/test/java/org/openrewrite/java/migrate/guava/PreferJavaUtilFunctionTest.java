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
import org.openrewrite.Issue;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class PreferJavaUtilFunctionTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipeFromResources("org.openrewrite.java.migrate.guava.PreferJavaUtilFunction")
          .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "guava"));
    }

    @DocumentExample
    @Test
    void changeFunction() {
        rewriteRun(
          //language=java
          java(
            """
              import com.google.common.base.Function;

              class A {
                  Integer length(String s) {
                      Function<String, Integer> length = String::length;
                      return length.apply(s);
                  }
              }
              """,
            """
              import java.util.function.Function;

              class A {
                  Integer length(String s) {
                      Function<String, Integer> length = String::length;
                      return length.apply(s);
                  }
              }
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/436")
    @Test
    void doNotChangeFunctionPassedToGuava() {
        rewriteRun(
          //language=java
          java(
            """
              import com.google.common.base.Function;
              import com.google.common.collect.Lists;
              import java.util.List;

              class A {
                  List<Integer> lengths(List<String> strings) {
                      Function<String, Integer> length = String::length;
                      return Lists.transform(strings, length);
                  }
              }
              """
          )
        );
    }

    @Test
    void composeFunctions() {
        rewriteRun(
          //language=java
          java(
            """
              import com.google.common.base.Function;
              import com.google.common.base.Functions;

              class A {
                  Function<String, Integer> nameLength(Function<String, String> name, Function<String, Integer> length) {
                      return Functions.compose(length, name);
                  }
              }
              """,
            """
              import java.util.function.Function;

              class A {
                  Function<String, Integer> nameLength(Function<String, String> name, Function<String, Integer> length) {
                      return length.compose(name);
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotComposeFunctionsPassedToGuava() {
        rewriteRun(
          //language=java
          java(
            """
              import com.google.common.base.Function;
              import com.google.common.base.Functions;
              import com.google.common.collect.Lists;
              import java.util.List;

              class A {
                  List<Integer> nameLengths(List<String> strings, Function<String, String> name, Function<String, Integer> length) {
                      return Lists.transform(strings, Functions.compose(length, name));
                  }
              }
              """
          )
        );
    }
}
