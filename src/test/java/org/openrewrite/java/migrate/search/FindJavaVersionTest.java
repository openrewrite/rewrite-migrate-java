package org.openrewrite.java.migrate.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.Tree;
import org.openrewrite.java.marker.JavaVersion;
import org.openrewrite.java.migrate.table.JavaVersionTable;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.java.Assertions.java;

public class FindJavaVersionTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FindJavaVersion());
    }

    @Test
    void test() {
        JavaVersion jv = new JavaVersion(randomId(), "Sam", "Shelter", "17", "8");
        rewriteRun(
          spec -> spec.dataTable(JavaVersionTable.Row.class, rows -> {
              assertThat(rows).containsExactly(
                new JavaVersionTable.Row("17", "8")
              );
          }),
          //language=java
          java("""
            class A {
            }
            """,
            spec -> spec.markers(jv)),
          //language=java
          java("""
            class B {
            }
            """,
            spec -> spec.markers(jv))
        );
    }
}
