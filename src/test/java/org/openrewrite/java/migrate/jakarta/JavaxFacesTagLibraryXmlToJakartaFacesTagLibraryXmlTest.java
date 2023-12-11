package org.openrewrite.java.migrate.jakarta;

import static org.openrewrite.xml.Assertions.xml;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.config.Environment;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

public class JavaxFacesTagLibraryXmlToJakartaFacesTagLibraryXmlTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion()
            .classpathFromResources(new InMemoryExecutionContext()))
          .recipe(Environment.builder().scanRuntimeClasspath("org.openrewrite.java.migrate.jakarta")
            .build()
            .activateRecipes("org.openrewrite.java.migrate.jakarta.JavaxFacesTagLibraryXmlToJakartaFacesTagLibraryXml"));
    }

    @Test
    void migrateSun() {
        rewriteRun(
          //language=xml
          xml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <facelet-taglib version="1.0"
                              xmlns="http://java.sun.com/xml/ns/javaee"
                              xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                              xsi:schemaLocation="http://java.sun.com/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-facelettaglibrary_1.0.xsd">
                  <function>
                      <function-name>getFileContent</function-name>
                      <function-class>javax.util.ShowcaseUtil</function-class>
                      <function-signature>java.lang.String getFileContent(java.lang.String)</function-signature>
                  </function>
              </facelet-taglib>
              """,
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <facelet-taglib version="3.0"
                              xmlns="https://jakarta.ee/xml/ns/jakartaee"
                              xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                              xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/web-facelettaglibrary_3_0.xsd">
                  <function>
                      <function-name>getFileContent</function-name>
                      <function-class>jakarta.util.ShowcaseUtil</function-class>
                      <function-signature>java.lang.String getFileContent(java.lang.String)</function-signature>
                  </function>
              </facelet-taglib>
              """,
            sourceSpecs -> sourceSpecs.path("faces-taglib.xml")
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
              <facelet-taglib version="2.2"
                              xmlns="http://xmlns.jcp.org/xml/ns/javaee"
                              xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                              xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-facelettaglibrary_2_2.xsd">
                  <function>
                      <function-name>getFileContent</function-name>
                      <function-class>javax.util.ShowcaseUtil</function-class>
                      <function-signature>java.lang.String getFileContent(java.lang.String)</function-signature>
                  </function>
              </facelet-taglib>
              """,
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <facelet-taglib version="3.0"
                              xmlns="https://jakarta.ee/xml/ns/jakartaee"
                              xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                              xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/web-facelettaglibrary_3_0.xsd">
                  <function>
                      <function-name>getFileContent</function-name>
                      <function-class>jakarta.util.ShowcaseUtil</function-class>
                      <function-signature>java.lang.String getFileContent(java.lang.String)</function-signature>
                  </function>
              </facelet-taglib>
              """,
            sourceSpecs -> sourceSpecs.path("faces-taglib.xml")
          )
        );
    }

    @Nested
    class NoChanges {
        @Test
        void fileNotTagLibraryXml() {
            rewriteRun(
              //language=xml
              xml(
                """
                  <beans xmlns="http://java.sun.com/xml/ns/javaee" 
                      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                      xsi:schemaLocation="http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/beans_1_0.xsd">
                  </beans> 
                  """,
                sourceSpecs -> sourceSpecs.path("not-faces-taglib.xml")
              )
            );
        }
    }
}