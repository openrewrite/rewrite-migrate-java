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
package org.openrewrite.java.migrate;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class AddStaticVariableOnProducerSessionBeanTest implements RewriteTest {
    @Language("java")
    private static final String jakarta_produces =
      """
        package jakarta.enterprise.inject;
        public @interface Produces {}
        """;

    @Language("java")
    private static final String jakarta_stateless =
      """
        package jakarta.ejb;
        public @interface Stateless {}
        """;

    @Language("java")
    private static final String someDependency =
      """
        package com.test;
        public class SomeDependency {}
        """;

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AddStaticVariableOnProducerSessionBean())
          .parser(JavaParser.fromJavaVersion()
            .dependsOn(jakarta_produces, jakarta_stateless, someDependency));
    }

    @Test
    @DocumentExample
    void addProducesFieldStaticOnSessionBean() {
        rewriteRun(
          //language=java
          java(
            """
              package com.test;
              import jakarta.ejb.Stateless;
              import jakarta.enterprise.inject.Produces;

              @Stateless
              public class MySessionBean {
                  @Produces
                  private SomeDependency someDependency;
                  void exampleMethod() {
                     return;
                  }
              }
              """,
            """
              package com.test;
              import jakarta.ejb.Stateless;
              import jakarta.enterprise.inject.Produces;

              @Stateless
              public class MySessionBean {
                  @Produces
                  private static SomeDependency someDependency;
                  void exampleMethod() {
                     return;
                  }
              }
              """
          )
        );
    }

    @Test
    void noChangeOnStaticVariable() {
        rewriteRun(
          //language=java
          java(
            """
              package com.test;

              import jakarta.ejb.Stateless;
              import jakarta.enterprise.inject.Produces;

              @Stateless
              public class MySessionBean {
                  @Produces
                  private static SomeDependency someDependency;
              }
              """
          )
        );
    }
}
