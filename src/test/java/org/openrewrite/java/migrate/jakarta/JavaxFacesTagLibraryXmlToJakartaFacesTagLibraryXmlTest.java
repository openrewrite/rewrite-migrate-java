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

class JavaxFacesTagLibraryXmlToJakartaFacesTagLibraryXmlTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(Environment.builder().scanRuntimeClasspath("org.openrewrite.java.migrate.jakarta").build()
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
              <facelet-taglib version="4.0"
                              xmlns="https://jakarta.ee/xml/ns/jakartaee"
                              xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                              xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/web-facelettaglibrary_4_0.xsd">
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
              <facelet-taglib version="4.0"
                              xmlns="https://jakarta.ee/xml/ns/jakartaee"
                              xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                              xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/web-facelettaglibrary_4_0.xsd">
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