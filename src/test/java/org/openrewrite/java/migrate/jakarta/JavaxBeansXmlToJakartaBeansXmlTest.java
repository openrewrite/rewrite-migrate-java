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
package org.openrewrite.java.migrate.jakarta;

import static org.openrewrite.xml.Assertions.xml;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.config.Environment;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

class JavaxBeansXmlToJakartaBeansXmlTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion()
            .classpathFromResources(new InMemoryExecutionContext(), "cdi-api-2.0.SP1", "jsf-api-2.1.29-11", "jakarta.enterprise.cdi-api-4.0.1"))
          .recipe(Environment.builder().scanRuntimeClasspath("org.openrewrite.java.migrate.jakarta")
            .build()
            .activateRecipes("org.openrewrite.java.migrate.jakarta.JavaxBeansXmlToJakartaBeansXml"));
    }

    @DocumentExample
    @Test
    void noSchemaCD1() {
        rewriteRun(
          spec -> spec.expectedCyclesThatMakeChanges(2),
          //language=xml
          xml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <beans xmlns="http://java.sun.com/xml/ns/javaee"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xsi:schemaLocation="https://jakarta.ee/xml/ns/jakarXXtaee https://jakarta.ee/xml/ns/jakartaee/beans111_3_0.xsd">
              </beans>
              """,
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <beans xmlns="https://jakarta.ee/xml/ns/jakartaee"
                  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                  xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_4_0.xsd">
              </beans>
              """,
            sourceSpecs -> sourceSpecs.path("beans.xml")
          )
        );
    }

    @Test
    void noSchemaCD12() {
        rewriteRun(
          spec -> spec.expectedCyclesThatMakeChanges(2),
          //language=xml
          xml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <beans xmlns="http://xmlns.jcp.org/xml/ns/javaee" 
                 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                 xsi:schemaLocation="http://xmlns.jcp.org/DDDDxml/ns/javaee22 http://xmlns.jcp.org11/xml/ns/javaee777/beans_1_1.xsd">
              </beans> 
              """,
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <beans xmlns="https://jakarta.ee/xml/ns/jakartaee" 
                 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                 xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/beans_4_0.xsd">
              </beans> 
              """,
            sourceSpecs -> sourceSpecs.path("beans.xml")
          )
        );
    }
}