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

    @Test
    void deleteDirectory() {
        rewriteRun(
          spec -> spec.recipes(
            new PlexusFileUtilsRecipes.DeleteDirectoryFileRecipe(),
            new PlexusFileUtilsRecipes.DeleteDirectoryStringRecipe()
          ),
          //language=java
          java(
            """
              import java.io.File;
              import java.io.IOException;
              import org.codehaus.plexus.util.FileUtils;
              class Test {
                  void test() throws IOException {
                      FileUtils.deleteDirectory("test");
                      org.codehaus.plexus.util.FileUtils.deleteDirectory("test");

                      File file = new File("test");
                      FileUtils.deleteDirectory(file);
                      org.codehaus.plexus.util.FileUtils.deleteDirectory(file);

                      FileUtils.dirname("/foo/bar"); // Unused
                  }
              }
              """,
            """
              import java.io.File;
              import java.io.IOException;
              import org.apache.commons.io.FileUtils;
              class Test {
                  void test() throws IOException {
                      FileUtils.deleteDirectory(new File("test"));
                      org.apache.commons.io.FileUtils.deleteDirectory(new File("test"));

                      File file = new File("test");
                      FileUtils.deleteDirectory(file);
                      org.apache.commons.io.FileUtils.deleteDirectory(file);

                      FileUtils.dirname("/foo/bar"); // Unused
                  }
              }
              """
          )
        );
    }

    @Test
    void deleteDirectoryMinimal() {
        rewriteRun(
          spec -> spec.recipes(
            new PlexusFileUtilsRecipes.DeleteDirectoryStringRecipe()
          ),
          //language=java
          java(
            """
              import java.io.File;
              import java.io.IOException;
              class Test {
                  void test() throws IOException {
                      org.codehaus.plexus.util.FileUtils.deleteDirectory("test");
                  }
              }
              """,
            """
              import java.io.File;
              import java.io.IOException;
              class Test {
                  void test() throws IOException {
                      org.apache.commons.io.FileUtils.deleteDirectory(new File("test"));
                  }
              }
              """
          )
        );
    }
}