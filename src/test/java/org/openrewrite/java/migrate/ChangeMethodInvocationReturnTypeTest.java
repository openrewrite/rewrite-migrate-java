package org.openrewrite.java.migrate;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class ChangeMethodInvocationReturnTypeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ChangeMethodInvocationReturnType("java.lang.Integer parseInt(String)", "long"));
    }

    @Test
    @DocumentExample
    void replaceVariableAssignment() {
        rewriteRun(
          //language=java
          java(
            """
              class Foo {
                  void bar() {
                      int one = Integer.parseInt("1");
                  }
              }
              """,
            """
              class Foo {
                  void bar() {
                      long one = Integer.parseInt("1");
                  }
              }
              """
          )
        );
    }

    @Test
    void shouldOnlyChangeTargetMethodAssignments() {
        rewriteRun(
          //language=java
          java(
            """
              class Foo {
                  void bar() {
                      int zero = Integer.valueOf("0");
                      int one = Integer.parseInt("1");
                      int two = Integer.valueOf("2");
                  }
              }
              """,
            """
              class Foo {
                  void bar() {
                      int zero = Integer.valueOf("0");
                      long one = Integer.parseInt("1");
                      int two = Integer.valueOf("2");
                  }
              }
              """
          )
        );
    }
}