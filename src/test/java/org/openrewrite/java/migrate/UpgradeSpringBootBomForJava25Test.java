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
package org.openrewrite.java.migrate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.maven.Assertions.pomXml;

class UpgradeSpringBootBomForJava25Test implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.migrate.UpgradeSpringBootBomForJava25");
    }

    @DocumentExample
    @Test
    void upgradesImportedStarterParentThroughProperty() {
        rewriteRun(
          pomXml(
            pom("spring-boot-starter-parent", "3.3.3"),
            spec -> spec.after(actual -> assertThat(actual)
              .containsPattern("<springboot.version>3\\.5\\.\\d+</springboot.version>")
              .actual())
          )
        );
    }

    @Test
    void includedInJava25Migration() {
        rewriteRun(
          spec -> spec.recipeFromResources("org.openrewrite.java.migrate.UpgradeToJava25"),
          pomXml(
            pom("spring-boot-dependencies", "3.2.1"),
            spec -> spec.after(actual -> assertThat(actual)
              .containsPattern("<springboot.version>3\\.5\\.\\d+</springboot.version>")
              .actual())
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"3.0.1", "3.3.3"})
    void upgradesImportedBootDependencies(String version) {
        rewriteRun(
          pomXml(
            pom("spring-boot-dependencies", version),
            spec -> spec.after(actual -> assertThat(actual)
              .containsPattern("<springboot.version>3\\.5\\.\\d+</springboot.version>")
              .actual())
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"2.7.18", "4.0.0"})
    void doesNotCrossBootMajorVersions(String version) {
        rewriteRun(pomXml(pom("spring-boot-dependencies", version)));
    }

    private static String pom(String artifactId, String version) {
        return """
          <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>com.example</groupId>
              <artifactId>app</artifactId>
              <version>1</version>
              <properties>
                  <springboot.version>%s</springboot.version>
              </properties>
              <dependencyManagement>
                  <dependencies>
                      <dependency>
                          <groupId>org.springframework.boot</groupId>
                          <artifactId>%s</artifactId>
                          <version>${springboot.version}</version>
                          <type>pom</type>
                          <scope>import</scope>
                      </dependency>
                  </dependencies>
              </dependencyManagement>
          </project>
          """.formatted(version, artifactId);
    }
}
