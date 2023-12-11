package org.openrewrite.java.migrate.jakarta;

import static org.openrewrite.xml.Assertions.xml;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.config.Environment;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

public class JavaxWebFragmentXmlToJakartaWebFragmentXmlTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion()
            .classpathFromResources(new InMemoryExecutionContext()))
          .recipe(Environment.builder().scanRuntimeClasspath("org.openrewrite.java.migrate.jakarta")
            .build()
            .activateRecipes("org.openrewrite.java.migrate.jakarta.JavaxWebFragmentXmlToJakartaWebFragmentXml"));
    }

    @Test
    void migrateSun() {
        rewriteRun(
          //language=xml
          xml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <web-fragment xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                            version="3.0"
                            xmlns="http://java.sun.com/xml/ns/javaee"
                            xsi:schemaLocation="http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-fragment_3_0.xsd">
                     <listener>
                         <listener-class>javax.faces.UploadedFileCleanerListener</listener-class>
                     </listener>
              </web-fragment>   
              """,
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <web-fragment xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                            version="5.0"
                            xmlns="https://jakarta.ee/xml/ns/jakartaee"
                            xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/web-fragment_5_0.xsd">
                     <listener>
                         <listener-class>jakarta.faces.UploadedFileCleanerListener</listener-class>
                     </listener>
              </web-fragment> 
              """,
            sourceSpecs -> sourceSpecs.path("web-fragment.xml")
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
              <web-fragment xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                            version="4.0"
                            xmlns="http://xmlns.jcp.org/xml/ns/javaee"
                            xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-fragment_4_0.xsd">
                     <listener>
                         <listener-class>javax.faces.UploadedFileCleanerListener</listener-class>
                     </listener>
              </web-fragment>   
              """,
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <web-fragment xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                            version="5.0"
                            xmlns="https://jakarta.ee/xml/ns/jakartaee"
                            xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/web-fragment_5_0.xsd">
                     <listener>
                         <listener-class>jakarta.faces.UploadedFileCleanerListener</listener-class>
                     </listener>
              </web-fragment> 
              """,
            sourceSpecs -> sourceSpecs.path("web-fragment.xml")
          )
        );
    }

    @Nested
    class NoChanges {
        @Test
        void fileNotWebFragmentXml() {
            rewriteRun(
              //language=xml
              xml(
                """
                  <beans xmlns="http://java.sun.com/xml/ns/javaee" 
                      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                      xsi:schemaLocation="http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/beans_1_0.xsd">
                  </beans> 
                  """,
                sourceSpecs -> sourceSpecs.path("not-web-fragment.xml")
              )
            );
        }
    }
}