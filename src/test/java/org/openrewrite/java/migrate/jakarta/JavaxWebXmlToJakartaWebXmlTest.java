package org.openrewrite.java.migrate.jakarta;

import static org.openrewrite.xml.Assertions.xml;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.config.Environment;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

public class JavaxWebXmlToJakartaWebXmlTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion()
            .classpathFromResources(new InMemoryExecutionContext()))
          .recipe(Environment.builder().scanRuntimeClasspath("org.openrewrite.java.migrate.jakarta")
            .build()
            .activateRecipes("org.openrewrite.java.migrate.jakarta.JavaxWebXmlToJakartaWebXml"));
    }

    @Test
    void migrateSun() {
        rewriteRun(
          //language=xml
          xml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <web-app xmlns="http://java.sun.com/xml/ns/javaee"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:schemaLocation="http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_2_0.xsd"
                       version="2.0">
                  <context-param>
                      <param-name>javax.faces.PROJECT_STAGE</param-name>
                      <param-value>Production</param-value>
                  </context-param>
              </web-fragment> 
                 """,
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <web-app xmlns="https://jakarta.ee/xml/ns/jakartaee"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/web-app_6_0.xsd"
                       version="6.0">
                  <context-param>
                      <param-name>jakarta.faces.PROJECT_STAGE</param-name>
                      <param-value>Production</param-value>
                  </context-param>
              </web-fragment> 
                 """,
            sourceSpecs -> sourceSpecs.path("web.xml")
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
              <web-app xmlns="http://xmlns.jcp.org/xml/ns/javaee"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_1.xsd"
                       version="3.1">
                  <context-param>
                      <param-name>javax.faces.PROJECT_STAGE</param-name>
                      <param-value>Production</param-value>
                  </context-param>
              </web-fragment> 
                 """,
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <web-app xmlns="https://jakarta.ee/xml/ns/jakartaee"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/web-app_6_0.xsd"
                       version="6.0">
                  <context-param>
                      <param-name>jakarta.faces.PROJECT_STAGE</param-name>
                      <param-value>Production</param-value>
                  </context-param>
              </web-fragment> 
                 """,
            sourceSpecs -> sourceSpecs.path("web.xml")
          )
        );
    }

    @Nested
    class NoChanges {
        @Test
        void fileNotWebXml() {
            rewriteRun(
              //language=xml
              xml(
                """
                  <beans xmlns="http://java.sun.com/xml/ns/javaee" 
                      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                      xsi:schemaLocation="http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/beans_1_0.xsd">
                  </beans> 
                  """,
                sourceSpecs -> sourceSpecs.path("not-web.xml")
              )
            );
        }
    }
}