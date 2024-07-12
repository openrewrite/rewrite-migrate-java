package org.openrewrite.java.migrate.jakarta;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.openrewrite.config.Environment;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.*;

public class JavaxTransactionMigrationToJakartaTransactionTest implements RewriteTest {
    @Language("java")
    private static final String javax_transaction =
      """
        package javax.transaction;
        public @interface Transactional {}
        """;

    @Language("java")
    private static final String javax_transaction_xa =
      """
        package javax.transaction.xa;
        public class XAResource {
            public static void stat() {}
            public void foo() {}
        }
        """;

    @Language("java")
    private static final String jakarta_transaction =
      """
        package jakarta.transaction;
        public @interface Transactional {}
        """;

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(
          Environment.builder()
            .scanRuntimeClasspath("org.openrewrite.java.migrate.jakarta")
            .build()
            .activateRecipes("org.openrewrite.java.migrate.jakarta.JavaxTransactionMigrationToJakartaTransaction")
        );
    }

    @Test
    void doNotChangeImportWhenPackageFromJavaSE() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(javax_transaction_xa)),
          //language=java
          java("""
            import javax.transaction.xa.*;
            public class A {
                XAResource xa;
            }
            """)
        );
    }

    @Test
    void changeImportWhenPackageFromJakartaTransaction() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(javax_transaction, jakarta_transaction)),
          //language=java
          java("""
              import javax.transaction.Transactional;
              @Transactional
              public class A {
                  public void foo() {}
              }
              """,
            """
              import jakarta.transaction.Transactional;
              @Transactional
              public class A {
                  public void foo() {}
              }
              """)
        );
    }
}
