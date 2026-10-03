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
import org.openrewrite.config.Environment;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

class WeldToJakartaTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(Environment.builder().scanRuntimeClasspath("org.openrewrite.java.migrate")
          .build().activateRecipes("org.openrewrite.java.migrate.jakarta.JavaxMigrationToJakarta"));
    }

    @ParameterizedTest
    @CsvSource({"weld-core,1.1.5.Final", "weld-core,2.4.8.Final", "weld-core-impl,3.1.9.Final"})
    void migrateJavaxWeldImplementation(String artifact, String version) {
        rewriteRun(
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>app</artifactId>
                  <version>1</version>
                  <dependencies>
                      <dependency>
                          <groupId>org.jboss.weld</groupId>
                          <artifactId>%s</artifactId>
                          <version>%s</version>
                          <scope>test</scope>
                      </dependency>
                  </dependencies>
              </project>
              """.formatted(artifact, version),
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>app</artifactId>
                  <version>1</version>
                  <dependencies>
                      <dependency>
                          <groupId>org.jboss.weld</groupId>
                          <artifactId>weld-core-impl</artifactId>
                          <version>4.0.3.Final</version>
                          <scope>test</scope>
                      </dependency>
                  </dependencies>
              </project>
              """
          )
        );
    }
}
