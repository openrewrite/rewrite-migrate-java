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
package org.openrewrite.java.migrate.jakarta;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.config.Environment;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class RemovedUIComponentConstantTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion().
            classpathFromResources(new InMemoryExecutionContext(), "jakarta.faces-3.0.3")).
          recipe(Environment.builder().scanRuntimeClasspath("org.openrewrite.java.migrate.jakarta")
            .build()
            .activateRecipes("org.openrewrite.java.migrate.jakarta.RemovedUIComponentConstant"));
    }

    @DocumentExample
    @Test
    void removedUIComponentConstant_1() {
        rewriteRun(
          //language=java
          java(
            """
              import jakarta.faces.component.UIComponent;

              class Bar {
                  void foo() {
                      String str = UIComponent.CURRENT_COMPONENT;
                      String str2 = UIComponent.CURRENT_COMPOSITE_COMPONENT;
                      System.out.println(str);
                      System.out.println(str2);
                  }
              }
              """,
            """
              import jakarta.faces.component.UIComponent;

              class Bar {
                  void foo() {
                      String str = UIComponent.getCurrentComponent();
                      String str2 = UIComponent.getCurrentCompositeComponent();
                      System.out.println(str);
                      System.out.println(str2);
                  }
              }
              """
          )
        );
    }
}
