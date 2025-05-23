/*
 * Copyright 2025 the original author or authors.
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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.xml.Assertions.xml;

class UpdateXJCBindingsToJakartaEE implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.migrate.jakarta.JavaxXmlToJakartaXmlXJCBinding");
    }

    @Test
    void noMigrate() {
        rewriteRun(
          xml(
            //language=xml
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <jxb:bindings version="3.0"
                          xmlns:jxb="https://jakarta.ee/xml/ns/jaxb"
                          xmlns:xs="http://www.w3.org/2001/XMLSchema">
            </jxb:bindings>
            """
          )
        );
    }

    @Test
    void noMigrateIBMFiles() {
        rewriteRun(
          //language=xml
          xml(
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <jxb:bindings version="1.0"
                          xmlns:jxb="http://java.sun.com/xml/ns/jaxb"
                          xmlns:xs="http://www.w3.org/2001/XMLSchema">
            </jxb:bindings>
            """,
            spec -> spec.path("ibm-web-ext.xml")
          )
        );
    }

    @Nested
    class Migrate {
        @Test
        void version() {
            rewriteRun(
              //language=xml
              xml(
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <jxb:bindings version="1.0"
                              xmlns:jxb="https://jakarta.ee/xml/ns/jaxb"
                              xmlns:xs="http://www.w3.org/2001/XMLSchema">
                </jxb:bindings>
                """,
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <jxb:bindings version="3.0"
                              xmlns:jxb="https://jakarta.ee/xml/ns/jaxb"
                              xmlns:xs="http://www.w3.org/2001/XMLSchema">
                </jxb:bindings>
                """
              )
            );
        }

        @Test
        void namespace() {
            rewriteRun(
              //language=xml
              xml(
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <jxb:bindings version="3.0"
                              xmlns:jxb="http://java.sun.com/xml/ns/jaxb"
                              xmlns:xs="http://www.w3.org/2001/XMLSchema">
                </jxb:bindings>
                """,
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <jxb:bindings version="3.0"
                              xmlns:jxb="https://jakarta.ee/xml/ns/jaxb"
                              xmlns:xs="http://www.w3.org/2001/XMLSchema">
                </jxb:bindings>
                """
              )
            );
        }

        @Test
        void both() {
            rewriteRun(
              //language=xml
              xml(
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <jxb:bindings version="1.0"
                              xmlns:jxb="http://java.sun.com/xml/ns/jaxb"
                              xmlns:xs="http://www.w3.org/2001/XMLSchema">
                </jxb:bindings>
                """,
                """
                <?xml version="1.0" encoding="UTF-8"?>
                <jxb:bindings version="3.0"
                              xmlns:jxb="https://jakarta.ee/xml/ns/jaxb"
                              xmlns:xs="http://www.w3.org/2001/XMLSchema">
                </jxb:bindings>
                """
              )
            );
        }
    }
}
