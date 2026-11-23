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

class Jersey1ClientToClientBuilderTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new Jersey1ClientToClientBuilder())
          .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(),
            "jersey-client-1", "jersey-core-1", "javax.ws.rs-api-2"));
    }

    @DocumentExample
    @Test
    void createAndConfigureClient() {
        rewriteRun(
          java(
            """
              import com.sun.jersey.api.client.Client;

              class Test {
                  Client client(int timeout) {
                      Client client = Client.create();
                      client.setConnectTimeout(timeout);
                      client.setReadTimeout(30000);
                      client.setFollowRedirects(false);
                      return client;
                  }
              }
              """,
            """
              import com.sun.jersey.api.client.Client;
              import org.glassfish.jersey.client.ClientProperties;

              import javax.ws.rs.client.ClientBuilder;

              class Test {
                  Client client(int timeout) {
                      Client client = ClientBuilder.newClient();
                      client.property(ClientProperties.CONNECT_TIMEOUT, timeout);
                      client.property(ClientProperties.READ_TIMEOUT, 30000);
                      client.property(ClientProperties.FOLLOW_REDIRECTS, false);
                      return client;
                  }
              }
              """
          )
        );
    }

    @Test
    void createWithConfigurationAndConstructor() {
        rewriteRun(
          java(
            """
              import com.sun.jersey.api.client.Client;
              import com.sun.jersey.api.client.config.ClientConfig;
              import com.sun.jersey.api.client.config.DefaultClientConfig;

              class Test {
                  Client configured() {
                      ClientConfig cc = new DefaultClientConfig();
                      return Client.create(cc);
                  }
                  Client constructed() {
                      return new Client();
                  }
              }
              """,
            """
              import com.sun.jersey.api.client.Client;
              import com.sun.jersey.api.client.config.ClientConfig;
              import com.sun.jersey.api.client.config.DefaultClientConfig;

              import javax.ws.rs.client.ClientBuilder;

              class Test {
                  Client configured() {
                      ClientConfig cc = new DefaultClientConfig();
                      return ClientBuilder.newClient(cc);
                  }
                  Client constructed() {
                      return ClientBuilder.newClient();
                  }
              }
              """
          )
        );
    }

    @Test
    void chunkedEncodingSizeTurnsOnChunkedStreaming() {
        rewriteRun(
          java(
            """
              import com.sun.jersey.api.client.Client;

              class Test {
                  void chunked(Client client) {
                      client.setChunkedEncodingSize(1024);
                  }
                  void notChunked(Client client) {
                      client.setChunkedEncodingSize(null);
                  }
              }
              """,
            """
              import com.sun.jersey.api.client.Client;
              import org.glassfish.jersey.client.ClientProperties;
              import org.glassfish.jersey.client.RequestEntityProcessing;

              class Test {
                  void chunked(Client client) {
                      client.property(ClientProperties.CHUNKED_ENCODING_SIZE, 1024).property(ClientProperties.REQUEST_ENTITY_PROCESSING, RequestEntityProcessing.CHUNKED);
                  }
                  void notChunked(Client client) {
                      client.property(ClientProperties.CHUNKED_ENCODING_SIZE, null);
                  }
              }
              """
          )
        );
    }

    @Test
    void createdClientIsUsedInline() {
        rewriteRun(
          java(
            """
              import com.sun.jersey.api.client.Client;
              import com.sun.jersey.api.client.WebResource;

              class Test {
                  WebResource resource(String uri) {
                      return Client.create().resource(uri);
                  }
              }
              """,
            """
              import com.sun.jersey.api.client.WebResource;

              import javax.ws.rs.client.ClientBuilder;

              class Test {
                  WebResource resource(String uri) {
                      return ClientBuilder.newClient().resource(uri);
                  }
              }
              """
          )
        );
    }

    @Test
    void propertyBagAndProviders() {
        rewriteRun(
          java(
            """
              import com.sun.jersey.api.client.Client;
              import com.sun.jersey.api.client.config.ClientConfig;
              import com.sun.jersey.api.client.config.DefaultClientConfig;

              class Test {
                  Client client(Object provider) {
                      ClientConfig cc = new DefaultClientConfig();
                      cc.getProperties().put(ClientConfig.PROPERTY_CONNECT_TIMEOUT, 5000);
                      cc.getProperties().put("com.sun.jersey.client.property.readTimeout", 10000);
                      cc.getProperties().put("custom", "value");
                      cc.getClasses().add(String.class);
                      cc.getSingletons().add(provider);
                      Client client = Client.create(cc);
                      client.getProperties().put(ClientConfig.PROPERTY_FOLLOW_REDIRECTS, false);
                      client.getProperties().put(DefaultClientConfig.PROPERTY_CHUNKED_ENCODING_SIZE, 0);
                      return client;
                  }
              }
              """,
            """
              import com.sun.jersey.api.client.Client;
              import com.sun.jersey.api.client.config.ClientConfig;
              import com.sun.jersey.api.client.config.DefaultClientConfig;
              import org.glassfish.jersey.client.ClientProperties;
              import org.glassfish.jersey.client.RequestEntityProcessing;

              import javax.ws.rs.client.ClientBuilder;

              class Test {
                  Client client(Object provider) {
                      ClientConfig cc = new DefaultClientConfig();
                      cc.property(ClientProperties.CONNECT_TIMEOUT, 5000);
                      cc.property(ClientProperties.READ_TIMEOUT, 10000);
                      cc.property("custom", "value");
                      cc.register(String.class);
                      cc.register(provider);
                      Client client = ClientBuilder.newClient(cc);
                      client.property(ClientProperties.FOLLOW_REDIRECTS, false);
                      client.property(ClientProperties.CHUNKED_ENCODING_SIZE, 0).property(ClientProperties.REQUEST_ENTITY_PROCESSING, RequestEntityProcessing.CHUNKED);
                      return client;
                  }
              }
              """
          )
        );
    }

    @Test
    void leavesPropertyBagAloneWhereItsResultIsUsedOrJersey2HasNoCounterpart() {
        rewriteRun(
          java(
            """
              import com.sun.jersey.api.client.Client;
              import com.sun.jersey.api.client.config.ClientConfig;

              class Test {
                  Object previous(Client client) {
                      client.getProperties().put(ClientConfig.PROPERTY_BUFFER_RESPONSE_ENTITY_ON_EXCEPTION, true);
                      return client.getProperties().put(ClientConfig.PROPERTY_READ_TIMEOUT, 1000);
                  }
              }
              """
          )
        );
    }

    @Test
    void leavesClientHandlerConstructorsAndProviderFactoryAlone() {
        rewriteRun(
          java(
            """
              import com.sun.jersey.api.client.Client;
              import com.sun.jersey.api.client.config.ClientConfig;
              import com.sun.jersey.client.urlconnection.URLConnectionClientHandler;
              import com.sun.jersey.core.spi.component.ioc.IoCComponentProviderFactory;

              class Test {
                  Client withHandler(ClientConfig cc) {
                      return new Client(new URLConnectionClientHandler(), cc);
                  }
                  Client withProviderFactory(ClientConfig cc, IoCComponentProviderFactory factory) {
                      return Client.create(cc, factory);
                  }
              }
              """
          )
        );
    }
}
