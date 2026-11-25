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
package org.openrewrite.java.migrate.snakeyaml;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.java.Assertions.srcMainJava;
import static org.openrewrite.maven.Assertions.pomXml;
import static org.openrewrite.properties.Assertions.properties;
import static org.openrewrite.toml.Assertions.toml;

class UpgradeSnakeYaml2Test implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.migrate.snakeyaml.UpgradeSnakeYaml_2")
          .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "snakeyaml-1.33"));
    }

    @DocumentExample
    @Test
    void mavenProjectWithSnakeYaml1() {
        rewriteRun(
          mavenProject("app",
            //language=xml
            pomXml(
              """
                <project>
                    <groupId>com.example</groupId>
                    <artifactId>app</artifactId>
                    <version>1.0.0</version>
                    <dependencies>
                        <dependency>
                            <groupId>org.yaml</groupId>
                            <artifactId>snakeyaml</artifactId>
                            <version>1.33</version>
                        </dependency>
                    </dependencies>
                </project>
                """,
              spec -> spec.after(pom -> {
                  assertThat(pom).containsPattern("<version>2\\.\\d+(\\.\\d+)?</version>");
                  return pom;
              })
            ),
            srcMainJava(
              //language=java
              java(
                """
                  import org.yaml.snakeyaml.Yaml;
                  import org.yaml.snakeyaml.constructor.SafeConstructor;

                  class Config {
                      static Object load(String yaml) {
                          return new Yaml(new SafeConstructor()).load(yaml);
                      }
                  }
                  """,
                """
                  import org.yaml.snakeyaml.LoaderOptions;
                  import org.yaml.snakeyaml.Yaml;
                  import org.yaml.snakeyaml.constructor.SafeConstructor;

                  class Config {
                      static Object load(String yaml) {
                          return new Yaml(new SafeConstructor(new LoaderOptions())).load(yaml);
                      }
                  }
                  """
              )
            )
          )
        );
    }

    @Test
    void oldSnakeYamlDeclaredInAParentProperty() {
        // SnakeYAML 1.20 has no Constructor that takes LoaderOptions, so the call needs the raised version
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "snakeyaml-1.20")),
          mavenProject("parent",
            //language=xml
            pomXml(
              """
                <project>
                    <groupId>com.example</groupId>
                    <artifactId>parent</artifactId>
                    <version>1.0.0</version>
                    <packaging>pom</packaging>
                    <modules>
                        <module>app</module>
                    </modules>
                    <properties>
                        <snakeyaml.version>1.20</snakeyaml.version>
                    </properties>
                    <dependencyManagement>
                        <dependencies>
                            <dependency>
                                <groupId>org.yaml</groupId>
                                <artifactId>snakeyaml</artifactId>
                                <version>${snakeyaml.version}</version>
                            </dependency>
                        </dependencies>
                    </dependencyManagement>
                </project>
                """,
              spec -> spec.after(pom -> {
                  assertThat(pom).containsPattern("<snakeyaml.version>2\\.\\d+(\\.\\d+)?</snakeyaml.version>");
                  return pom;
              })
            ),
            mavenProject("app",
              //language=xml
              pomXml(
                """
                  <project>
                      <parent>
                          <groupId>com.example</groupId>
                          <artifactId>parent</artifactId>
                          <version>1.0.0</version>
                      </parent>
                      <artifactId>app</artifactId>
                      <dependencies>
                          <dependency>
                              <groupId>org.yaml</groupId>
                              <artifactId>snakeyaml</artifactId>
                          </dependency>
                      </dependencies>
                  </project>
                  """
              ),
              srcMainJava(
                //language=java
                java(
                  """
                    import org.yaml.snakeyaml.Yaml;
                    import org.yaml.snakeyaml.constructor.Constructor;

                    class A {
                        Yaml yaml() {
                            return new Yaml(new Constructor(A.class));
                        }
                    }
                    """,
                  """
                    import org.yaml.snakeyaml.LoaderOptions;
                    import org.yaml.snakeyaml.Yaml;
                    import org.yaml.snakeyaml.constructor.Constructor;

                    class A {
                        Yaml yaml() {
                            return new Yaml(new Constructor(A.class, new LoaderOptions()));
                        }
                    }
                    """
                )
              )
            )
          )
        );
    }

    @Test
    void oldSnakeYamlInAGradleBuild() {
        // The call changes in the cycle after the version is raised, once the Gradle build has SnakeYAML 2
        rewriteRun(
          spec -> spec
            .beforeRecipe(withToolingApi())
            .parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "snakeyaml-1.20"))
            .expectedCyclesThatMakeChanges(2),
          //language=groovy
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }
              repositories {
                  mavenCentral()
              }
              dependencies {
                  implementation 'org.yaml:snakeyaml:1.20'
              }
              """,
            spec -> spec.after(gradle -> {
                assertThat(gradle).containsPattern("org\\.yaml:snakeyaml:2\\.\\d+(\\.\\d+)?'");
                return gradle;
            })
          ),
          srcMainJava(
            //language=java
            java(
              """
                import org.yaml.snakeyaml.Yaml;
                import org.yaml.snakeyaml.representer.Representer;

                class A {
                    Yaml yaml() {
                        return new Yaml(new Representer());
                    }
                }
                """,
              """
                import org.yaml.snakeyaml.DumperOptions;
                import org.yaml.snakeyaml.Yaml;
                import org.yaml.snakeyaml.representer.Representer;

                class A {
                    Yaml yaml() {
                        return new Yaml(new Representer(new DumperOptions()));
                    }
                }
                """
            )
          )
        );
    }

    @Test
    void versionInAProperty() {
        rewriteRun(
          //language=xml
          pomXml(
            """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>app</artifactId>
                  <version>1.0.0</version>
                  <properties>
                      <snakeyaml.version>1.20</snakeyaml.version>
                  </properties>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>org.yaml</groupId>
                              <artifactId>snakeyaml</artifactId>
                              <version>${snakeyaml.version}</version>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
              </project>
              """,
            spec -> spec.after(pom -> {
                assertThat(pom)
                  .containsPattern("<snakeyaml.version>2\\.\\d+(\\.\\d+)?</snakeyaml.version>")
                  .contains("<version>${snakeyaml.version}</version>");
                return pom;
            })
          )
        );
    }

    @Test
    void gradleDependency() {
        rewriteRun(
          spec -> spec.beforeRecipe(withToolingApi()),
          //language=groovy
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }
              repositories {
                  mavenCentral()
              }
              dependencies {
                  implementation 'org.yaml:snakeyaml:1.33'
              }
              """,
            spec -> spec.after(gradle -> {
                assertThat(gradle).containsPattern("org\\.yaml:snakeyaml:2\\.\\d+(\\.\\d+)?'");
                return gradle;
            })
          )
        );
    }

    @Test
    void mavenPluginPluginThatCannotReadSnakeYaml2() {
        rewriteRun(
          mavenProject("example-maven-plugin",
            //language=xml
            pomXml(
              """
                <project>
                    <groupId>com.example</groupId>
                    <artifactId>example-maven-plugin</artifactId>
                    <version>1.0.0</version>
                    <packaging>maven-plugin</packaging>
                    <dependencies>
                        <dependency>
                            <groupId>org.yaml</groupId>
                            <artifactId>snakeyaml</artifactId>
                            <version>1.16</version>
                        </dependency>
                    </dependencies>
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-plugin-plugin</artifactId>
                                <version>3.4</version>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """,
              spec -> spec.after(pom -> {
                  assertThat(pom)
                    .containsPattern("<version>2\\.\\d+(\\.\\d+)?</version>")
                    .containsPattern("<version>3\\.5\\.\\d+</version>");
                  return pom;
              })
            )
          )
        );
    }

    @Test
    void mavenPluginThatGetsSnakeYamlFromAnotherModule() {
        rewriteRun(
          //language=xml
          pomXml(
            """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>root</artifactId>
                  <version>1.0.0</version>
                  <packaging>pom</packaging>
                  <modules>
                      <module>core</module>
                      <module>plugin</module>
                  </modules>
              </project>
              """
          ),
          mavenProject("core",
            //language=xml
            pomXml(
              """
                <project>
                    <parent>
                        <groupId>com.example</groupId>
                        <artifactId>root</artifactId>
                        <version>1.0.0</version>
                    </parent>
                    <artifactId>core</artifactId>
                    <dependencies>
                        <dependency>
                            <groupId>org.yaml</groupId>
                            <artifactId>snakeyaml</artifactId>
                            <version>1.16</version>
                        </dependency>
                    </dependencies>
                </project>
                """,
              spec -> spec.after(pom -> {
                  assertThat(pom).containsPattern("<version>2\\.\\d+(\\.\\d+)?</version>");
                  return pom;
              })
            )
          ),
          mavenProject("plugin",
            //language=xml
            pomXml(
              """
                <project>
                    <parent>
                        <groupId>com.example</groupId>
                        <artifactId>root</artifactId>
                        <version>1.0.0</version>
                    </parent>
                    <artifactId>plugin</artifactId>
                    <packaging>maven-plugin</packaging>
                    <dependencies>
                        <dependency>
                            <groupId>com.example</groupId>
                            <artifactId>core</artifactId>
                            <version>1.0.0</version>
                        </dependency>
                    </dependencies>
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-plugin-plugin</artifactId>
                                <version>3.4</version>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """,
              spec -> spec.after(pom -> {
                  assertThat(pom).containsPattern("<version>3\\.5\\.\\d+</version>");
                  return pom;
              })
            )
          )
        );
    }

    @Test
    void mavenPluginPluginWithoutSnakeYamlIsLeftAlone() {
        rewriteRun(
          mavenProject("example-maven-plugin",
            //language=xml
            pomXml(
              """
                <project>
                    <groupId>com.example</groupId>
                    <artifactId>example-maven-plugin</artifactId>
                    <version>1.0.0</version>
                    <packaging>maven-plugin</packaging>
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-plugin-plugin</artifactId>
                                <version>3.4</version>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """
            )
          )
        );
    }

    @Test
    void versionInAGradleVersionCatalog() {
        // CustomClassLoaderConstructor only takes LoaderOptions from SnakeYAML 2.0
        rewriteRun(
          spec -> spec.beforeRecipe(withToolingApi()),
          //language=toml
          toml(
            """
              [versions]
              snakeyaml = "1.33"

              [libraries]
              snakeyaml = { module = "org.yaml:snakeyaml", version.ref = "snakeyaml" }
              """,
            spec -> spec.path("gradle/libs.versions.toml").after(catalog -> {
                assertThat(catalog).containsPattern("snakeyaml = \"2\\.\\d+(\\.\\d+)?\"");
                return catalog;
            })
          ),
          //language=groovy
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }
              repositories {
                  mavenCentral()
              }
              dependencies {
                  implementation libs.snakeyaml
              }
              """
          ),
          srcMainJava(
            //language=java
            java(
              """
                import org.yaml.snakeyaml.Yaml;
                import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor;

                class A {
                    Yaml yaml(ClassLoader loader) {
                        return new Yaml(new CustomClassLoaderConstructor(loader));
                    }
                }
                """,
              """
                import org.yaml.snakeyaml.LoaderOptions;
                import org.yaml.snakeyaml.Yaml;
                import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor;

                class A {
                    Yaml yaml(ClassLoader loader) {
                        return new Yaml(new CustomClassLoaderConstructor(loader, new LoaderOptions()));
                    }
                }
                """
            )
          )
        );
    }

    @Test
    void versionInGradleProperties() {
        // CustomClassLoaderConstructor only takes LoaderOptions from SnakeYAML 2.0
        rewriteRun(
          spec -> spec.beforeRecipe(withToolingApi()),
          properties(
            """
              snakeyamlVersion=1.33
              """,
            spec -> spec.path("gradle.properties").after(props -> {
                assertThat(props).containsPattern("snakeyamlVersion=2\\.\\d+(\\.\\d+)?");
                return props;
            })
          ),
          //language=groovy
          buildGradle(
            """
              plugins {
                  id 'java-library'
              }
              repositories {
                  mavenCentral()
              }
              dependencies {
                  implementation "org.yaml:snakeyaml:${snakeyamlVersion}"
              }
              """
          ),
          srcMainJava(
            //language=java
            java(
              """
                import org.yaml.snakeyaml.Yaml;
                import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor;

                class A {
                    Yaml yaml(ClassLoader loader) {
                        return new Yaml(new CustomClassLoaderConstructor(loader));
                    }
                }
                """,
              """
                import org.yaml.snakeyaml.LoaderOptions;
                import org.yaml.snakeyaml.Yaml;
                import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor;

                class A {
                    Yaml yaml(ClassLoader loader) {
                        return new Yaml(new CustomClassLoaderConstructor(loader, new LoaderOptions()));
                    }
                }
                """
            )
          )
        );
    }

    @Test
    void versionManagedBySpringBootIsLeftAlone() {
        rewriteRun(
          //language=xml
          pomXml(
            """
              <project>
                  <parent>
                      <groupId>org.springframework.boot</groupId>
                      <artifactId>spring-boot-starter-parent</artifactId>
                      <version>2.7.18</version>
                  </parent>
                  <groupId>com.example</groupId>
                  <artifactId>app</artifactId>
                  <version>1.0.0</version>
                  <dependencies>
                      <dependency>
                          <groupId>org.yaml</groupId>
                          <artifactId>snakeyaml</artifactId>
                      </dependency>
                  </dependencies>
              </project>
              """
          )
        );
    }
}
