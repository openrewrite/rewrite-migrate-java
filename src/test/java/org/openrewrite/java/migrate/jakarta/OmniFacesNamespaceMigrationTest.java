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

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.xml.Assertions.xml;

class OmniFacesNamespaceMigrationTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.migrate.jakarta.OmniFacesNamespaceMigration");
    }

    @DocumentExample
    @Test
    void migrateNamespaces() {
        rewriteRun(
          //language=xml
          xml(
            """
              <ui:composition xmlns:ui="jakarta.faces.facelets" xmlns:o="http://omnifaces.org/ui" xmlns:of="http://omnifaces.org/functions">
                  <o:form/>
              </ui:composition>
              """,
            """
              <ui:composition xmlns:ui="jakarta.faces.facelets" xmlns:o="omnifaces" xmlns:of="omnifaces">
                  <o:form/>
              </ui:composition>
              """,
            spec -> spec.path("src/main/webapp/index.xhtml")
          )
        );
    }
}
