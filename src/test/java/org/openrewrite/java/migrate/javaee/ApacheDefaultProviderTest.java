/*
 * Copyright 2023 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.migrate.javaee;

import org.junit.jupiter.api.Test;
import org.openrewrite.config.Environment;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.xml.Assertions.xml;

class ApacheDefaultProviderTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(Environment.builder()
          .scanRuntimeClasspath("org.openrewrite.java.migrate.javaee8")
          .build()
          .activateRecipes("org.openrewrite.java.migrate.javaee8.ApacheDefaultProvider"));
    }

    @Test
    void replaceApache() {
        rewriteRun(
          //language=xml
          xml("""
            <?xml version="1.0" encoding="UTF-8"?>
            <validation-config
                xmlns="http://jboss.org/xml/ns/javax/validation/configuration"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xsi:schemaLocation="http://jboss.org/xml/ns/javax/validation/configuration validation-configuration-1.1.xsd"
                version="1.1">
                <default-provider>org.apache.bval.jsr303.ApacheValidationProvider</default-provider>
                <message-interpolator>org.apache.bval.jsr.DefaultMessageInterpolator</message-interpolator>
                <traversable-resolver>org.apache.bval.jsr.resolver.DefaultTraversableResolver</traversable-resolver>
                <constraint-validator-factory>org.apache.bval.jsr.DefaultConstraintValidatorFactory</constraint-validator-factory>
                <parameter-name-provider>org.apache.bval.jsr.parameter.DefaultParameterNameProvider</parameter-name-provider>
            </validation-config>
             """, """
            <?xml version="1.0" encoding="UTF-8"?>
            <validation-config
                xmlns="http://jboss.org/xml/ns/javax/validation/configuration"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xsi:schemaLocation="http://jboss.org/xml/ns/javax/validation/configuration validation-configuration-1.1.xsd"
                version="1.1">
                <default-provider>org.hibernate.validator.HibernateValidator</default-provider>
                <message-interpolator>org.hibernate.validator.messageinterpolation.ResourceBundleMessageInterpolator</message-interpolator>
                <traversable-resolver>org.hibernate.validator.engine.resolver.DefaultTraversableResolver</traversable-resolver>
                <constraint-validator-factory>org.hibernate.validator.engine.ConstraintValidatorFactoryImpl</constraint-validator-factory>                                                                                                                 
            </validation-config>
             """
          )
        );
    }
}
