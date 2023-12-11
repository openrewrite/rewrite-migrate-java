package org.openrewrite.java.migrate.jakarta;

import static org.openrewrite.xml.Assertions.xml;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.config.Environment;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

public class JavaxFacesConfigXmlToJakartaFacesConfigXmlTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion()
            .classpathFromResources(new InMemoryExecutionContext()))
          .recipe(Environment.builder().scanRuntimeClasspath("org.openrewrite.java.migrate.jakarta")
            .build()
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