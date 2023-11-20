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