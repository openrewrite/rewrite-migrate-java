/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.java.migrate.plexus;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class PlexusFileUtilsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .parser(JavaParser.fromJavaVersion().classpath("commons-io", "plexus-utils"))
          .recipe(new PlexusFileUtilsRecipes());
    }

    @Nested
    class DeleteDirectory {

        @Test
        void deleteDirectoryFullyQualified() {
            rewriteRun(
              //language=java
              java(
                """
                  import java.io.File;
                                
                  class Test {
                      void test() throws Exception {
                          org.codehaus.plexus.util.FileUtils.deleteDirectory("test");
                          File file = new File("test");
                          org.codehaus.plexus.util.FileUtils.deleteDirectory(file);
                      }
                  }
                  """,
                """
                  import org.apache.commons.io.FileUtils;
                                
                  import java.io.File;
                                
                  class Test {
                      void test() throws Exception {
                          FileUtils.deleteDirectory(new File("test"));
                          File file = new File("test");
                          FileUtils.deleteDirectory(file);
                      }
                  }
                  """
              )
            );
        }

        @Test
        @Disabled("Fails to clear out imports")
            // FIXME clear out imports
        void deleteDirectorySimpleImport() {
            rewriteRun(
              //language=java
              java(
                """
                  import org.codehaus.plexus.util.FileUtils;
                                
                  import java.io.File;
                                
                  class Test {
                      void test() throws Exception {
                          FileUtils.deleteDirectory("test");
                      }
                  }
                  """,
                """
                  import org.apache.commons.io.FileUtils;
                                
                  import java.io.File;
                                
                  class Test {
                      void test() throws Exception {
                          FileUtils.deleteDirectory(new File("test"));
                      }
                  }
                  """
              )
            );
        }

        @Test
        void deleteDirectoryRetainedImport() {
            rewriteRun(
              //language=java
              java(
                """
                  import org.codehaus.plexus.util.FileUtils;
                                
                  import java.io.File;
                                
                  class Test {
                      void test() throws Exception {
                          FileUtils.deleteDirectory("test");
                          FileUtils.dirname("/foo/bar");
                      }
                  }
                  """,
                """
                  import org.codehaus.plexus.util.FileUtils;
                                
                  import java.io.File;
                                
                  class Test {
                      void test() throws Exception {
                          org.apache.commons.io.FileUtils.deleteDirectory(new File("test"));
                          FileUtils.dirname("/foo/bar");
                      }
                  }
                  """
              )
            );
        }
    }

    @Nested
    class FileExists {
        @Test
        void fileExists() {
            rewriteRun(
              //language=java
              java(
                """
                  import org.codehaus.plexus.util.FileUtils;
                      
                  class Test {
                      boolean test(String fileName) throws Exception {
                          return FileUtils.fileExists(fileName);
                      }
                  }
                  """,
                """
                  import java.io.File;
                      
                  class Test {
                      boolean test(String fileName) throws Exception {
                          return new File(fileName).exists();
                      }
                  }
                  """
              )
            );
        }
    }
}