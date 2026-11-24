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
import static org.openrewrite.xml.Assertions.xml;

class MigrateJersey1ContainerFiltersInWebXmlTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigrateJersey1ContainerFiltersInWebXml())
          .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(),
            "jersey-server-1", "jersey-core-1", "javax.ws.rs-api-2"));
    }

    //language=java
    private static final String CORS_FILTER = """
      package com.example;

      import com.sun.jersey.spi.container.ContainerRequest;
      import com.sun.jersey.spi.container.ContainerResponse;
      import com.sun.jersey.spi.container.ContainerResponseFilter;

      public class CORSFilter implements ContainerResponseFilter {
          @Override
          public ContainerResponse filter(ContainerRequest request, ContainerResponse response) {
              response.getHttpHeaders().add("Access-Control-Allow-Origin", "*");
              return response;
          }
      }
      """;

    //language=java
    private static final String TIMING_FILTER = """
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
      """;

    @DocumentExample
    @Test
    void responseFilterBecomesProvider() {
        rewriteRun(
          java(CORS_FILTER),
          //language=xml
          xml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <web-app xmlns="http://java.sun.com/xml/ns/javaee" version="3.0">
                  <servlet>
                      <servlet-name>api</servlet-name>
                      <servlet-class>com.sun.jersey.spi.container.servlet.ServletContainer</servlet-class>
                      <init-param>
                          <param-name>com.sun.jersey.config.property.packages</param-name>
                          <param-value>com.example</param-value>
                      </init-param>
                      <init-param>
                          <param-name>com.sun.jersey.spi.container.ContainerResponseFilters</param-name>
                          <param-value>com.example.CORSFilter</param-value>
                      </init-param>
                      <load-on-startup>1</load-on-startup>
                  </servlet>
              </web-app>
              """,
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <web-app xmlns="http://java.sun.com/xml/ns/javaee" version="3.0">
                  <servlet>
                      <servlet-name>api</servlet-name>
                      <servlet-class>com.sun.jersey.spi.container.servlet.ServletContainer</servlet-class>
                      <init-param>
                          <param-name>com.sun.jersey.config.property.packages</param-name>
                          <param-value>com.example</param-value>
                      </init-param>
                      <init-param>
                          <param-name>jersey.config.server.provider.classnames</param-name>
                          <param-value>com.example.CORSFilter</param-value>
                      </init-param>
                      <load-on-startup>1</load-on-startup>
                  </servlet>
              </web-app>
              """,
            spec -> spec.path("src/main/webapp/WEB-INF/web.xml")
          )
        );
    }

    @Test
    void mergesWithClassNamesAndKeepsWhatDoesNotConvert() {
        rewriteRun(
          java(CORS_FILTER),
          java(TIMING_FILTER),
          //language=xml
          xml(
            """
              <web-app>
                  <filter>
                      <filter-name>jersey</filter-name>
                      <filter-class>org.glassfish.jersey.servlet.ServletContainer</filter-class>
                      <init-param>
                          <param-name>jersey.config.server.provider.classnames</param-name>
                          <param-value>com.example.JsonProvider;com.example.HelloResource</param-value>
                      </init-param>
                      <init-param>
                          <param-name>com.sun.jersey.spi.container.ContainerRequestFilters</param-name>
                          <param-value>
                              com.example.TimingFilter,
                              com.example.lib.AuditFilter
                          </param-value>
                      </init-param>
                      <init-param>
                          <param-name>com.sun.jersey.spi.container.ContainerResponseFilters</param-name>
                          <param-value>com.example.CORSFilter com.example.TimingFilter</param-value>
                      </init-param>
                  </filter>
              </web-app>
              """,
            """
              <web-app>
                  <filter>
                      <filter-name>jersey</filter-name>
                      <filter-class>org.glassfish.jersey.servlet.ServletContainer</filter-class>
                      <init-param>
                          <param-name>jersey.config.server.provider.classnames</param-name>
                          <param-value>com.example.JsonProvider;com.example.HelloResource;com.example.TimingFilter;com.example.CORSFilter</param-value>
                      </init-param>
                      <init-param>
                          <param-name>com.sun.jersey.spi.container.ContainerRequestFilters</param-name>
                          <param-value>
                              com.example.lib.AuditFilter
                          </param-value>
                      </init-param>
                  </filter>
              </web-app>
              """,
            spec -> spec.path("src/main/webapp/WEB-INF/web.xml")
          )
        );
    }

    @Test
    void jersey1FiltersWithJersey2Counterparts() {
        rewriteRun(
          //language=xml
          xml(
            """
              <web-app>
                  <servlet>
                      <servlet-name>jersey</servlet-name>
                      <servlet-class>com.sun.jersey.spi.container.servlet.ServletContainer</servlet-class>
                      <init-param>
                          <param-name>com.sun.jersey.spi.container.ContainerRequestFilters</param-name>
                          <param-value>com.sun.jersey.api.container.filter.LoggingFilter;com.sun.jersey.api.container.filter.GZIPContentEncodingFilter;com.sun.jersey.api.container.filter.CsrfProtectionFilter;com.sun.jersey.api.container.filter.PostReplaceFilter</param-value>
                      </init-param>
                      <init-param>
                          <param-name>com.sun.jersey.spi.container.ContainerResponseFilters</param-name>
                          <param-value>com.sun.jersey.api.container.filter.LoggingFilter;com.sun.jersey.api.container.filter.GZIPContentEncodingFilter</param-value>
                      </init-param>
                  </servlet>
              </web-app>
              """,
            """
              <web-app>
                  <servlet>
                      <servlet-name>jersey</servlet-name>
                      <servlet-class>com.sun.jersey.spi.container.servlet.ServletContainer</servlet-class>
                      <init-param>
                          <param-name>com.sun.jersey.spi.container.ContainerRequestFilters</param-name>
                          <param-value>com.sun.jersey.api.container.filter.LoggingFilter</param-value>
                      </init-param>
                      <init-param>
                          <param-name>com.sun.jersey.spi.container.ContainerResponseFilters</param-name>
                          <param-value>com.sun.jersey.api.container.filter.LoggingFilter</param-value>
                      </init-param>
                      <init-param>
                          <param-name>jersey.config.server.provider.classnames</param-name>
                          <param-value>org.glassfish.jersey.message.GZipEncoder,org.glassfish.jersey.server.filter.CsrfProtectionFilter,org.glassfish.jersey.server.filter.HttpMethodOverrideFilter,org.glassfish.jersey.server.filter.EncodingFilter</param-value>
                      </init-param>
                  </servlet>
              </web-app>
              """,
            spec -> spec.path("src/main/webapp/WEB-INF/web.xml")
          )
        );
    }

    @Test
    void leavesFiltersThatDoNotConvertAlone() {
        rewriteRun(
          java(TIMING_FILTER),
          java(
            """
              package com.example;

              import com.sun.jersey.spi.container.ContainerRequest;
              import com.sun.jersey.spi.container.ContainerRequestFilter;

              public class DecodingFilter implements ContainerRequestFilter {
                  @Override
                  public ContainerRequest filter(ContainerRequest request) {
                      request.getQueryParameters().putSingle("token", "decoded");
                      return request;
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
                          <param-name>com.sun.jersey.spi.container.ContainerRequestFilters</param-name>
                          <param-value>com.example.DecodingFilter;com.example.lib.AuditFilter</param-value>
                      </init-param>
                      <init-param>
                          <param-name>com.sun.jersey.spi.container.ContainerResponseFilters</param-name>
                          <param-value>com.example.TimingFilter</param-value>
                      </init-param>
                      <init-param>
                          <param-name>com.sun.jersey.api.container.filter.PostReplaceFilterConfig</param-name>
                          <param-value>header</param-value>
                      </init-param>
                  </servlet>
                  <servlet>
                      <servlet-name>spring</servlet-name>
                      <servlet-class>com.sun.jersey.spi.spring.container.servlet.SpringServlet</servlet-class>
                      <init-param>
                          <param-name>com.sun.jersey.spi.container.ContainerRequestFilters</param-name>
                          <param-value>com.sun.jersey.api.container.filter.PostReplaceFilter</param-value>
                      </init-param>
                  </servlet>
              </web-app>
              """,
            spec -> spec.path("src/main/webapp/WEB-INF/web.xml")
          )
        );
    }

    @Test
    void togetherWithTheOtherWebXmlAndFilterRecipes() {
        rewriteRun(
          spec -> spec.recipes(new MigrateJersey1WebXml(), new MigrateJersey1ContainerFiltersInWebXml(),
            new Jersey1ContainerFilterToJaxRsFilter()),
          java(
            CORS_FILTER,
            """
              package com.example;

              import javax.ws.rs.container.ContainerRequestContext;
              import javax.ws.rs.container.ContainerResponseContext;
              import javax.ws.rs.container.ContainerResponseFilter;

              public class CORSFilter implements ContainerResponseFilter {
                  @Override
                  public void filter(ContainerRequestContext request, ContainerResponseContext response) {
                      response.getHeaders().add("Access-Control-Allow-Origin", "*");
                  }
              }
              """
          ),
          //language=xml
          xml(
            """
              <web-app>
                  <servlet>
                      <servlet-name>api</servlet-name>
                      <servlet-class>com.sun.jersey.spi.container.servlet.ServletContainer</servlet-class>
                      <init-param>
                          <param-name>com.sun.jersey.config.property.packages</param-name>
                          <param-value>com.example</param-value>
                      </init-param>
                      <init-param>
                          <param-name>com.sun.jersey.api.json.POJOMappingFeature</param-name>
                          <param-value>true</param-value>
                      </init-param>
                      <init-param>
                          <param-name>com.sun.jersey.spi.container.ContainerResponseFilters</param-name>
                          <param-value>com.example.CORSFilter</param-value>
                      </init-param>
                  </servlet>
              </web-app>
              """,
            """
              <web-app>
                  <servlet>
                      <servlet-name>api</servlet-name>
                      <servlet-class>org.glassfish.jersey.servlet.ServletContainer</servlet-class>
                      <init-param>
                          <param-name>jersey.config.server.provider.packages</param-name>
                          <param-value>com.example</param-value>
                      </init-param>
                      <init-param>
                          <param-name>jersey.config.server.provider.classnames</param-name>
                          <param-value>org.codehaus.jackson.jaxrs.JacksonJaxbJsonProvider,com.example.CORSFilter</param-value>
                      </init-param>
                  </servlet>
              </web-app>
              """,
            spec -> spec.path("src/main/webapp/WEB-INF/web.xml")
          )
        );
    }
}
