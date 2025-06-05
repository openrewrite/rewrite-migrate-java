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
