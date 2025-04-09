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

class JavaxBeanValidationXmlToJakartaBeanValidationXmlTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(Environment.builder().scanRuntimeClasspath("org.openrewrite.java.migrate.jakarta").build()
          .activateRecipes("org.openrewrite.java.migrate.jakarta.JavaxBeanValidationXmlToJakartaBeanValidationXml"));
    }

    @Test
    @DocumentExample
    void migrateJCP() {
        rewriteRun(
          //language=xml
          xml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <validation-config
                   xmlns="http://xmlns.jcp.org/xml/ns/validation/configuration"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/validation/configuration
                   http://xmlns.jcp.org/xml/ns/validation/configuration/validation-configuration2.0.xsd"
                   version="2.0">

                  <default-provider>javax.acme.ValidationProvider</default-provider>

                  <message-interpolator>javax.acme.MessageInterpolator</message-interpolator>
                  <traversable-resolver>javax.acme.TraversableResolver</traversable-resolver>
                  <constraint-validator-factory>
                      javax.acme.ConstraintValidatorFactory
                  </constraint-validator-factory>
                  <parameter-name-provider>javax.acme.ParameterNameProvider</parameter-name-provider>

                  <executable-validation enabled="true">
                      <default-validated-executable-types>
                          <executable-type>CONSTRUCTORS</executable-type>
                          <executable-type>NON_GETTER_METHODS</executable-type>
                          <executable-type>GETTER_METHODS</executable-type>
                      </default-validated-executable-types>
                  </executable-validation>

                  <constraint-mapping>META-INF/validation/constraints-car.xml</constraint-mapping>

                  <property name="javax.validator.fail_fast">false</property>
              </validation-config>
              """,
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <validation-config
                   xmlns="https://jakarta.ee/xml/ns/jakartaee"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/validation/configuration/validation-configuration-3.0.xsd"
                   version="3.0">

                  <default-provider>jakarta.acme.ValidationProvider</default-provider>

                  <message-interpolator>jakarta.acme.MessageInterpolator</message-interpolator>
                  <traversable-resolver>jakarta.acme.TraversableResolver</traversable-resolver>
                  <constraint-validator-factory>
                      jakarta.acme.ConstraintValidatorFactory
                  </constraint-validator-factory>
                  <parameter-name-provider>jakarta.acme.ParameterNameProvider</parameter-name-provider>

                  <executable-validation enabled="true">
                      <default-validated-executable-types>
                          <executable-type>CONSTRUCTORS</executable-type>
                          <executable-type>NON_GETTER_METHODS</executable-type>
                          <executable-type>GETTER_METHODS</executable-type>
                      </default-validated-executable-types>
                  </executable-validation>

                  <constraint-mapping>META-INF/validation/constraints-car.xml</constraint-mapping>

                  <property name="jakarta.validator.fail_fast">false</property>
              </validation-config>
              """,
            sourceSpecs -> sourceSpecs.path("validation.xml")
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
                sourceSpecs -> sourceSpecs.path("not-validation.xml")
              )
            );
        }
    }
}
