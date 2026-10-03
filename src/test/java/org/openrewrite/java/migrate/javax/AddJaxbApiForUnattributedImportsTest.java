/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.java.migrate.javax;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.TypeValidation;

import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.maven.Assertions.pomXml;

class AddJaxbApiForUnattributedImportsTest implements RewriteTest {
    @Test
    void addApiForUnresolvedImport() {
        rewriteRun(
          spec -> spec.recipeFromResources("org.openrewrite.java.migrate.javax.AddJaxbAPIDependencies")
            .typeValidationOptions(TypeValidation.none()),
          java(
            """
              import javax.xml.bind.DatatypeConverter;
              class Test {
                  String encode(byte[] bytes) {
                      return DatatypeConverter.printBase64Binary(bytes);
                  }
              }
              """,
            spec -> spec.path("src/main/java/Test.java")
          ),
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>example</groupId>
                  <artifactId>app</artifactId>
                  <version>1</version>
              </project>
              """,
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>example</groupId>
                  <artifactId>app</artifactId>
                  <version>1</version>
                  <dependencies>
                      <dependency>
                          <groupId>jakarta.xml.bind</groupId>
                          <artifactId>jakarta.xml.bind-api</artifactId>
                          <version>2.3.3</version>
                      </dependency>
                  </dependencies>
              </project>
              """
          )
        );
    }
    @Test
    void scopeToNearestModuleAndTestSources() {
        rewriteRun(
          spec -> spec.recipe(new AddJaxbApiForImports()).typeValidationOptions(TypeValidation.none()),
          java(
            "import javax.xml.bind.DatatypeConverter; class Test {}",
            spec -> spec.path("app/src/test/java/Test.java")
          ),
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>example</groupId>
                  <artifactId>root</artifactId>
                  <version>1</version>
              </project>
              """
          ),
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>example</groupId>
                  <artifactId>app</artifactId>
                  <version>1</version>
              </project>
              """,
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>example</groupId>
                  <artifactId>app</artifactId>
                  <version>1</version>
                  <dependencies>
                      <dependency>
                          <groupId>jakarta.xml.bind</groupId>
                          <artifactId>jakarta.xml.bind-api</artifactId>
                          <version>2.3.3</version>
                          <scope>test</scope>
                      </dependency>
                  </dependencies>
              </project>
              """,
            spec -> spec.path("app/pom.xml")
          )
        );
    }
}
