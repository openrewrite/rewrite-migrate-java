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

package org.openrewrite.java.migrate.io;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.javaVersion;

class ReplaceFileInOrOutputStreamFinalizeWithCloseTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipe(new ReplaceFileInOrOutputStreamFinalizeWithClose())
          .allSources(s -> s.markers(javaVersion(11)));
    }

    @Test
    @DocumentExample
    void removeFinalizerForFileInputStream() {
        //language=java
        rewriteRun(
          java(
            """
              import java.io.FileInputStream;
              import java.io.IOException;

              class FooBar {
                 public void test() throws IOException {
                     FileInputStream obj = new FileInputStream("foo");
                     obj.finalize();
                 }
              }
              """,
            """
              import java.io.FileInputStream;
              import java.io.IOException;

              class FooBar {
                 public void test() throws IOException {
                     FileInputStream obj = new FileInputStream("foo");
                     obj.close();
                 }
              }
              """
          )
        );
    }

    @Test
    void replaceDirectCall() {
        //language=java
        rewriteRun(
          java(
            """
              import java.io.FileInputStream;
              import java.io.IOException;

              class FooBar {
                 public void test() throws IOException {
                     new FileInputStream("foo").finalize();
                 }
              }
              """,
            """
              import java.io.FileInputStream;
              import java.io.IOException;

              class FooBar {
                 public void test() throws IOException {
                     new FileInputStream("foo").close();
                 }
              }
              """
          )
        );
    }

    @Test
    void replaceOnExtends() {
        //language=java
        rewriteRun(
          java(
            """
              import java.io.FileInputStream;
              import java.io.IOException;

              class FooBar extends FileInputStream {
                 FooBar() throws IOException {
                     super("foo");
                 }
                 public void test() throws IOException {
                     new FooBar().finalize();
                 }
              }
              """,
            """
              import java.io.FileInputStream;
              import java.io.IOException;

              class FooBar extends FileInputStream {
                 FooBar() throws IOException {
                     super("foo");
                 }
                 public void test() throws IOException {
                     new FooBar().close();
                 }
              }
              """
          )
        );
    }

    @Test
    void noChangeOnAnyOtherFinalize() {
        //language=java
        rewriteRun(
          java(
            """
              import java.io.FileInputStream;
              import java.io.IOException;

              class FooBar extends FileInputStream {
                 FooBar() throws IOException {
                     super("foo");
                 }
                 public void test() {
                     new Object().finalize();
                 }
              }
              """
          )
        );
    }

    @Test
    void noChangeWithCloseForFileInputStream() {
        //language=java
        rewriteRun(
          java(
            """
              import java.io.FileInputStream;
              import java.io.IOException;

              class FooBar extends FileInputStream {
                 FooBar() throws IOException {
                     super("foo");
                 }
                 public void test() throws IOException {
                     FileInputStream obj = new FileInputStream("foo");
                     obj.close();
                 }
              }
              """
          )
        );
    }

    @Test
    void noFinalizerUsedForFileInputStream() {
        //language=java
        rewriteRun(
          java(
            """
              import java.io.FileInputStream;
              import java.io.IOException;

              class FooBar {
                 public void test() throws IOException {
                     FileInputStream obj = new FileInputStream("foo");
                     obj.read();
                 }
              }
              """
          )
        );
    }

    @Test
    void removeFinalizerForFileOutputStream() {
        //language=java
        rewriteRun(
          java(
            """
              import java.io.FileOutputStream;
              import java.io.IOException;

              class FooBar {
                 public void test() throws IOException {
                     FileOutputStream obj = new FileOutputStream("foo");
                     obj.finalize();
                 }
              }
              """,
            """
              import java.io.FileOutputStream;
              import java.io.IOException;

              class FooBar {
                 public void test() throws IOException {
                     FileOutputStream obj = new FileOutputStream("foo");
                     obj.close();
                 }
              }
              """
          )
        );
    }
}
