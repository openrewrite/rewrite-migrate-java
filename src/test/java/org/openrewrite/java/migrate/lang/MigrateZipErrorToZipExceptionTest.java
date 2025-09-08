/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.java.migrate.lang;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.javaVersion;

class MigrateZipErrorToZipExceptionTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipeFromResource("/META-INF/rewrite/java-version-25.yml", "org.openrewrite.java.migrate.MigrateZipErrorToZipException")
          .parser(JavaParser.fromJavaVersion())
          .allSources(s -> s.markers(javaVersion(25)));
    }

    @DocumentExample
    @Test
    void migrateZipErrorInCatchClause() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.zip.ZipError;

              class Test {
                  void test() {
                      try {
                          // Some zip operation
                      } catch (ZipError e) {
                          System.out.println("Zip error occurred: " + e.getMessage());
                      }
                  }
              }
              """,
            """
              import java.util.zip.ZipException;

              class Test {
                  void test() {
                      try {
                          // Some zip operation
                      } catch (ZipException e) {
                          System.out.println("Zip error occurred: " + e.getMessage());
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void migrateZipErrorInThrowsClause() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.zip.ZipError;

              class Test {
                  void processZip() throws ZipError {
                      // Implementation
                  }
              }
              """,
            """
              import java.util.zip.ZipException;

              class Test {
                  void processZip() throws ZipException {
                      // Implementation
                  }
              }
              """
          )
        );
    }

    @Test
    void migrateZipErrorInThrowStatement() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.zip.ZipError;

              class Test {
                  void validateZip() {
                      if (isInvalidZip()) {
                          throw new ZipError("Invalid zip file");
                      }
                  }

                  private boolean isInvalidZip() {
                      return true;
                  }
              }
              """,
            """
              import java.util.zip.ZipException;

              class Test {
                  void validateZip() {
                      if (isInvalidZip()) {
                          throw new ZipException("Invalid zip file");
                      }
                  }

                  private boolean isInvalidZip() {
                      return true;
                  }
              }
              """
          )
        );
    }

    @Test
    void migrateZipErrorInFieldDeclaration() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.zip.ZipError;

              class Test {
                  private ZipError lastError;

                  void setError(ZipError error) {
                      this.lastError = error;
                  }
              }
              """,
            """
              import java.util.zip.ZipException;

              class Test {
                  private ZipException lastError;

                  void setError(ZipException error) {
                      this.lastError = error;
                  }
              }
              """
          )
        );
    }

    @Test
    void migrateZipErrorInGenericType() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.List;
              import java.util.zip.ZipError;

              class Test {
                  List<ZipError> errors;

                  void handleErrors(List<ZipError> zipErrors) {
                      this.errors = zipErrors;
                  }
              }
              """,
            """
              import java.util.List;
              import java.util.zip.ZipException;

              class Test {
                  List<ZipException> errors;

                  void handleErrors(List<ZipException> zipErrors) {
                      this.errors = zipErrors;
                  }
              }
              """
          )
        );
    }

    @Test
    void migrateZipErrorInMultipleCatchClauses() {
        rewriteRun(
          //language=java
          java(
            """
              import java.io.IOException;
              import java.util.zip.ZipError;

              class Test {
                  void test() {
                      try {
                          // Some zip operation
                      } catch (IOException e) {
                          System.out.println("IO error: " + e.getMessage());
                      } catch (ZipError e) {
                          System.out.println("Zip error: " + e.getMessage());
                      }
                  }
              }
              """,
            """
              import java.io.IOException;
              import java.util.zip.ZipException;

              class Test {
                  void test() {
                      try {
                          // Some zip operation
                      } catch (IOException e) {
                          System.out.println("IO error: " + e.getMessage());
                      } catch (ZipException e) {
                          System.out.println("Zip error: " + e.getMessage());
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void noChangeWhenZipErrorNotUsed() {
        rewriteRun(
          //language=java
          java(
            """
              import java.io.IOException;

              class Test {
                  void test() {
                      try {
                          // Some operation
                      } catch (IOException e) {
                          System.out.println("Error: " + e.getMessage());
                      }
                  }
              }
              """
          )
        );
    }
}
