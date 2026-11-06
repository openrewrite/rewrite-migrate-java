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
package org.openrewrite.java.migrate.jakarta;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.xml.ChangeTagAttribute;

import static org.openrewrite.xml.Assertions.xml;

class JakartaFacesXhtmlEE9Test implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new JakartaFacesXhtmlEE9());
    }

    @DocumentExample
    @Test
    void migrateSun() {
        rewriteRun(
          //language=xml
          xml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <ui:composition
                      xmlns="http://www.w3.org/1999/xhtml"
                      xmlns:f="http://java.sun.com/jsf/core"
                      xmlns:h="http://java.sun.com/jsf/html"
                      xmlns:ui="http://java.sun.com/jsf/facelets"
                      xmlns:c="http://java.sun.com/jsp/jstl/core"
                      xmlns:p="http://primefaces.org/ui"
                      xmlns:pe="http://primefaces.org/ui/extensions">
              <script src="https://www.gstatic.com/charts/loader.js"></script>
              <p:outputPanel id="container" layout="block">
                  <h:panelGrid columns="4">
                      <p:inputText converter="javax.faces.Integer" value="#{basicGChartController.mushrooms}" />
                      <p:inputText converter="javax.faces.Integer" value="#{basicGChartController.onions}" />
                  </h:panelGrid>
                  <c:forEach items="#{sheetDynamicController.hoursOfDay}" var="hourOfDay" varStatus="status">
                          <pe:sheetcolumn styleClass="htRight #{row.cells[status.index].style}"
                                          headerText="#{hourOfDay}"
                                          value="#{row.cells[status.index].value}"
                                          readonlyCell="#{row.readOnly}"
                                          colType="numeric">
                              <f:converter converterId="javax.faces.Integer"/>
                          </pe:sheetcolumn>
                  </c:forEach>
              </p:outputPanel>
              </ui:composition>
              """,
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <ui:composition
                      xmlns="http://www.w3.org/1999/xhtml"
                      xmlns:f="http://java.sun.com/jsf/core"
                      xmlns:h="http://java.sun.com/jsf/html"
                      xmlns:ui="http://java.sun.com/jsf/facelets"
                      xmlns:c="http://java.sun.com/jsp/jstl/core"
                      xmlns:p="http://primefaces.org/ui"
                      xmlns:pe="http://primefaces.org/ui/extensions">
              <script src="https://www.gstatic.com/charts/loader.js"></script>
              <p:outputPanel id="container" layout="block">
                  <h:panelGrid columns="4">
                      <p:inputText converter="jakarta.faces.Integer" value="#{basicGChartController.mushrooms}" />
                      <p:inputText converter="jakarta.faces.Integer" value="#{basicGChartController.onions}" />
                  </h:panelGrid>
                  <c:forEach items="#{sheetDynamicController.hoursOfDay}" var="hourOfDay" varStatus="status">
                          <pe:sheetcolumn styleClass="htRight #{row.cells[status.index].style}"
                                          headerText="#{hourOfDay}"
                                          value="#{row.cells[status.index].value}"
                                          readonlyCell="#{row.readOnly}"
                                          colType="numeric">
                              <f:converter converterId="jakarta.faces.Integer"/>
                          </pe:sheetcolumn>
                  </c:forEach>
              </p:outputPanel>
              </ui:composition>
              """,
            sourceSpecs -> sourceSpecs.path("gchart-sun.xhtml")
          )
        );
    }

    @Test
    void migrateJCP() {
        rewriteRun(
          //language=xml
          xml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <ui:composition
                      xmlns="http://www.w3.org/1999/xhtml"
                      xmlns:f="http://xmlns.jcp.org/jsf/core"
                      xmlns:h="http://xmlns.jcp.org/jsf/html"
                      xmlns:ui="http://xmlns.jcp.org/jsf/facelets"
                      xmlns:c="http://xmlns.jcp.org/jsp/jstl/core"
                      xmlns:pt="http://xmlns.jcp.org/jsf/passthrough"
                      xmlns:fn="http://xmlns.jcp.org/jsp/jstl/functions"
                      xmlns:cc="http://xmlns.jcp.org/jsf/composite"
                      xmlns:p="http://primefaces.org/ui"
                      xmlns:pe="http://primefaces.org/ui/extensions">
              <script src="https://www.gstatic.com/charts/loader.js"></script>
              <div pt:id="container">
                  <h:panelGrid columns="4">
                      <p:inputText converter="javax.faces.Integer" value="#{basicGChartController.mushrooms}" />
                      <p:inputText converter="javax.faces.Integer" value="#{basicGChartController.onions}" />
                  </h:panelGrid>
                  <c:forEach items="#{sheetDynamicController.hoursOfDay}" var="hourOfDay" varStatus="status">
                          <pe:sheetcolumn styleClass="htRight #{row.cells[status.index].style}"
                                          headerText="#{hourOfDay}"
                                          value="#{row.cells[status.index].value}"
                                          readonlyCell="#{row.readOnly}"
                                          colType="numeric">
                              <f:converter converterId="javax.faces.Integer"/>
                          </pe:sheetcolumn>
                  </c:forEach>
              </div>
              </ui:composition>
              """,
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <ui:composition
                      xmlns="http://www.w3.org/1999/xhtml"
                      xmlns:f="http://xmlns.jcp.org/jsf/core"
                      xmlns:h="http://xmlns.jcp.org/jsf/html"
                      xmlns:ui="http://xmlns.jcp.org/jsf/facelets"
                      xmlns:c="http://xmlns.jcp.org/jsp/jstl/core"
                      xmlns:pt="http://xmlns.jcp.org/jsf/passthrough"
                      xmlns:fn="http://xmlns.jcp.org/jsp/jstl/functions"
                      xmlns:cc="http://xmlns.jcp.org/jsf/composite"
                      xmlns:p="http://primefaces.org/ui"
                      xmlns:pe="http://primefaces.org/ui/extensions">
              <script src="https://www.gstatic.com/charts/loader.js"></script>
              <div pt:id="container">
                  <h:panelGrid columns="4">
                      <p:inputText converter="jakarta.faces.Integer" value="#{basicGChartController.mushrooms}" />
                      <p:inputText converter="jakarta.faces.Integer" value="#{basicGChartController.onions}" />
                  </h:panelGrid>
                  <c:forEach items="#{sheetDynamicController.hoursOfDay}" var="hourOfDay" varStatus="status">
                          <pe:sheetcolumn styleClass="htRight #{row.cells[status.index].style}"
                                          headerText="#{hourOfDay}"
                                          value="#{row.cells[status.index].value}"
                                          readonlyCell="#{row.readOnly}"
                                          colType="numeric">
                              <f:converter converterId="jakarta.faces.Integer"/>
                          </pe:sheetcolumn>
                  </c:forEach>
              </div>
              </ui:composition>
              """,
            sourceSpecs -> sourceSpecs.path("gchart-jcp.xhtml")
          )
        );
    }

    @Test
    void replaceInTextCdataAndComments() {
        rewriteRun(
          //language=xml
          xml(
            """
              <ui:composition xmlns:ui="jakarta.faces.facelets" xmlns:h="jakarta.faces.html">
                  <!-- converts with javax.faces.Integer -->
                  <h:outputText value="#{requestScope['javax.servlet.error.message']}"/>
                  <h:outputScript>document.getElementsByName('javax.faces.ViewState')</h:outputScript>
                  <h:outputScript><![CDATA[if (a && b) { faces.ajax.request('javax.faces.ViewState'); }]]></h:outputScript>
              </ui:composition>
              """,
            """
              <ui:composition xmlns:ui="jakarta.faces.facelets" xmlns:h="jakarta.faces.html">
                  <!-- converts with jakarta.faces.Integer -->
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
            new JakartaFacesXhtmlEE9(),
            new ChangeTagAttribute("//*[local-name()='converter']", "converterId", "jakarta.faces.Long", "jakarta.faces.Integer", null)
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
    void leaveOtherXmlFiles() {
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
