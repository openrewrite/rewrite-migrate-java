package org.openrewrite.java.migrate;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.javaVersion;

public class ThreadStopDestroy implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.migrate.ThreadStopDestroy")
          .allSources(src -> src.markers(javaVersion(8)));
    }

    @Test
    @DocumentExample
    void retainCommentIfPresent() {
        rewriteRun(
          //language=java
          java(
            """
              class Foo {
                  void bar() {
                      // I know, I know, but it's a legacy codebase and we're not ready to migrate yet
                      Thread.currentThread().stop();
                  }
              }
              """
          )
        );
    }
}
