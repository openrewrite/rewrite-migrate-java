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
package org.openrewrite.java.migrate.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.nio.CharBuffer;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.javaVersion;

class MigrateStringReaderToReaderOfTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigrateStringReaderToReaderOf())
            .parser(JavaParser.fromJavaVersion())
            .allSources(s -> s.markers(javaVersion(25)));
    }

    @DocumentExample
    @Test
    void migrateReaderVariableWithString() {
        rewriteRun(
          //language=java
          java(
            """
              import java.io.Reader;
              import java.io.StringReader;

              class Test {
                  void test(String content) {
                      Reader reader = new StringReader(content);
                  }
              }
              """,
            """
              import java.io.Reader;

              class Test {
                  void test(String content) {
                      Reader reader = Reader.of(content);
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotMigrateStringReaderVariable() {
        rewriteRun(
          //language=java
          java(
            """
              import java.io.StringReader;

              class Test {
                  void test(String content) {
                      StringReader reader = new StringReader(content);
                  }
              }
              """
          )
        );
    }

    @Test
    void migrateMethodReturningReader() {
        rewriteRun(
          //language=java
          java(
            """
              import java.io.Reader;
              import java.io.StringReader;

              class Test {
                  Reader createReader(String content) {
                      return new StringReader(content);
                  }
              }
              """,
            """
              import java.io.Reader;

              class Test {
                  Reader createReader(String content) {
                      return Reader.of(content);
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotMigrateMethodReturningStringReader() {
        rewriteRun(
          //language=java
          java(
            """
              import java.io.StringReader;

              class Test {
                  StringReader createReader(String content) {
                      return new StringReader(content);
                  }
              }
              """
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"StringBuilder", "StringBuffer", "CharBuffer", "CharSequence"})
    void migrateCharSequenceVariants(String className) {
        String extraImport = className.equals("CharBuffer") ? "\nimport java.nio.CharBuffer;" : "";
        rewriteRun(
          //language=java
          java(
            String.format("""
                import java.io.Reader;
                import java.io.StringReader;%s

                class Test {
                    void test(%s cs) {
                        Reader reader = new StringReader(cs.toString());
                    }
                }
                """, extraImport, className),
            String.format("""
                import java.io.Reader;%s

                class Test {
                    void test(%s cs) {
                        Reader reader = Reader.of(cs);
                    }
                }
                """, extraImport, className)
          )
        );
    }

    @Test
    void migrateMultipleReaderVariables() {
        rewriteRun(
          //language=java
          java(
            """
              import java.io.Reader;
              import java.io.StringReader;

              class Test {
                  void test(String s1, String s2) {
                      Reader reader1 = new StringReader(s1);
                      Reader reader2 = new StringReader(s2);
                  }
              }
              """,
            """
              import java.io.Reader;

              class Test {
                  void test(String s1, String s2) {
                      Reader reader1 = Reader.of(s1);
                      Reader reader2 = Reader.of(s2);
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotMigrateAsMethodArgument() {
        rewriteRun(
          //language=java
          java(
            """
              import java.io.BufferedReader;
              import java.io.StringReader;

              class Test {
                  void test(String content) {
                      BufferedReader br = new BufferedReader(new StringReader(content));
                  }
              }
              """
          )
        );
    }

    @Test
    void migrateInTryWithResources() {
        rewriteRun(
          //language=java
          java(
            """
              import java.io.Reader;
              import java.io.StringReader;

              class Test {
                  void test(String content) throws Exception {
                      try (Reader reader = new StringReader(content)) {
                          // use reader
                      }
                  }
              }
              """,
            """
              import java.io.Reader;

              class Test {
                  void test(String content) throws Exception {
                      try (Reader reader = Reader.of(content)) {
                          // use reader
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void migrateWithLiteral() {
        rewriteRun(
          //language=java
          java(
            """
              import java.io.Reader;
              import java.io.StringReader;

              class Test {
                  void test() {
                      Reader reader = new StringReader("Hello World");
                  }
              }
              """,
            """
              import java.io.Reader;

              class Test {
                  void test() {
                      Reader reader = Reader.of("Hello World");
                  }
              }
              """
          )
        );
    }

    @Test
    void migrateReaderFieldAssignment() {
        rewriteRun(
          //language=java
          java(
            """
              import java.io.Reader;
              import java.io.StringReader;

              class Test {
                  private Reader reader;

                  void test(String content) {
                      reader = new StringReader(content);
                  }
              }
              """,
            """
              import java.io.Reader;

              class Test {
                  private Reader reader;

                  void test(String content) {
                      reader = Reader.of(content);
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotMigrateBeforeJava25() {
        rewriteRun(
          spec -> spec.allSources(s -> s.markers(javaVersion(24))),
          //language=java
          java(
            """
              import java.io.Reader;
              import java.io.StringReader;

              class Test {
                  void test(String content) {
                      Reader reader = new StringReader(content);
                  }
              }
              """
          )
        );
    }

    @Test
    void migrateComplexReturn() {
        rewriteRun(
          //language=java
          java(
            """
              import java.io.Reader;
              import java.io.StringReader;

              class Test {
                  Reader getReader(boolean flag, String s1, String s2) {
                      if (flag) {
                          return new StringReader(s1);
                      } else {
                          return new StringReader(s2);
                      }
                  }
              }
              """,
            """
              import java.io.Reader;

              class Test {
                  Reader getReader(boolean flag, String s1, String s2) {
                      if (flag) {
                          return Reader.of(s1);
                      } else {
                          return Reader.of(s2);
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotMigrateLambdaReturn() {
        rewriteRun(
          //language=java
          java(
            """
              import java.io.Reader;
              import java.io.StringReader;
              import java.util.function.Function;

              class Test {
                  Function<String, Reader> factory = s -> new StringReader(s);
              }
              """
          )
        );
    }

    @Test
    void doNotMigrateMethodReference() {
        rewriteRun(
          //language=java
          java(
            """
              import java.io.Reader;
              import java.io.StringReader;
              import java.util.function.Function;

              class Test {
                  Function<String, Reader> factory = StringReader::new;
              }
              """
          )
        );
    }
}
