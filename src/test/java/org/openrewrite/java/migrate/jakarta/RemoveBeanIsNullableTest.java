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
package org.openrewrite.java.migrate.jakarta;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class RemoveBeanIsNullableTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveBeanIsNullable())
          .parser(JavaParser.fromJavaVersion()
            .classpathFromResources(new InMemoryExecutionContext(), "jakarta.enterprise.cdi-api-3.0.0-M4"));
    }

    @Test
    @DocumentExample
    void removeBeanIsNullable() {
        rewriteRun(
          //language=java
          java(
            """
              import jakarta.enterprise.inject.spi.Bean;
              
              class Test {
                  void test(Bean<?> bean) {
                      if (bean.isNullable()) {
                          System.out.println("is null");
                      } else {
                          System.out.println("not null");
                      }
                  }
              }
              """,
            """
              import jakarta.enterprise.inject.spi.Bean;
              
              class Test {
                  void test(Bean<?> bean) {
                      System.out.println("not null");
                  }
              }
              """
          )
        );
    }
}
