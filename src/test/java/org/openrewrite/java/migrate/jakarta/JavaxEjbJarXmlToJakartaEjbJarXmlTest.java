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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.config.Environment;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.xml.Assertions.xml;

class JavaxEjbJarXmlToJakartaEjbJarXmlTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(Environment.builder().scanRuntimeClasspath("org.openrewrite.java.migrate.jakarta").build()
          .activateRecipes("org.openrewrite.java.migrate.jakarta.JavaxEjbJarXmlToJakartaEjbJarXml"));
    }

    @DocumentExample
    @Test
    void migrateJCP() {
        rewriteRun(
          //language=xml
          xml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <ejb-jar xmlns="http://java.sun.com/xml/ns/javaee"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xsi:schemaLocation="http://java.sun.com/xml/ns/javaee  http://java.sun.com/xml/ns/javaee/ejb-jar_3_0.xsd"
                  version="3.0">
                  <enterprise-beans>
                      <session>
                          <ejb-name>HelloSessionBean</ejb-name>
                          <mapped-name>ejb/HelloSessionBean</mapped-name>
                          <business-local>com.mydomain.HelloSessionBeanLocal</business-local>
                          <business-remote>com.mydomain.HelloSessionBeanRemote</business-remote>
                          <ejb-class>com.mydomain.HelloSessionBean</ejb-class>
                          <session-type>Stateless</session-type>
                          <transaction-type>Container</transaction-type>
                      </session>
                      <message-driven>
                          <ejb-name>MessageBean</ejb-name>
                          <ejb-class>samples.mdb.ejb.MessageBean</ejb-class>
                          <transaction-type>Container</transaction-type>
                          <message-driven-destination>
                              <destination-type>javax.jms.Queue</destination-type>
                          </message-driven-destination>
                          <resource-ref>
                              <res-ref-name>jms/QueueConnectionFactory</res-ref-name>
                              <res-type>javax.jms.QueueConnectionFactory</res-type>
                              <res-auth>Container</res-auth>
                          </resource-ref>
                      </message-driven>
                      <assembly-descriptor>
                          <container-transaction>
                              <method>
                                  <ejb-name>MessageBean</ejb-name>
                                  <method-intf>Bean</method-intf>
                                  <method-name>onMessage</method-name>
                                  <method-params>
                                      <method-param>javax.jms.Message</method-param>
                                  </method-params>
                              </method>
                              <trans-attribute>NotSupported</trans-attribute>
                          </container-transaction>
                      </assembly-descriptor>
                  </enterprise-beans>
              </ejb-jar>
              """,
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <ejb-jar xmlns="https://jakarta.ee/xml/ns/jakartaee"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/ejb-jar_4_0.xsd"
                  version="4.0">
                  <enterprise-beans>
                      <session>
                          <ejb-name>HelloSessionBean</ejb-name>
                          <mapped-name>ejb/HelloSessionBean</mapped-name>
                          <business-local>com.mydomain.HelloSessionBeanLocal</business-local>
                          <business-remote>com.mydomain.HelloSessionBeanRemote</business-remote>
                          <ejb-class>com.mydomain.HelloSessionBean</ejb-class>
                          <session-type>Stateless</session-type>
                          <transaction-type>Container</transaction-type>
                      </session>
                      <message-driven>
                          <ejb-name>MessageBean</ejb-name>
                          <ejb-class>samples.mdb.ejb.MessageBean</ejb-class>
                          <transaction-type>Container</transaction-type>
                          <message-driven-destination>
                              <destination-type>jakarta.jms.Queue</destination-type>
                          </message-driven-destination>
                          <resource-ref>
                              <res-ref-name>jms/QueueConnectionFactory</res-ref-name>
                              <res-type>jakarta.jms.QueueConnectionFactory</res-type>
                              <res-auth>Container</res-auth>
                          </resource-ref>
                      </message-driven>
                      <assembly-descriptor>
                          <container-transaction>
                              <method>
                                  <ejb-name>MessageBean</ejb-name>
                                  <method-intf>Bean</method-intf>
                                  <method-name>onMessage</method-name>
                                  <method-params>
                                      <method-param>jakarta.jms.Message</method-param>
                                  </method-params>
                              </method>
                              <trans-attribute>NotSupported</trans-attribute>
                          </container-transaction>
                      </assembly-descriptor>
                  </enterprise-beans>
              </ejb-jar>
              """,
            sourceSpecs -> sourceSpecs.path("ejb-jar.xml")
          )
        );
    }

    @Nested
    class NoChanges {
        @Test
        void fileNotWebXml() {
            rewriteRun(
              //language=xml
              xml(
                """
                  <beans xmlns="http://java.sun.com/xml/ns/javaee"
                      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                      xsi:schemaLocation="http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/beans_1_0.xsd">
                  </beans>
                  """,
                sourceSpecs -> sourceSpecs.path("not-ejb-jar.xml")
              )
            );
        }
    }
}
