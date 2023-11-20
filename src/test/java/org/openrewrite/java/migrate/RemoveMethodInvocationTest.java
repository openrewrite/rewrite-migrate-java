package org.openrewrite.java.migrate;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class RemoveMethodInvocationTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveMethodInvocation("java.lang.System getProperty(String)"));
    }

    @Test
    void removeMethodInvocation() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  void test() {
                      System.getProperty("user.dir");
                  }
              }
              """,
            """
              class Test {
                  void test() {
                  }
              }
              """
          )
        );
    }

    @Test
    void mismatchParameters() {
        rewriteRun(
          //language=java
          java(
            """
              class Test {
                  void test() {
                      System.getProperty("user.dir", "/home/user");
                  }
              }
              """
          )
        );
    }
}
