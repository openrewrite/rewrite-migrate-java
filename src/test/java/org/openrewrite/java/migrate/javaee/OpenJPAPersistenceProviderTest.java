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
package org.openrewrite.java.migrate.javaee;

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
}
