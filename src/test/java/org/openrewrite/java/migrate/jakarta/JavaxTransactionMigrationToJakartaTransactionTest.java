/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.java.migrate.jakarta;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.config.Environment;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class JavaxTransactionMigrationToJakartaTransactionTest implements RewriteTest {
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

    @DocumentExample
    @Test
    void changeImportWhenPackageFromJakartaTransaction() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(javax_transaction, jakarta_transaction)),
          //language=java
          java(
            """
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
              """
          )
        );
    }

    @Test
    void doNotChangeImportWhenPackageFromJavaSE() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().dependsOn(javax_transaction_xa)),
          //language=java
          java(
            """
              import javax.transaction.xa.*;
              public class A {
                  XAResource xa;
              }
              """
          )
        );
    }
}
