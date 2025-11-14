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

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;
import static org.openrewrite.maven.Assertions.pomXml;

class UpdateApacheShiroDependenciesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.migrate.jakarta.UpdateApacheShiroDependencies");
    }

    @DocumentExample
    @Test
    void migrateShiroDependenciesMaven() {
        rewriteRun(
          //language=xml
          pomXml(
            """
              <project>
                  <groupId>com.example.shiro</groupId>
                  <artifactId>shiro-legacy</artifactId>
                  <version>1.0.0</version>
                  <dependencies>
                      <dependency>
                          <groupId>org.apache.shiro</groupId>
                          <artifactId>shiro-core</artifactId>
                          <version>1.13.0</version>
                      </dependency>
                      <dependency>
                          <groupId>org.apache.shiro</groupId>
                          <artifactId>shiro-web</artifactId>
                          <version>1.13.0</version>
                      </dependency>
                  </dependencies>
              </project>
              """,
            spec -> spec.after(pom -> assertThat(pom)
              .containsPattern("<version>2.0.\\d+</version>")
              .contains("<classifier>jakarta</classifier>")
              .actual())
          )
        );
    }

    @Test
    void migrateShiroDependenciesGradle() {
        rewriteRun(
          spec -> spec.beforeRecipe(withToolingApi()),
          buildGradle(
            //language=groovy
            """
              plugins {
                  id("java-library")
              }
              repositories {
                  mavenCentral()
              }
              dependencies {
                  implementation 'org.apache.shiro:shiro-core:1.13.0'
                  implementation 'org.apache.shiro:shiro-web:1.13.0'
              }
              """,
            spec -> spec.after(gradle -> assertThat(gradle)
              .containsPattern("2.0.\\d+:jakarta")
              .actual())
          )
        );
    }
}
