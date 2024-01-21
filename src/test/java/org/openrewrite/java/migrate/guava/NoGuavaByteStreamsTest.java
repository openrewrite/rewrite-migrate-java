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

    @Test
    void replace() {
        rewriteRun(
          //language=java
          java(
            """
              import com.google.common.io.ByteStreams;
              import java.io.ByteArrayInputStream;
              import java.io.ByteArrayOutputStream;
              import java.io.IOException;

              class InputStreamRulesTest {
                long testInputStreamTransferTo() throws IOException {
                  return ByteStreams.copy(new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream());
                }
                byte[] testInputStreamReadAllBytes() throws IOException {
                  return ByteStreams.toByteArray(new ByteArrayInputStream(new byte[0]));
                }
              }
              """,
            """
              import java.io.ByteArrayInputStream;
              import java.io.ByteArrayOutputStream;
              import java.io.IOException;

              class InputStreamRulesTest {
                long testInputStreamTransferTo() throws IOException {
                  return new ByteArrayInputStream(new byte[0]).transferTo(new ByteArrayOutputStream());
                }
                byte[] testInputStreamReadAllBytes() throws IOException {
                  return new ByteArrayInputStream(new byte[0]).readAllBytes();
                }
              }
              """
          )
        );
    }
}
