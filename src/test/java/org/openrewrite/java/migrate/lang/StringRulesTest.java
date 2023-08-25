package org.openrewrite.java.migrate.lang;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class StringRulesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new StringRulesRecipes());
    }

    @Test
    @SuppressWarnings("StringOperationCanBeSimplified")
    void substring() {
        rewriteRun(
          java(
            """
              class Test {
                  String s1 = "hello".substring(0, "hello".length());
                  String s2 = "hello".substring(0);
              }
              """,
            """
              class Test {
                  String s1 = "hello";
                  String s2 = "hello";
              }
              """
          )
        );
    }

    @Test
    @SuppressWarnings("StringOperationCanBeSimplified")
    void indexOf() {
        rewriteRun(
          java(
            """
              class Test {
                  int i1 = "hello".indexOf("hello", 0);
                  int i2 = "hello".indexOf("hello");
                  int i3 = "hello".indexOf('h', 0);
                  int i4 = "hello".indexOf('h');
              }
              """,
            """
              class Test {
                  int i1 = "hello".indexOf("hello");
                  int i2 = "hello".indexOf("hello");
                  int i3 = "hello".indexOf('h');
                  int i4 = "hello".indexOf('h');
              }
              """
          )
        );
    }

}