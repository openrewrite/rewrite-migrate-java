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

class Jersey1ClientResponseToResponseTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new Jersey1ClientResponseToResponse())
          .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(),
            "jersey-client-1", "jersey-core-1", "javax.ws.rs-api-2"));
    }

    @DocumentExample
    @Test
    void statusAndEntityStream() {
        rewriteRun(
          java(
            """
              import com.sun.jersey.api.client.ClientResponse;
              import com.sun.jersey.api.client.WebResource;

              import java.io.InputStream;

              class Test {
                  InputStream download(WebResource resource) {
                      ClientResponse response = resource.get(ClientResponse.class);
                      if (response.getClientResponseStatus() != ClientResponse.Status.OK) {
                          throw new IllegalStateException("Failed: " + response.getClientResponseStatus());
                      }
                      return response.getEntityInputStream();
                  }
              }
              """,
            """
              import com.sun.jersey.api.client.ClientResponse;
              import com.sun.jersey.api.client.WebResource;

              import java.io.InputStream;

              import javax.ws.rs.core.Response;

              class Test {
                  InputStream download(WebResource resource) {
                      ClientResponse response = resource.get(ClientResponse.class);
                      if (Response.Status.fromStatusCode(response.getStatus()) != ClientResponse.Status.OK) {
                          throw new IllegalStateException("Failed: " + Response.Status.fromStatusCode(response.getStatus()));
                      }
                      return response.readEntity(InputStream.class);
                  }
              }
              """
          )
        );
    }

    @Test
    void deprecatedStatusInSwitch() {
        rewriteRun(
          java(
            """
              import com.sun.jersey.api.client.ClientResponse;

              class Test {
                  boolean ok(ClientResponse response) {
                      switch (response.getResponseStatus()) {
                          case OK:
                          case CREATED:
                              return true;
                          default:
                              return false;
                      }
                  }
              }
              """,
            """
              import com.sun.jersey.api.client.ClientResponse;

              import javax.ws.rs.core.Response;

              class Test {
                  boolean ok(ClientResponse response) {
                      switch (Response.Status.fromStatusCode(response.getStatus())) {
                          case OK:
                          case CREATED:
                              return true;
                          default:
                              return false;
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void headersThatChangedType() {
        rewriteRun(
          java(
            """
              import com.sun.jersey.api.client.ClientResponse;

              import javax.ws.rs.core.NewCookie;
              import java.util.List;

              class Test {
                  String language(ClientResponse response) {
                      return response.getLanguage();
                  }
                  List<NewCookie> cookies(ClientResponse response) {
                      return response.getCookies();
                  }
              }
              """,
            """
              import com.sun.jersey.api.client.ClientResponse;

              import javax.ws.rs.core.HttpHeaders;
              import javax.ws.rs.core.NewCookie;

              import java.util.ArrayList;
              import java.util.List;

              class Test {
                  String language(ClientResponse response) {
                      return response.getHeaderString(HttpHeaders.CONTENT_LANGUAGE);
                  }
                  List<NewCookie> cookies(ClientResponse response) {
                      return new ArrayList<NewCookie>(response.getCookies().values());
                  }
              }
              """
          )
        );
    }

    @Test
    void statusFamilyAndMisspeltConstant() {
        rewriteRun(
          java(
            """
              import com.sun.jersey.api.client.ClientResponse;

              import javax.ws.rs.core.Response;

              class Test {
                  Response.Status.Family family(ClientResponse response) {
                      return ClientResponse.Status.getFamilyByStatusCode(response.getStatus());
                  }
                  boolean unsatisfiable(ClientResponse.Status status) {
                      switch (status) {
                          case REQUESTED_RANGE_NOT_SATIFIABLE:
                              return true;
                          default:
                              return status == ClientResponse.Status.REQUESTED_RANGE_NOT_SATIFIABLE;
                      }
                  }
              }
              """,
            """
              import com.sun.jersey.api.client.ClientResponse;

              import javax.ws.rs.core.Response;

              class Test {
                  Response.Status.Family family(ClientResponse response) {
                      return Response.Status.Family.familyOf(response.getStatus());
                  }
                  boolean unsatisfiable(ClientResponse.Status status) {
                      switch (status) {
                          case REQUESTED_RANGE_NOT_SATISFIABLE:
                              return true;
                          default:
                              return status == ClientResponse.Status.REQUESTED_RANGE_NOT_SATISFIABLE;
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void responseOfAnotherLibraryIsImported() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion()
            .classpathFromResources(new InMemoryExecutionContext(), "jersey-client-1", "jersey-core-1", "javax.ws.rs-api-2")
            .dependsOn("package okhttp3; public class Response { public int code() { return 200; } }")),
          java(
            """
              import com.sun.jersey.api.client.ClientResponse;
              import okhttp3.Response;

              class Test {
                  boolean same(ClientResponse response, Response other) {
                      return response.getClientResponseStatus().getStatusCode() == other.code();
                  }
              }
              """,
            """
              import com.sun.jersey.api.client.ClientResponse;
              import okhttp3.Response;

              class Test {
                  boolean same(ClientResponse response, Response other) {
                      return javax.ws.rs.core.Response.Status.fromStatusCode(response.getStatus()).getStatusCode() == other.code();
                  }
              }
              """
          )
        );
    }

    @Test
    void leavesConstantWithoutCounterpartAlone() {
        rewriteRun(
          java(
            """
              import com.sun.jersey.api.client.ClientResponse;

              class Test {
                  boolean nonAuthoritative(ClientResponse.Status status) {
                      return status == ClientResponse.Status.NON_AUTHORITIVE_INFORMATION;
                  }
              }
              """
          )
        );
    }
}
