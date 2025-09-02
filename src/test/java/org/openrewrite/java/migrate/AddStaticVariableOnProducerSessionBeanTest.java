/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.java.migrate;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.xml.Assertions.xml;

class AddStaticVariableOnProducerSessionBeanTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AddStaticVariableOnProducerSessionBean())
          //language=java
          .parser(JavaParser.fromJavaVersion()
            .dependsOn(
              """
                package jakarta.enterprise.inject;
                public @interface Produces {}
                """,
              """
                package jakarta.ejb;
                public @interface Stateless {}
                """,
              """
                package jakarta.ejb;
                public @interface Stateful {}
                """,
              """
                package jakarta.ejb;
                public @interface Singleton {}
                """,
              """
                package com.test;
                public class SomeDependency {}
                """
            )
          );
    }

    @DocumentExample
    @Test
    void addStaticOnProducesMarkedStateless() {
        rewriteRun(
          //language=java
          java(
            """
              package com.test;
              import jakarta.ejb.Stateless;
              import jakarta.enterprise.inject.Produces;

              @Stateless
              public class MySessionBean {
                  @Produces
                  private SomeDependency someDependency;
                  void exampleMethod() {
                     return;
                  }
              }
              """,
            """
              package com.test;
              import jakarta.ejb.Stateless;
              import jakarta.enterprise.inject.Produces;

              @Stateless
              public class MySessionBean {
                  @Produces
                  private static SomeDependency someDependency;
                  void exampleMethod() {
                     return;
                  }
              }
              """
          )
        );
    }

    @Test
    void addStaticToProducesFieldFromXml() {
        rewriteRun(
          xml(
            //language=xml
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <ejb-jar xmlns="http://java.sun.com/xml/ns/javaee" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xsi:schemaLocation="http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/ejb-jar_3_1.xsd" version="3.1">
              	<display-name>testTransaction</display-name>
              	<enterprise-beans>
                      <session>
                          <ejb-name>MySessionBean</ejb-name>
                          <ejb-class>com.test.MySessionBean</ejb-class>
                          <session-type>Stateless</session-type>
                          <transaction-type>Container</transaction-type>
                      </session>
                  </enterprise-beans>
              </ejb-jar>
              """,
            sourceSpecs -> sourceSpecs.path("ejb-jar.xml")
          ),
          //language=java
          java(
            """
              package com.test;
              import jakarta.enterprise.inject.Produces;

              public class MySessionBean {
                  @Produces
                  private SomeDependency someDependency;
                  void exampleMethod() {
                     return;
                  }
              }
              """,
            """
              package com.test;
              import jakarta.enterprise.inject.Produces;

              public class MySessionBean {
                  @Produces
                  private static SomeDependency someDependency;
                  void exampleMethod() {
                     return;
                  }
              }
              """
          )
        );
    }

    @Test
    void noChangeWhenBeanNotMentionedInXml() {
        rewriteRun(
          xml(
            //language=xml
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <ejb-jar xmlns="http://java.sun.com/xml/ns/javaee" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xsi:schemaLocation="http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/ejb-jar_3_1.xsd" version="3.1">
              	<display-name>testTransaction</display-name>
              	<enterprise-beans>
                      <session>
                          <ejb-name>TestProducerFieldStaticOnSessionBean</ejb-name>
                          <ejb-class>org.test.ejb.TestProducerFieldStaticOnSessionBean</ejb-class>
                          <session-type>Stateless</session-type>
                          <transaction-type>Container</transaction-type>
                      </session>
                      <session>
                           <ejb-name>TestProducerFieldNonStaticOnSessionBean</ejb-name>
                           <ejb-class>org.test.ejb.TestProducerFieldNonStaticOnSessionBean</ejb-class>
                           <session-type>Singleton</session-type>
                           <transaction-type>Container</transaction-type>
                      </session>
                  </enterprise-beans>
              </ejb-jar>
              """,
            sourceSpecs -> sourceSpecs.path("ejb-jar.xml")
          ),
          //language=java
          java(
            """
              package com.test;
              import jakarta.enterprise.inject.Produces;

              public class MySessionBean {
                  @Produces
                  private SomeDependency someDependency;
                  void exampleMethod() {
                     return;
                  }
              }
              """
          )
        );
    }

    @Test
    void addStaticOnProducesMarkedStateful() {
        rewriteRun(
          //language=java
          java(
            """
              package com.test;
              import jakarta.ejb.Stateful;
              import jakarta.enterprise.inject.Produces;

              @Stateful
              public class MySessionBean {
                  @Produces
                  private SomeDependency someDependency;
                  void exampleMethod() {
                     return;
                  }
              }
              """,
            """
              package com.test;
              import jakarta.ejb.Stateful;
              import jakarta.enterprise.inject.Produces;

              @Stateful
              public class MySessionBean {
                  @Produces
                  private static SomeDependency someDependency;
                  void exampleMethod() {
                     return;
                  }
              }
              """
          )
        );
    }

    @Test
    void addStaticOnProducesMarkedSingleton() {
        rewriteRun(
          //language=java
          java(
            """
              package com.test;
              import jakarta.ejb.Singleton;
              import jakarta.enterprise.inject.Produces;

              @Singleton
              public class MySessionBean {
                  @Produces
                  private SomeDependency someDependency;
                  void exampleMethod() {
                     return;
                  }
              }
              """,
            """
              package com.test;
              import jakarta.ejb.Singleton;
              import jakarta.enterprise.inject.Produces;

              @Singleton
              public class MySessionBean {
                  @Produces
                  private static SomeDependency someDependency;
                  void exampleMethod() {
                     return;
                  }
              }
              """
          )
        );
    }

    @Test
    void noChangeOnStaticVariable() {
        rewriteRun(
          //language=java
          java(
            """
              package com.test;
              import jakarta.ejb.Singleton;
              import jakarta.enterprise.inject.Produces;

              @Singleton
              public class MySessionBean {
                  @Produces
                  private static SomeDependency someDependency;
              }
              """
          )
        );
    }
}
