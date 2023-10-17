package org.openrewrite.java.migrate.jakarta;

import org.junit.jupiter.api.Test;
import org.openrewrite.config.Environment;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.xml.Assertions.xml;

public class BeansXmlNoVersionNowImplicitTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(
          Environment.builder()
            .scanRuntimeClasspath("org.openrewrite.java.liberty")
            .build()
            .activateRecipes("org.openrewrite.java.migrate.jakarta.BeansXmlNoVersionNowImplicit"));
    }
    @Test
    void beansXmlNoVersionNowImplicit_sun() {
        rewriteRun(
          //language=xml
          xml(
            """
              <beans 
                  xmlns="http://java.sun.com/xml/ns/javaee">
              </beans>
              """,
            """
              <beans xmlns="http://java.sun.com/xml/ns/javaee" bean-discovery-mode="all">
              </beans>
              """
          )
        );
    }
    @Test
    void beansXmlNoVersionNowImplicit_j2ee() {
        rewriteRun(
          //language=xml
          xml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <beans xmlns="https://jakarta.ee/xml/ns/jakartaee"
                          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                          xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_4_0.xsd">
              </beans>
              """,
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <beans xmlns="https://jakarta.ee/xml/ns/jakartaee"
                          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                          xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_4_0.xsd"
                          bean-discovery-mode="all"
                          version="4.0">
              </beans>
              """
          )
        );
    }

}
