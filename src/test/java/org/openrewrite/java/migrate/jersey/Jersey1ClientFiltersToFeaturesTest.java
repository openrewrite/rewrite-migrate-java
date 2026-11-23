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

class Jersey1ClientFiltersToFeaturesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new Jersey1ClientFiltersToFeatures())
          .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(),
            "jersey-client-1", "jersey-core-1", "javax.ws.rs-api-2"));
    }

    @DocumentExample
    @Test
    void basicAuthentication() {
        rewriteRun(
          java(
            """
              import com.sun.jersey.api.client.Client;
              import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;

              class Test {
                  void authenticate(Client client, String user, String password) {
                      client.addFilter(new HTTPBasicAuthFilter(user, password));
                  }
              }
              """,
            """
              import com.sun.jersey.api.client.Client;
              import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;

              class Test {
                  void authenticate(Client client, String user, String password) {
                      client.register(HttpAuthenticationFeature.basic(user, password));
                  }
              }
              """
          )
        );
    }

    @Test
    void authenticationOnWebResourceWithPasswordBytes() {
        rewriteRun(
          java(
            """
              import com.sun.jersey.api.client.WebResource;
              import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
              import com.sun.jersey.api.client.filter.HTTPDigestAuthFilter;

              class Test {
                  void basic(WebResource resource, String user, byte[] password) {
                      resource.addFilter(new HTTPBasicAuthFilter(user, password));
                  }
                  void digest(WebResource resource, String user, String password) {
                      resource.addFilter(new HTTPDigestAuthFilter(user, password));
                  }
              }
              """,
            """
              import com.sun.jersey.api.client.WebResource;
              import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;

              class Test {
                  void basic(WebResource resource, String user, byte[] password) {
                      resource.register(HttpAuthenticationFeature.basic(user, password));
                  }
                  void digest(WebResource resource, String user, String password) {
                      resource.register(HttpAuthenticationFeature.digest(user, password));
                  }
              }
              """
          )
        );
    }

    @Test
    void logging() {
        rewriteRun(
          java(
            """
              import com.sun.jersey.api.client.Client;
              import com.sun.jersey.api.client.filter.LoggingFilter;

              import java.util.logging.Logger;

              class Test {
                  private static final Logger LOG = Logger.getLogger("client");

                  void byDefault(Client client) {
                      client.addFilter(new LoggingFilter());
                  }
                  void toLogger(Client client) {
                      client.addFilter(new LoggingFilter(LOG));
                  }
                  void toLoggerWithEntitySize(Client client) {
                      client.addFilter(new LoggingFilter(LOG, 1024));
                  }
              }
              """,
            """
              import com.sun.jersey.api.client.Client;
              import org.glassfish.jersey.logging.LoggingFeature;

              import java.util.logging.Level;
              import java.util.logging.Logger;

              class Test {
                  private static final Logger LOG = Logger.getLogger("client");

                  void byDefault(Client client) {
                      client.register(new LoggingFeature(Logger.getLogger("com.sun.jersey.api.client.filter.LoggingFilter"), Level.INFO, LoggingFeature.Verbosity.PAYLOAD_ANY, 10 * 1024));
                  }
                  void toLogger(Client client) {
                      client.register(new LoggingFeature(LOG, Level.INFO, LoggingFeature.Verbosity.PAYLOAD_ANY, 10 * 1024));
                  }
                  void toLoggerWithEntitySize(Client client) {
                      client.register(new LoggingFeature(LOG, Level.INFO, LoggingFeature.Verbosity.PAYLOAD_ANY, 1024));
                  }
              }
              """
          )
        );
    }

    @Test
    void loggingWhenAnotherLoggerIsImported() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion()
            .classpathFromResources(new InMemoryExecutionContext(), "jersey-client-1", "jersey-core-1", "javax.ws.rs-api-2")
            .dependsOn(
              """
                package org.slf4j;
                public interface Logger { void info(String message); }
                """,
              """
                package org.apache.log4j;
                public class Level { public static final Level INFO = new Level(); }
                """
            )),
          java(
            """
              import com.sun.jersey.api.client.Client;
              import com.sun.jersey.api.client.filter.LoggingFilter;
              import org.apache.log4j.Level;
              import org.slf4j.Logger;

              class Test {
                  Logger log;
                  Level level = Level.INFO;

                  void byDefault(Client client) {
                      client.addFilter(new LoggingFilter());
                  }
              }
              """,
            """
              import com.sun.jersey.api.client.Client;
              import org.apache.log4j.Level;
              import org.glassfish.jersey.logging.LoggingFeature;
              import org.slf4j.Logger;

              class Test {
                  Logger log;
                  Level level = Level.INFO;

                  void byDefault(Client client) {
                      client.register(new LoggingFeature(java.util.logging.Logger.getLogger("com.sun.jersey.api.client.filter.LoggingFilter"), java.util.logging.Level.INFO, LoggingFeature.Verbosity.PAYLOAD_ANY, 10 * 1024));
                  }
              }
              """
          )
        );
    }

    @Test
    void gzip() {
        rewriteRun(
          java(
            """
              import com.sun.jersey.api.client.Client;
              import com.sun.jersey.api.client.filter.GZIPContentEncodingFilter;

              class Test {
                  void compressing(Client client) {
                      client.addFilter(new GZIPContentEncodingFilter());
                  }
                  void compressingExplicitly(Client client) {
                      client.addFilter(new GZIPContentEncodingFilter(true));
                  }
                  void onlyWhenAsked(Client client) {
                      client.addFilter(new GZIPContentEncodingFilter(false));
                  }
                  void configured(Client client, boolean compress) {
                      client.addFilter(new GZIPContentEncodingFilter(compress));
                  }
              }
              """,
            """
              import com.sun.jersey.api.client.Client;
              import org.glassfish.jersey.client.filter.EncodingFeature;
              import org.glassfish.jersey.message.GZipEncoder;

              class Test {
                  void compressing(Client client) {
                      client.register(new EncodingFeature("gzip", GZipEncoder.class));
                  }
                  void compressingExplicitly(Client client) {
                      client.register(new EncodingFeature("gzip", GZipEncoder.class));
                  }
                  void onlyWhenAsked(Client client) {
                      client.register(new EncodingFeature(GZipEncoder.class));
                  }
                  void configured(Client client, boolean compress) {
                      client.register(new EncodingFeature(compress ? "gzip" : null, GZipEncoder.class));
                  }
              }
              """
          )
        );
    }

    @Test
    void csrfProtection() {
        rewriteRun(
          java(
            """
              import com.sun.jersey.api.client.Client;
              import com.sun.jersey.api.client.filter.CsrfProtectionFilter;

              class Test {
                  void protect(Client client) {
                      client.addFilter(new CsrfProtectionFilter());
                      client.addFilter(new CsrfProtectionFilter("me"));
                  }
              }
              """,
            """
              import com.sun.jersey.api.client.Client;
              import org.glassfish.jersey.client.filter.CsrfProtectionFilter;

              class Test {
                  void protect(Client client) {
                      client.register(new CsrfProtectionFilter());
                      client.register(new CsrfProtectionFilter("me"));
                  }
              }
              """
          )
        );
    }

    @Test
    void leavesCsrfProtectionAloneWhenItIsUsedElsewhereInTheFile() {
        rewriteRun(
          java(
            """
              import com.sun.jersey.api.client.Client;
              import com.sun.jersey.api.client.filter.CsrfProtectionFilter;

              class Test {
                  CsrfProtectionFilter kept = new CsrfProtectionFilter();

                  void protect(Client client) {
                      client.addFilter(new CsrfProtectionFilter());
                  }
              }
              """
          )
        );
    }

    @Test
    void leavesWhatHasNoFaithfulCounterpartAlone() {
        rewriteRun(
          java(
            """
              import com.sun.jersey.api.client.Client;
              import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
              import com.sun.jersey.api.client.filter.LoggingFilter;

              class Test {
                  void printStream(Client client) {
                      client.addFilter(new LoggingFilter(System.out));
                  }
                  void notCreatedInline(Client client, String user, String password) {
                      HTTPBasicAuthFilter filter = new HTTPBasicAuthFilter(user, password);
                      client.addFilter(filter);
                      client.removeFilter(filter);
                  }
              }
              """
          )
        );
    }
}
