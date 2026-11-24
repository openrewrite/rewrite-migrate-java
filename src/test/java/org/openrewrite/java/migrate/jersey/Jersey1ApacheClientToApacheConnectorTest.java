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

class Jersey1ApacheClientToApacheConnectorTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.migrate.jersey.Jersey1ClientToJersey2",
            "org.openrewrite.java.migrate.jersey.Jersey1ClientMethodsToJaxRs",
            "org.openrewrite.java.migrate.jersey.Jersey1TypesToJersey2")
          .parser(JavaParser.fromJavaVersion()
            .classpathFromResources(new InMemoryExecutionContext(), "jersey-client-1.19", "jersey-core-1.19",
              "jersey-apache-client4-1.19", "jersey-apache-client-1.19", "javax.ws.rs-api-2.1"));
    }

    @DocumentExample
    @Test
    void apacheClientWithProxy() {
        rewriteRun(
          //language=java
          java(
            """
              import com.sun.jersey.api.client.Client;
              import com.sun.jersey.api.client.config.ClientConfig;
              import com.sun.jersey.api.client.config.DefaultClientConfig;
              import com.sun.jersey.client.apache4.ApacheHttpClient4;
              import com.sun.jersey.client.apache4.config.ApacheHttpClient4Config;

              import java.net.URI;

              class Proxied {
                  private Client client;

                  Proxied(URI proxy, String user, String password) {
                      ClientConfig config = new DefaultClientConfig();
                      config.getProperties().put(ApacheHttpClient4Config.PROPERTY_PROXY_URI, proxy);
                      config.getProperties().put(ApacheHttpClient4Config.PROPERTY_PROXY_USERNAME, user);
                      config.getProperties().put(ApacheHttpClient4Config.PROPERTY_PROXY_PASSWORD, password);
                      client = ApacheHttpClient4.create(config);
                      client.setFollowRedirects(Boolean.TRUE);
                  }
              }
              """,
            """
              import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
              import org.glassfish.jersey.client.ClientConfig;
              import org.glassfish.jersey.client.ClientProperties;

              import java.net.URI;

              import javax.ws.rs.client.Client;
              import javax.ws.rs.client.ClientBuilder;

              class Proxied {
                  private Client client;

                  Proxied(URI proxy, String user, String password) {
                      ClientConfig config = new ClientConfig();
                      config.property(ClientProperties.PROXY_URI, proxy);
                      config.property(ClientProperties.PROXY_USERNAME, user);
                      config.property(ClientProperties.PROXY_PASSWORD, password);
                      client = ClientBuilder.newClient(config.connectorProvider(new ApacheConnectorProvider()));
                      client.property(ClientProperties.FOLLOW_REDIRECTS, Boolean.TRUE);
                  }
              }
              """
          )
        );
    }

    @Test
    void apacheClientsAndTheirConfigurations() {
        rewriteRun(
          //language=java
          java(
            """
              import com.sun.jersey.api.client.Client;
              import com.sun.jersey.client.apache.ApacheHttpClient;
              import com.sun.jersey.client.apache.config.DefaultApacheHttpClientConfig;
              import com.sun.jersey.client.apache4.ApacheHttpClient4;
              import com.sun.jersey.client.apache4.config.DefaultApacheHttpClient4Config;

              class Clients {
                  Client plain() {
                      return ApacheHttpClient4.create();
                  }

                  ApacheHttpClient4 configured() {
                      DefaultApacheHttpClient4Config config = new DefaultApacheHttpClient4Config();
                      ApacheHttpClient4 client = ApacheHttpClient4.create(config);
                      client.setReadTimeout(1000);
                      return client;
                  }

                  Client legacy() {
                      return ApacheHttpClient.create(new DefaultApacheHttpClientConfig());
                  }
              }
              """,
            """
              import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
              import org.glassfish.jersey.client.ClientConfig;
              import org.glassfish.jersey.client.ClientProperties;

              import javax.ws.rs.client.Client;
              import javax.ws.rs.client.ClientBuilder;

              class Clients {
                  Client plain() {
                      return ClientBuilder.newClient(new ClientConfig().connectorProvider(new ApacheConnectorProvider()));
                  }

                  Client configured() {
                      ClientConfig config = new ClientConfig();
                      Client client = ClientBuilder.newClient(config.connectorProvider(new ApacheConnectorProvider()));
                      client.property(ClientProperties.READ_TIMEOUT, 1000);
                      return client;
                  }

                  Client legacy() {
                      return ClientBuilder.newClient(new ClientConfig().connectorProvider(new ApacheConnectorProvider()));
                  }
              }
              """
          )
        );
    }
}
