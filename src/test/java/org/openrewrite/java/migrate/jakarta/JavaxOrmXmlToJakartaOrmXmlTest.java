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

class JavaxOrmXmlToJakartaOrmXmlTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.migrate.jakarta.JavaxOrmXmlToJakartaOrmXml");
    }

    @DocumentExample
    @Test
    void migrateJavaEE8() {
        rewriteRun(
          //language=xml
          xml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <entity-mappings xmlns="http://xmlns.jcp.org/xml/ns/persistence/orm"
                               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                               xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/persistence/orm http://xmlns.jcp.org/xml/ns/persistence/orm_2_2.xsd"
                               version="2.2">
              </entity-mappings>
              """,
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <entity-mappings xmlns="https://jakarta.ee/xml/ns/persistence/orm"
                               xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                               xsi:schemaLocation="https://jakarta.ee/xml/ns/persistence/orm https://jakarta.ee/xml/ns/persistence/orm/orm_3_0.xsd"
                               version="3.0">
              </entity-mappings>
              """,
            sourceSpecs -> sourceSpecs.path("src/main/resources/META-INF/orm.xml")
          )
        );
    }

    @Nested
    class NoChanges {
        @Test
        void hibernateOrmXmlAsPartOfJakartaEE10() {
            rewriteRun(
              spec -> spec.recipeFromResources("org.openrewrite.java.migrate.jakarta.JakartaEE10"),
              //language=xml
              xml(
                """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <entity-mappings xmlns="http://www.hibernate.org/xsd/orm/mapping"
                                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                                   xsi:schemaLocation="http://www.hibernate.org/xsd/orm/mapping https://hibernate.org/xsd/orm/mapping/mapping-7.0.xsd"
                                   version="7.0">
                  </entity-mappings>
                  """,
                sourceSpecs -> sourceSpecs.path("src/main/resources/META-INF/orm.xml")
              )
            );
        }

        @Test
        void keepJakartaEE10AsPartOfJakartaEE10() {
            rewriteRun(
              spec -> spec.recipeFromResources("org.openrewrite.java.migrate.jakarta.JakartaEE10"),
              //language=xml
              xml(
                """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <entity-mappings xmlns="https://jakarta.ee/xml/ns/persistence/orm"
                                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                                   xsi:schemaLocation="https://jakarta.ee/xml/ns/persistence/orm https://jakarta.ee/xml/ns/persistence/orm/orm_3_1.xsd"
                                   version="3.1">
                  </entity-mappings>
                  """,
                sourceSpecs -> sourceSpecs.path("src/main/resources/META-INF/orm.xml")
              )
            );
        }

        @Test
        void keepJakartaEE11AsPartOfJakartaEE10() {
            rewriteRun(
              spec -> spec.recipeFromResources("org.openrewrite.java.migrate.jakarta.JakartaEE10"),
              //language=xml
              xml(
                """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <entity-mappings xmlns="https://jakarta.ee/xml/ns/persistence/orm"
                                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                                   xsi:schemaLocation="https://jakarta.ee/xml/ns/persistence/orm https://jakarta.ee/xml/ns/persistence/orm/orm_3_2.xsd"
                                   version="3.2">
                  </entity-mappings>
                  """,
                sourceSpecs -> sourceSpecs.path("src/main/resources/META-INF/orm.xml")
              )
            );
        }
    }
}
