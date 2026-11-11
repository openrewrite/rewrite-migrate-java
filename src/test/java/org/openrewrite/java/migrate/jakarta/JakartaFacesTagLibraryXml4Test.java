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

class JakartaFacesTagLibraryXml4Test implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.migrate.jakarta.JakartaFacesTagLibraryXml4");
    }

    @DocumentExample
    @Test
    void upgradeFromJakartaEE9() {
        rewriteRun(
          //language=xml
          xml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <facelet-taglib version="3.0"
                              xmlns="https://jakarta.ee/xml/ns/jakartaee"
                              xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                              xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/web-facelettaglibrary_3_0.xsd">
              </facelet-taglib>
              """,
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <facelet-taglib version="4.0"
                              xmlns="https://jakarta.ee/xml/ns/jakartaee"
                              xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                              xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/web-facelettaglibrary_4_0.xsd">
              </facelet-taglib>
              """,
            sourceSpecs -> sourceSpecs.path("faces-taglib.xml")
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
              <facelet-taglib version="3.0"
                              xmlns="https://jakarta.ee/xml/ns/jakartaee"
                              xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                              xsi:schemaLocation="http://jakarta.ee/xml/ns/jakartaee http://jakarta.ee/xml/ns/jakartaee/web-facelettaglibrary_3_0.xsd">
              </facelet-taglib>
              """,
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <facelet-taglib version="4.0"
                              xmlns="https://jakarta.ee/xml/ns/jakartaee"
                              xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                              xsi:schemaLocation="http://jakarta.ee/xml/ns/jakartaee http://jakarta.ee/xml/ns/jakartaee/web-facelettaglibrary_4_0.xsd">
              </facelet-taglib>
              """,
            sourceSpecs -> sourceSpecs.path("faces-taglib.xml")
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
                  <facelet-taglib version="2.3"
                                  xmlns="http://xmlns.jcp.org/xml/ns/javaee"
                                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                                  xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-facelettaglibrary_2_3.xsd">
                  </facelet-taglib>
                  """,
                sourceSpecs -> sourceSpecs.path("faces-taglib.xml")
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
                  <facelet-taglib version="4.1"
                                  xmlns="https://jakarta.ee/xml/ns/jakartaee"
                                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                                  xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/web-facelettaglibrary_4_1.xsd">
                  </facelet-taglib>
                  """,
                sourceSpecs -> sourceSpecs.path("faces-taglib.xml")
              )
            );
        }
    }
}
