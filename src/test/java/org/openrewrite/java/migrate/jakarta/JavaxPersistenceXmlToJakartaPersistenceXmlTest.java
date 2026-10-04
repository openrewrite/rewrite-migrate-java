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

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.xml.Assertions.xml;

class JavaxPersistenceXmlToJakartaPersistenceXmlTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.migrate.jakarta.JavaxPersistenceXmlToJakartaPersistenceXml");
    }

    @Test
    void migratesCustomResourceName() {
        rewriteRun(
          xml(
            """
              <persistence xmlns="http://java.sun.com/xml/ns/persistence" version="2.0"/>
              """,
            """
              <persistence xmlns="https://jakarta.ee/xml/ns/persistence" version="3.0"/>
              """,
            spec -> spec.path("src/test/resources/test-persistence.xml")
          )
        );
    }

    @Test
    void preservesUnrelatedXmlAndNewerJakartaDescriptors() {
        rewriteRun(
          xml("<persistence xmlns=\"urn:example\" version=\"1.0\"/>",
            spec -> spec.path("config.xml")),
          xml("<persistence xmlns=\"https://jakarta.ee/xml/ns/persistence\" version=\"3.2\"/>",
            spec -> spec.path("META-INF/persistence.xml"))
        );
    }
}
