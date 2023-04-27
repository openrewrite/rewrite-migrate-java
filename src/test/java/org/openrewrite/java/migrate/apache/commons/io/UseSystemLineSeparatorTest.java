/*
 * Copyright 2021 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.migrate.apache.commons.io;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.config.Environment;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class UseSystemLineSeparatorTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipe(Environment.builder()
            .scanRuntimeClasspath("org.openrewrite.java.migrate.apache.commons.io")
            .build()
            .activateRecipes("org.openrewrite.java.migrate.apache.commons.io.UseSystemLineSeparator"))
          .parser(JavaParser.fromJavaVersion().classpath("commons-io"));
    }


    @DocumentExample
    @Test
    void migratesQualifiedField() {
        //language=java
        rewriteRun(
          java(
            """
              import org.apache.commons.io.IOUtils;

              class A {
                  static String lineSeparator() {
                      return IOUtils.LINE_SEPARATOR;
                  }
              }
              """,
            """
              class A {
                  static String lineSeparator() {
                      return System.lineSeparator();
                  }
              }
              """
)
);
    }

    @Test
    void migratesStaticImportedField() {
        //language=java
        rewriteRun(
          java(
            """
              import static org.apache.commons.io.IOUtils.LINE_SEPARATOR;

              class A {
                  static String lineSeparator() {
                      return LINE_SEPARATOR;
                  }
              }
              """,
            """
              class A {
                  static String lineSeparator() {
                      return System.lineSeparator();
                  }
              }
              """
          )
        );
    }

    @Test
    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/54")
    void migratesFieldInitializer() {
        //language=java
        rewriteRun(
          java(
            """
              import org.apache.commons.io.IOUtils;

              class A {
                  private final String LINE_SEPARATOR_AND_INDENTATION = IOUtils.LINE_SEPARATOR;
              }
              """,
            """
              class A {
                  private final String LINE_SEPARATOR_AND_INDENTATION = System.lineSeparator();
              }
              """
          )
        );
    }

    @Test
    void ignoreUnrelatedFields() {
        //language=java
        rewriteRun(
          java(
            """
              class A {
                  private static final String LINE_SEPARATOR = System.lineSeparator();

                  static String lineSeparator() {
                      return LINE_SEPARATOR;
                  }
              }
              """
          )
        );
    }
}
