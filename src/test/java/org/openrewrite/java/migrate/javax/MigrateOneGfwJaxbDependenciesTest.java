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
import org.openrewrite.DocumentExample;
import org.openrewrite.config.Environment;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;
import static org.openrewrite.maven.Assertions.pomXml;

class MigrateOneGfwJaxbDependenciesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(Environment.builder()
          .scanRuntimeClasspath("org.openrewrite.java.migrate.javax")
          .build()
          .activateRecipes("org.openrewrite.java.migrate.javax.MigrateOneGfwJaxbDependencies"));
    }

    @DocumentExample
    @Test
    void jaxbApiMaven() {
        rewriteRun(
          pomXml(
            //language=xml
            """
              <project>
                  <groupId>com.example.jaxb</groupId>
                  <artifactId>jaxb-example</artifactId>
                  <version>1.0.0</version>
                  <dependencies>
                      <dependency>
                          <groupId>one.gfw</groupId>
                          <artifactId>jaxb-api</artifactId>
                          <version>2.3.1.1</version>
                      </dependency>
                  </dependencies>
              </project>
              """,
            //language=xml
            """
              <project>
                  <groupId>com.example.jaxb</groupId>
                  <artifactId>jaxb-example</artifactId>
                  <version>1.0.0</version>
                  <dependencies>
                      <dependency>
                          <groupId>javax.xml.bind</groupId>
                          <artifactId>jaxb-api</artifactId>
                          <version>2.3.1</version>
                      </dependency>
                  </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void jaxbApiGradle() {
        rewriteRun(
          spec -> spec.beforeRecipe(withToolingApi()),
          buildGradle(
            //language=gradle
            """
              plugins {
                  id "java-library"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation "one.gfw:jaxb-api:2.3.1.1"
              }
              """,
            //language=gradle
            """
              plugins {
                  id "java-library"
              }

              repositories {
                  mavenCentral()
              }

              dependencies {
                  implementation "javax.xml.bind:jaxb-api:2.3.1"
              }
              """
          )
        );
    }

    @Test
    void jakartaApiAndRuntimeIncludingManaged() {
        rewriteRun(
          pomXml(
            //language=xml
            """
              <project>
                  <groupId>com.example.jaxb</groupId>
                  <artifactId>jaxb-example</artifactId>
                  <version>1.0.0</version>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>one.gfw</groupId>
                              <artifactId>jaxb-core</artifactId>
                              <version>4.0.2.1</version>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
                  <dependencies>
                      <dependency>
                          <groupId>one.gfw</groupId>
                          <artifactId>jakarta.xml.bind-api</artifactId>
                          <version>4.0.0.1</version>
                      </dependency>
                      <dependency>
                          <groupId>one.gfw</groupId>
                          <artifactId>jaxb-impl</artifactId>
                          <version>4.0.2.1</version>
                          <scope>runtime</scope>
                      </dependency>
                  </dependencies>
              </project>
              """,
            spec -> spec.after(pom -> assertThat(pom)
              .doesNotContain("one.gfw")
              .containsPattern("<groupId>jakarta.xml.bind</groupId>\\s*<artifactId>jakarta.xml.bind-api</artifactId>\\s*<version>4\\.0\\.\\d+</version>")
              .containsPattern("<groupId>com.sun.xml.bind</groupId>\\s*<artifactId>jaxb-core</artifactId>\\s*<version>4\\.0\\.\\d+</version>")
              .containsPattern("<groupId>com.sun.xml.bind</groupId>\\s*<artifactId>jaxb-impl</artifactId>\\s*<version>4\\.0\\.\\d+</version>")
              .actual())
          )
        );
    }
}
