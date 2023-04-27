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
package org.openrewrite.java.migrate.guava;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

@SuppressWarnings({"UnstableApiUsage", "ResultOfMethodCallIgnored"})
class NoGuavaCreateTempDirTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipe(new NoGuavaCreateTempDir())
          .parser(JavaParser.fromJavaVersion().classpath("guava"));
    }

    @DocumentExample
    @Test
    void inMethodThrowingException() {
        //language=java
        rewriteRun(
          java(
            """
              import java.io.File;
              import java.io.IOException;
              import com.google.common.io.Files;
                            
              class A {
                  void doSomething() throws IOException {
                      File dir = Files.createTempDir();
                      dir.createNewFile();
                  }
                  void doSomethingElse() throws Exception {
                      File dir = Files.createTempDir();
                      dir.createNewFile();
                  }
              }
              """,
            """
              import java.io.File;
              import java.io.IOException;
              import java.nio.file.Files;
                            
              class A {
                  void doSomething() throws IOException {
                      File dir = Files.createTempDirectory(null).toFile();
                      dir.createNewFile();
                  }
                  void doSomethingElse() throws Exception {
                      File dir = Files.createTempDirectory(null).toFile();
                      dir.createNewFile();
                  }
              }
              """
          )
        );
    }

    @Test
    void tempDirIsFieldVar() {
        //language=java
        rewriteRun(
          java(
            """
              import java.io.File;
              import com.google.common.io.Files;
              
              class A {
                  File gDir = Files.createTempDir();
              }
              """
          )
        );
    }

    @Test
    void tempDirIsStaticBlock() {
        //language=java
        rewriteRun(
          java(
            """
              import java.io.File;
              import java.io.IOException;
              import com.google.common.io.Files;
              
              class A {
                  public void doSomething() {
                      try {
                          File dir = Files.createTempDir();
                      } catch (IOException ex) {
                          throw new RuntimeException(ex);
                      }
                  }
                  public void doSomethingElse() {
                      try {
                          File dir = Files.createTempDir();
                      } catch (Exception ex) {
                          throw new RuntimeException(ex);
                      }
                  }
              }
              """,
            """
              import java.io.File;
              import java.io.IOException;
              import java.nio.file.Files;
              
              class A {
                  public void doSomething() {
                      try {
                          File dir = Files.createTempDirectory(null).toFile();
                      } catch (IOException ex) {
                          throw new RuntimeException(ex);
                      }
                  }
                  public void doSomethingElse() {
                      try {
                          File dir = Files.createTempDirectory(null).toFile();
                      } catch (Exception ex) {
                          throw new RuntimeException(ex);
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void tempDirIsInMethodNotThrowingIOException() {
        //language=java
        rewriteRun(
          java(
            """
              import java.io.File;
              import com.google.common.io.Files;
              
              class A {
                  void doSomething() {
                      File gDir = Files.createTempDir();
                  }
              }
              """
          )
        );
    }

    @Test
    void guavaCreateTempDirIsMethodArgument() {
        //language=java
        rewriteRun(
          java(
            """
              import java.io.File;
              import com.google.common.io.Files;
              
              class Test {
                  void someTest() {
                      doSomething(Files.createTempDir(), "some text");
                  }
                  void doSomething(File file, String content) {
                  }
              }
              """
          )
        );
    }
}
