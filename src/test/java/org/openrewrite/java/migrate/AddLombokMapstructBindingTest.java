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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;
import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.maven.Assertions.pomXml;

class AddLombokMapstructBindingTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.migrate.AddLombokMapstructBinding");
    }

    @DocumentExample
    @Test
    void addForGradle() {
        rewriteRun(
          spec -> spec.beforeRecipe(withToolingApi()),
          mavenProject("project",
            //language=groovy
            buildGradle(
              """
                plugins { id 'java' }
                repositories { mavenCentral() }
                dependencies {
                    implementation "org.projectlombok:lombok:1.18.42"
                    implementation "org.mapstruct:mapstruct:1.6.3"
                }
                """,
              """
                plugins { id 'java' }
                repositories { mavenCentral() }
                dependencies {
                    annotationProcessor "org.projectlombok:lombok-mapstruct-binding:0.2.0"

                    implementation "org.projectlombok:lombok:1.18.42"
                    implementation "org.mapstruct:mapstruct:1.6.3"
                }
                """
            )
          )
        );
    }

    @Test
    void doesNotDuplicateGradle() {
        rewriteRun(
          spec -> spec.beforeRecipe(withToolingApi()),
          mavenProject("project",
            //language=groovy
            buildGradle(
              """
                plugins { id 'java' }
                repositories { mavenCentral() }
                dependencies {
                    annotationProcessor "org.projectlombok:lombok-mapstruct-binding:0.2.0"

                    implementation "org.projectlombok:lombok:1.18.42"
                    implementation "org.mapstruct:mapstruct:1.6.3"
                }
                """
            )
          )
        );
    }

    @Test
    void addForMaven() {
        rewriteRun(
          mavenProject("project",
            //language=xml
            pomXml(
              """
                <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <dependencies>
                        <dependency>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>1.18.42</version>
                        </dependency>
                        <dependency>
                            <groupId>org.mapstruct</groupId>
                            <artifactId>mapstruct</artifactId>
                            <version>1.6.3</version>
                        </dependency>
                    </dependencies>
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-compiler-plugin</artifactId>
                                <configuration>
                                    <annotationProcessorPaths>
                                        <path>
                                            <groupId>org.projectlombok</groupId>
                                            <artifactId>lombok</artifactId>
                                        </path>
                                    </annotationProcessorPaths>
                                </configuration>
                                <version>3.14.1</version>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """,
              """
                <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <dependencies>
                        <dependency>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>1.18.42</version>
                        </dependency>
                        <dependency>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok-mapstruct-binding</artifactId>
                            <version>0.2.0</version>
                        </dependency>
                        <dependency>
                            <groupId>org.mapstruct</groupId>
                            <artifactId>mapstruct</artifactId>
                            <version>1.6.3</version>
                        </dependency>
                    </dependencies>
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-compiler-plugin</artifactId>
                                <configuration>
                                    <annotationProcessorPaths>
                                        <path>
                                            <groupId>org.projectlombok</groupId>
                                            <artifactId>lombok</artifactId>
                                        </path>
                                        <path>
                                            <groupId>org.projectlombok</groupId>
                                            <artifactId>lombok-mapstruct-binding</artifactId>
                                            <version>0.2.0</version>
                                        </path>
                                    </annotationProcessorPaths>
                                </configuration>
                                <version>3.14.1</version>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """
            ),
            mavenProject("anotherProject",
              //language=xml
              pomXml(
                """
                  <project>
                      <groupId>org.someother</groupId>
                      <artifactId>anotherproject</artifactId>
                      <version>1.0.0</version>
                      <properties>
                          <maven.compiler.release>17</maven.compiler.release>
                      </properties>
                      <dependencies>
                      </dependencies>
                  </project>
                  """
              )
            )
          )
        );
    }

    @Test
    void doesNotDuplicateMaven() {
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
                        <lombok.mapstruct.binding.version>0.2.0</lombok.mapstruct.binding.version>
                    </properties>
                    <dependencies>
                        <dependency>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>1.18.42</version>
                            <scope>provided</scope>
                        </dependency>
                        <dependency>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok-mapstruct-binding</artifactId>
                            <version>${lombok.mapstruct.binding.version}</version>
                            <scope>provided</scope>
                        </dependency>
                        <dependency>
                            <groupId>org.mapstruct</groupId>
                            <artifactId>mapstruct</artifactId>
                            <version>1.6.3</version>
                        </dependency>
                    </dependencies>
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-compiler-plugin</artifactId>
                                <configuration>
                                    <annotationProcessorPaths>
                                        <path>
                                            <groupId>org.projectlombok</groupId>
                                            <artifactId>lombok</artifactId>
                                        </path>
                                        <path>
                                            <groupId>org.projectlombok</groupId>
                                            <artifactId>lombok-mapstruct-binding</artifactId>
                                            <version>0.2.0</version>
                                        </path>
                                    </annotationProcessorPaths>
                                </configuration>
                                <version>3.14.1</version>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """
            )
          )
        );
    }
}
