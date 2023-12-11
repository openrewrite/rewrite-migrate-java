package org.openrewrite.java.migrate.jakarta;

import static org.openrewrite.xml.Assertions.xml;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.config.Environment;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

public class JakartaFacesXhtmlTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion()
            .classpathFromResources(new InMemoryExecutionContext()))
          .recipe(Environment.builder().scanRuntimeClasspath("org.openrewrite.java.migrate.jakarta")
            .build()
            .activateRecipes("org.openrewrite.java.migrate.jakarta.JakartaFacesXhtml"));
    }

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
                      xmlns:f="jakarta.faces.core"
                      xmlns:h="jakarta.faces.html"
                      xmlns:ui="jakarta.faces.facelets"
                      xmlns:c="jakarta.tags.core"
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
                      xmlns:f="jakarta.faces.core"
                      xmlns:h="jakarta.faces.html"
                      xmlns:ui="jakarta.faces.facelets"
                      xmlns:c="jakarta.tags.core"
                      xmlns:pt="jakarta.faces.passthrough"
                      xmlns:fn="jakarta.tags.functions"
                      xmlns:cc="jakarta.faces.composite"
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
}