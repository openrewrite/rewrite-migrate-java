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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.*;
import static org.openrewrite.maven.Assertions.pomXml;

class ProvidedJakartaPlatformApiTest implements RewriteTest {
    @ParameterizedTest
    @CsvSource({
      "JavaxEnterpriseToJakartaEnterprise, javax.enterprise.event.Event, javax, javaee-api, 7.0",
      "JavaxEnterpriseToJakartaEnterprise, javax.enterprise.event.Event, jakarta.platform, jakarta.jakartaee-api, 9.1.0",
      "JavaxEjbToJakartaEjb, javax.ejb.EJBContext, javax, javaee-api, 7.0",
      "JavaxEjbToJakartaEjb, javax.ejb.EJBContext, jakarta.platform, jakarta.jakartaee-api, 9.1.0",
      "JavaxInjectMigrationToJakartaInject, javax.inject.Provider, javax, javaee-api, 7.0",
      "JavaxInjectMigrationToJakartaInject, javax.inject.Provider, jakarta.platform, jakarta.jakartaee-api, 9.1.0",
      "JavaxWsToJakartaWs, javax.ws.rs.core.Response, javax, javaee-api, 7.0",
      "JavaxWsToJakartaWs, javax.ws.rs.core.Response, jakarta.platform, jakarta.jakartaee-api, 9.1.0"
    })
    void platformAlreadySuppliesApi(String recipe, String type, String group, String artifact, String version) {
        int dot = type.lastIndexOf('.');
        rewriteRun(
          spec -> spec.recipeFromResource("/META-INF/rewrite/jakarta-ee-9.yml", "org.openrewrite.java.migrate.jakarta." + recipe)
            .parser(JavaParser.fromJavaVersion().dependsOn("package " + type.substring(0, dot) + "; public interface " + type.substring(dot + 1) + " {}")),
          mavenProject("app",
            srcMainJava(java("class A { " + type + " value; }", "class A { " + type.replace("javax.", "jakarta.") + " value; }")),
            pomXml(
              """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>app</artifactId>
                    <version>1</version>
                    <dependencies>
                        <dependency>
                            <groupId>%s</groupId>
                            <artifactId>%s</artifactId>
                            <version>%s</version>
                            <scope>provided</scope>
                        </dependency>
                    </dependencies>
                </project>
                """.formatted(group, artifact, version)
            )
          )
        );
    }
}
