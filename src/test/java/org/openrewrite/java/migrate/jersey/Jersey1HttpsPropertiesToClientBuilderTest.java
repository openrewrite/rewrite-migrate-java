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
    void factoryMethodReturnsTheConfiguration() {
        rewriteRun(
          java(
            """
              import com.sun.jersey.api.client.Client;
              import com.sun.jersey.api.client.config.ClientConfig;
              import com.sun.jersey.api.client.config.DefaultClientConfig;
              import com.sun.jersey.client.urlconnection.HTTPSProperties;

              import javax.net.ssl.HostnameVerifier;
              import javax.net.ssl.SSLContext;
              import javax.net.ssl.TrustManager;
              import java.security.KeyManagementException;
              import java.security.NoSuchAlgorithmException;
              import java.security.SecureRandom;

              class Test {
                  Client client(TrustManager[] trustAllCerts) {
                      ClientConfig config = getClientConfig(trustAllCerts);
                      return Client.create(config);
                  }

                  private ClientConfig getClientConfig(TrustManager[] trustAllCerts) {
                      try {
                          HostnameVerifier hv = (hostname, session) -> true;
                          SSLContext sc = SSLContext.getInstance("SSL");
                          sc.init(null, trustAllCerts, new SecureRandom());

                          HTTPSProperties prop = new HTTPSProperties(hv, sc);

                          ClientConfig config = new DefaultClientConfig();
                          config.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, prop);

                          return config;
                      } catch (NoSuchAlgorithmException | KeyManagementException e) {
                          System.err.println(e);
                      }
                      return null;
                  }
              }
              """,
            """
              import com.sun.jersey.api.client.Client;
              import com.sun.jersey.api.client.config.ClientConfig;
              import com.sun.jersey.api.client.config.DefaultClientConfig;
              import org.glassfish.jersey.client.HttpUrlConnectorProvider;

              import javax.net.ssl.HostnameVerifier;
              import javax.net.ssl.HttpsURLConnection;
              import javax.net.ssl.SSLContext;
              import javax.net.ssl.TrustManager;

              import java.net.HttpURLConnection;
              import java.security.KeyManagementException;
              import java.security.NoSuchAlgorithmException;
              import java.security.SecureRandom;

              class Test {
                  Client client(TrustManager[] trustAllCerts) {
                      ClientConfig config = getClientConfig(trustAllCerts);
                      return Client.create(config);
                  }

                  private ClientConfig getClientConfig(TrustManager[] trustAllCerts) {
                      try {
                          HostnameVerifier hv = (hostname, session) -> true;
                          SSLContext sc = SSLContext.getInstance("SSL");
                          sc.init(null, trustAllCerts, new SecureRandom());

                          HostnameVerifier hostnameVerifier = hv;
                          SSLContext sslContext = sc;

                          ClientConfig config = new DefaultClientConfig();
                          config.connectorProvider(new HttpUrlConnectorProvider().connectionFactory(url -> {
                              HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                              if (connection instanceof HttpsURLConnection) {
                                  ((HttpsURLConnection) connection).setHostnameVerifier(hostnameVerifier);
                                  ((HttpsURLConnection) connection).setSSLSocketFactory(sslContext.getSocketFactory());
                              }
                              return connection;
                          }));

                          return config;
                      } catch (NoSuchAlgorithmException | KeyManagementException e) {
                          System.err.println(e);
                      }
                      return null;
                  }
              }
              """
          )
        );
    }

    @Test
    void putInsideTry() {
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
              import javax.net.ssl.TrustManager;
              import java.security.SecureRandom;

              class Test {
                  static ClientConfig configureClient(TrustManager[] certs) {
                      SSLContext ctx = null;
                      try {
                          ctx = SSLContext.getInstance("TLS");
                          ctx.init(null, certs, new SecureRandom());
                      } catch (java.security.GeneralSecurityException ex) {
                          return null;
                      }
                      ClientConfig config = new DefaultClientConfig();
                      config.getClasses().add(String.class);
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

                  static Client createClient(TrustManager[] certs) {
                      return Client.create(configureClient(certs));
                  }
              }
              """,
            """
              import com.sun.jersey.api.client.Client;
              import com.sun.jersey.api.client.config.ClientConfig;
              import com.sun.jersey.api.client.config.DefaultClientConfig;
              import org.glassfish.jersey.client.HttpUrlConnectorProvider;

              import javax.net.ssl.*;

              import java.net.HttpURLConnection;
              import java.security.SecureRandom;

              class Test {
                  static ClientConfig configureClient(TrustManager[] certs) {
                      SSLContext ctx = null;
                      try {
                          ctx = SSLContext.getInstance("TLS");
                          ctx.init(null, certs, new SecureRandom());
                      } catch (java.security.GeneralSecurityException ex) {
                          return null;
                      }
                      ClientConfig config = new DefaultClientConfig();
                      config.getClasses().add(String.class);
                      try {
                          HostnameVerifier hostnameVerifier = new HostnameVerifier() {
                              @Override
                              public boolean verify(String hostname, SSLSession session) {
                                  return true;
                              }
                          };
                          SSLContext sslContext = ctx;
                          config.connectorProvider(new HttpUrlConnectorProvider().connectionFactory(url -> {
                              HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                              if (connection instanceof HttpsURLConnection) {
                                  ((HttpsURLConnection) connection).setHostnameVerifier(hostnameVerifier);
                                  ((HttpsURLConnection) connection).setSSLSocketFactory(sslContext.getSocketFactory());
                              }
                              return connection;
                          }));
                      } catch (Exception e) {
                          System.err.println(e.getMessage());
                      }
                      return config;
                  }

                  static Client createClient(TrustManager[] certs) {
                      return Client.create(configureClient(certs));
                  }
              }
              """
          )
        );
    }

    @Test
    void putInsideIf() {
        rewriteRun(
          java(
            """
              import com.sun.jersey.api.client.Client;
              import com.sun.jersey.api.client.config.ClientConfig;
              import com.sun.jersey.api.client.config.DefaultClientConfig;
              import com.sun.jersey.client.urlconnection.HTTPSProperties;

              import javax.net.ssl.HttpsURLConnection;
              import javax.net.ssl.SSLContext;

              abstract class Test {
                  abstract SSLContext trustAll();

                  ClientConfig initSSLConfig(boolean insecure) {
                      final ClientConfig config = new DefaultClientConfig();
                      if (insecure) {
                          SSLContext ctx = trustAll();
                          config.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties(HttpsURLConnection.getDefaultHostnameVerifier(), ctx));
                      }
                      return config;
                  }

                  Client create(String endpoint) {
                      ClientConfig clientConfig = new DefaultClientConfig();
                      if (endpoint.startsWith("https")) {
                          clientConfig.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES,
                              new HTTPSProperties(null, trustAll()));
                      }
                      clientConfig.getClasses().add(String.class);
                      return Client.create(clientConfig);
                  }
              }
              """,
            """
              import com.sun.jersey.api.client.Client;
              import com.sun.jersey.api.client.config.ClientConfig;
              import com.sun.jersey.api.client.config.DefaultClientConfig;
              import org.glassfish.jersey.client.HttpUrlConnectorProvider;

              import javax.net.ssl.HostnameVerifier;
              import javax.net.ssl.HttpsURLConnection;
              import javax.net.ssl.SSLContext;

              import java.net.HttpURLConnection;

              abstract class Test {
                  abstract SSLContext trustAll();

                  ClientConfig initSSLConfig(boolean insecure) {
                      final ClientConfig config = new DefaultClientConfig();
                      if (insecure) {
                          SSLContext ctx = trustAll();
                          HostnameVerifier hostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier();
                          SSLContext sslContext = ctx;
                          config.connectorProvider(new HttpUrlConnectorProvider().connectionFactory(url -> {
                              HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                              if (connection instanceof HttpsURLConnection) {
                                  ((HttpsURLConnection) connection).setHostnameVerifier(hostnameVerifier);
                                  ((HttpsURLConnection) connection).setSSLSocketFactory(sslContext.getSocketFactory());
                              }
                              return connection;
                          }));
                      }
                      return config;
                  }

                  Client create(String endpoint) {
                      ClientConfig clientConfig = new DefaultClientConfig();
                      if (endpoint.startsWith("https")) {
                          SSLContext sslContext = trustAll();
                          clientConfig.connectorProvider(new HttpUrlConnectorProvider().connectionFactory(url -> {
                              HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                              if (connection instanceof HttpsURLConnection) {
                                  ((HttpsURLConnection) connection).setSSLSocketFactory(sslContext.getSocketFactory());
                              }
                              return connection;
                          }));
                      }
                      clientConfig.getClasses().add(String.class);
                      return Client.create(clientConfig);
                  }
              }
              """
          )
        );
    }

    @Test
    void connectorProviderWhereTheClientBuilderWouldNotDo() {
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

              abstract class Test {
                  SSLContext sslContext;

                  abstract SSLContext context();

                  List<Client> twoClients(HostnameVerifier hv, String url) {
                      ClientConfig config = new DefaultClientConfig();
                      config.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties(hv, sslContext));
                      return List.of(Client.create(config), Client.create(config));
                  }

                  Client contextReassigned(HostnameVerifier hv, SSLContext ctx) {
                      ClientConfig config = new DefaultClientConfig();
                      config.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties(hv, ctx));
                      ctx = context();
                      return Client.create(config);
                  }
              }
              """,
            """
              import com.sun.jersey.api.client.Client;
              import com.sun.jersey.api.client.config.ClientConfig;
              import com.sun.jersey.api.client.config.DefaultClientConfig;
              import org.glassfish.jersey.client.HttpUrlConnectorProvider;

              import javax.net.ssl.HostnameVerifier;
              import javax.net.ssl.HttpsURLConnection;
              import javax.net.ssl.SSLContext;

              import java.net.HttpURLConnection;
              import java.util.List;

              abstract class Test {
                  SSLContext sslContext;

                  abstract SSLContext context();

                  List<Client> twoClients(HostnameVerifier hv, String url) {
                      ClientConfig config = new DefaultClientConfig();
                      HostnameVerifier hostnameVerifier = hv;
                      SSLContext sslContext1 = sslContext;
                      config.connectorProvider(new HttpUrlConnectorProvider().connectionFactory(url1 -> {
                          HttpURLConnection connection = (HttpURLConnection) url1.openConnection();
                          if (connection instanceof HttpsURLConnection) {
                              ((HttpsURLConnection) connection).setHostnameVerifier(hostnameVerifier);
                              ((HttpsURLConnection) connection).setSSLSocketFactory(sslContext1.getSocketFactory());
                          }
                          return connection;
                      }));
                      return List.of(Client.create(config), Client.create(config));
                  }

                  Client contextReassigned(HostnameVerifier hv, SSLContext ctx) {
                      ClientConfig config = new DefaultClientConfig();
                      HostnameVerifier hostnameVerifier = hv;
                      SSLContext sslContext1 = ctx;
                      config.connectorProvider(new HttpUrlConnectorProvider().connectionFactory(url -> {
                          HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                          if (connection instanceof HttpsURLConnection) {
                              ((HttpsURLConnection) connection).setHostnameVerifier(hostnameVerifier);
                              ((HttpsURLConnection) connection).setSSLSocketFactory(sslContext1.getSocketFactory());
                          }
                          return connection;
                      }));
                      ctx = context();
                      return Client.create(config);
                  }
              }
              """
          )
        );
    }

    @Test
    void leavesWhatItCannotMoveAlone() {
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
                  HTTPSProperties last;

                  Client propertiesUsedAgain(HostnameVerifier hv, SSLContext ctx) {
                      HTTPSProperties prop = new HTTPSProperties(hv, ctx);
                      ClientConfig config = new DefaultClientConfig();
                      config.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, prop);
                      last = prop;
                      return Client.create(config);
                  }

                  Client subclassed(HostnameVerifier hv, SSLContext ctx) {
                      ClientConfig config = new DefaultClientConfig();
                      config.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties(hv, ctx) {
                      });
                      return Client.create(config);
                  }

                  ClientConfig ownContext(HostnameVerifier hv) throws NoSuchAlgorithmException {
                      ClientConfig config = new DefaultClientConfig();
                      config.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties(hv));
                      return config;
                  }

                  ClientConfig nullContext(HostnameVerifier hv) {
                      ClientConfig config = new DefaultClientConfig();
                      config.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties(hv, null));
                      return config;
                  }

                  void client(Client client, HostnameVerifier hv, SSLContext ctx) {
                      client.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties(hv, ctx));
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
