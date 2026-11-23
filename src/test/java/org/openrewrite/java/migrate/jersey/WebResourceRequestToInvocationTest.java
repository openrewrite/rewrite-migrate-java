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

class WebResourceRequestToInvocationTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new WebResourceRequestToInvocation())
          .parser(JavaParser.fromJavaVersion()
            .classpathFromResources(new InMemoryExecutionContext(), "jersey-client-1.19", "jersey-core-1.19", "javax.ws.rs-api-2.1"));
    }

    @DocumentExample
    @Test
    void acceptBecomesRequestAndTypeBecomesEntity() {
        rewriteRun(
          //language=java
          java(
            """
              import com.sun.jersey.api.client.ClientResponse;
              import com.sun.jersey.api.client.WebResource;

              import javax.ws.rs.core.MediaType;

              class A {
                  String find(WebResource resource, String id) {
                      return resource.path("items")
                              .path(id)
                              .accept(MediaType.APPLICATION_JSON)
                              .get(String.class);
                  }

                  ClientResponse create(WebResource resource, Object item) {
                      return resource.path("items")
                              .accept(MediaType.APPLICATION_JSON)
                              .type(MediaType.APPLICATION_JSON_TYPE)
                              .post(ClientResponse.class, item);
                  }
              }
              """,
            """
              import com.sun.jersey.api.client.ClientResponse;
              import com.sun.jersey.api.client.WebResource;

              import javax.ws.rs.client.Entity;
              import javax.ws.rs.core.MediaType;

              class A {
                  String find(WebResource resource, String id) {
                      return resource.path("items")
                              .path(id)
                              .request(MediaType.APPLICATION_JSON)
                              .get(String.class);
                  }

                  ClientResponse create(WebResource resource, Object item) {
                      return resource.path("items")
                              .request(MediaType.APPLICATION_JSON)
                              .post(Entity.entity(item, MediaType.APPLICATION_JSON_TYPE), ClientResponse.class);
                  }
              }
              """
          )
        );
    }

    @Test
    void requestSentFromTheResourceItself() {
        rewriteRun(
          //language=java
          java(
            """
              import com.sun.jersey.api.client.ClientResponse;
              import com.sun.jersey.api.client.WebResource;

              class A {
                  void run(WebResource resource) {
                      String body = resource.queryParam("q", "x").get(String.class);
                      ClientResponse response = resource.type("application/xml").put(ClientResponse.class, body);
                      resource.delete();
                      resource.header("X-Token", "t").post("text");
                  }
              }
              """,
            """
              import com.sun.jersey.api.client.ClientResponse;
              import com.sun.jersey.api.client.WebResource;

              import javax.ws.rs.client.Entity;

              class A {
                  void run(WebResource resource) {
                      String body = resource.queryParam("q", "x").request().get(String.class);
                      ClientResponse response = resource.request().put(Entity.entity(body, "application/xml"), ClientResponse.class);
                      resource.request().delete(String.class);
                      resource.request().header("X-Token", "t").post(Entity.text("text"), String.class);
                  }
              }
              """
          )
        );
    }

    @Test
    void mediaTypeFollowsTheEntityWhenNotGiven() {
        rewriteRun(
          //language=java
          java(
            """
              import com.sun.jersey.api.client.ClientResponse;
              import com.sun.jersey.api.client.WebResource;
              import com.sun.jersey.core.util.MultivaluedMapImpl;

              import java.io.InputStream;

              class A {
                  void run(WebResource resource, InputStream in, MultivaluedMapImpl form, Item item) {
                      resource.post(ClientResponse.class, in);
                      resource.post(ClientResponse.class, form);
                      resource.post(ClientResponse.class, item);
                      resource.post(ClientResponse.class);
                      resource.post(ClientResponse.class, null);
                  }

                  static class Item {
                  }
              }
              """,
            """
              import com.sun.jersey.api.client.ClientResponse;
              import com.sun.jersey.api.client.WebResource;
              import com.sun.jersey.core.util.MultivaluedMapImpl;

              import java.io.InputStream;

              import javax.ws.rs.client.Entity;
              import javax.ws.rs.core.MediaType;

              class A {
                  void run(WebResource resource, InputStream in, MultivaluedMapImpl form, Item item) {
                      resource.request().post(Entity.entity(in, MediaType.APPLICATION_OCTET_STREAM_TYPE), ClientResponse.class);
                      resource.request().post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE), ClientResponse.class);
                      resource.request().post(Entity.json(item), ClientResponse.class);
                      resource.request().post(null, ClientResponse.class);
                      resource.request().post(null, ClientResponse.class);
                  }

                  static class Item {
                  }
              }
              """
          )
        );
    }

    @Test
    void builderVariables() {
        rewriteRun(
          //language=java
          java(
            """
              import com.sun.jersey.api.client.ClientResponse;
              import com.sun.jersey.api.client.WebResource;

              import java.util.Map;

              class A {
                  ClientResponse call(WebResource resource, String accept, Map<String, String> headers, String method, Object body) {
                      WebResource.Builder builder = resource.accept(accept);
                      for (Map.Entry<String, String> header : headers.entrySet()) {
                          builder = builder.header(header.getKey(), header.getValue());
                      }
                      if ("GET".equals(method)) {
                          return builder.get(ClientResponse.class);
                      } else if ("DELETE".equals(method)) {
                          return builder.type("application/json").delete(ClientResponse.class, body);
                      }
                      return builder.type("application/json").method(method, ClientResponse.class, body);
                  }

                  WebResource.Builder start(WebResource resource) {
                      return resource.getRequestBuilder();
                  }
              }
              """,
            """
              import com.sun.jersey.api.client.ClientResponse;
              import com.sun.jersey.api.client.WebResource;

              import java.util.Map;

              import javax.ws.rs.client.Entity;

              class A {
                  ClientResponse call(WebResource resource, String accept, Map<String, String> headers, String method, Object body) {
                      WebResource.Builder builder = resource.request(accept);
                      for (Map.Entry<String, String> header : headers.entrySet()) {
                          builder = builder.header(header.getKey(), header.getValue());
                      }
                      if ("GET".equals(method)) {
                          return builder.get(ClientResponse.class);
                      } else if ("DELETE".equals(method)) {
                          return builder.method("DELETE", Entity.entity(body, "application/json"), ClientResponse.class);
                      }
                      return builder.method(method, Entity.entity(body, "application/json"), ClientResponse.class);
                  }

                  WebResource.Builder start(WebResource resource) {
                      return resource.getRequestBuilder();
                  }
              }
              """
          )
        );
    }

    @Test
    void genericTypesAndEntitySteps() {
        rewriteRun(
          //language=java
          java(
            """
              import com.sun.jersey.api.client.GenericType;
              import com.sun.jersey.api.client.WebResource;

              import java.util.List;

              class A {
                  List<String> names(WebResource resource, String json) {
                      resource.entity(json, "application/json").put();
                      return resource.accept("application/json").get(new GenericType<List<String>>() {
                      });
                  }
              }
              """,
            """
              import com.sun.jersey.api.client.GenericType;
              import com.sun.jersey.api.client.WebResource;

              import java.util.List;

              import javax.ws.rs.client.Entity;

              class A {
                  List<String> names(WebResource resource, String json) {
                      resource.request().put(Entity.entity(json, "application/json"), String.class);
                      return resource.request("application/json").get(new GenericType<List<String>>() {
                      });
                  }
              }
              """
          )
        );
    }

    @Test
    void leaveWhatJerseyTwoCannotExpress() {
        rewriteRun(
          //language=java
          java(
            """
              import com.sun.jersey.api.client.WebResource;

              class A {
                  WebResource.Builder typed(WebResource.Builder builder) {
                      return builder.type("application/json");
                  }

                  void emptyPut(WebResource resource) {
                      resource.put();
                  }
              }
              """
          )
        );
    }
}
