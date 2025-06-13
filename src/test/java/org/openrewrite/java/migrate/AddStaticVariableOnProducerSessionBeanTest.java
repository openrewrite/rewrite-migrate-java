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
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class AddStaticVariableOnProducerSessionBeanTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AddStaticVariableOnProducerSessionBean())
          //language=java
          .parser(JavaParser.fromJavaVersion()
            .dependsOn(
              """
                package jakarta.enterprise.inject;
                public @interface Produces {}
                """,
              """
                package jakarta.ejb;
                public @interface Stateless {}
                """,
              """
                package jakarta.ejb;
                public @interface Stateful {}
                """,
              """
                package jakarta.ejb;
                public @interface Singleton {}
                """,
              """
                package com.test;
                public class SomeDependency {}
                """
            )
          );
    }

    @Test
    @DocumentExample
    void addStaticOnProducesMarkedStateless() {
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
    void addStaticOnProducesMarkedStateful() {
        rewriteRun(
          //language=java
          java(
            """
              package com.test;
              import jakarta.ejb.Stateful;
              import jakarta.enterprise.inject.Produces;

              @Stateful
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
              import jakarta.ejb.Stateful;
              import jakarta.enterprise.inject.Produces;

              @Stateful
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
    void addStaticOnProducesMarkedSingleton() {
        rewriteRun(
          //language=java
          java(
            """
              package com.test;
              import jakarta.ejb.Singleton;
              import jakarta.enterprise.inject.Produces;

              @Singleton
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
              import jakarta.ejb.Singleton;
              import jakarta.enterprise.inject.Produces;

              @Singleton
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
              import jakarta.ejb.Singleton;
              import jakarta.enterprise.inject.Produces;

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
