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
package org.openrewrite.java.migrate.jakarta;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.maven.Assertions.pomXml;

class FileuploadToFileUpload2Test implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.migrate.jakarta.FileuploadToFileUpload2")
          .parser(JavaParser.fromJavaVersion().dependsOn(
            "package org.apache.commons.fileupload; public interface FileItem {}",
            "package org.apache.commons.fileupload; public class FileUploadException extends Exception {}",
            "package org.apache.commons.fileupload.disk; public class DiskFileItemFactory {}",
            "package org.apache.commons.fileupload.servlet; public class ServletFileUpload {}",
            "package org.apache.commons.fileupload.servlet; public class ServletRequestContext {}",
            "package org.apache.commons.fileupload.servlet; public class FileCleanerCleanup {}",
            "package org.apache.commons.fileupload2.jakarta; public class JakartaServletFileUpload {}",
            "package org.apache.commons.fileupload2.jakarta.servlet5; public class JakartaServletFileUpload {}"
          ));
    }

    @DocumentExample
    @Test
    void changeCommonsFileuploadToJakartaServlet5() {
        rewriteRun(
          //language=xml
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>demo</artifactId>
                  <version>1.0</version>
                  <dependencies>
                      <dependency>
                          <groupId>commons-fileupload</groupId>
                          <artifactId>commons-fileupload</artifactId>
                          <version>1.5</version>
                      </dependency>
                  </dependencies>
              </project>
              """,
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>demo</artifactId>
                  <version>1.0</version>
                  <dependencies>
                      <dependency>
                          <groupId>org.apache.commons</groupId>
                          <artifactId>commons-fileupload2-jakarta-servlet5</artifactId>
                          <version>2.0.0-M5</version>
                      </dependency>
                  </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void changeCommonsFileupload2JakartaMilestoneToJakartaServlet5() {
        rewriteRun(
          //language=xml
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>demo</artifactId>
                  <version>1.0</version>
                  <dependencies>
                      <dependency>
                          <groupId>org.apache.commons</groupId>
                          <artifactId>commons-fileupload2-jakarta</artifactId>
                          <version>2.0.0-M1</version>
                      </dependency>
                  </dependencies>
              </project>
              """,
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>demo</artifactId>
                  <version>1.0</version>
                  <dependencies>
                      <dependency>
                          <groupId>org.apache.commons</groupId>
                          <artifactId>commons-fileupload2-jakarta-servlet5</artifactId>
                          <version>2.0.0-M5</version>
                      </dependency>
                  </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void migrateFileupload1Types() {
        rewriteRun(
          //language=java
          java(
            """
              import org.apache.commons.fileupload.FileItem;
              import org.apache.commons.fileupload.FileUploadException;
              import org.apache.commons.fileupload.disk.DiskFileItemFactory;
              import org.apache.commons.fileupload.servlet.FileCleanerCleanup;
              import org.apache.commons.fileupload.servlet.ServletFileUpload;
              import org.apache.commons.fileupload.servlet.ServletRequestContext;

              class Upload {
                  FileItem item;
                  FileUploadException exception;
                  DiskFileItemFactory factory;
                  FileCleanerCleanup cleaner;
                  ServletFileUpload upload;
                  ServletRequestContext context;
              }
              """,
            """
              import org.apache.commons.fileupload2.core.FileItem;
              import org.apache.commons.fileupload2.core.FileUploadException;
              import org.apache.commons.fileupload2.core.DiskFileItemFactory;
              import org.apache.commons.fileupload2.jakarta.servlet5.JakartaFileCleaner;
              import org.apache.commons.fileupload2.jakarta.servlet5.JakartaServletFileUpload;
              import org.apache.commons.fileupload2.jakarta.servlet5.JakartaServletRequestContext;

              class Upload {
                  FileItem item;
                  FileUploadException exception;
                  DiskFileItemFactory factory;
                  JakartaFileCleaner cleaner;
                  JakartaServletFileUpload upload;
                  JakartaServletRequestContext context;
              }
              """
          )
        );
    }

    @Test
    void migrateFileupload2JakartaMilestonePackage() {
        rewriteRun(
          //language=java
          java(
            """
              import org.apache.commons.fileupload2.jakarta.JakartaServletFileUpload;

              class Upload {
                  JakartaServletFileUpload upload;
              }
              """,
            """
              import org.apache.commons.fileupload2.jakarta.servlet5.JakartaServletFileUpload;

              class Upload {
                  JakartaServletFileUpload upload;
              }
              """
          )
        );
    }

    @Test
    void leaveJakartaServlet5Untouched() {
        rewriteRun(
          //language=java
          java(
            """
              import org.apache.commons.fileupload2.jakarta.servlet5.JakartaServletFileUpload;

              class Upload {
                  JakartaServletFileUpload upload;
              }
              """
          )
        );
    }
}
