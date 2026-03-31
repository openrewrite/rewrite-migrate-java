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
import org.openrewrite.DocumentExample;
import org.openrewrite.Tree;
import org.openrewrite.marker.BuildTool;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;
import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.maven.Assertions.pomXml;
import static org.openrewrite.properties.Assertions.properties;
import static org.openrewrite.test.SourceSpecs.other;
import static org.openrewrite.test.SourceSpecs.text;

class UpgradeToJava25Test implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.migrate.UpgradeToJava25");
    }

    @DocumentExample
    @Test
    void updateCompilerVersion() {
        rewriteRun(
          mavenProject("project",
            //language=xml
            pomXml(
              """
                <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <properties>
                        <maven.compiler.release>17</maven.compiler.release>
                    </properties>
                </project>
                """,
              """
                <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <properties>
                        <maven.compiler.release>25</maven.compiler.release>
                    </properties>
                </project>
                """
            )
          )
        );
    }

    @Test
    void upgradesMavenPluginsForJava25() {
        rewriteRun(
          mavenProject("project",
            pomXml(
              //language=xml
              """
                <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <properties>
                        <maven.compiler.release>17</maven.compiler.release>
                    </properties>
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-compiler-plugin</artifactId>
                                <version>3.13.0</version>
                            </plugin>
                            <plugin>
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-surefire-plugin</artifactId>
                                <version>2.22.2</version>
                            </plugin>
                            <plugin>
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-failsafe-plugin</artifactId>
                                <version>2.22.2</version>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """,
              spec -> spec.after(actual ->
                assertThat(actual)
                  .contains("<maven.compiler.release>25</maven.compiler.release>")
                  .containsPattern("maven-compiler-plugin</artifactId>\\s*<version>3\\.15\\.")
                  .containsPattern("maven-surefire-plugin</artifactId>\\s*<version>3\\.5\\.")
                  .containsPattern("maven-failsafe-plugin</artifactId>\\s*<version>3\\.5\\.")
                  .actual())
            )
          )
        );
    }

    @Test
    void upgradesGradleWrapperForJava25() {
        rewriteRun(
          spec -> spec.recipeFromResources("org.openrewrite.java.migrate.UpgradePluginsForJava25")
            .beforeRecipe(withToolingApi())
            .allSources(source -> source.markers(new BuildTool(Tree.randomId(), BuildTool.Type.Gradle, "8.5"))),
          properties(
            """
              distributionBase=GRADLE_USER_HOME
              distributionPath=wrapper/dists
              distributionUrl=https\\://services.gradle.org/distributions/gradle-8.5-bin.zip
              zipStoreBase=GRADLE_USER_HOME
              zipStorePath=wrapper/dists
              """,
            spec -> spec.path("gradle/wrapper/gradle-wrapper.properties")
              .after(actual -> {
                  assertThat(actual).containsPattern("gradle-9\\.1\\.\\d+-bin\\.zip");
                  return actual;
              })
          ),
          text("", spec -> spec.path("gradlew").after(a -> {
              assertThat(a).isNotEmpty();
              return a + "\n";
          })),
          text("", spec -> spec.path("gradlew.bat").after(a -> {
              assertThat(a).isNotEmpty();
              return a + "\n";
          })),
          other("", spec -> spec.path("gradle/wrapper/gradle-wrapper.jar"))
        );
    }

    @Test
    void kotlinOlderThan2_3CapsJavaVersionAt24() {
        rewriteRun(
          mavenProject("project",
            pomXml(
              //language=xml
              """
                <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <properties>
                        <maven.compiler.release>17</maven.compiler.release>
                    </properties>
                    <dependencies>
                        <dependency>
                            <groupId>org.jetbrains.kotlin</groupId>
                            <artifactId>kotlin-stdlib</artifactId>
                            <version>2.2.0</version>
                        </dependency>
                    </dependencies>
                </project>
                """,
              """
                <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <properties>
                        <maven.compiler.release>24</maven.compiler.release>
                    </properties>
                    <dependencies>
                        <dependency>
                            <groupId>org.jetbrains.kotlin</groupId>
                            <artifactId>kotlin-stdlib</artifactId>
                            <version>2.2.0</version>
                        </dependency>
                    </dependencies>
                </project>
                """
            )
          )
        );
    }

    @Test
    void kotlinNewerThan2_3UpgradesToJava25() {
        rewriteRun(
          mavenProject("project",
            pomXml(
              //language=xml
              """
                <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <properties>
                        <maven.compiler.release>17</maven.compiler.release>
                    </properties>
                    <dependencies>
                        <dependency>
                            <groupId>org.jetbrains.kotlin</groupId>
                            <artifactId>kotlin-stdlib</artifactId>
                            <version>2.3.0</version>
                        </dependency>
                    </dependencies>
                </project>
                """,
              """
                <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <properties>
                        <maven.compiler.release>25</maven.compiler.release>
                    </properties>
                    <dependencies>
                        <dependency>
                            <groupId>org.jetbrains.kotlin</groupId>
                            <artifactId>kotlin-stdlib</artifactId>
                            <version>2.3.0</version>
                        </dependency>
                    </dependencies>
                </project>
                """
            )
          )
        );
    }

    @Test
    void addsLombokAnnotationProcessor() {
        rewriteRun(
          spec -> spec.cycles(1).expectedCyclesThatMakeChanges(1),
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
                            <version>1.18.40</version>
                        </dependency>
                    </dependencies>
                </project>
                """,
              spec -> spec.after(actual ->
                assertThat(actual)
                  .contains("<maven.compiler.release>25</maven.compiler.release>")
                  // check we have the expected annotation processor
                  .containsPattern("<annotationProcessorPaths>(.|\\n)*<path>(.|\\n)*<groupId>org.projectlombok")
                  .containsPattern("<annotationProcessorPaths>(.|\\n)*<path>(.|\\n)*<artifactId>lombok")
                  .actual()
              )
            )
          )
        );
    }
}
