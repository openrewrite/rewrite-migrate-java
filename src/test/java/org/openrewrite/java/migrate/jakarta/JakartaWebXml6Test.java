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
package org.openrewrite.java.migrate.jakarta;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.xml.Assertions.xml;

class JakartaWebXml6Test implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.migrate.jakarta.JakartaWebXml6");
    }

    @DocumentExample
    @Test
    void upgradeFromJakartaEE9() {
        rewriteRun(
          //language=xml
          xml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <web-app xmlns="https://jakarta.ee/xml/ns/jakartaee"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/web-app_5_0.xsd"
                       version="5.0">
              </web-app>
              """,
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <web-app xmlns="https://jakarta.ee/xml/ns/jakartaee"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/web-app_6_0.xsd"
                       version="6.0">
              </web-app>
              """,
            sourceSpecs -> sourceSpecs.path("web.xml")
          )
        );
    }

    @Test
    void upgradeWithHttpSchemaLocation() {
        rewriteRun(
          //language=xml
          xml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <web-app xmlns="https://jakarta.ee/xml/ns/jakartaee"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:schemaLocation="http://jakarta.ee/xml/ns/jakartaee http://jakarta.ee/xml/ns/jakartaee/web-app_5_0.xsd"
                       version="5.0">
              </web-app>
              """,
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <web-app xmlns="https://jakarta.ee/xml/ns/jakartaee"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:schemaLocation="http://jakarta.ee/xml/ns/jakartaee http://jakarta.ee/xml/ns/jakartaee/web-app_6_0.xsd"
                       version="6.0">
              </web-app>
              """,
            sourceSpecs -> sourceSpecs.path("web.xml")
          )
        );
    }

    @Nested
    class NoChanges {
        @Test
        void javaEEDescriptor() {
            rewriteRun(
              //language=xml
              xml(
                """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <web-app xmlns="http://xmlns.jcp.org/xml/ns/javaee"
                      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                           xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-app_4_0.xsd"
                           version="4.0">
                  </web-app>
                  """,
                sourceSpecs -> sourceSpecs.path("web.xml")
              )
            );
        }

        @Test
        void alreadyOnJakartaEE11() {
            rewriteRun(
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
                sourceSpecs -> sourceSpecs.path("web.xml")
              )
            );
        }
    }
}
