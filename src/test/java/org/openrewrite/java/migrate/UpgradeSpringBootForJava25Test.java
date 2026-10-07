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
package org.openrewrite.java.migrate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.maven.Assertions.pomXml;

class UpgradeSpringBootForJava25Test implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.migrate.UpgradePluginsForJava25");
    }

    @ParameterizedTest
    @ValueSource(strings = {"3.0.1", "3.1.0", "3.2.1", "3.5.0"})
    void upgradesBoot3Parent(String version) {
        rewriteRun(
          pomXml(
            pom(version),
            spec -> spec.after(actual -> assertThat(actual)
              .containsPattern("<version>3\\.5\\.\\d+</version>")
              .actual())
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"2.7.18", "4.0.0"})
    void doesNotCrossBootMajorVersions(String version) {
        rewriteRun(pomXml(pom(version)));
    }

    @Test
    void scopesUpgradeToBoot3InMixedProjects() {
        rewriteRun(
          pomXml(pom("2.7.18"), spec -> spec.path("boot2/pom.xml")),
          pomXml(pom("3.2.1"), spec -> spec.path("boot3/pom.xml")
            .after(actual -> assertThat(actual).containsPattern("<version>3\\.5\\.\\d+</version>").actual())),
          pomXml(pom("4.0.0"), spec -> spec.path("boot4/pom.xml"))
        );
    }

    @Test
    void includedInJava25Migration() {
        rewriteRun(
          spec -> spec.recipeFromResources("org.openrewrite.java.migrate.UpgradeToJava25"),
          pomXml(
            pom("3.2.1").replace("<java.version>25", "<java.version>17"),
            spec -> spec.after(actual -> assertThat(actual)
              .contains("<java.version>25</java.version>")
              .containsPattern("<version>3\\.5\\.\\d+</version>").actual())
          )
        );
    }

    private static String pom(String version) {
        return """
          <project>
              <modelVersion>4.0.0</modelVersion>
              <parent>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-parent</artifactId>
                  <version>%s</version>
                  <relativePath/>
              </parent>
              <groupId>com.example</groupId>
              <artifactId>app</artifactId>
              <version>1</version>
              <properties>
                  <java.version>25</java.version>
              </properties>
          </project>
          """.formatted(version);
    }
}
