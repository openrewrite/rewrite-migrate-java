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
import org.openrewrite.DocumentExample;
import org.openrewrite.config.Environment;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.xml.Assertions.xml;

class JavaxWebFragmentXmlToJakartaWebFragmentXmlTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(Environment.builder().scanRuntimeClasspath("org.openrewrite.java.migrate.jakarta").build()
          .activateRecipes("org.openrewrite.java.migrate.jakarta.JavaxWebFragmentXmlToJakartaWebFragmentXml"));
    }

    @DocumentExample
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