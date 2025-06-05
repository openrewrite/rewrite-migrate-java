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

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class AddStaticVariableOnProducerSessionBeanTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AddStaticVariableOnProducerSessionBean());
    }

    @Test
    @DocumentExample
    void addProducesFieldStaticOnSessionBean() {
        rewriteRun(
          //language=java
          java(
            """
              package com.test;
              @interface Stateless {}
              @interface Produces {}

              class SomeDependency {}
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
              @interface Stateless {}
              @interface Produces {}

              class SomeDependency {}
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
              import javax.ejb.Singleton;
              import javax.enterprise.inject.Produces;

              @Singleton
              public class MySessionBean {
                  @Produces
                  private static SomeDependency someDependency;
              }
              """
          )
        );
    }
}
