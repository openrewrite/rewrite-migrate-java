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
package org.openrewrite.java.migrate.guava;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class NoGuavaFunctionsComposeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipe(new NoGuavaFunctionsCompose())
          .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "guava"));
    }

    @DocumentExample
    @Test
    void replaceFunctionsCompose() {
        //language=java
        rewriteRun(
          java(
            """
              import com.google.common.base.Function;
              import com.google.common.base.Functions;

              class Test {
                  public static void test() {
                      Function<Object, Integer> composed = Functions.compose(new Function<String, Integer>() {
                          @Override
                          public Integer apply(String input) {
                              return input.length();
                          }
                      }, new Function<Object, String>() {
                          @Override
                          public String apply(Object input) {
                              return input.toString();
                          }
                      });
                  }
              }
              """,
            """
              import com.google.common.base.Function;

              class Test {
                  public static void test() {
                      Function<Object, Integer> composed = new Function<String, Integer>() {
                          @Override
                          public Integer apply(String input) {
                              return input.length();
                          }
                      }.compose(new Function<Object, String>() {
                          @Override
                          public String apply(Object input) {
                              return input.toString();
                          }
                      });
                  }
              }
              """
          )
        );
    }
}
