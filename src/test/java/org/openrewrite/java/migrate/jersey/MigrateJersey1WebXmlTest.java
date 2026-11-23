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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.xml.Assertions.xml;

class MigrateJersey1WebXmlTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigrateJersey1WebXml());
    }

    @DocumentExample
    @Test
    void servletWithPackagesAndPojoMapping() {
        rewriteRun(
          //language=xml
          xml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <web-app xmlns="http://java.sun.com/xml/ns/javaee" version="3.0">
                  <servlet>
                      <servlet-name>jersey</servlet-name>
                      <servlet-class>com.sun.jersey.spi.container.servlet.ServletContainer</servlet-class>
                      <init-param>
                          <param-name>com.sun.jersey.config.property.packages</param-name>
                          <param-value>com.example.api;com.example.providers</param-value>
                      </init-param>
                      <init-param>
                          <param-name>com.sun.jersey.api.json.POJOMappingFeature</param-name>
                          <param-value>true</param-value>
                      </init-param>
                      <init-param>
                          <param-name>com.sun.jersey.config.feature.DisableWADL</param-name>
                          <param-value>TRUE</param-value>
                      </init-param>
                      <load-on-startup>1</load-on-startup>
                  </servlet>
              </web-app>
              """,
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <web-app xmlns="http://java.sun.com/xml/ns/javaee" version="3.0">
                  <servlet>
                      <servlet-name>jersey</servlet-name>
                      <servlet-class>org.glassfish.jersey.servlet.ServletContainer</servlet-class>
                      <init-param>
                          <param-name>jersey.config.server.provider.packages</param-name>
                          <param-value>com.example.api;com.example.providers</param-value>
                      </init-param>
                      <init-param>
                          <param-name>jersey.config.server.wadl.disableWadl</param-name>
                          <param-value>true</param-value>
                      </init-param>
                      <init-param>
                          <param-name>jersey.config.server.provider.classnames</param-name>
                          <param-value>org.codehaus.jackson.jaxrs.JacksonJaxbJsonProvider</param-value>
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
    void filterWithClassNamesResourceConfig() {
        rewriteRun(
          //language=xml
          xml(
            """
              <web-app>
                  <filter>
                      <filter-name>jersey</filter-name>
                      <filter-class>com.sun.jersey.spi.container.servlet.ServletContainer</filter-class>
                      <init-param>
                          <param-name>com.sun.jersey.config.property.resourceConfigClass</param-name>
                          <param-value>com.sun.jersey.api.core.ClassNamesResourceConfig</param-value>
                      </init-param>
                      <init-param>
                          <param-name>com.sun.jersey.config.property.classnames</param-name>
                          <param-value>com.example.HelloResource;com.example.JsonProvider</param-value>
                      </init-param>
                      <init-param>
                          <param-name>com.sun.jersey.api.json.POJOMappingFeature</param-name>
                          <param-value>true</param-value>
                      </init-param>
                      <init-param>
                          <param-name>com.sun.jersey.config.property.WebPageContentRegex</param-name>
                          <param-value>/(images|css|jsp)/.*</param-value>
                      </init-param>
                      <init-param>
                          <param-name>com.sun.jersey.config.feature.FilterForwardOn404</param-name>
                          <param-value>true</param-value>
                      </init-param>
                      <init-param>
                          <param-name>com.sun.jersey.config.property.JSPTemplatesBasePath</param-name>
                          <param-value>/WEB-INF/jsp</param-value>
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
                          <param-value>com.example.HelloResource;com.example.JsonProvider;org.codehaus.jackson.jaxrs.JacksonJaxbJsonProvider</param-value>
                      </init-param>
                      <init-param>
                          <param-name>jersey.config.servlet.filter.staticContentRegex</param-name>
                          <param-value>/(images|css|jsp)/.*</param-value>
                      </init-param>
                      <init-param>
                          <param-name>jersey.config.servlet.filter.forwardOn404</param-name>
                          <param-value>true</param-value>
                      </init-param>
                      <init-param>
                          <param-name>jersey.config.server.mvc.templateBasePath.jsp</param-name>
                          <param-value>/WEB-INF/jsp</param-value>
                      </init-param>
                  </filter>
              </web-app>
              """,
            spec -> spec.path("WEB-INF/web.xml")
          )
        );
    }

    @Test
    void userResourceConfigBecomesApplicationAndTracing() {
        rewriteRun(
          //language=xml
          xml(
            """
              <web-app>
                  <servlet>
                      <servlet-name>jersey</servlet-name>
                      <servlet-class>
                          com.sun.jersey.spi.container.servlet.ServletContainer
                      </servlet-class>
                      <init-param>
                          <param-name>com.sun.jersey.config.property.resourceConfigClass</param-name>
                          <param-value>com.example.AppConfig</param-value>
                      </init-param>
                      <init-param>
                          <param-name>com.sun.jersey.config.feature.Trace</param-name>
                          <param-value>true</param-value>
                      </init-param>
                      <init-param>
                          <param-name>com.sun.jersey.spi.container.ContainerRequestFilters</param-name>
                          <param-value>com.sun.jersey.api.container.filter.LoggingFilter</param-value>
                      </init-param>
                  </servlet>
                  <servlet>
                      <servlet-name>other</servlet-name>
                      <servlet-class>com.sun.jersey.spi.container.servlet.ServletContainer</servlet-class>
                      <init-param>
                          <param-name>javax.ws.rs.Application</param-name>
                          <param-value>com.example.OtherApplication</param-value>
                      </init-param>
                      <init-param>
                          <param-name>com.sun.jersey.config.feature.TracePerRequest</param-name>
                          <param-value>true</param-value>
                      </init-param>
                      <init-param>
                          <param-name>com.sun.jersey.api.json.POJOMappingFeature</param-name>
                          <param-value>false</param-value>
                      </init-param>
                  </servlet>
              </web-app>
              """,
            """
              <web-app>
                  <servlet>
                      <servlet-name>jersey</servlet-name>
                      <servlet-class>
                          org.glassfish.jersey.servlet.ServletContainer
                      </servlet-class>
                      <init-param>
                          <param-name>javax.ws.rs.Application</param-name>
                          <param-value>com.example.AppConfig</param-value>
                      </init-param>
                      <init-param>
                          <param-name>jersey.config.server.tracing.type</param-name>
                          <param-value>ALL</param-value>
                      </init-param>
                      <init-param>
                          <param-name>com.sun.jersey.spi.container.ContainerRequestFilters</param-name>
                          <param-value>com.sun.jersey.api.container.filter.LoggingFilter</param-value>
                      </init-param>
                  </servlet>
                  <servlet>
                      <servlet-name>other</servlet-name>
                      <servlet-class>org.glassfish.jersey.servlet.ServletContainer</servlet-class>
                      <init-param>
                          <param-name>javax.ws.rs.Application</param-name>
                          <param-value>com.example.OtherApplication</param-value>
                      </init-param>
                      <init-param>
                          <param-name>jersey.config.server.tracing.type</param-name>
                          <param-value>ON_DEMAND</param-value>
                      </init-param>
                  </servlet>
              </web-app>
              """,
            spec -> spec.path("web.xml")
          )
        );
    }

    @Test
    void servletWithoutConfigurationScansTheWebApp() {
        rewriteRun(
          //language=xml
          xml(
            """
              <web-fragment>
                  <servlet>
                      <servlet-name>jersey</servlet-name>
                      <servlet-class>com.sun.jersey.spi.container.servlet.ServletContainer</servlet-class>
                      <load-on-startup>1</load-on-startup>
                  </servlet>
              </web-fragment>
              """,
            """
              <web-fragment>
                  <servlet>
                      <servlet-name>jersey</servlet-name>
                      <servlet-class>org.glassfish.jersey.servlet.ServletContainer</servlet-class>
                      <init-param>
                          <param-name>jersey.config.servlet.provider.webapp</param-name>
                          <param-value>true</param-value>
                      </init-param>
                      <load-on-startup>1</load-on-startup>
                  </servlet>
              </web-fragment>
              """,
            spec -> spec.path("src/main/resources/META-INF/web-fragment.xml")
          )
        );
    }

    @Test
    void leavesOtherServletsAndOtherFilesAlone() {
        rewriteRun(
          //language=xml
          xml(
            """
              <web-app>
                  <servlet>
                      <servlet-name>guice</servlet-name>
                      <servlet-class>com.sun.jersey.guice.spi.container.servlet.GuiceContainer</servlet-class>
                      <init-param>
                          <param-name>com.sun.jersey.config.property.packages</param-name>
                          <param-value>com.example</param-value>
                      </init-param>
                  </servlet>
                  <context-param>
                      <param-name>com.sun.jersey.config.property.packages</param-name>
                      <param-value>com.example</param-value>
                  </context-param>
              </web-app>
              """,
            spec -> spec.path("web.xml")
          ),
          //language=xml
          xml(
            """
              <beans>
                  <servlet>
                      <servlet-class>com.sun.jersey.spi.container.servlet.ServletContainer</servlet-class>
                  </servlet>
              </beans>
              """,
            spec -> spec.path("beans.xml")
          )
        );
    }
}
