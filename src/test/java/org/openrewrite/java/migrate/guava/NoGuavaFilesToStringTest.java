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
package org.openrewrite.java.migrate.guava;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Issue;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

@Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/91")
class NoGuavaFilesToStringTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipe(new NoGuavaFilesToString())
          .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "guava"));
    }

    @DocumentExample
    @Test
    void filesToString() {
        rewriteRun(
          //language=java
          java(
            """
              import com.google.common.io.Files;

              import java.io.File;
              import java.io.IOException;
              import java.nio.charset.StandardCharsets;

              class A {
                  String read(File file) throws IOException {
                      return Files.toString(file, StandardCharsets.UTF_8);
                  }
              }
              """,
            """
              import java.io.File;
              import java.io.IOException;
              import java.nio.charset.StandardCharsets;
              import java.nio.file.Files;

              class A {
                  String read(File file) throws IOException {
                      return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                  }
              }
              """
          )
        );
    }

    @Test
    void charSourceRead() {
        rewriteRun(
          //language=java
          java(
            """
              import com.google.common.io.Files;

              import java.io.File;
              import java.io.IOException;
              import java.nio.charset.StandardCharsets;

              class A {
                  String read(File file) throws IOException {
                      return Files.asCharSource(file, StandardCharsets.UTF_8).read();
                  }
              }
              """,
            """
              import java.io.File;
              import java.io.IOException;
              import java.nio.charset.StandardCharsets;
              import java.nio.file.Files;

              class A {
                  String read(File file) throws IOException {
                      return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                  }
              }
              """
          )
        );
    }

    @Test
    void qualifyWhenGuavaFilesStaysImported() {
        rewriteRun(
          //language=java
          java(
            """
              import com.google.common.io.Files;

              import java.io.File;
              import java.io.IOException;
              import java.nio.charset.StandardCharsets;

              class A {
                  String read(File file) throws IOException {
                      Files.createParentDirs(file);
                      return Files.toString(file, StandardCharsets.UTF_8);
                  }
              }
              """,
            """
              import com.google.common.io.Files;

              import java.io.File;
              import java.io.IOException;
              import java.nio.charset.StandardCharsets;

              class A {
                  String read(File file) throws IOException {
                      Files.createParentDirs(file);
                      return new String(java.nio.file.Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                  }
              }
              """
          )
        );
    }
}
