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
package org.openrewrite.java.migrate;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.xml.Assertions.xml;

class BeanDiscoveryTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new BeanDiscovery());
    }

    @DocumentExample
    @Test
    void noVersionOrMode() {
        rewriteRun(
          //language=xml
          xml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <beans xmlns="https://jakarta.ee/xml/ns/jakartaee"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_3_0.xsd">
              </beans>
              """,
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <beans xmlns="https://jakarta.ee/xml/ns/jakartaee"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_3_0.xsd" bean-discovery-mode="all" version="3.0">
              </beans>
              """,
            sourceSpecs -> sourceSpecs.path("beans.xml")
          )
        );
    }

    @Test
    void noVersionDifferentMode() {
        rewriteRun(
          //language=xml
          xml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <beans xmlns="https://jakarta.ee/xml/ns/jakartaee"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_4_0.xsd"
                  bean-discovery-mode="none">
              </beans>
              """,
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <beans xmlns="https://jakarta.ee/xml/ns/jakartaee"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_4_0.xsd"
                  bean-discovery-mode="all" version="4.0">
              </beans>
              """,
            sourceSpecs -> sourceSpecs.path("beans.xml")
          )
        );
    }

    @Nested
    class NoChanges {
        @Test
        void fileNotNamedBeansXml() {
            rewriteRun(
              //language=xml
              xml(
                """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <beans xmlns="https://jakarta.ee/xml/ns/jakartaee"
                      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                      xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_3_0.xsd">
                  </beans>
                  """,
                sourceSpecs -> sourceSpecs.path("not-beans.xml")
              )
            );
        }

        @Test
        void alreadyHasVersionAndMode() {
            rewriteRun(
              //language=xml
              xml(
                """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <beans xmlns="https://jakarta.ee/xml/ns/jakartaee"
                      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                      xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_3_0.xsd"
                      bean-discovery-mode="all"
                      version="3.0">
                  </beans>
                  """,
                sourceSpecs -> sourceSpecs.path("beans.xml")
              )
            );
        }

    }
}
