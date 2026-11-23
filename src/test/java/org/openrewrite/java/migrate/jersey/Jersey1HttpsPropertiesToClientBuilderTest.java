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

class Jersey1HttpsPropertiesToClientBuilderTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new Jersey1HttpsPropertiesToClientBuilder())
          .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(),
            "jersey-client-1", "jersey-core-1", "javax.ws.rs-api-2"));
    }

    @DocumentExample
    @Test
    void lambdaVerifierAndContext() {
        rewriteRun(
          java(
            """
              import com.sun.jersey.api.client.Client;
              import com.sun.jersey.api.client.config.ClientConfig;
              import com.sun.jersey.api.client.config.DefaultClientConfig;
              import com.sun.jersey.client.urlconnection.HTTPSProperties;

              import javax.net.ssl.SSLContext;
              import javax.net.ssl.TrustManager;
              import java.security.SecureRandom;

              class Test {
                  Client client(TrustManager[] certs, int connectTimeout) {
                      try {
                          SSLContext ctx = SSLContext.getInstance("SSL");
                          ctx.init(null, certs, new SecureRandom());

                          ClientConfig config = new DefaultClientConfig();
                          config.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES,
                                  new HTTPSProperties((hostname, session) -> true, ctx));
                          Client client = Client.create(config);
                          client.setConnectTimeout(connectTimeout);
                          return client;
                      } catch (Exception e) {
                          throw new RuntimeException(e);
                      }
                  }
              }
              """,
            """
              import com.sun.jersey.api.client.Client;
              import com.sun.jersey.api.client.config.ClientConfig;
              import com.sun.jersey.api.client.config.DefaultClientConfig;

              import javax.net.ssl.SSLContext;
              import javax.net.ssl.TrustManager;
              import javax.ws.rs.client.ClientBuilder;

              import java.security.SecureRandom;

              class Test {
                  Client client(TrustManager[] certs, int connectTimeout) {
                      try {
                          SSLContext ctx = SSLContext.getInstance("SSL");
                          ctx.init(null, certs, new SecureRandom());

                          ClientConfig config = new DefaultClientConfig();
                          Client client = ClientBuilder.newBuilder().withConfig(config).hostnameVerifier((hostname, session) -> true).sslContext(ctx).build();
                          client.setConnectTimeout(connectTimeout);
                          return client;
                      } catch (Exception e) {
                          throw new RuntimeException(e);
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void anonymousVerifier() {
        rewriteRun(
          java(
            """
              import com.sun.jersey.api.client.Client;
              import com.sun.jersey.api.client.config.ClientConfig;
              import com.sun.jersey.api.client.config.DefaultClientConfig;
              import com.sun.jersey.client.urlconnection.HTTPSProperties;

              import javax.net.ssl.HostnameVerifier;
              import javax.net.ssl.SSLContext;
              import javax.net.ssl.SSLSession;

              class Test {
                  Client client(SSLContext ctx) {
                      ClientConfig config = new DefaultClientConfig();
                      config.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES,
                              new HTTPSProperties(new HostnameVerifier() {
                                  @Override
                                  public boolean verify(String hostname, SSLSession session) {
                                      return true;
                                  }
                              }, ctx));
                      return Client.create(config);
                  }
              }
              """,
            """
              import com.sun.jersey.api.client.Client;
              import com.sun.jersey.api.client.config.ClientConfig;
              import com.sun.jersey.api.client.config.DefaultClientConfig;

              import javax.net.ssl.HostnameVerifier;
              import javax.net.ssl.SSLContext;
              import javax.net.ssl.SSLSession;
              import javax.ws.rs.client.ClientBuilder;

              class Test {
                  Client client(SSLContext ctx) {
                      ClientConfig config = new DefaultClientConfig();
                      return ClientBuilder.newBuilder().withConfig(config).hostnameVerifier(new HostnameVerifier() {
                          @Override
                          public boolean verify(String hostname, SSLSession session) {
                              return true;
                          }
                      }).sslContext(ctx).build();
                  }
              }
              """
          )
        );
    }

    @Test
    void localPropertiesVariableIsInlined() {
        rewriteRun(
          java(
            """
              import com.sun.jersey.api.client.Client;
              import com.sun.jersey.api.client.config.ClientConfig;
              import com.sun.jersey.api.client.config.DefaultClientConfig;
              import com.sun.jersey.client.urlconnection.HTTPSProperties;

              import javax.net.ssl.HostnameVerifier;
              import javax.net.ssl.HttpsURLConnection;
              import javax.net.ssl.SSLContext;

              class Test {
                  Client client(HostnameVerifier hv, SSLContext sc) {
                      HTTPSProperties prop = new HTTPSProperties(hv, sc);

                      ClientConfig config = new DefaultClientConfig();
                      config.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, prop);
                      HttpsURLConnection.setDefaultHostnameVerifier(hv);

                      return Client.create(config);
                  }
              }
              """,
            """
              import com.sun.jersey.api.client.Client;
              import com.sun.jersey.api.client.config.ClientConfig;
              import com.sun.jersey.api.client.config.DefaultClientConfig;

              import javax.net.ssl.HostnameVerifier;
              import javax.net.ssl.HttpsURLConnection;
              import javax.net.ssl.SSLContext;
              import javax.ws.rs.client.ClientBuilder;

              class Test {
                  Client client(HostnameVerifier hv, SSLContext sc) {
                      ClientConfig config = new DefaultClientConfig();
                      HttpsURLConnection.setDefaultHostnameVerifier(hv);

                      return ClientBuilder.newBuilder().withConfig(config).hostnameVerifier(hv).sslContext(sc).build();
                  }
              }
              """
          )
        );
    }

    @Test
    void fullyQualifiedCreateWithAnotherClientBuilderImported() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(),
            "jersey-client-1", "jersey-core-1", "javax.ws.rs-api-2", "jakarta.ws.rs-api-3")),
          java(
            """
              import com.sun.jersey.api.client.config.DefaultClientConfig;
              import com.sun.jersey.client.urlconnection.HTTPSProperties;
              import jakarta.ws.rs.client.Client;
              import jakarta.ws.rs.client.ClientBuilder;

              import javax.net.ssl.HostnameVerifier;
              import javax.net.ssl.SSLContext;

              class ClientConfig {
                  interface SSLFactory {
                      HostnameVerifier getHostnameVerifier();
                      SSLContext getSslContext();
                  }

                  Client jerseyClient(SSLFactory sslFactory) {
                      return ClientBuilder.newBuilder().sslContext(sslFactory.getSslContext()).build();
                  }

                  com.sun.jersey.api.client.Client oldJerseyClient(SSLFactory sslFactory) {
                      var clientConfig = new DefaultClientConfig();
                      clientConfig.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties(sslFactory.getHostnameVerifier(), sslFactory.getSslContext()));
                      return com.sun.jersey.api.client.Client.create(clientConfig);
                  }
              }
              """,
            """
              import com.sun.jersey.api.client.config.DefaultClientConfig;
              import jakarta.ws.rs.client.Client;
              import jakarta.ws.rs.client.ClientBuilder;

              import javax.net.ssl.HostnameVerifier;
              import javax.net.ssl.SSLContext;

              class ClientConfig {
                  interface SSLFactory {
                      HostnameVerifier getHostnameVerifier();
                      SSLContext getSslContext();
                  }

                  Client jerseyClient(SSLFactory sslFactory) {
                      return ClientBuilder.newBuilder().sslContext(sslFactory.getSslContext()).build();
                  }

                  com.sun.jersey.api.client.Client oldJerseyClient(SSLFactory sslFactory) {
                      var clientConfig = new DefaultClientConfig();
                      return javax.ws.rs.client.ClientBuilder.newBuilder().withConfig(clientConfig).hostnameVerifier(sslFactory.getHostnameVerifier()).sslContext(sslFactory.getSslContext()).build();
                  }
              }
              """
          )
        );
    }

    @Test
    void missingOrNullVerifierAndContextAreLeftOut() {
        rewriteRun(
          java(
            """
              import com.sun.jersey.api.client.Client;
              import com.sun.jersey.api.client.config.ClientConfig;
              import com.sun.jersey.api.client.config.DefaultClientConfig;
              import com.sun.jersey.client.urlconnection.HTTPSProperties;

              import javax.net.ssl.HostnameVerifier;
              import javax.net.ssl.SSLContext;
              import java.security.NoSuchAlgorithmException;

              class Test {
                  Client verifierOnly(HostnameVerifier hv) throws NoSuchAlgorithmException {
                      ClientConfig config = new DefaultClientConfig();
                      config.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties(hv));
                      return Client.create(config);
                  }
                  Client contextOnly(SSLContext ctx) {
                      ClientConfig config = new DefaultClientConfig();
                      config.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties(null, ctx));
                      return Client.create(config);
                  }
                  Client neither() throws NoSuchAlgorithmException {
                      ClientConfig config = new DefaultClientConfig();
                      config.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties());
                      return Client.create(config);
                  }
              }
              """,
            """
              import com.sun.jersey.api.client.Client;
              import com.sun.jersey.api.client.config.ClientConfig;
              import com.sun.jersey.api.client.config.DefaultClientConfig;

              import javax.net.ssl.HostnameVerifier;
              import javax.net.ssl.SSLContext;
              import javax.ws.rs.client.ClientBuilder;

              import java.security.NoSuchAlgorithmException;

              class Test {
                  Client verifierOnly(HostnameVerifier hv) throws NoSuchAlgorithmException {
                      ClientConfig config = new DefaultClientConfig();
                      return ClientBuilder.newBuilder().withConfig(config).hostnameVerifier(hv).build();
                  }
                  Client contextOnly(SSLContext ctx) {
                      ClientConfig config = new DefaultClientConfig();
                      return ClientBuilder.newBuilder().withConfig(config).sslContext(ctx).build();
                  }
                  Client neither() throws NoSuchAlgorithmException {
                      ClientConfig config = new DefaultClientConfig();
                      return Client.create(config);
                  }
              }
              """
          )
        );
    }

    @Test
    void staticImportLiteralKeyAndCallsOnTheConfiguration() {
        rewriteRun(
          java(
            """
              import com.sun.jersey.api.client.Client;
              import com.sun.jersey.api.client.WebResource;
              import com.sun.jersey.api.client.config.DefaultClientConfig;
              import com.sun.jersey.client.urlconnection.HTTPSProperties;

              import javax.net.ssl.HostnameVerifier;
              import javax.net.ssl.SSLContext;

              import static com.sun.jersey.client.urlconnection.HTTPSProperties.PROPERTY_HTTPS_PROPERTIES;

              abstract class Test {
                  abstract HostnameVerifier verifier();
                  abstract SSLContext context();

                  WebResource staticImport(String uri) {
                      DefaultClientConfig config = new DefaultClientConfig();
                      config.getProperties().put(PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties(verifier(), context()));
                      config.getClasses().add(String.class);
                      return Client.create(config).resource(uri);
                  }

                  WebResource literalKey(String uri) {
                      DefaultClientConfig config = new DefaultClientConfig();
                      config.getProperties().put("com.sun.jersey.client.impl.urlconnection.httpsProperties", new HTTPSProperties(verifier(), context()));
                      return Client.create(config).resource(uri);
                  }
              }
              """,
            """
              import com.sun.jersey.api.client.WebResource;
              import com.sun.jersey.api.client.config.DefaultClientConfig;

              import javax.net.ssl.HostnameVerifier;
              import javax.net.ssl.SSLContext;
              import javax.ws.rs.client.ClientBuilder;

              abstract class Test {
                  abstract HostnameVerifier verifier();
                  abstract SSLContext context();

                  WebResource staticImport(String uri) {
                      DefaultClientConfig config = new DefaultClientConfig();
                      config.getClasses().add(String.class);
                      return ClientBuilder.newBuilder().withConfig(config).hostnameVerifier(verifier()).sslContext(context()).build().resource(uri);
                  }

                  WebResource literalKey(String uri) {
                      DefaultClientConfig config = new DefaultClientConfig();
                      return ClientBuilder.newBuilder().withConfig(config).hostnameVerifier(verifier()).sslContext(context()).build().resource(uri);
                  }
              }
              """
          )
        );
    }

    @Test
    void leavesConfigurationThatIsReturnedAlone() {
        rewriteRun(
          java(
            """
              import com.sun.jersey.api.client.Client;
              import com.sun.jersey.api.client.config.ClientConfig;
              import com.sun.jersey.api.client.config.DefaultClientConfig;
              import com.sun.jersey.client.urlconnection.HTTPSProperties;

              import javax.net.ssl.HostnameVerifier;
              import javax.net.ssl.SSLContext;
              import javax.net.ssl.SSLSession;

              class Test {
                  static ClientConfig configureClient(SSLContext ctx) {
                      ClientConfig config = new DefaultClientConfig();
                      try {
                          config.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES,
                                  new HTTPSProperties(new HostnameVerifier() {
                                      @Override
                                      public boolean verify(String hostname, SSLSession session) {
                                          return true;
                                      }
                                  }, ctx));
                      } catch (Exception e) {
                          System.err.println(e.getMessage());
                      }
                      return config;
                  }

                  static ClientConfig localProperties(HostnameVerifier hv, SSLContext sc) {
                      HTTPSProperties prop = new HTTPSProperties(hv, sc);
                      ClientConfig config = new DefaultClientConfig();
                      config.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, prop);
                      return config;
                  }

                  static Client createClient(SSLContext ctx) {
                      return Client.create(configureClient(ctx));
                  }
              }
              """
          )
        );
    }

    @Test
    void leavesConfigurationUsedElsewhereAlone() {
        rewriteRun(
          java(
            """
              import com.sun.jersey.api.client.Client;
              import com.sun.jersey.api.client.config.ClientConfig;
              import com.sun.jersey.api.client.config.DefaultClientConfig;
              import com.sun.jersey.client.urlconnection.HTTPSProperties;

              import javax.net.ssl.HostnameVerifier;
              import javax.net.ssl.SSLContext;
              import java.util.List;

              class Test {
                  List<Client> twoClients(HostnameVerifier hv, SSLContext ctx) {
                      ClientConfig config = new DefaultClientConfig();
                      config.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties(hv, ctx));
                      return List.of(Client.create(config), Client.create(config));
                  }

                  Client passedOn(HostnameVerifier hv, SSLContext ctx, List<ClientConfig> configs) {
                      ClientConfig config = new DefaultClientConfig();
                      config.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties(hv, ctx));
                      configs.add(config);
                      return Client.create(config);
                  }

                  Client parameter(ClientConfig config, HostnameVerifier hv, SSLContext ctx) {
                      config.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties(hv, ctx));
                      return Client.create(config);
                  }

                  Client createdFirst(HostnameVerifier hv, SSLContext ctx) {
                      ClientConfig config = new DefaultClientConfig();
                      Client client = Client.create(config);
                      config.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties(hv, ctx));
                      return client;
                  }

                  Client putTwice(HostnameVerifier hv, SSLContext ctx) {
                      ClientConfig config = new DefaultClientConfig();
                      config.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties(hv, ctx));
                      config.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties(null, ctx));
                      return Client.create(config);
                  }
              }
              """
          )
        );
    }

    @Test
    void leavesPropertiesThatEscapeOrArePutInANestedBlockAlone() {
        rewriteRun(
          java(
            """
              import com.sun.jersey.api.client.Client;
              import com.sun.jersey.api.client.config.ClientConfig;
              import com.sun.jersey.api.client.config.DefaultClientConfig;
              import com.sun.jersey.client.urlconnection.HTTPSProperties;

              import javax.net.ssl.HostnameVerifier;
              import javax.net.ssl.SSLContext;
              import java.util.function.Supplier;

              class Test {
                  HTTPSProperties last;

                  Client propertiesUsedAgain(HostnameVerifier hv, SSLContext ctx) {
                      HTTPSProperties prop = new HTTPSProperties(hv, ctx);
                      ClientConfig config = new DefaultClientConfig();
                      config.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, prop);
                      last = prop;
                      return Client.create(config);
                  }

                  Client conditional(boolean https, HostnameVerifier hv, SSLContext ctx) {
                      ClientConfig config = new DefaultClientConfig();
                      if (https) {
                          config.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties(hv, ctx));
                      }
                      return Client.create(config);
                  }

                  Supplier<Client> createdInLambda(HostnameVerifier hv, SSLContext ctx) {
                      ClientConfig config = new DefaultClientConfig();
                      config.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties(hv, ctx));
                      return () -> Client.create(config);
                  }

                  Client subclassed(HostnameVerifier hv, SSLContext ctx) {
                      ClientConfig config = new DefaultClientConfig();
                      config.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties(hv, ctx) {
                      });
                      return Client.create(config);
                  }
              }
              """
          )
        );
    }

    @Test
    void leavesWhatWouldChangeMeaningOrStopCompilingAlone() {
        rewriteRun(
          java(
            """
              import com.sun.jersey.api.client.Client;
              import com.sun.jersey.api.client.config.ClientConfig;
              import com.sun.jersey.api.client.config.DefaultClientConfig;
              import com.sun.jersey.client.urlconnection.HTTPSProperties;

              import javax.net.ssl.HostnameVerifier;
              import javax.net.ssl.SSLContext;
              import java.security.NoSuchAlgorithmException;

              abstract class Test {
                  abstract SSLContext context();
                  abstract void initContexts();

                  Client contextChangesInBetween(HostnameVerifier hv) {
                      ClientConfig config = new DefaultClientConfig();
                      config.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties(hv, context()));
                      initContexts();
                      return Client.create(config);
                  }

                  Client contextReassigned(HostnameVerifier hv, SSLContext ctx) {
                      ClientConfig config = new DefaultClientConfig();
                      config.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties(hv, ctx));
                      ctx = context();
                      return Client.create(config);
                  }

                  Client exceptionNoLongerThrown(HostnameVerifier hv) {
                      try {
                          ClientConfig config = new DefaultClientConfig();
                          config.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties(hv));
                          return Client.create(config);
                      } catch (NoSuchAlgorithmException e) {
                          throw new IllegalStateException(e);
                      }
                  }
              }
              """
          )
        );
    }
}
