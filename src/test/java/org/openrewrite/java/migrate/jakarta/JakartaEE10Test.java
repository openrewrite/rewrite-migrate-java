/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.java.migrate.jakarta;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.maven.Assertions.pomXml;
import static org.openrewrite.xml.Assertions.xml;

class JakartaEE10Test implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.migrate.jakarta.JakartaEE10");
    }

    @DocumentExample
    @Test
    void chainedJettyUpgradeEE10() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>demo</artifactId>
                  <version>0.0.1-SNAPSHOT</version>
                  <dependencies>
                      <dependency>
                          <groupId>org.eclipse.jetty.websocket</groupId>
                          <artifactId>websocket-server</artifactId>
                          <version>9.4.58.v20250814</version>
                      </dependency>
                  </dependencies>
              </project>
              """,
            spec -> spec.after(pom ->
              assertThat(pom)
                .as("websocket dependencies").doesNotContain("org.eclipse.jetty.websocket", "websocket-server")
                .as("EE9 dependencies").doesNotContain("org.eclipse.jetty.ee9.websocket", "jetty-ee9-websocket-jetty-server")
                .as("EE10 dependencies").contains("org.eclipse.jetty.ee10.websocket", "jetty-ee10-websocket-jetty-server")
                .actual())
          )
        );
    }

    @Test
    void keepJakartaEE11Descriptors() {
        rewriteRun(
          //language=xml
          xml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <faces-config xmlns="https://jakarta.ee/xml/ns/jakartaee"
                            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                            xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/web-facesconfig_4_1.xsd"
                            version="4.1">
              </faces-config>
              """,
            sourceSpecs -> sourceSpecs.path("src/main/webapp/WEB-INF/faces-config.xml")
          ),
          //language=xml
          xml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <facelet-taglib version="4.1"
                              xmlns="https://jakarta.ee/xml/ns/jakartaee"
                              xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                              xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/web-facelettaglibrary_4_1.xsd">
              </facelet-taglib>
              """,
            sourceSpecs -> sourceSpecs.path("src/main/resources/META-INF/app.taglib.xml")
          ),
          //language=xml
          xml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <web-app xmlns="https://jakarta.ee/xml/ns/jakartaee"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/web-app_6_1.xsd"
                       version="6.1">
              </web-app>
              """,
            sourceSpecs -> sourceSpecs.path("src/main/webapp/WEB-INF/web.xml")
          ),
          //language=xml
          xml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <web-fragment xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                            version="6.1"
                            xmlns="https://jakarta.ee/xml/ns/jakartaee"
                            xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/web-fragment_6_1.xsd">
              </web-fragment>
              """,
            sourceSpecs -> sourceSpecs.path("src/main/resources/META-INF/web-fragment.xml")
          )
        );
    }

    @Test
    void migrateJavaEEDescriptors() {
        rewriteRun(
          //language=xml
          xml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <faces-config xmlns="http://xmlns.jcp.org/xml/ns/javaee"
                            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                            xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-facesconfig_2_3.xsd"
                            version="2.3">
                  <application>
                      <el-resolver>javax.faces.el.CustomResolver</el-resolver>
                  </application>
              </faces-config>
              """,
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <faces-config xmlns="https://jakarta.ee/xml/ns/jakartaee"
                            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                            xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/web-facesconfig_4_0.xsd"
                            version="4.0">
                  <application>
                      <el-resolver>jakarta.faces.el.CustomResolver</el-resolver>
                  </application>
              </faces-config>
              """,
            sourceSpecs -> sourceSpecs.path("src/main/webapp/WEB-INF/faces-config.xml")
          ),
          //language=xml
          xml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <facelet-taglib version="2.3"
                              xmlns="http://xmlns.jcp.org/xml/ns/javaee"
                              xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                              xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-facelettaglibrary_2_3.xsd">
                  <tag>
                      <tag-name>panel</tag-name>
                      <handler-class>javax.faces.view.facelets.ComponentHandler</handler-class>
                  </tag>
              </facelet-taglib>
              """,
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <facelet-taglib version="4.0"
                              xmlns="https://jakarta.ee/xml/ns/jakartaee"
                              xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                              xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/web-facelettaglibrary_4_0.xsd">
                  <tag>
                      <tag-name>panel</tag-name>
                      <handler-class>jakarta.faces.view.facelets.ComponentHandler</handler-class>
                  </tag>
              </facelet-taglib>
              """,
            sourceSpecs -> sourceSpecs.path("src/main/resources/META-INF/app.taglib.xml")
          ),
          //language=xml
          xml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <web-fragment xmlns="http://xmlns.jcp.org/xml/ns/javaee"
                            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                            xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-fragment_4_0.xsd"
                            version="4.0">
                  <servlet>
                      <servlet-name>Faces Servlet</servlet-name>
                      <servlet-class>javax.faces.webapp.FacesServlet</servlet-class>
                  </servlet>
              </web-fragment>
              """,
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <web-fragment xmlns="https://jakarta.ee/xml/ns/jakartaee"
                            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                            xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/web-fragment_6_0.xsd"
                            version="6.0">
                  <servlet>
                      <servlet-name>Faces Servlet</servlet-name>
                      <servlet-class>jakarta.faces.webapp.FacesServlet</servlet-class>
                  </servlet>
              </web-fragment>
              """,
            sourceSpecs -> sourceSpecs.path("src/main/resources/META-INF/web-fragment.xml")
          )
        );
    }
}
