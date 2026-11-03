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
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;
import static org.openrewrite.java.Assertions.*;
import static org.openrewrite.maven.Assertions.pomXml;

class UpgradeLombokForJavaVersionTest implements RewriteTest {

    @Test
    void upgradesLombokToMinimumVersionForJava21() {
        rewriteRun(
          spec -> spec.recipeFromResources("org.openrewrite.java.migrate.UpgradePluginsForJava21"),
          mavenProject("project",
            pomXml(
              //language=xml
              """
                <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <dependencies>
                        <dependency>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>1.18.26</version>
                        </dependency>
                    </dependencies>
                </project>
                """,
              spec -> spec.after(actual ->
                assertThat(actual)
                  .doesNotContain("<version>1.18.26</version>")
                  .containsPattern("<version>1\\.18\\.(3[0-9]|[4-9][0-9])</version>")
                  .actual()
              )
            )
          )
        );
    }

    @Test
    void upgradesLombokToMinimumVersionForJava25() {
        rewriteRun(
          spec -> spec.recipeFromResources("org.openrewrite.java.migrate.UpgradePluginsForJava25"),
          mavenProject("project",
            pomXml(
              //language=xml
              """
                <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <dependencies>
                        <dependency>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>1.18.26</version>
                        </dependency>
                    </dependencies>
                </project>
                """,
              spec -> spec.after(actual ->
                assertThat(actual)
                  .doesNotContain("<version>1.18.26</version>")
                  .containsPattern("<version>1\\.18\\.(4[0-9]|[5-9][0-9])</version>")
                  .actual()
              )
            )
          )
        );
    }

    @Test
    void upgradesLombokForGradleJava25() {
        rewriteRun(
          spec -> spec
            .recipeFromResources("org.openrewrite.java.migrate.UpgradePluginsForJava25")
            .beforeRecipe(withToolingApi()),
          mavenProject("project",
            srcMainJava(
              //language=java
              java(
                """
                  import lombok.Data;
                  @Data
                  public class A {
                      String name;
                  }
                  """
              )
            ),
            buildGradle(
              """
                plugins {
                    id 'java-library'
                }

                dependencies {
                    compileOnly 'org.projectlombok:lombok:1.18.26'
                }
                """,
              spec -> spec.after(actual ->
                assertThat(actual)
                  .doesNotContain("1.18.26")
                  .contains("compileOnly 'org.projectlombok:lombok:1.18.40'")
                  .actual()
              )
            )
          )
        );
    }
}
