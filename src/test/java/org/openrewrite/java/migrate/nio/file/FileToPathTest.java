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
        @DocumentExample
        void toPath() {
            rewriteRun(
              //language=java
              java(
                """
                  import java.io.File;
                  import java.nio.file.Path;
                  class A {
                      Path file = new File("").toPath();
                  }
                  """,
                """
                  import java.nio.file.Path;
                  class A {
                      Path file = Path.of("");
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
    @Nested
    class AdditionalCases {

        @Test
        void fileAsMethodReturnType() {
            rewriteRun(
              //language=java
              java(
                """
                  import java.io.File;
                  class A {
                      File getFile() {
                          return new File("example.txt");
                      }
                  }
                  """,
                """
                  import java.nio.file.Path;
                  class A {
                      Path getFile() {
                          return Path.of("example.txt");
                      }
                  }
                  """
              )
            );
        }

        @Test
        void fileAsMethodArgument() {
            rewriteRun(
              //language=java
              java(
                """
                  import java.io.File;
                  class A {
                      void save(File file) {}
                      void test() {
                          save(new File("data.txt"));
                      }
                  }
                  """,
                """
                  import java.nio.file.Path;
                  class A {
                      void save(Path file) {}
                      void test() {
                          save(Path.of("data.txt"));
                      }
                  }
                  """
              )
            );
        }

        @Test
        void castToFile() {
            rewriteRun(
              //language=java
              java(
                """
                  import java.io.File;
                  class A {
                      File file = (File) getFileObject();
                      Object getFileObject() { return null; }
                  }
                  """,
                """
                  import java.nio.file.Path;
                  class A {
                      Path file = (Path) getFileObject();
                      Object getFileObject() { return null; }
                  }
                  """
              )
            );
        }

        @Test
        void fileArrayInitialization() {
            rewriteRun(
              //language=java
              java(
                """
                  import java.io.File;
                  class A {
                      File[] files = new File[] { new File("a"), new File("b") };
                  }
                  """,
                """
                  import java.nio.file.Path;
                  class A {
                      Path[] files = new Path[] { Path.of("a"), Path.of("b") };
                  }
                  """
              )
            );
        }

        @Test
        void legacyApiUsesFile() {
            rewriteRun(
              //language=java
              java(
                """
                  import java.io.File;
                  import java.awt.Desktop;
                  class A {
                      void open() throws Exception {
                          Desktop.getDesktop().open(new File("report.pdf"));
                      }
                  }
                  """,
                """
                  import java.nio.file.Path;
                  import java.awt.Desktop;
                  import java.io.IOException;
                  class A {
                      void open() throws IOException {
                          Desktop.getDesktop().open(Path.of("report.pdf").toFile());
                      }
                  }
                  """
              )
            );
        }

        @Test
        void nestedTernaryConversion() {
            rewriteRun(
              //language=java
              java(
                """
                  import java.io.File;
                  class A {
                      File file = condition1 ? new File("a") : condition2 ? new File("b") : new File("c");
                  }
                  """,
                """
                  import java.nio.file.Path;
                  class A {
                      Path file = condition1 ? Path.of("a") : condition2 ? Path.of("b") : Path.of("c");
                  }
                  """
              )
            );
        }
    }
    @Nested
    class AdvancedScenarios {
        @Test
        void fileInAnnotations() {
            rewriteRun(
              //language=java
              java(
                """
                import java.io.File;
                class A {
                    @Deprecated(since = "1.0", forRemoval = File.separator.equals("/"))
                    void legacyMethod() {}
                }
                """,
                """
                import java.nio.file.FileSystems;
                class A {
                    @Deprecated(since = "1.0", forRemoval = FileSystems.getDefault().getSeparator().equals("/"))
                    void legacyMethod() {}
                }
                """
              )
            );
        }

        @Test
        void complexGenerics() {
            rewriteRun(
              //language=java
              java(
                """
                import java.io.File;
                import java.util.Map;
                class A {
                    Map<String, List<File>> fileMap;
                }
                """,
                """
                import java.nio.file.Path;
                import java.util.Map;
                class A {
                    Map<String, List<Path>> fileMap;
                }
                """
              )
            );
        }

        @Test
        void switchExpression() {
            rewriteRun(
              //language=java
              java(
                """
                import java.io.File;
                class A {
                    String getType(File f) {
                        return switch(f.getName()) {
                            case ".txt" -> "Text";
                            case ".jpg" -> "Image";
                            default -> "Unknown";
                        };
                    }
                }
                """,
                """
                import java.nio.file.Path;
                class A {
                    String getType(Path f) {
                        return switch(f.getFileName().toString()) {
                            case ".txt" -> "Text";
                            case ".jpg" -> "Image";
                            default -> "Unknown";
                        };
                    }
                }
                """
              )
            );
        }

        @Test
        void recordClass() {
            rewriteRun(
              //language=java
              java(
                """
                import java.io.File;
                record FileRecord(File source, File target) {}
                """,
                """
                import java.nio.file.Path;
                record FileRecord(Path source, Path target) {}
                """
              )
            );
        }

        @Test
        void sealedClass() {
            rewriteRun(
              //language=java
              java(
                """
                import java.io.File;
                sealed interface FileSystemEntity permits File, Directory {}
                final class Directory extends File implements FileSystemEntity {
                    Directory(String path) { super(path); }
                }
                """,
                """
                import java.nio.file.Path;
                sealed interface FileSystemEntity permits Path, Directory {}
                final class Directory implements FileSystemEntity {
                    private final Path path;
                    Directory(String path) { this.path = Path.of(path); }
                }
                """
              )
            );
        }

        @Test
        void patternMatching() {
            rewriteRun(
              //language=java
              java(
                """
                import java.io.File;
                class A {
                    void process(Object obj) {
                        if (obj instanceof File f) {
                            System.out.println(f.getName());
                        }
                    }
                }
                """,
                """
                import java.nio.file.Path;
                class A {
                    void process(Object obj) {
                        if (obj instanceof Path p) {
                            System.out.println(p.getFileName());
                        }
                    }
                }
                """
              )
            );
        }

        @Test
        void fileSeparator() {
            rewriteRun(
              //language=java
              java(
                """
                import java.io.File;
                class A {
                    String sep = File.separator;
                }
                """,
                """
                import java.nio.file.FileSystems;
                class A {
                    String sep = FileSystems.getDefault().getSeparator();
                }
                """
              )
            );
        }

        @Test
        void listFiles() {
            rewriteRun(
              //language=java
              java(
                """
                import java.io.File;
                class A {
                    File[] children = new File(".").listFiles();
                }
                """,
                """
                import java.nio.file.Files;
                import java.nio.file.Path;
                import java.util.stream.Stream;
                class A {
                    Path[] children;
                    {
                        try (Stream<Path> paths = Files.list(Path.of("."))) {
                            children = paths.toArray(Path[]::new);
                        }
                    }
                }
                """
              )
            );
        }

        @Test
        void filePermissions() {
            rewriteRun(
              //language=java
              java(
                """
                import java.io.File;
                class A {
                    boolean canWrite = new File(".").canWrite();
                }
                """,
                """
                import java.nio.file.Files;
                import java.nio.file.Path;
                class A {
                    boolean canWrite = Files.isWritable(Path.of("."));
                }
                """
              )
            );
        }

        @Test
        void renameFile() {
            rewriteRun(
              //language=java
              java(
                """
                import java.io.File;
                class A {
                    boolean success = new File("old").renameTo(new File("new"));
                }
                """,
                """
                import java.nio.file.Files;
                import java.nio.file.Path;
                import java.nio.file.StandardCopyOption;
                class A {
                    boolean success;
                    {
                        try {
                            Files.move(Path.of("old"), Path.of("new"), StandardCopyOption.REPLACE_EXISTING);
                            success = true;
                        } catch (Exception e) {
                            success = false;
                        }
                    }
                }
                """
              )
            );
        }

        @Test
        void tempFileCreation() {
            rewriteRun(
              //language=java
              java(
                """
                import java.io.File;
                class A {
                    File temp = File.createTempFile("prefix", ".suffix");
                }
                """,
                """
                import java.nio.file.Files;
                import java.nio.file.Path;
                class A {
                    Path temp = Files.createTempFile("prefix", ".suffix");
                }
                """
              )
            );
        }
    }
}
