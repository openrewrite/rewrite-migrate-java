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
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class ApacheFileUtilsToJavaFilesTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipe(new ApacheFileUtilsToJavaFiles())
          .parser(JavaParser.fromJavaVersion()
            .classpath("commons-io"));
    }

    @Test
    void convertTest() {
        rewriteRun(
          java("""
                  import java.io.File;
                  import java.nio.charset.Charset;
                  import org.apache.commons.io.FileUtils;
                  import java.util.List;
                  
                  class A {
                      byte[] readFileBytes(File file) {
                          return FileUtils.readFileToByteArray(file);
                      }
                      List<String> readLines(File file) {
                          return FileUtils.readLines(file);
                      }
                      List<String> readLinesWithCharset(File file, Charset charset) {
                          return FileUtils.readLines(file, charset);
                      }
                      List<String> readLinesWithCharsetId(File file) {
                          return FileUtils.readLines(file, "UTF_8");
                      }
                  }
              """,
            """
                  import java.io.File;
                  import java.nio.charset.Charset;
                  import java.nio.file.Files;
                  
                  import java.util.List;
                  
                  class A {
                      byte[] readFileBytes(File file) {
                          return Files.readAllBytes(file.toPath());
                      }
                      List<String> readLines(File file) {
                          return Files.readAllLines(file.toPath());
                      }
                      List<String> readLinesWithCharset(File file, Charset charset) {
                          return Files.readAllLines(file.toPath(), charset);
                      }
                      List<String> readLinesWithCharsetId(File file) {
                          return Files.readAllLines(file.toPath(), Charset.forName("UTF_8"));
                      }
                  }
              """));
    }
}
