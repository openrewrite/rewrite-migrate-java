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

class Jersey1ClientFilterToClientRequestFilterTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new Jersey1ClientFilterToClientRequestFilter())
          .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(),
            "jersey-client-1", "jersey-core-1", "javax.ws.rs-api-2"));
    }

    @DocumentExample
    @Test
    void anonymousFilterThatAddsAHeader() {
        rewriteRun(
          java(
            """
              import com.sun.jersey.api.client.Client;
              import com.sun.jersey.api.client.ClientHandlerException;
              import com.sun.jersey.api.client.ClientRequest;
              import com.sun.jersey.api.client.ClientResponse;
              import com.sun.jersey.api.client.filter.ClientFilter;

              class Test {
                  void authenticate(Client client, final String auth) {
                      client.addFilter(new ClientFilter() {
                          @Override
                          public ClientResponse handle(ClientRequest cr) throws ClientHandlerException {
                              cr.getHeaders().add("Authorization", "Basic " + auth);
                              return getNext().handle(cr);
                          }
                      });
                  }
              }
              """,
            """
              import com.sun.jersey.api.client.Client;

              import javax.ws.rs.client.ClientRequestContext;
              import javax.ws.rs.client.ClientRequestFilter;

              import java.io.IOException;

              class Test {
                  void authenticate(Client client, final String auth) {
                      client.register(new ClientRequestFilter() {
                          @Override
                          public void filter(ClientRequestContext cr) throws IOException {
                              cr.getHeaders().add("Authorization", "Basic " + auth);
                          }
                      });
                  }
              }
              """
          )
        );
    }

    @Test
    void topLevelFilterRegisteredFromAnotherFile() {
        rewriteRun(
          java(
            """
              package com.example;

              import com.sun.jersey.api.client.ClientRequest;
              import com.sun.jersey.api.client.ClientResponse;
              import com.sun.jersey.api.client.filter.ClientFilter;

              import java.io.Serializable;
              import java.net.URI;
              import javax.ws.rs.core.UriBuilder;

              public class RewritingFilter extends ClientFilter implements Serializable {
                  private final String token;

                  public RewritingFilter(String token) {
                      this.token = token;
                  }

                  @Override
                  public ClientResponse handle(final ClientRequest request) {
                      if (token == null) {
                          return getNext().handle(request);
                      }
                      URI uri = UriBuilder.fromUri(request.getURI()).queryParam("token", token).build();
                      request.setURI(uri);
                      if ("PATCH".equals(request.getMethod())) {
                          request.setMethod("POST");
                          request.getMetadata().putSingle("X-HTTP-Method-Override", "PATCH");
                      }
                      return this.getNext().handle(request);
                  }
              }
              """,
            """
              package com.example;

              import java.io.IOException;
              import java.io.Serializable;
              import java.net.URI;

              import javax.ws.rs.client.ClientRequestContext;
              import javax.ws.rs.client.ClientRequestFilter;
              import javax.ws.rs.core.UriBuilder;

              public class RewritingFilter implements Serializable, ClientRequestFilter {
                  private final String token;

                  public RewritingFilter(String token) {
                      this.token = token;
                  }

                  @Override
                  public void filter(final ClientRequestContext request) throws IOException {
                      if (token == null) {
                          return;
                      }
                      URI uri = UriBuilder.fromUri(request.getUri()).queryParam("token", token).build();
                      request.setUri(uri);
                      if ("PATCH".equals(request.getMethod())) {
                          request.setMethod("POST");
                          request.getHeaders().putSingle("X-HTTP-Method-Override", "PATCH");
                      }
                  }
              }
              """
          ),
          java(
            """
              package com.example;

              import com.sun.jersey.api.client.Client;
              import com.sun.jersey.api.client.WebResource;

              class Usage {
                  private final RewritingFilter filter = new RewritingFilter("secret");

                  void install(Client client, WebResource resource) {
                      client.addFilter(new RewritingFilter("t"));
                      resource.addFilter(filter);
                  }
              }
              """,
            """
              package com.example;

              import com.sun.jersey.api.client.Client;
              import com.sun.jersey.api.client.WebResource;

              class Usage {
                  private final RewritingFilter filter = new RewritingFilter("secret");

                  void install(Client client, WebResource resource) {
                      client.register(new RewritingFilter("t"));
                      resource.register(filter);
                  }
              }
              """
          )
        );
    }

    @Test
    void innerFilterWithoutThrowsClause() {
        rewriteRun(
          java(
            """
              import com.sun.jersey.api.client.Client;
              import com.sun.jersey.api.client.ClientRequest;
              import com.sun.jersey.api.client.ClientResponse;
              import com.sun.jersey.api.client.filter.ClientFilter;

              class Test {
                  static class UserAgentFilter extends ClientFilter {
                      @Override
                      public ClientResponse handle(ClientRequest request) {
                          request.getHeaders().putSingle("User-Agent", "test");
                          return getNext().handle(request);
                      }
                  }

                  void install(Client client) {
                      client.addFilter(new UserAgentFilter());
                  }
              }
              """,
            """
              import com.sun.jersey.api.client.Client;

              import javax.ws.rs.client.ClientRequestContext;
              import javax.ws.rs.client.ClientRequestFilter;

              import java.io.IOException;

              class Test {
                  static class UserAgentFilter implements ClientRequestFilter {
                      @Override
                      public void filter(ClientRequestContext request) throws IOException {
                          request.getHeaders().putSingle("User-Agent", "test");
                      }
                  }

                  void install(Client client) {
                      client.register(new UserAgentFilter());
                  }
              }
              """
          )
        );
    }

    @Test
    void leavesFiltersThatUseTheResponseAlone() {
        rewriteRun(
          java(
            """
              import com.sun.jersey.api.client.Client;
              import com.sun.jersey.api.client.ClientRequest;
              import com.sun.jersey.api.client.ClientResponse;
              import com.sun.jersey.api.client.filter.ClientFilter;

              class Test {
                  void install(Client client) {
                      client.addFilter(new ClientFilter() {
                          @Override
                          public ClientResponse handle(ClientRequest request) {
                              ClientResponse response = getNext().handle(request);
                              if (response.getStatus() == 401) {
                                  throw new IllegalStateException();
                              }
                              return response;
                          }
                      });
                      client.addFilter(new ClientFilter() {
                          @Override
                          public ClientResponse handle(ClientRequest request) {
                              request.getHeaders().add("X-Retry", "1");
                              getNext().handle(request);
                              return getNext().handle(request);
                          }
                      });
                  }
              }
              """
          )
        );
    }

    @Test
    void leavesFiltersThatUseTheRequestOtherwiseAlone() {
        rewriteRun(
          java(
            """
              import com.sun.jersey.api.client.Client;
              import com.sun.jersey.api.client.ClientRequest;
              import com.sun.jersey.api.client.ClientResponse;
              import com.sun.jersey.api.client.filter.ClientFilter;

              class Test {
                  void install(Client client) {
                      client.addFilter(new ClientFilter() {
                          @Override
                          public ClientResponse handle(ClientRequest request) {
                              sign(request);
                              return getNext().handle(request);
                          }
                      });
                      client.addFilter(new ClientFilter() {
                          @Override
                          public ClientResponse handle(ClientRequest request) {
                              request.getProperties().put("started", System.nanoTime());
                              return getNext().handle(request);
                          }
                      });
                  }

                  void sign(ClientRequest request) {
                  }
              }
              """
          )
        );
    }

    @Test
    void leavesFiltersUsedAsClientFiltersAlone() {
        rewriteRun(
          java(
            """
              import com.sun.jersey.api.client.Client;
              import com.sun.jersey.api.client.ClientRequest;
              import com.sun.jersey.api.client.ClientResponse;
              import com.sun.jersey.api.client.filter.ClientFilter;

              class Test {
                  static class Removed extends ClientFilter {
                      @Override
                      public ClientResponse handle(ClientRequest request) {
                          request.getHeaders().add("X-Removed", "1");
                          return getNext().handle(request);
                      }
                  }

                  static class Extended extends ClientFilter {
                      @Override
                      public ClientResponse handle(ClientRequest request) {
                          request.getHeaders().add("X-Extended", "1");
                          return getNext().handle(request);
                      }
                  }

                  static class Extending extends Extended {
                  }

                  private final ClientFilter anonymous = new ClientFilter() {
                      @Override
                      public ClientResponse handle(ClientRequest request) {
                          return getNext().handle(request);
                      }
                  };

                  void install(Client client) {
                      Removed removed = new Removed();
                      client.addFilter(removed);
                      client.removeFilter(removed);
                      client.addFilter(new Extending());
                      client.addFilter(anonymous);
                  }
              }
              """
          )
        );
    }
}
