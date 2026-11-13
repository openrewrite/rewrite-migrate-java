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
package org.openrewrite.java.migrate.twilio;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.kotlin.KotlinParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.kotlin.Assertions.kotlin;

class TwilioJwtSecretToByteArrayTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new TwilioJwtSecretToByteArray())
          .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "twilio"));
    }

    @DocumentExample
    @Test
    void accessTokenSecret() {
        rewriteRun(
          //language=java
          java(
            """
              import com.twilio.jwt.accesstoken.AccessToken;

              class Tokens {
                  String mint(String accountSid, String apiKey, String apiSecret) {
                      return new AccessToken.Builder(accountSid, apiKey, apiSecret).identity("alice").build().toJwt();
                  }
              }
              """,
            """
              import com.twilio.jwt.accesstoken.AccessToken;

              import java.nio.charset.StandardCharsets;

              class Tokens {
                  String mint(String accountSid, String apiKey, String apiSecret) {
                      return new AccessToken.Builder(accountSid, apiKey, apiSecret.getBytes(StandardCharsets.UTF_8)).identity("alice").build().toJwt();
                  }
              }
              """
          )
        );
    }

    @Test
    void clientCapabilityAuthToken() {
        rewriteRun(
          //language=java
          java(
            """
              import com.twilio.jwt.client.ClientCapability;

              class Tokens {
                  ClientCapability capability(String accountSid, String authToken) {
                      return new ClientCapability.Builder(accountSid, authToken).build();
                  }
              }
              """,
            """
              import com.twilio.jwt.client.ClientCapability;

              import java.nio.charset.StandardCharsets;

              class Tokens {
                  ClientCapability capability(String accountSid, String authToken) {
                      return new ClientCapability.Builder(accountSid, authToken.getBytes(StandardCharsets.UTF_8)).build();
                  }
              }
              """
          )
        );
    }

    @Test
    void taskRouterCapabilityAuthTokenIsNotTheLastArgument() {
        rewriteRun(
          //language=java
          java(
            """
              import com.twilio.jwt.taskrouter.TaskRouterCapability;

              class Tokens {
                  TaskRouterCapability capability(String accountSid, String authToken, String workspaceSid, String channelId) {
                      return new TaskRouterCapability.Builder(accountSid, authToken, workspaceSid, channelId).build();
                  }
              }
              """,
            """
              import com.twilio.jwt.taskrouter.TaskRouterCapability;

              import java.nio.charset.StandardCharsets;

              class Tokens {
                  TaskRouterCapability capability(String accountSid, String authToken, String workspaceSid, String channelId) {
                      return new TaskRouterCapability.Builder(accountSid, authToken.getBytes(StandardCharsets.UTF_8), workspaceSid, channelId).build();
                  }
              }
              """
          )
        );
    }

    @Test
    void keepsTheLayoutOfArgumentsOnTheirOwnLines() {
        rewriteRun(
          //language=java
          java(
            """
              import com.twilio.jwt.accesstoken.AccessToken;

              class Tokens {
                  AccessToken.Builder builder() {
                      return new AccessToken.Builder(
                              System.getProperty("ACCOUNT_SID"),
                              System.getProperty("API_KEY"),
                              System.getProperty("API_SECRET")
                      ).identity("alice");
                  }
              }
              """,
            """
              import com.twilio.jwt.accesstoken.AccessToken;

              import java.nio.charset.StandardCharsets;

              class Tokens {
                  AccessToken.Builder builder() {
                      return new AccessToken.Builder(
                              System.getProperty("ACCOUNT_SID"),
                              System.getProperty("API_KEY"),
                              System.getProperty("API_SECRET").getBytes(StandardCharsets.UTF_8)
                      ).identity("alice");
                  }
              }
              """
          )
        );
    }

    @Test
    void concatenationKeepsItsPrecedence() {
        rewriteRun(
          //language=java
          java(
            """
              import com.twilio.jwt.accesstoken.AccessToken;

              class Tokens {
                  AccessToken.Builder builder(String sid, String key, String head, String tail) {
                      return new AccessToken.Builder(sid, key, head + tail);
                  }
              }
              """,
            """
              import com.twilio.jwt.accesstoken.AccessToken;

              import java.nio.charset.StandardCharsets;

              class Tokens {
                  AccessToken.Builder builder(String sid, String key, String head, String tail) {
                      return new AccessToken.Builder(sid, key, (head + tail).getBytes(StandardCharsets.UTF_8));
                  }
              }
              """
          )
        );
    }

    @Test
    void ternaryKeepsItsPrecedence() {
        rewriteRun(
          //language=java
          java(
            """
              import com.twilio.jwt.accesstoken.AccessToken;

              class Tokens {
                  AccessToken.Builder builder(String sid, String key, boolean live, String a, String b) {
                      return new AccessToken.Builder(sid, key, live ? a : b);
                  }
              }
              """,
            """
              import com.twilio.jwt.accesstoken.AccessToken;

              import java.nio.charset.StandardCharsets;

              class Tokens {
                  AccessToken.Builder builder(String sid, String key, boolean live, String a, String b) {
                      return new AccessToken.Builder(sid, key, (live ? a : b).getBytes(StandardCharsets.UTF_8));
                  }
              }
              """
          )
        );
    }

    @Test
    void methodCallAndLiteralNeedNoParentheses() {
        rewriteRun(
          //language=java
          java(
            """
              import com.twilio.jwt.accesstoken.AccessToken;

              class Tokens {
                  AccessToken.Builder fromEnvironment(String sid, String key) {
                      return new AccessToken.Builder(sid, key, System.getenv("TWILIO_API_SECRET"));
                  }

                  AccessToken.Builder fromLiteral(String sid, String key) {
                      return new AccessToken.Builder(sid, key, "abcdefghijklmnopqrstuvwxyz012345");
                  }
              }
              """,
            """
              import com.twilio.jwt.accesstoken.AccessToken;

              import java.nio.charset.StandardCharsets;

              class Tokens {
                  AccessToken.Builder fromEnvironment(String sid, String key) {
                      return new AccessToken.Builder(sid, key, System.getenv("TWILIO_API_SECRET").getBytes(StandardCharsets.UTF_8));
                  }

                  AccessToken.Builder fromLiteral(String sid, String key) {
                      return new AccessToken.Builder(sid, key, "abcdefghijklmnopqrstuvwxyz012345".getBytes(StandardCharsets.UTF_8));
                  }
              }
              """
          )
        );
    }

    @Test
    void reusesAnExistingImport() {
        rewriteRun(
          //language=java
          java(
            """
              import com.twilio.jwt.accesstoken.AccessToken;

              import java.nio.charset.StandardCharsets;

              class Tokens {
                  byte[] utf8 = "x".getBytes(StandardCharsets.UTF_8);

                  AccessToken.Builder builder(String sid, String key, String secret) {
                      return new AccessToken.Builder(sid, key, secret);
                  }
              }
              """,
            """
              import com.twilio.jwt.accesstoken.AccessToken;

              import java.nio.charset.StandardCharsets;

              class Tokens {
                  byte[] utf8 = "x".getBytes(StandardCharsets.UTF_8);

                  AccessToken.Builder builder(String sid, String key, String secret) {
                      return new AccessToken.Builder(sid, key, secret.getBytes(StandardCharsets.UTF_8));
                  }
              }
              """
          )
        );
    }

    @Test
    void nullSecretCompilesAsItIs() {
        rewriteRun(
          //language=java
          java(
            """
              import com.twilio.jwt.accesstoken.AccessToken;

              class Tokens {
                  AccessToken.Builder builder(String sid, String key) {
                      return new AccessToken.Builder(sid, key, null);
                  }
              }
              """
          )
        );
    }

    @Test
    void kotlinIsLeftForTheCompilerToPointOut() {
        rewriteRun(
          spec -> spec.parser(KotlinParser.builder().classpathFromResources(new InMemoryExecutionContext(), "twilio")),
          //language=kotlin
          kotlin(
            """
              import com.twilio.jwt.accesstoken.AccessToken

              fun builder(sid: String, key: String, secret: String): AccessToken.Builder {
                  return AccessToken.Builder(sid, key, secret)
              }
              """
          )
        );
    }

    @Test
    void otherConstructorsAreLeftAlone() {
        rewriteRun(
          //language=java
          java(
            """
              class Builder {
                  Builder(String accountSid, String keySid, String secret) {
                  }
              }

              class Tokens {
                  Builder builder(String sid, String key, String secret) {
                      return new Builder(sid, key, secret);
                  }
              }
              """
          )
        );
    }
}
