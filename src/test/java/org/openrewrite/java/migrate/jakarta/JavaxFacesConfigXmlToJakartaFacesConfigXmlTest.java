/*
 * Copyright 2023 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.migrate.jakarta;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.config.Environment;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.xml.Assertions.xml;

class JavaxFacesConfigXmlToJakartaFacesConfigXmlTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(Environment.builder().scanRuntimeClasspath("org.openrewrite.java.migrate.jakarta").build()
          .activateRecipes("org.openrewrite.java.migrate.jakarta.JavaxFacesConfigXmlToJakartaFacesConfigXml"));
    }

    @Test
    void migrateSun() {
        rewriteRun(
          //language=xml
          xml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <faces-config xmlns="http://java.sun.com/xml/ns/javaee"
                            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                            xsi:schemaLocation="http://java.sun.com/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-facesconfig_1.0.xsd"
                            version="1.0">
                  <render-kit>
                      <renderer>
                          <component-family>javax.faces.Output</component-family>
                          <renderer-type>javax.faces.Head</renderer-type>
                          <renderer-class>org.apache.myfaces.renderkit.html.HtmlHeadRenderer</renderer-class>
                      </renderer>
                  </render-kit>
              </faces-config>
              """,
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <faces-config xmlns="https://jakarta.ee/xml/ns/jakartaee"
                            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                            xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/web-facesconfig_4_0.xsd"
                            version="4.0">
                  <render-kit>
                      <renderer>
                          <component-family>jakarta.faces.Output</component-family>
                          <renderer-type>jakarta.faces.Head</renderer-type>
                          <renderer-class>org.apache.myfaces.renderkit.html.HtmlHeadRenderer</renderer-class>
                      </renderer>
                  </render-kit>
              </faces-config>
              """,
            sourceSpecs -> sourceSpecs.path("faces-config.xml")
          )
        );
    }

    @Test
    void migrateJCP() {
        rewriteRun(
          //language=xml
          xml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <faces-config xmlns="http://xmlns.jcp.org/xml/ns/javaee"
                            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                            xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-facesconfig_2_2.xsd"
                            version="2.2">
                  <render-kit>
                      <renderer>
                          <component-family>javax.faces.Output</component-family>
                          <renderer-type>javax.faces.Head</renderer-type>
                          <renderer-class>org.apache.myfaces.renderkit.html.HtmlHeadRenderer</renderer-class>
                      </renderer>
                  </render-kit>
              </faces-config>
              """,
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <faces-config xmlns="https://jakarta.ee/xml/ns/jakartaee"
                            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                            xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/web-facesconfig_4_0.xsd"
                            version="4.0">
                  <render-kit>
                      <renderer>
                          <component-family>jakarta.faces.Output</component-family>
                          <renderer-type>jakarta.faces.Head</renderer-type>
                          <renderer-class>org.apache.myfaces.renderkit.html.HtmlHeadRenderer</renderer-class>
                      </renderer>
                  </render-kit>
              </faces-config>
              """,
            sourceSpecs -> sourceSpecs.path("faces-config.xml")
          )
        );
    }

    @Nested
    class NoChanges {
        @Test
        void fileNotFacesConfigXml() {
            rewriteRun(
              //language=xml
              xml(
                """
                  <beans xmlns="http://java.sun.com/xml/ns/javaee" 
                      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                      xsi:schemaLocation="http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/beans_1_0.xsd">
                  </beans> 
                  """,
                sourceSpecs -> sourceSpecs.path("not-faces-config.xml")
              )
            );
        }
    }
}