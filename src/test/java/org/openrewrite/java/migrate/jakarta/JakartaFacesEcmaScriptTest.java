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
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.properties.Assertions.properties;
import static org.openrewrite.test.SourceSpecs.text;

class JakartaFacesEcmaScriptTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.migrate.jakarta.JakartaFacesEcmaScript");
    }

    @DocumentExample
    @Test
    void migrateFacesReferences() {
        rewriteRun(
          text(
            """
              var viewState = document.getElementsByName("javax.faces.ViewState")[0];
              window.jsf.ajax.request(source, event);
              """,
            """
              var viewState = document.getElementsByName("jakarta.faces.ViewState")[0];
              window.faces.ajax.request(source, event);
              """,
            spec -> spec.path("src/main/webapp/resources/app.js")
          ),
          properties(
            "javax.faces.component.UIInput.REQUIRED=Value is required",
            "jakarta.faces.component.UIInput.REQUIRED=Value is required",
            spec -> spec.path("src/main/resources/messages.properties")
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite-migrate-java/issues/1369")
    @Test
    void retainNonJakartaEePackages() {
        rewriteRun(
          text(
            "var amount = 'javax.money.MonetaryAmount';",
            spec -> spec.path("src/main/webapp/resources/app.js")
          ),
          properties(
            """
              javax.money.defaults=DEFAULT
              javax.net.ssl.trustStore=/etc/truststore.jks
              """,
            spec -> spec.path("src/main/resources/application.properties")
          )
        );
    }
}
