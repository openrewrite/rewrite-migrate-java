/*
 * Copyright 2025 the original author or authors.
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
import org.openrewrite.DocumentExample;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.Scope;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.function.UnaryOperator.identity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;
import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.maven.Assertions.pomXml;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;

public class ComIntelliJAnnotationsToOrgJetbrainsAnnotationsTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.beforeRecipe(withToolingApi());
        spec.recipeFromResource(
          "/META-INF/rewrite/intellij-annotations-to-jetbrains-annotations.yml",
          "org.openrewrite.java.migrate.ComIntelliJAnnotationsToOrgJetbrainsAnnotations"
        );
    }

    @DocumentExample
    @Test
    void mavenDependencyUpdate() {
        rewriteRun(
          mavenProject("project",
            pomXml(
              //language=xml
              """
                <project>
                  <modelVersion>4.0.0</modelVersion>

                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>

                  <dependencies>
                    <dependency>
                      <groupId>com.intellij</groupId>
                      <artifactId>annotations</artifactId>
                      <version>5.1</version>
                    </dependency>
                  </dependencies>
                </project>
                """,
              spec -> spec
                .after(identity())
                .afterRecipe(doc -> assertThat(doc.getMarkers().findFirst(MavenResolutionResult.class)
                  .get().getDependencies().get(Scope.Compile))
                  .filteredOn(rd -> rd.getDepth() == 0)
                  .satisfiesExactly(
                    rd -> {
                        assertThat(rd.getGroupId()).isEqualTo("org.jetbrains");
                        assertThat(rd.getArtifactId()).isEqualTo("annotations");
                    }))
            )
          )
        );
    }

    @DocumentExample
    @Test
    void gradleDependencyUpdates() {
        rewriteRun(
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
                  implementation("com.intellij:annotations:5.1")
                  implementation "com.intellij:annotations:6.0.3"
                  implementation group: "com.intellij", name: "annotations", version: "12.0"
              }
              """,
            spec -> spec.after(buildGradle -> {
                Matcher version = Pattern.compile("\\d+\\.\\d+(\\.\\d+)?").matcher(buildGradle);
                assertThat(version.find()).isTrue();
                String dependencyVersion = version.group(0);
                return """
                  plugins {
                      id("java-library")
                  }
                  repositories {
                      mavenCentral()
                  }
                  dependencies {
                      implementation("org.jetbrains:annotations:%1$s")
                      implementation "org.jetbrains:annotations:%1$s"
                      implementation group: "org.jetbrains", name: "annotations", version: "%1$s"
                  }
                  """.formatted(dependencyVersion);
            })
          )
        );
    }
}
