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
import org.openrewrite.xml.ChangeTagAttribute;

import static org.openrewrite.xml.Assertions.xml;

class FindAndReplaceInXmlTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new FindAndReplaceInXml("javax.", "jakarta.", "**/*.xhtml"));
    }

    @DocumentExample
    @Test
    void replaceInAttributesTextCdataAndComments() {
        rewriteRun(
          //language=xml
          xml(
            """
              <ui:composition xmlns:ui="jakarta.faces.facelets" xmlns:f="jakarta.faces.core" xmlns:h="jakarta.faces.html">
                  <!-- converts with javax.faces.Integer -->
                  <h:inputText value="#{bean.count}">
                      <f:converter converterId="javax.faces.Integer"/>
                  </h:inputText>
                  <h:outputText value="#{requestScope['javax.servlet.error.message']}"/>
                  <h:outputScript>document.getElementsByName('javax.faces.ViewState')</h:outputScript>
                  <h:outputScript><![CDATA[if (a && b) { faces.ajax.request('javax.faces.ViewState'); }]]></h:outputScript>
              </ui:composition>
              """,
            """
              <ui:composition xmlns:ui="jakarta.faces.facelets" xmlns:f="jakarta.faces.core" xmlns:h="jakarta.faces.html">
                  <!-- converts with jakarta.faces.Integer -->
                  <h:inputText value="#{bean.count}">
                      <f:converter converterId="jakarta.faces.Integer"/>
                  </h:inputText>
                  <h:outputText value="#{requestScope['jakarta.servlet.error.message']}"/>
                  <h:outputScript>document.getElementsByName('jakarta.faces.ViewState')</h:outputScript>
                  <h:outputScript><![CDATA[if (a && b) { faces.ajax.request('jakarta.faces.ViewState'); }]]></h:outputScript>
              </ui:composition>
              """,
            spec -> spec.path("src/main/webapp/index.xhtml")
          )
        );
    }

    @Test
    void laterXmlRecipesStillApply() {
        rewriteRun(
          spec -> spec.recipes(
            new FindAndReplaceInXml("javax.", "jakarta.", "**/*.xhtml"),
            new ChangeTagAttribute("//*[local-name()='converter']","converterId", "jakarta.faces.Long", "jakarta.faces.Integer", null)
          ),
          //language=xml
          xml(
            """
              <ui:composition xmlns:ui="jakarta.faces.facelets" xmlns:f="jakarta.faces.core">
                  <f:converter converterId="javax.faces.Integer"/>
              </ui:composition>
              """,
            """
              <ui:composition xmlns:ui="jakarta.faces.facelets" xmlns:f="jakarta.faces.core">
                  <f:converter converterId="jakarta.faces.Long"/>
              </ui:composition>
              """,
            spec -> spec.path("src/main/webapp/index.xhtml")
          )
        );
    }

    @Test
    void replaceOnceWhenReplacementContainsFind() {
        rewriteRun(
          spec -> spec.recipe(new FindAndReplaceInXml("faces", "jakarta.faces", null)),
          //language=xml
          xml(
            """
              <taglib namespace="faces"/>
              """,
            """
              <taglib namespace="jakarta.faces"/>
              """
          )
        );
    }

    @Test
    void onlyFilesMatchingThePattern() {
        rewriteRun(
          //language=xml
          xml(
            """
              <beans>
                  <bean class="javax.sql.DataSource"/>
              </beans>
              """,
            spec -> spec.path("src/main/resources/beans.xml")
          )
        );
    }
}
