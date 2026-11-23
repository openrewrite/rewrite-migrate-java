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
package org.openrewrite.java.migrate.jersey;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class Jersey1Base64ToJavaUtilBase64Test implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new Jersey1Base64ToJavaUtilBase64())
          .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "jersey-core-1"));
    }

    @DocumentExample
    @Test
    void encodeAndDecode() {
        rewriteRun(
          java(
            """
              import com.sun.jersey.core.util.Base64;

              class Test {
                  String authorization(String user, String password) {
                      return "Basic " + new String(Base64.encode(user + ":" + password));
                  }
                  byte[] encodeBytes(byte[] bytes) {
                      return Base64.encode(bytes);
                  }
                  byte[] decodeBytes(byte[] bytes) {
                      return Base64.decode(bytes);
                  }
                  byte[] decodeString(String s) {
                      return Base64.decode(s);
                  }
                  String decodeToString(String s) {
                      return Base64.base64Decode(s);
                  }
              }
              """,
            """
              import java.nio.charset.StandardCharsets;
              import java.util.Base64;

              class Test {
                  String authorization(String user, String password) {
                      return "Basic " + new String(Base64.getEncoder().encode((user + ":" + password).getBytes()));
                  }
                  byte[] encodeBytes(byte[] bytes) {
                      return Base64.getEncoder().encode(bytes);
                  }
                  byte[] decodeBytes(byte[] bytes) {
                      return Base64.getDecoder().decode(bytes);
                  }
                  byte[] decodeString(String s) {
                      return Base64.getDecoder().decode(s);
                  }
                  String decodeToString(String s) {
                      return new String(Base64.getMimeDecoder().decode(s), StandardCharsets.ISO_8859_1);
                  }
              }
              """
          )
        );
    }

    @Test
    void qualifiesWhileTheJerseyClassIsStillNeeded() {
        rewriteRun(
          java(
            """
              import com.sun.jersey.core.util.Base64;

              class Test {
                  byte[] decode(String s) {
                      return Base64.isBase64(s) ? Base64.decode(s) : null;
                  }
              }
              """,
            """
              import com.sun.jersey.core.util.Base64;

              class Test {
                  byte[] decode(String s) {
                      return Base64.isBase64(s) ? java.util.Base64.getDecoder().decode(s) : null;
                  }
              }
              """
          )
        );
    }

    @Test
    void fullyQualifiedJerseyClass() {
        rewriteRun(
          java(
            """
              class Test {
                  byte[] encode(byte[] bytes) {
                      return com.sun.jersey.core.util.Base64.encode(bytes);
                  }
              }
              """,
            """
              import java.util.Base64;

              class Test {
                  byte[] encode(byte[] bytes) {
                      return Base64.getEncoder().encode(bytes);
                  }
              }
              """
          )
        );
    }

    @Test
    void staticImport() {
        rewriteRun(
          java(
            """
              import static com.sun.jersey.core.util.Base64.encode;

              class Test {
                  byte[] encodeBytes(byte[] bytes) {
                      return encode(bytes);
                  }
              }
              """,
            """
              import java.util.Base64;

              class Test {
                  byte[] encodeBytes(byte[] bytes) {
                      return Base64.getEncoder().encode(bytes);
                  }
              }
              """
          )
        );
    }
}
