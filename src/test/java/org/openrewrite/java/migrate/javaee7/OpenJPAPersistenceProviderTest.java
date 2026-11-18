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
package org.openrewrite.java.migrate.javaee7;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.config.Environment;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.xml.Assertions.xml;

class OpenJPAPersistenceProviderTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(Environment.builder()
          .scanRuntimeClasspath("org.openrewrite.java.migrate.javaee7")
          .build()
          .activateRecipes("org.openrewrite.java.migrate.javaee7.OpenJPAPersistenceProvider"));
    }

    @DocumentExample
    @Test
    void openJPAPersistenceProvider() {
        rewriteRun(
          //language=xml
          xml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <persistence xmlns="http://java.sun.com/xml/ns/persistence"
                           xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                           xsi:schemaLocation="http://java.sun.com/xml/ns/persistence http://java.sun.com/xml/ns/persistence/persistence_1_0.xsd"
                           version="1.0">
                  <persistence-unit name="JPAService">
                      <provider>org.apache.openjpa.persistence.PersistenceProviderImpl</provider>
                      <jta-data-source>java:comp/env/jdbc/DB2Connection</jta-data-source>
                      <class>my.jpa.Account</class>
                  </persistence-unit>
                  <persistence-unit name="JPAService1">
                      <provider>com.ibm.websphere.persistence.PersistenceProviderImpl</provider>
                      <jta-data-source>java:comp/env/jdbc/DB2Connection</jta-data-source>
                      <class>my.jpa.Account</class>
                  </persistence-unit>
                  <persistence-unit name="JPAService2">
                      <provider>org.hibernate.ejb.HibernatePersistence</provider>
                      <jta-data-source>java:comp/env/jdbc/DB2Connection</jta-data-source>
                      <class>my.jpa.Account</class>
                  </persistence-unit>
                  <persistence-unit name="JPAService3">s
                      <provider>com.ibm.websphere.persistence.PersistenceProviderImpl</provider>
                      <jta-data-source>java:comp/env/jdbc/DB2Connection</jta-data-source>
                      <class>my.jpa.Account</class>
                  </persistence-unit>
                  <persistence-unit name="JPAService4">
                      <provider>com.ibm.websphere.persistence.PersistenceProviderImpl</provider>
                  </persistence-unit>
              </persistence>
              """,
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <persistence xmlns="http://java.sun.com/xml/ns/persistence"
                           xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                           xsi:schemaLocation="http://java.sun.com/xml/ns/persistence http://java.sun.com/xml/ns/persistence/persistence_1_0.xsd"
                           version="1.0">
                  <persistence-unit name="JPAService">
                      <provider>org.eclipse.persistence.jpa.PersistenceProvider</provider>
                      <jta-data-source>java:comp/env/jdbc/DB2Connection</jta-data-source>
                      <class>my.jpa.Account</class>
                  </persistence-unit>
                  <persistence-unit name="JPAService1">
                      <provider>com.ibm.websphere.persistence.PersistenceProviderImpl</provider>
                      <jta-data-source>java:comp/env/jdbc/DB2Connection</jta-data-source>
                      <class>my.jpa.Account</class>
                  </persistence-unit>
                  <persistence-unit name="JPAService2">
                      <provider>org.hibernate.ejb.HibernatePersistence</provider>
                      <jta-data-source>java:comp/env/jdbc/DB2Connection</jta-data-source>
                      <class>my.jpa.Account</class>
                  </persistence-unit>
                  <persistence-unit name="JPAService3">s
                      <provider>com.ibm.websphere.persistence.PersistenceProviderImpl</provider>
                      <jta-data-source>java:comp/env/jdbc/DB2Connection</jta-data-source>
                      <class>my.jpa.Account</class>
                  </persistence-unit>
                  <persistence-unit name="JPAService4">
                      <provider>com.ibm.websphere.persistence.PersistenceProviderImpl</provider>
                  </persistence-unit>
              </persistence>
              """
          )
        );
    }

    @Test
    void keepOnlyOpenJPAUnitsUncachedAsPartOfJavaEE7() {
        rewriteRun(
          spec -> spec.recipeFromResources("org.openrewrite.java.migrate.javaee7"),
          //language=xml
          xml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <persistence xmlns="http://java.sun.com/xml/ns/persistence" version="2.0">
                  <persistence-unit name="openjpa">
                      <provider>org.apache.openjpa.persistence.PersistenceProviderImpl</provider>
                      <class>my.jpa.Account</class>


                      <class>my.jpa.Customer</class>
                  </persistence-unit>
                  <persistence-unit name="hibernate">
                      <provider>org.hibernate.jpa.HibernatePersistenceProvider</provider>
                      <class>my.jpa.Account</class>
                  </persistence-unit>
                  <persistence-unit name="eclipselink">
                      <class>my.jpa.Account</class>
                      <properties>
                          <property name="eclipselink.ddl-generation" value="create-or-extend-tables"/>
                      </properties>
                  </persistence-unit>
                  <persistence-unit name="websphere">
                      <provider>com.ibm.websphere.persistence.PersistenceProviderImpl</provider>
                      <class>my.jpa.Account</class>
                  </persistence-unit>
                  <persistence-unit name="hibernate-properties">
                      <class>my.jpa.Account</class>
                      <properties>
                          <property name="hibernate.hbm2ddl.auto" value="update"/>
                      </properties>
                  </persistence-unit>
                  <persistence-unit name="openjpa-properties">
                      <class>my.jpa.Account</class>
                      <properties>
                          <property name="openjpa.Log" value="SQL=TRACE"/>
                          <property name="eclipselink.logging.level" value="FINE"/>
                      </properties>
                  </persistence-unit>
              </persistence>
              """,
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <persistence xmlns="http://java.sun.com/xml/ns/persistence" version="2.0">
                  <persistence-unit name="openjpa">
                      <provider>org.eclipse.persistence.jpa.PersistenceProvider</provider>
                      <class>my.jpa.Account</class>


                      <class>my.jpa.Customer</class>
                      <shared-cache-mode>NONE</shared-cache-mode>
                  </persistence-unit>
                  <persistence-unit name="hibernate">
                      <provider>org.hibernate.jpa.HibernatePersistenceProvider</provider>
                      <class>my.jpa.Account</class>
                  </persistence-unit>
                  <persistence-unit name="eclipselink">
                      <class>my.jpa.Account</class>
                      <properties>
                          <property name="eclipselink.ddl-generation" value="create-or-extend-tables"/>
                      </properties>
                  </persistence-unit>
                  <persistence-unit name="websphere">
                      <provider>com.ibm.websphere.persistence.PersistenceProviderImpl</provider>
                      <class>my.jpa.Account</class>
                      <shared-cache-mode>NONE</shared-cache-mode>
                  </persistence-unit>
                  <persistence-unit name="hibernate-properties">
                      <class>my.jpa.Account</class>
                      <properties>
                          <property name="hibernate.hbm2ddl.auto" value="update"/>
                      </properties>
                  </persistence-unit>
                  <persistence-unit name="openjpa-properties">
                      <class>my.jpa.Account</class>
                      <shared-cache-mode>NONE</shared-cache-mode>
                      <properties>
                          <property name="openjpa.Log" value="SQL=TRACE"/>
                          <property name="eclipselink.logging.level" value="FINE"/>
                      </properties>
                  </persistence-unit>
              </persistence>
              """,
            sourceSpecs -> sourceSpecs.path("src/main/resources/META-INF/persistence.xml")
          ),
          //language=xml
          xml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <persistence xmlns="http://xmlns.jcp.org/xml/ns/persistence" version="2.1">
                  <persistence-unit name="default-provider">
                      <class>my.jpa.Account</class>
                  </persistence-unit>
              </persistence>
              """,
            sourceSpecs -> sourceSpecs.path("module/src/main/resources/META-INF/persistence.xml")
          )
        );
    }
}
