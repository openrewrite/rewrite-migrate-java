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
                    <dependencies>
                        <dependency>
                            <groupId>org.mockito</groupId>
                            <artifactId>mockito-core</artifactId>
                            <version>5.14.0</version>
                            <scope>test</scope>
                        </dependency>
                    </dependencies>
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
                            <plugin>
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-pmd-plugin</artifactId>
                                <version>3.21.0</version>
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
                  .containsPattern("maven-pmd-plugin</artifactId>\\s*<version>3\\.28\\.")
                  .contains("<argLine>@{argLine} -javaagent:${org.mockito:mockito-core:jar}</argLine>")
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
                  return assertThat(actual).containsPattern("gradle-9\\.1\\.\\d+-bin\\.zip").actual();
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
    void kotlin1xCapsJavaVersionAt24WithComment() {
        // Kotlin 1.x is left untouched: crossing the K2 compiler default introduced in Kotlin 2.0 is source-breaking,
        // so the module stays on Kotlin 1.x and is capped at Java 24 (with an explanatory comment) rather than upgraded.
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
                            <version>1.9.24</version>
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
                        <!-- Capped at Java 24: this module compiles Kotlin and depends on kotlin-stdlib 1.9.24, and Kotlin before 2.3 cannot target Java 25 bytecode. Upgrade Kotlin (kotlin-stdlib and the Kotlin compiler) to 2.3 or later, then re-run "Migrate to Java 25" to move this module to Java 25. -->
                        <maven.compiler.release>24</maven.compiler.release>
                    </properties>
                    <dependencies>
                        <dependency>
                            <groupId>org.jetbrains.kotlin</groupId>
                            <artifactId>kotlin-stdlib</artifactId>
                            <version>1.9.24</version>
                        </dependency>
                    </dependencies>
                </project>
                """
            ),
            other("fun main() {}", spec -> spec.path("src/main/kotlin/App.kt"))
          )
        );
    }

    @Test
    void kotlin2xUpgradedToLatest2_3AndJava25() {
        // Kotlin 2.0-2.2 is already on the K2 compiler, so bumping to the latest 2.3 is safe and unblocks Java 25.
        // The Kotlin bump happens in the first cycle; the Java 25 upgrade then applies once the module is on 2.3.
        rewriteRun(
          spec -> spec.expectedCyclesThatMakeChanges(2),
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
              spec -> spec.after(actual ->
                assertThat(actual)
                  .contains("<maven.compiler.release>25</maven.compiler.release>")
                  .containsPattern("kotlin-stdlib</artifactId>\\s*<version>2\\.3\\.")
                  // The module reaches Java 25, so no "capped at Java 24" comment should be left behind.
                  .doesNotContain("Capped at Java 24")
                  .actual())
            ),
            other("fun main() {}", spec -> spec.path("src/main/kotlin/App.kt"))
          )
        );
    }

    @Test
    void kotlin2xIsFlooredAtJava24AsSafetyNet() {
        // Running only the Kotlin step: it bumps Kotlin to 2.3 and floors the module at Java 24. Were the Kotlin bump
        // to fail (e.g. the version is managed by a parent or BOM), this floor still lands the module on Java 24 rather
        // than leaving it behind. The full UpgradeToJava25 then raises it to Java 25 once Kotlin actually reaches 2.3.
        rewriteRun(
          spec -> spec.recipeFromResources("org.openrewrite.java.migrate.UpgradeKotlinForJava25"),
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
              spec -> spec.after(actual ->
                assertThat(actual)
                  .contains("<maven.compiler.release>24</maven.compiler.release>")
                  .containsPattern("kotlin-stdlib</artifactId>\\s*<version>2\\.3\\.")
                  .doesNotContain("Capped at Java 24")
                  .actual())
            ),
            other("fun main() {}", spec -> spec.path("src/main/kotlin/App.kt"))
          )
        );
    }

    @Test
    void commentExplainsKotlin2xModuleLeftAtJava24() {
        // Running only the comment step on a Kotlin module that ended at Java 24 (e.g. a 2.0-2.2 module whose Kotlin
        // upgrade could not be applied): it is explained just like a Kotlin 1.x cap, naming the kotlin-stdlib found.
        rewriteRun(
          spec -> spec.recipeFromResources("org.openrewrite.java.migrate.CommentKotlinModulesCappedAtJava24"),
          mavenProject("project",
            pomXml(
              //language=xml
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
                """,
              """
                <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <properties>
                        <!-- Capped at Java 24: this module compiles Kotlin and depends on kotlin-stdlib 2.2.0, and Kotlin before 2.3 cannot target Java 25 bytecode. Upgrade Kotlin (kotlin-stdlib and the Kotlin compiler) to 2.3 or later, then re-run "Migrate to Java 25" to move this module to Java 25. -->
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
            ),
            other("fun main() {}", spec -> spec.path("src/main/kotlin/App.kt"))
          )
        );
    }

    @Test
    void commentIsRemovedOnceKotlinModuleReachesJava25() {
        // Self-healing: a module that earlier carried the cap comment but has since reached Java 25 (its Kotlin was
        // upgraded) has the now-stale comment removed, even though the named kotlin-stdlib version has changed.
        rewriteRun(
          spec -> spec.recipeFromResources("org.openrewrite.java.migrate.CommentKotlinModulesCappedAtJava24"),
          mavenProject("project",
            pomXml(
              //language=xml
              """
                <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <properties>
                        <!-- Capped at Java 24: this module compiles Kotlin and depends on kotlin-stdlib 2.2.0, and Kotlin before 2.3 cannot target Java 25 bytecode. Upgrade Kotlin (kotlin-stdlib and the Kotlin compiler) to 2.3 or later, then re-run "Migrate to Java 25" to move this module to Java 25. -->
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
            ),
            other("fun main() {}", spec -> spec.path("src/main/kotlin/App.kt"))
          )
        );
    }

    @Test
    void transitiveKotlinStdlibDoesNotCapJavaVersion() {
        // https://github.com/moderneinc/customer-requests/issues/2236
        // OkHttp brings kotlin-stdlib in transitively; without Kotlin sources the project should still upgrade to Java 25.
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
                        <maven.compiler.release>21</maven.compiler.release>
                    </properties>
                    <dependencies>
                        <dependency>
                            <groupId>com.squareup.okhttp3</groupId>
                            <artifactId>okhttp</artifactId>
                            <version>4.12.0</version>
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
                            <groupId>com.squareup.okhttp3</groupId>
                            <artifactId>okhttp</artifactId>
                            <version>4.12.0</version>
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
