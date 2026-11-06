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

class JakartaFacesCompositeLibraryNamespacesTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new JakartaFacesCompositeLibraryNamespaces());
    }

    @DocumentExample
    @Test
    void migrateLibraryNamespaces() {
        rewriteRun(
          //language=xml
          xml(
            """
              <ui:composition xmlns:ui="jakarta.faces.facelets"
                              xmlns:my="http://xmlns.jcp.org/jsf/composite/components"
                              xmlns:old="http://java.sun.com/jsf/composite/legacy/widgets">
                  <my:widget/>
                  <old:widget/>
              </ui:composition>
              """,
            """
              <ui:composition xmlns:ui="jakarta.faces.facelets"
                              xmlns:my="jakarta.faces.composite/components"
                              xmlns:old="jakarta.faces.composite/legacy/widgets">
                  <my:widget/>
                  <old:widget/>
              </ui:composition>
              """,
            spec -> spec.path("src/main/webapp/page.xhtml")
          )
        );
    }

    @Test
    void leaveCompositeTagLibraryAndOtherAttributes() {
        rewriteRun(
          //language=xml
          xml(
            """
              <ui:component xmlns:ui="http://xmlns.jcp.org/jsf/facelets" xmlns:cc="http://xmlns.jcp.org/jsf/composite">
                  <a href="http://xmlns.jcp.org/jsf/composite/components">composite components</a>
              </ui:component>
              """,
            spec -> spec.path("src/main/webapp/resources/components/widget.xhtml")
          )
        );
    }

    @Test
    void leaveOtherXmlFiles() {
        rewriteRun(
          //language=xml
          xml(
            """
              <facelet-taglib xmlns:my="http://xmlns.jcp.org/jsf/composite/components"/>
              """,
            spec -> spec.path("src/main/webapp/WEB-INF/my.taglib.xml")
          )
        );
    }
}
