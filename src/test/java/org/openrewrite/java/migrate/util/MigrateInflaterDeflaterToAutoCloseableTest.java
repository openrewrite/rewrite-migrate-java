/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.java.migrate.util;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.javaVersion;

class MigrateInflaterDeflaterToAutoCloseableTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipe(new MigrateInflaterDeflaterToAutoCloseable())
          .parser(JavaParser.fromJavaVersion())
          .allSources(s -> s.markers(javaVersion(25)));
    }

    @DocumentExample
    @Test
    void migrateInflaterEndInTryFinally() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.zip.Inflater;

              class Test {
                  public void test(byte[] arg) {
                      Inflater inflater = new Inflater();
                      try {
                          inflater.inflate(arg);
                      } finally {
                          inflater.end();
                      }
                  }
              }
              """,
            """
              import java.util.zip.Inflater;

              class Test {
                  public void test(byte[] arg) {
                      Inflater inflater = new Inflater();
                      try {
                          inflater.inflate(arg);
                      } finally {
                          inflater.close();
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void migrateDeflaterEndInTryFinally() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.zip.Deflater;

              class Test {
                  public void test(byte[] arg) {
                      Deflater deflater = new Deflater();
                      try {
                          deflater.setInput(arg);
                          deflater.finish();
                      } finally {
                          deflater.end();
                      }
                  }
              }
              """,
            """
              import java.util.zip.Deflater;

              class Test {
                  public void test(byte[] arg) {
                      Deflater deflater = new Deflater();
                      try {
                          deflater.setInput(arg);
                          deflater.finish();
                      } finally {
                          deflater.close();
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void migrateInflaterEndDirectCall() {
        rewriteRun(
          //language=java
          java(
            """
              import java.nio.ByteBuffer;
              import java.util.zip.DataFormatException;
              import java.util.zip.Inflater;

              class Test {
                  public void test(ByteBuffer arg) throws DataFormatException {
                      Inflater inflater = new Inflater();
                      inflater.inflate(arg);
                      inflater.end();
                  }
              }
              """,
            """
              import java.nio.ByteBuffer;
              import java.util.zip.DataFormatException;
              import java.util.zip.Inflater;

              class Test {
                  public void test(ByteBuffer arg) throws DataFormatException {
                      Inflater inflater = new Inflater();
                      inflater.inflate(arg);
                      inflater.close();
                  }
              }
              """
          )
        );
    }

    @Test
    void migrateDeflaterEndDirectCall() {
        rewriteRun(
          //language=java
          java(
            """
              import java.nio.ByteBuffer;
              import java.util.zip.DataFormatException;
              import java.util.zip.Deflater;

              class Test {
                  public void test(ByteBuffer arg) throws DataFormatException {
                      Deflater deflater = new Deflater();
                      deflater.deflate(arg);
                      deflater.end();
                  }
              }
              """,
            """
              import java.nio.ByteBuffer;
              import java.util.zip.DataFormatException;
              import java.util.zip.Deflater;

              class Test {
                  public void test(ByteBuffer arg) throws DataFormatException {
                      Deflater deflater = new Deflater();
                      deflater.deflate(arg);
                      deflater.close();
                  }
              }
              """
          )
        );
    }

    @Test
    void migrateBothInflaterAndDeflaterEnd() {
        rewriteRun(
          //language=java
          java(
            """
              import java.nio.ByteBuffer;
              import java.util.zip.DataFormatException;
              import java.util.zip.Deflater;
              import java.util.zip.Inflater;

              class Test {
                  public void test(ByteBuffer arg) {
                      Deflater deflater = new Deflater();
                      Inflater inflater = new Inflater();
                      try {
                          deflater.deflate(arg);
                          inflater.inflate(arg);
                      } catch (DataFormatException e) {
                          throw new RuntimeException(e);
                      } finally {
                          deflater.end();
                          inflater.end();
                      }
                  }
              }
              """,
            """
              import java.nio.ByteBuffer;
              import java.util.zip.DataFormatException;
              import java.util.zip.Deflater;
              import java.util.zip.Inflater;

              class Test {
                  public void test(ByteBuffer arg) {
                      Deflater deflater = new Deflater();
                      Inflater inflater = new Inflater();
                      try {
                          deflater.deflate(arg);
                          inflater.inflate(arg);
                      } catch (DataFormatException e) {
                          throw new RuntimeException(e);
                      } finally {
                          deflater.close();
                          inflater.close();
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void migrateCustomInflaterEndCall() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.zip.Inflater;

              class CustomInflater extends Inflater {
                  void customMethod() {
                  }
              }

              class Test {
                  public void test() {
                      CustomInflater inflater = new CustomInflater();
                      inflater.customMethod();
                      inflater.end();
                  }
              }
              """,
            """
              import java.util.zip.Inflater;

              class CustomInflater extends Inflater {
                  void customMethod() {
                  }
              }

              class Test {
                  public void test() {
                      CustomInflater inflater = new CustomInflater();
                      inflater.customMethod();
                      inflater.close();
                  }
              }
              """
          )
        );
    }

    @Test
    void migrateCustomDeflaterEndCall() {
        rewriteRun(
          //language=java
          java(
            """
              import java.util.zip.Deflater;

              class CustomDeflater extends Deflater {
                  void customMethod() {
                  }
              }

              class Test {
                  public void test() {
                      CustomDeflater deflater = new CustomDeflater();
                      deflater.customMethod();
                      deflater.end();
                  }
              }
              """,
            """
              import java.util.zip.Deflater;

              class CustomDeflater extends Deflater {
                  void customMethod() {
                  }
              }

              class Test {
                  public void test() {
                      CustomDeflater deflater = new CustomDeflater();
                      deflater.customMethod();
                      deflater.close();
                  }
              }
              """
          )
        );
    }

    @Test
    void noChangeOnJava24() {
        rewriteRun(
          spec -> spec.allSources(s -> s.markers(javaVersion(24))),
          //language=java
          java(
            """
              import java.util.zip.Inflater;

              class Test {
                  void test() {
                      Inflater inflater = new Inflater();
                      inflater.end();
                  }
              }
              """
          )
        );
    }
}
