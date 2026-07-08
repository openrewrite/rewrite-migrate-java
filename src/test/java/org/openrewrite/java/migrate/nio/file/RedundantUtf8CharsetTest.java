/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.java.migrate.nio.file;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class RedundantUtf8CharsetTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RedundantUtf8Charset());
    }

    @DocumentExample
    @Test
    void readString() {
        //language=java
        rewriteRun(
          java(
            """
              import java.io.IOException;
              import java.nio.charset.StandardCharsets;
              import java.nio.file.Files;
              import java.nio.file.Path;

              class Test {
                  String read(Path path) throws IOException {
                      return Files.readString(path, StandardCharsets.UTF_8);
                  }
              }
              """,
            """
              import java.io.IOException;
              import java.nio.file.Files;
              import java.nio.file.Path;

              class Test {
                  String read(Path path) throws IOException {
                      return Files.readString(path);
                  }
              }
              """
          )
        );
    }

    @Test
    void writeStringWithOpenOptions() {
        //language=java
        rewriteRun(
          java(
            """
              import java.io.IOException;
              import java.nio.charset.StandardCharsets;
              import java.nio.file.Files;
              import java.nio.file.Path;
              import java.nio.file.StandardOpenOption;

              class Test {
                  void write(Path path, String content) throws IOException {
                      Files.writeString(path, content, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
                  }
              }
              """,
            """
              import java.io.IOException;
              import java.nio.file.Files;
              import java.nio.file.Path;
              import java.nio.file.StandardOpenOption;

              class Test {
                  void write(Path path, String content) throws IOException {
                      Files.writeString(path, content, StandardOpenOption.APPEND);
                  }
              }
              """
          )
        );
    }

    @Test
    void readAllLines() {
        //language=java
        rewriteRun(
          java(
            """
              import java.io.IOException;
              import java.nio.charset.StandardCharsets;
              import java.nio.file.Files;
              import java.nio.file.Path;
              import java.util.List;

              class Test {
                  List<String> read(Path path) throws IOException {
                      return Files.readAllLines(path, StandardCharsets.UTF_8);
                  }
              }
              """,
            """
              import java.io.IOException;
              import java.nio.file.Files;
              import java.nio.file.Path;
              import java.util.List;

              class Test {
                  List<String> read(Path path) throws IOException {
                      return Files.readAllLines(path);
                  }
              }
              """
          )
        );
    }

    @Test
    void newBufferedReaderAndWriter() {
        //language=java
        rewriteRun(
          java(
            """
              import java.io.BufferedReader;
              import java.io.BufferedWriter;
              import java.io.IOException;
              import java.nio.charset.StandardCharsets;
              import java.nio.file.Files;
              import java.nio.file.Path;

              class Test {
                  BufferedReader reader(Path path) throws IOException {
                      return Files.newBufferedReader(path, StandardCharsets.UTF_8);
                  }

                  BufferedWriter writer(Path path) throws IOException {
                      return Files.newBufferedWriter(path, StandardCharsets.UTF_8);
                  }
              }
              """,
            """
              import java.io.BufferedReader;
              import java.io.BufferedWriter;
              import java.io.IOException;
              import java.nio.file.Files;
              import java.nio.file.Path;

              class Test {
                  BufferedReader reader(Path path) throws IOException {
                      return Files.newBufferedReader(path);
                  }

                  BufferedWriter writer(Path path) throws IOException {
                      return Files.newBufferedWriter(path);
                  }
              }
              """
          )
        );
    }

    @Test
    void staticImportedUtf8() {
        //language=java
        rewriteRun(
          java(
            """
              import java.io.IOException;
              import java.nio.file.Files;
              import java.nio.file.Path;

              import static java.nio.charset.StandardCharsets.UTF_8;

              class Test {
                  String read(Path path) throws IOException {
                      return Files.readString(path, UTF_8);
                  }
              }
              """,
            """
              import java.io.IOException;
              import java.nio.file.Files;
              import java.nio.file.Path;

              class Test {
                  String read(Path path) throws IOException {
                      return Files.readString(path);
                  }
              }
              """
          )
        );
    }

    @Test
    void writeLines() {
        //language=java
        rewriteRun(
          java(
            """
              import java.io.IOException;
              import java.nio.charset.StandardCharsets;
              import java.nio.file.Files;
              import java.nio.file.Path;
              import java.util.List;

              class Test {
                  void write(Path path, List<String> lines) throws IOException {
                      Files.write(path, lines, StandardCharsets.UTF_8);
                  }
              }
              """,
            """
              import java.io.IOException;
              import java.nio.file.Files;
              import java.nio.file.Path;
              import java.util.List;

              class Test {
                  void write(Path path, List<String> lines) throws IOException {
                      Files.write(path, lines);
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotChangeNonUtf8Charset() {
        //language=java
        rewriteRun(
          java(
            """
              import java.io.IOException;
              import java.nio.charset.StandardCharsets;
              import java.nio.file.Files;
              import java.nio.file.Path;

              class Test {
                  String read(Path path) throws IOException {
                      return Files.readString(path, StandardCharsets.ISO_8859_1);
                  }
              }
              """
          )
        );
    }
}
