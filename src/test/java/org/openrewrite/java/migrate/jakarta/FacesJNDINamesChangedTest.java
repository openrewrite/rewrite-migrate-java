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

import org.junit.jupiter.api.Test;
import org.openrewrite.config.Environment;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.xml.Assertions.xml;

public class FacesJNDINamesChangedTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(
          Environment.builder()
            .scanRuntimeClasspath("org.openrewrite.java.liberty")
            .build()
            .activateRecipes("org.openrewrite.java.migrate.jakarta.FacesJNDINamesChanged"));
    }

    @Test
    void replaceJNDI() {
        rewriteRun(
          //language=xml
          xml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <web-app xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://xmlns.jcp.org/xml/ns/javaee" xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-app_3_1.xsd" version="3.1">
                  <env-entry>
                      <env-entry-name>jsf/ClientSideSecretKey</env-entry-name>
                      <env-entry-type>java.lang.String</env-entry-type>
                      <env-entry-value>secret</env-entry-value>
                  </env-entry>
                  <env-entry>
                       <env-entry-name>jsf/FlashSecretKey</env-entry-name>
                       <env-entry-type>java.lang.String</env-entry-type>
                       <env-entry-value>secret</env-entry-value>
                   </env-entry>
              </web-app>
              """,
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <web-app xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://xmlns.jcp.org/xml/ns/javaee" xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/web-app_3_1.xsd" version="3.1">
                  <env-entry>
                      <env-entry-name>faces/ClientSideSecretKey</env-entry-name>
                      <env-entry-type>java.lang.String</env-entry-type>
                      <env-entry-value>secret</env-entry-value>
                  </env-entry>
                  <env-entry>
                       <env-entry-name>faces/FlashSecretKey</env-entry-name>
                       <env-entry-type>java.lang.String</env-entry-type>
                       <env-entry-value>secret</env-entry-value>
                   </env-entry>
              </web-app>
              """
          )
        );
    }
}
