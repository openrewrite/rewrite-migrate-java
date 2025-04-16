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
package org.openrewrite.java.migrate.nio.file;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class FileToPathTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.migrate.Java8toJava11");
    }

    @Nested
    class BasicConversions {
        @Test
        @DocumentExample
        void getPath() {
            rewriteRun(
              //language=java
              java(
                """
                  import java.io.File;
                  class A {
                      File file = new File("").getPath();
                  }
                  """,
                """
                  import java.nio.file.Path;
                  class A {
                      Path file = Path.of("").toAbsolutePath().toString();
                  }
                  """
              )
            );
        }

        @Test
        void constructorConversion() {
            rewriteRun(
              //language=java
              java(
                """
                  import java.io.File;
                  class A {
                      File file = new File("/path/to/file");
                  }
                  """,
                """
                  import java.nio.file.Path;
                  class A {
                      Path file = Path.of("/path/to/file");
                  }
                  """
              )
            );
        }
    }

    @Nested
    class MethodConversions {
        @Test
        void getAbsolutePath() {
            rewriteRun(
              //language=java
              java(
                """
                  import java.io.File;
                  class A {
                      File file = new File("").getAbsolutePath();
                  }
                  """,
                """
                  import java.nio.file.Path;
                  class A {
                      Path file = Path.of("").getFileName().toString();
                  }
                  """
              )
            );
        }

        @Test
        void lastModified() {
            rewriteRun(
              //language=java
              java(
                """
                  import java.io.File;
                  class A {
                      File file = null;
                      long timestamp = file.lastModified();
                  }
                  """,
                """
                  import java.nio.file.Path;
                  import java.nio.file.Files;
                  import java.util.concurrent.TimeUnit;
                  class A {
                      Path file = null;
                      long timestamp = Files.getLastModifiedTime(file).to(TimeUnit.MILLISECONDS);
                  }
                  """
              )
            );
        }

        @Test
        void existsCheck() {
            rewriteRun(
              //language=java
              java(
                """
                  import java.io.File;
                  class A {
                      boolean exists = new File("").exists();
                  }
                  """,
                """
                  import java.nio.file.Path;
                  import java.nio.file.Files;
                  class A {
                      boolean exists = Files.exists(Path.of(""));
                  }
                  """
              )
            );
        }

        @Test
        void isDirectoryCheck() {
            rewriteRun(
              //language=java
              java(
                """
                  import java.io.File;
                  class A {
                      boolean isDir = new File("").isDirectory();
                  }
                  """,
                """
                  import java.nio.file.Path;
                  import java.nio.file.Files;
                  class A {
                      boolean isDir = Files.isDirectory(Path.of(""));
                  }
                  """
              )
            );
        }
    }

    @Nested
    class CollectionAndArrayHandling {
        @Test
        void field() {
            rewriteRun(
              //language=java
              java(
                """
                  import java.io.File;
                  class A {
                      List<File> files = null;
                  }
                  """,
                """
                  import java.nio.file.Path;
                  class A {
                      List<Path> files = null;
                  }
                  """
              )
            );
        }

        @Test
        void arrayField() {
            rewriteRun(
              //language=java
              java(
                """
                  import java.io.File;
                  class A {
                      File[] files = null;
                  }
                  """,
                """
                  import java.nio.file.Path;
                  class A {
                      Path[] files = null;
                  }
                  """
              )
            );
        }

        @Test
        void methodParameter() {
            rewriteRun(
              //language=java
              java(
                """
                  import java.io.File;
                  class A {
                      void processFiles(List<File> files) {}
                  }
                  """,
                """
                  import java.nio.file.Path;
                  class A {
                      void processFiles(List<Path> files) {}
                  }
                  """
              )
            );
        }
    }

    @Nested
    class ComplexScenarios {
        @Test
        void chainedOperations() {
            rewriteRun(
              //language=java
              java(
                """
                  import java.io.File;
                  class A {
                      String path = new File("").getAbsoluteFile().getParent();
                  }
                  """,
                """
                  import java.nio.file.Path;
                  class A {
                      String path = Path.of("").toAbsolutePath().getParent().toString();
                  }
                  """
              )
            );
        }

        @Test
        void staticMethodCall() {
            rewriteRun(
              //language=java
              java(
                """
                  import java.io.File;
                  class A {
                      void test() {
                          File.listRoots();
                      }
                  }
                  """,
                """
                  import java.nio.file.FileSystems;
                  class A {
                      void test() {
                          FileSystems.getDefault().getRootDirectories();
                      }
                  }
                  """
              )
            );
        }

        @Test
        void inTryWithResources() {
            rewriteRun(
              //language=java
              java(
                """
                  import java.io.File;
                  import java.io.FileInputStream;
                  class A {
                      void test() {
                          try (FileInputStream fis = new FileInputStream(new File("test.txt"))) {
                              // do something
                          }
                      }
                  }
                  """,
                """
                  import java.nio.file.Path;
                  import java.nio.file.Files;
                  class A {
                      void test() {
                          try (var fis = Files.newInputStream(Path.of("test.txt"))) {
                              // do something
                          }
                      }
                  }
                  """
              )
            );
        }
    }

    @Nested
    class EdgeCases {
        @Test
        void nullHandling() {
            rewriteRun(
              //language=java
              java(
                """
                  import java.io.File;
                  class A {
                      File file = null;
                  }
                  """,
                """
                  import java.nio.file.Path;
                  class A {
                      Path file = null;
                  }
                  """
              )
            );
        }

        @Test
        void ternaryOperator() {
            rewriteRun(
              //language=java
              java(
                """
                  import java.io.File;
                  class A {
                      File file = condition ? new File("a") : new File("b");
                  }
                  """,
                """
                  import java.nio.file.Path;
                  class A {
                      Path file = condition ? Path.of("a") : Path.of("b");
                  }
                  """
              )
            );
        }

        @Test
        void lambdaExpression() {
            rewriteRun(
              //language=java
              java(
                """
                  import java.io.File;
                  import java.util.function.Function;
                  class A {
                      Function<String, File> fileMapper = File::new;
                  }
                  """,
                """
                  import java.nio.file.Path;
                  import java.util.function.Function;
                  class A {
                      Function<String, Path> fileMapper = Path::of;
                  }
                  """
              )
            );
        }
    }
}
