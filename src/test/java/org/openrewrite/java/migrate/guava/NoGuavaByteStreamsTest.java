/*
 * Copyright 2024 the original author or authors.
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

class NoGuavaByteStreamsTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec
          .recipe(new NoGuavaByteStreamsRecipes())
          .parser(JavaParser.fromJavaVersion().classpath("guava"));
    }

    @DocumentExample
    @Test
    void replace() {
        rewriteRun(
          //language=java
          java(
            """
              import com.google.common.io.ByteStreams;

              import java.io.IOException;
              import java.io.InputStream;
              import java.io.OutputStream;

              class Foo {
                long testInputStreamTransferTo(InputStream from, OutputStream to) throws IOException {
                  return ByteStreams.copy(from, to);
                }
                byte[] testInputStreamReadAllBytes(InputStream from) throws IOException {
                  return ByteStreams.toByteArray(from);
                }
              }
              """,
            """
              import java.io.IOException;
              import java.io.InputStream;
              import java.io.OutputStream;

              class Foo {
                long testInputStreamTransferTo(InputStream from, OutputStream to) throws IOException {
                  return from.transferTo(to);
                }
                byte[] testInputStreamReadAllBytes(InputStream from) throws IOException {
                  return from.readAllBytes();
                }
              }
              """
          )
        );
    }
}
