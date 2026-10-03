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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.maven.Assertions.pomXml;
import static org.openrewrite.properties.Assertions.properties;

class UpgradeToJava17MavenWrapperTest implements RewriteTest {
    private static final String POM = """
      <project>
          <modelVersion>4.0.0</modelVersion>
          <groupId>com.example</groupId>
          <artifactId>demo</artifactId>
          <version>1</version>
          <properties>
              <java.version>1.8</java.version>
          </properties>
          <build>
              <plugins>
                  <plugin>
                      <groupId>org.apache.maven.plugins</groupId>
                      <artifactId>maven-compiler-plugin</artifactId>
                      <version>3.7.0</version>
                  </plugin>
              </plugins>
          </build>
      </project>
      """;

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.migrate.UpgradeToJava17");
    }

    @Test
    void upgradeExistingWrapperAlongsideCompilerPlugin() {
        rewriteRun(
          pomXml(POM, spec -> spec.after(actual -> assertThat(actual)
            .contains("<java.version>17</java.version>")
            .doesNotContain("<version>3.7.0</version>")
            .actual())),
          properties(
            "distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.5.0/apache-maven-3.5.0-bin.zip",
            spec -> spec.path(".mvn/wrapper/maven-wrapper.properties")
              .after(actual -> assertThat(actual)
                .containsPattern("distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3\\.9\\.\\d+/apache-maven-3\\.9\\.\\d+-bin.zip")
                .doesNotContain("apache-maven-3.5.0")
                .actual())
          )
        );
    }

    @Test
    void doNotCreateWrapperWhenAbsent() {
        rewriteRun(
          spec -> spec.afterRecipe(run -> assertThat(run.getChangeset().getAllResults())
            .allSatisfy(result -> assertThat(result.getAfter().getSourcePath().toString()).isEqualTo("pom.xml"))),
          pomXml(POM, spec -> spec.after(actual -> assertThat(actual)
            .contains("<java.version>17</java.version>")
            .doesNotContain("<version>3.7.0</version>")
            .actual()))
        );
    }

    @Test
    void doNotDowngradeMaven4Wrapper() {
        rewriteRun(
          pomXml(POM, spec -> spec.after(actual -> assertThat(actual)
            .contains("<java.version>17</java.version>")
            .doesNotContain("<version>3.7.0</version>")
            .actual())),
          properties(
            "distributionUrl=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/4.0.0-rc-5/apache-maven-4.0.0-rc-5-bin.zip",
            spec -> spec.path(".mvn/wrapper/maven-wrapper.properties")
          )
        );
    }
}
