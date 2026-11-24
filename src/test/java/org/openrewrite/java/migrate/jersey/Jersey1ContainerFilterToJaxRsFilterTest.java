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
import org.openrewrite.java.search.FindMethods;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.xml.Assertions.xml;

class Jersey1ContainerFilterToJaxRsFilterTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new Jersey1ContainerFilterToJaxRsFilter())
          .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(),
            "jersey-server-1", "jersey-core-1", "javax.ws.rs-api-2", "jersey-server-2", "jersey-common-2"));
    }

    @DocumentExample
    @Test
    void corsResponseFilterRegisteredOnResourceConfig() {
        rewriteRun(
          java(
            """
              package com.example;

              import com.sun.jersey.spi.container.ContainerRequest;
              import com.sun.jersey.spi.container.ContainerResponse;
              import com.sun.jersey.spi.container.ContainerResponseFilter;

              public class CorsFilter implements ContainerResponseFilter {
                  @Override
                  public ContainerResponse filter(ContainerRequest request, ContainerResponse response) {
                      response.getHttpHeaders().add("Access-Control-Allow-Origin", "*");
                      if (request.getRequestHeaders().containsKey("Access-Control-Request-Headers")) {
                          for (String value : request.getRequestHeaders().get("Access-Control-Request-Headers")) {
                              response.getHttpHeaders().add("Access-Control-Allow-Headers", value);
                          }
                      }
                      return response;
                  }
              }
              """,
            """
              package com.example;

              import javax.ws.rs.container.ContainerRequestContext;
              import javax.ws.rs.container.ContainerResponseContext;
              import javax.ws.rs.container.ContainerResponseFilter;

              public class CorsFilter implements ContainerResponseFilter {
                  @Override
                  public void filter(ContainerRequestContext request, ContainerResponseContext response) {
                      response.getHeaders().add("Access-Control-Allow-Origin", "*");
                      if (request.getHeaders().containsKey("Access-Control-Request-Headers")) {
                          for (String value : request.getHeaders().get("Access-Control-Request-Headers")) {
                              response.getHeaders().add("Access-Control-Allow-Headers", value);
                          }
                      }
                  }
              }
              """
          ),
          java(
            """
              package com.example;

              import com.sun.jersey.api.core.PackagesResourceConfig;
              import com.sun.jersey.api.core.ResourceConfig;

              class Server {
                  ResourceConfig config() {
                      ResourceConfig resourceConfig = new PackagesResourceConfig("com.example");
                      resourceConfig.getContainerResponseFilters().add(new CorsFilter());
                      return resourceConfig;
                  }
              }
              """,
            """
              package com.example;

              import com.sun.jersey.api.core.PackagesResourceConfig;
              import com.sun.jersey.api.core.ResourceConfig;

              class Server {
                  ResourceConfig config() {
                      ResourceConfig resourceConfig = new PackagesResourceConfig("com.example");
                      resourceConfig.register(new CorsFilter());
                      return resourceConfig;
                  }
              }
              """,
            spec -> spec.afterRecipe(cu -> assertThat(FindMethods.find(cu,
              "org.glassfish.jersey.server.ResourceConfig register(Object)")).hasSize(1))
          )
        );
    }

    @Test
    void requestAndResponseFilterRegisteredAsResponseFilter() {
        rewriteRun(
          java(
            """
              package com.example;

              import com.sun.jersey.spi.container.ContainerRequest;
              import com.sun.jersey.spi.container.ContainerRequestFilter;
              import com.sun.jersey.spi.container.ContainerResponse;
              import com.sun.jersey.spi.container.ContainerResponseFilter;

              import javax.ws.rs.core.HttpHeaders;
              import javax.ws.rs.core.MediaType;
              import java.io.File;

              public class IOFilter implements ContainerRequestFilter, ContainerResponseFilter {

                  @Override
                  public ContainerRequest filter(ContainerRequest containerRequest) {
                      System.out.println(containerRequest);
                      return containerRequest;
                  }

                  @Override
                  public ContainerResponse filter(ContainerRequest containerRequest, ContainerResponse containerResponse) {
                      Object entity = containerResponse.getEntity();
                      if (!(entity instanceof File)) {
                          containerResponse.getHttpHeaders().putSingle(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
                          if (!(entity instanceof String)) {
                              containerResponse.setEntity(String.valueOf(entity));
                          }
                      }

                      return containerResponse;
                  }
              }
              """,
            """
              package com.example;

              import javax.ws.rs.container.*;
              import javax.ws.rs.core.HttpHeaders;
              import javax.ws.rs.core.MediaType;
              import java.io.File;

              @PreMatching
              public class IOFilter implements ContainerRequestFilter, ContainerResponseFilter {

                  @Override
                  public void filter(ContainerRequestContext containerRequest) {
                      System.out.println(containerRequest);
                  }

                  @Override
                  public void filter(ContainerRequestContext containerRequest, ContainerResponseContext containerResponse) {
                      Object entity = containerResponse.getEntity();
                      if (!(entity instanceof File)) {
                          containerResponse.getHeaders().putSingle(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
                          if (!(entity instanceof String)) {
                              containerResponse.setEntity(String.valueOf(entity));
                          }
                      }
                  }
              }
              """
          ),
          java(
            """
              package com.example;

              import com.sun.jersey.api.core.PackagesResourceConfig;
              import com.sun.jersey.api.core.ResourceConfig;

              class TestServerMain {
                  static ResourceConfig config() {
                      ResourceConfig resourceConfig = new PackagesResourceConfig("com.example");
                      resourceConfig.getContainerResponseFilters().add(new IOFilter());
                      return resourceConfig;
                  }
              }
              """,
            """
              package com.example;

              import com.sun.jersey.api.core.PackagesResourceConfig;
              import com.sun.jersey.api.core.ResourceConfig;

              import javax.ws.rs.container.ContainerResponseFilter;

              class TestServerMain {
                  static ResourceConfig config() {
                      ResourceConfig resourceConfig = new PackagesResourceConfig("com.example");
                      resourceConfig.register(new IOFilter(), ContainerResponseFilter.class);
                      return resourceConfig;
                  }
              }
              """
          )
        );
    }

    @Test
    void requestFilterMovesCallsBehindUriInfoAndSecurityContext() {
        rewriteRun(
          java(
            """
              package com.example;

              import com.sun.jersey.spi.container.ContainerRequest;
              import com.sun.jersey.spi.container.ContainerRequestFilter;

              import javax.ws.rs.WebApplicationException;
              import java.net.URI;

              public class ForwardedFilter implements ContainerRequestFilter {
                  @Override
                  public ContainerRequest filter(ContainerRequest request) {
                      String host = request.getHeaderValue("X-Forwarded-Host");
                      if (host == null || request.getQueryParameters().containsKey("direct")) {
                          return request;
                      }
                      if (!request.isUserInRole("proxy") || request.getUserPrincipal() == null) {
                          throw new WebApplicationException(403);
                      }
                      URI base = request.getBaseUriBuilder().host(host).build();
                      URI uri = request.getRequestUriBuilder().host(host).build();
                      request.setUris(base, uri);
                      if ("POST".equals(request.getMethod()) && request.getPath().endsWith("/delete")) {
                          request.setMethod("DELETE");
                      }
                      return request;
                  }
              }
              """,
            """
              package com.example;

              import javax.ws.rs.WebApplicationException;
              import javax.ws.rs.container.ContainerRequestContext;
              import javax.ws.rs.container.ContainerRequestFilter;
              import javax.ws.rs.container.PreMatching;

              import java.net.URI;

              @PreMatching
              public class ForwardedFilter implements ContainerRequestFilter {
                  @Override
                  public void filter(ContainerRequestContext request) {
                      String host = request.getHeaderString("X-Forwarded-Host");
                      if (host == null || request.getUriInfo().getQueryParameters().containsKey("direct")) {
                          return;
                      }
                      if (!request.getSecurityContext().isUserInRole("proxy") || request.getSecurityContext().getUserPrincipal() == null) {
                          throw new WebApplicationException(403);
                      }
                      URI base = request.getUriInfo().getBaseUriBuilder().host(host).build();
                      URI uri = request.getUriInfo().getRequestUriBuilder().host(host).build();
                      request.setRequestUri(base, uri);
                      if ("POST".equals(request.getMethod()) && request.getUriInfo().getPath().endsWith("/delete")) {
                          request.setMethod("DELETE");
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void registeredAsClassInResourceConfigSubclass() {
        rewriteRun(
          java(
            """
              package com.example;

              import com.sun.jersey.spi.container.ContainerRequest;
              import com.sun.jersey.spi.container.ContainerRequestFilter;

              public class AuthFilter implements ContainerRequestFilter {
                  @Override
                  public ContainerRequest filter(ContainerRequest request) {
                      if (request.getRequestHeader("Authorization") == null) {
                          throw new SecurityException("no credentials for " + request);
                      }
                      return request;
                  }
              }
              """,
            """
              package com.example;

              import javax.ws.rs.container.ContainerRequestContext;
              import javax.ws.rs.container.ContainerRequestFilter;
              import javax.ws.rs.container.PreMatching;

              @PreMatching
              public class AuthFilter implements ContainerRequestFilter {
                  @Override
                  public void filter(ContainerRequestContext request) {
                      if (request.getHeaders().get("Authorization") == null) {
                          throw new SecurityException("no credentials for " + request);
                      }
                  }
              }
              """
          ),
          java(
            """
              package com.example;

              import com.sun.jersey.api.core.DefaultResourceConfig;
              import com.sun.jersey.api.core.ResourceConfig;

              class AppConfig extends DefaultResourceConfig {
                  AppConfig() {
                      getContainerRequestFilters().add(AuthFilter.class);
                      getProperties().put(ResourceConfig.PROPERTY_CONTAINER_REQUEST_FILTERS, AuthFilter.class.getName());
                  }
              }
              """,
            """
              package com.example;

              import com.sun.jersey.api.core.DefaultResourceConfig;

              class AppConfig extends DefaultResourceConfig {
                  AppConfig() {
                      register(AuthFilter.class);
                      register(AuthFilter.class);
                  }
              }
              """
          )
        );
    }

    @Test
    void propertyThatAnotherRecipeAlreadyMovedToJersey2() {
        rewriteRun(
          java(
            """
              package com.example;

              import com.sun.jersey.spi.container.ContainerRequest;
              import com.sun.jersey.spi.container.ContainerResponse;
              import com.sun.jersey.spi.container.ContainerResponseFilter;

              public class StatusFilter implements ContainerResponseFilter {
                  @Override
                  public ContainerResponse filter(ContainerRequest request, ContainerResponse response) {
                      if (response.getStatus() == 204 && response.getMediaType() == null) {
                          response.setStatus(200);
                      }
                      return response;
                  }
              }
              """,
            """
              package com.example;

              import javax.ws.rs.container.ContainerRequestContext;
              import javax.ws.rs.container.ContainerResponseContext;
              import javax.ws.rs.container.ContainerResponseFilter;

              public class StatusFilter implements ContainerResponseFilter {
                  @Override
                  public void filter(ContainerRequestContext request, ContainerResponseContext response) {
                      if (response.getStatus() == 204 && response.getMediaType() == null) {
                          response.setStatus(200);
                      }
                  }
              }
              """
          ),
          java(
            """
              package com.example;

              import org.glassfish.jersey.server.ResourceConfig;

              class Server {
                  void configure(ResourceConfig config) {
                      config.property(com.sun.jersey.api.core.ResourceConfig.PROPERTY_CONTAINER_RESPONSE_FILTERS, StatusFilter.class);
                  }
              }
              """,
            """
              package com.example;

              import org.glassfish.jersey.server.ResourceConfig;

              class Server {
                  void configure(ResourceConfig config) {
                      config.register(StatusFilter.class);
                  }
              }
              """
          )
        );
    }

    @Test
    void leavesFiltersWithoutAFaithfulCounterpartAlone() {
        rewriteRun(
          java(
            """
              package com.example;

              import com.sun.jersey.spi.container.ContainerRequest;
              import com.sun.jersey.spi.container.ContainerRequestFilter;
              import com.sun.jersey.spi.container.ContainerResponse;
              import com.sun.jersey.spi.container.ContainerResponseFilter;

              import javax.ws.rs.core.Response;

              class Filters {
                  static class DecodingFilter implements ContainerRequestFilter {
                      @Override
                      public ContainerRequest filter(ContainerRequest request) {
                          request.getQueryParameters().putSingle("token", "decoded");
                          return request;
                      }
                  }

                  static class EscapingFilter implements ContainerRequestFilter {
                      @Override
                      public ContainerRequest filter(ContainerRequest request) {
                          audit(request);
                          return request;
                      }

                      private void audit(ContainerRequest request) {
                      }
                  }

                  static class ReplacingFilter implements ContainerResponseFilter {
                      @Override
                      public ContainerResponse filter(ContainerRequest request, ContainerResponse response) {
                          response.setResponse(Response.fromResponse(response.getResponse()).header("X-Seen", "1").build());
                          return response;
                      }
                  }

                  static class RewritingFilter implements ContainerResponseFilter {
                      @Override
                      public ContainerResponse filter(ContainerRequest request, ContainerResponse response) {
                          request.setMethod("GET");
                          return response;
                      }
                  }

                  static class SwappingFilter implements ContainerRequestFilter {
                      private ContainerRequest last;

                      @Override
                      public ContainerRequest filter(ContainerRequest request) {
                          ContainerRequest previous = last;
                          last = request;
                          return previous == null ? request : previous;
                      }
                  }
              }
              """
          )
        );
    }

    @Test
    void leavesFiltersUsedAsJersey1TypesAlone() {
        rewriteRun(
          java(
            """
              package com.example;

              import com.sun.jersey.api.core.ResourceConfig;
              import com.sun.jersey.spi.container.ContainerRequest;
              import com.sun.jersey.spi.container.ContainerRequestFilter;
              import com.sun.jersey.spi.container.ContainerResponseFilter;
              import com.sun.jersey.spi.container.ResourceFilter;

              public class RestFilter implements ContainerRequestFilter, ResourceFilter {
                  @Override
                  public ContainerRequest filter(ContainerRequest request) {
                      return request;
                  }

                  @Override
                  public ContainerRequestFilter getRequestFilter() {
                      return this;
                  }

                  @Override
                  public ContainerResponseFilter getResponseFilter() {
                      return null;
                  }
              }
              """
          ),
          java(
            """
              package com.example;

              import com.sun.jersey.spi.container.ContainerRequest;
              import com.sun.jersey.spi.container.ContainerRequestFilter;

              public class ForwardFilter implements ContainerRequestFilter {
                  @Override
                  public ContainerRequest filter(ContainerRequest request) {
                      request.getRequestHeaders().remove("X-Forwarded-For");
                      return request;
                  }
              }
              """
          ),
          java(
            """
              package com.example;

              import com.sun.jersey.spi.container.ContainerRequest;

              class ForwardFilterTest {
                  ContainerRequest check(ContainerRequest request) {
                      return new ForwardFilter().filter(request);
                  }
              }
              """
          ),
          java(
            """
              package com.example;

              import com.sun.jersey.spi.container.ContainerRequest;
              import com.sun.jersey.spi.container.ContainerRequestFilter;

              public class BaseFilter implements ContainerRequestFilter {
                  @Override
                  public ContainerRequest filter(ContainerRequest request) {
                      return request;
                  }
              }
              """
          ),
          java(
            """
              package com.example;

              import com.sun.jersey.api.core.ResourceConfig;

              class Server {
                  void configure(ResourceConfig config) {
                      config.getContainerRequestFilters().add(new BaseFilter() {
                      });
                      config.getContainerRequestFilters().add(new com.sun.jersey.api.container.filter.LoggingFilter());
                  }
              }
              """
          )
        );
    }

    @Test
    void leavesRequestAndResponseFilterThatWebXmlListsOnceAlone() {
        rewriteRun(
          java(
            """
              package com.example;

              import com.sun.jersey.spi.container.ContainerRequest;
              import com.sun.jersey.spi.container.ContainerRequestFilter;
              import com.sun.jersey.spi.container.ContainerResponse;
              import com.sun.jersey.spi.container.ContainerResponseFilter;

              public class TimingFilter implements ContainerRequestFilter, ContainerResponseFilter {
                  @Override
                  public ContainerRequest filter(ContainerRequest request) {
                      return request;
                  }

                  @Override
                  public ContainerResponse filter(ContainerRequest request, ContainerResponse response) {
                      response.getHttpHeaders().putSingle("X-Time", System.nanoTime());
                      return response;
                  }
              }
              """
          ),
          //language=xml
          xml(
            """
              <web-app>
                  <servlet>
                      <servlet-name>jersey</servlet-name>
                      <servlet-class>com.sun.jersey.spi.container.servlet.ServletContainer</servlet-class>
                      <init-param>
                          <param-name>com.sun.jersey.spi.container.ContainerResponseFilters</param-name>
                          <param-value>com.example.TimingFilter</param-value>
                      </init-param>
                  </servlet>
              </web-app>
              """,
            spec -> spec.path("src/main/webapp/WEB-INF/web.xml")
          )
        );
    }
}
