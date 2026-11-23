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
package org.openrewrite.java.migrate.fastjson;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class MigrateFastjsonWriteJSONStringTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigrateFastjsonWriteJSONString())
          .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "fastjson"));
    }

    @DocumentExample
    @Test
    void utf8() {
        rewriteRun(
          //language=java
          java(
            """
              import com.alibaba.fastjson.JSON;
              import com.alibaba.fastjson.serializer.SerializerFeature;

              import java.io.IOException;
              import java.io.OutputStream;
              import java.nio.charset.Charset;
              import java.nio.charset.StandardCharsets;

              class A {
                  private static final Charset UTF8 = Charset.forName("UTF-8");

                  void write(OutputStream out, Object o) throws IOException {
                      JSON.writeJSONString(out, o);
                      JSON.writeJSONString(out, o, SerializerFeature.PrettyFormat);
                      JSON.writeJSONString(out, StandardCharsets.UTF_8, o);
                      JSON.writeJSONString(out, UTF8, o, SerializerFeature.PrettyFormat);
                  }
              }
              """,
            """
              import com.alibaba.fastjson.JSON;
              import com.alibaba.fastjson.serializer.SerializerFeature;

              import java.io.IOException;
              import java.io.OutputStream;
              import java.nio.charset.Charset;

              class A {
                  private static final Charset UTF8 = Charset.forName("UTF-8");

                  void write(OutputStream out, Object o) throws IOException {
                      JSON.writeTo(out, o);
                      JSON.writeTo(out, o, SerializerFeature.PrettyFormat);
                      JSON.writeTo(out, o);
                      JSON.writeTo(out, o, SerializerFeature.PrettyFormat);
                  }
              }
              """
          )
        );
    }

    @Test
    void anotherCharsetIsLeftAlone() {
        rewriteRun(
          //language=java
          java(
            """
              import com.alibaba.fastjson.JSON;

              import java.io.IOException;
              import java.io.OutputStream;
              import java.nio.charset.Charset;
              import java.nio.charset.StandardCharsets;

              class A {
                  void write(OutputStream out, Object o, Charset charset) throws IOException {
                      JSON.writeJSONString(out, StandardCharsets.ISO_8859_1, o);
                      JSON.writeJSONString(out, charset, o);
                  }
              }
              """
          )
        );
    }

    @Test
    void writer() {
        rewriteRun(
          //language=java
          java(
            """
              import com.alibaba.fastjson.JSON;
              import com.alibaba.fastjson.serializer.SerializerFeature;

              import java.io.IOException;
              import java.io.UncheckedIOException;
              import java.io.Writer;

              class A {
                  void declared(Writer writer, Object o) throws IOException {
                      JSON.writeJSONString(writer, o, SerializerFeature.PrettyFormat);
                  }

                  void caught(Writer writer, Object o) {
                      try {
                          JSON.writeJSONString(writer, o);
                      } catch (IOException e) {
                          throw new UncheckedIOException(e);
                      }
                  }
              }
              """,
            """
              import com.alibaba.fastjson.JSON;
              import com.alibaba.fastjson.serializer.SerializerFeature;

              import java.io.IOException;
              import java.io.UncheckedIOException;
              import java.io.Writer;

              class A {
                  void declared(Writer writer, Object o) throws IOException {
                      writer.write(JSON.toJSONString(o, SerializerFeature.PrettyFormat));
                  }

                  void caught(Writer writer, Object o) {
                      try {
                          writer.write(JSON.toJSONString(o));
                      } catch (IOException e) {
                          throw new UncheckedIOException(e);
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void writerWhereIOExceptionIsNotHandled() {
        rewriteRun(
          //language=java
          java(
            """
              import com.alibaba.fastjson.JSON;

              import java.io.IOException;
              import java.io.Writer;
              import java.util.List;

              class A {
                  void undeclared(Writer writer, Object o) {
                      JSON.writeJSONString(writer, o);
                  }

                  void inLambda(Writer writer, List<Object> list) throws IOException {
                      list.forEach(o -> JSON.writeJSONString(writer, o));
                  }

                  void inFinally(Writer writer, Object o) {
                      try {
                          writer.flush();
                      } catch (IOException e) {
                          throw new RuntimeException(e);
                      } finally {
                          JSON.writeJSONString(writer, o);
                      }
                  }
              }
              """
          )
        );
    }
}
