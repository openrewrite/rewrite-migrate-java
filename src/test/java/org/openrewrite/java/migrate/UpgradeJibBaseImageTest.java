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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.buildGradleKts;
import static org.openrewrite.groovy.Assertions.groovy;
import static org.openrewrite.maven.Assertions.pomXml;

class UpgradeJibBaseImageTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UpgradeJibBaseImage(25));
    }

    @Nested
    class Maven {
        @DocumentExample
        @Test
        void pluginBaseImage() {
            rewriteRun(
              pomXml(
                //language=xml
                """
                  <project>
                      <groupId>org.example</groupId>
                      <artifactId>demo</artifactId>
                      <version>1.0.0</version>
                      <build>
                          <plugins>
                              <plugin>
                                  <groupId>com.google.cloud.tools</groupId>
                                  <artifactId>jib-maven-plugin</artifactId>
                                  <version>3.4.6</version>
                                  <configuration>
                                      <from>
                                          <image>eclipse-temurin:17-jre-alpine</image>
                                      </from>
                                      <to>
                                          <image>gcr.io/my-project/demo</image>
                                      </to>
                                  </configuration>
                              </plugin>
                          </plugins>
                      </build>
                  </project>
                  """,
                //language=xml
                """
                  <project>
                      <groupId>org.example</groupId>
                      <artifactId>demo</artifactId>
                      <version>1.0.0</version>
                      <build>
                          <plugins>
                              <plugin>
                                  <groupId>com.google.cloud.tools</groupId>
                                  <artifactId>jib-maven-plugin</artifactId>
                                  <version>3.4.6</version>
                                  <configuration>
                                      <from>
                                          <image>eclipse-temurin:25-jre-alpine</image>
                                      </from>
                                      <to>
                                          <image>gcr.io/my-project/demo</image>
                                      </to>
                                  </configuration>
                              </plugin>
                          </plugins>
                      </build>
                  </project>
                  """
              )
            );
        }

        @Test
        void executionAndPluginManagementBaseImages() {
            rewriteRun(
              pomXml(
                //language=xml
                """
                  <project>
                      <groupId>org.example</groupId>
                      <artifactId>demo</artifactId>
                      <version>1.0.0</version>
                      <build>
                          <pluginManagement>
                              <plugins>
                                  <plugin>
                                      <groupId>com.google.cloud.tools</groupId>
                                      <artifactId>jib-maven-plugin</artifactId>
                                      <version>3.4.6</version>
                                      <configuration>
                                          <from>
                                              <image>docker://openjdk:11-jre-slim</image>
                                          </from>
                                      </configuration>
                                  </plugin>
                              </plugins>
                          </pluginManagement>
                          <plugins>
                              <plugin>
                                  <groupId>com.google.cloud.tools</groupId>
                                  <artifactId>jib-maven-plugin</artifactId>
                                  <executions>
                                      <execution>
                                          <phase>package</phase>
                                          <goals>
                                              <goal>build</goal>
                                          </goals>
                                          <configuration>
                                              <from>
                                                  <image>amazoncorretto:21@sha256:0123456789abcdef</image>
                                              </from>
                                          </configuration>
                                      </execution>
                                  </executions>
                              </plugin>
                          </plugins>
                      </build>
                  </project>
                  """,
                //language=xml
                """
                  <project>
                      <groupId>org.example</groupId>
                      <artifactId>demo</artifactId>
                      <version>1.0.0</version>
                      <build>
                          <pluginManagement>
                              <plugins>
                                  <plugin>
                                      <groupId>com.google.cloud.tools</groupId>
                                      <artifactId>jib-maven-plugin</artifactId>
                                      <version>3.4.6</version>
                                      <configuration>
                                          <from>
                                              <image>docker://eclipse-temurin:25-jre-slim</image>
                                          </from>
                                      </configuration>
                                  </plugin>
                              </plugins>
                          </pluginManagement>
                          <plugins>
                              <plugin>
                                  <groupId>com.google.cloud.tools</groupId>
                                  <artifactId>jib-maven-plugin</artifactId>
                                  <executions>
                                      <execution>
                                          <phase>package</phase>
                                          <goals>
                                              <goal>build</goal>
                                          </goals>
                                          <configuration>
                                              <from>
                                                  <image>amazoncorretto:25</image>
                                              </from>
                                          </configuration>
                                      </execution>
                                  </executions>
                              </plugin>
                          </plugins>
                      </build>
                  </project>
                  """
              )
            );
        }

        @Test
        void propertiesHoldingBaseImage() {
            rewriteRun(
              pomXml(
                //language=xml
                """
                  <project>
                      <groupId>org.example</groupId>
                      <artifactId>demo</artifactId>
                      <version>1.0.0</version>
                      <properties>
                          <jib.from.image>eclipse-temurin:17-jre</jib.from.image>
                          <base.image>eclipse-temurin:21-jdk</base.image>
                          <unrelated.image>eclipse-temurin:17-jdk</unrelated.image>
                      </properties>
                      <build>
                          <plugins>
                              <plugin>
                                  <groupId>com.google.cloud.tools</groupId>
                                  <artifactId>jib-maven-plugin</artifactId>
                                  <version>3.4.6</version>
                                  <configuration>
                                      <from>
                                          <image>${base.image}</image>
                                      </from>
                                  </configuration>
                              </plugin>
                          </plugins>
                      </build>
                  </project>
                  """,
                //language=xml
                """
                  <project>
                      <groupId>org.example</groupId>
                      <artifactId>demo</artifactId>
                      <version>1.0.0</version>
                      <properties>
                          <jib.from.image>eclipse-temurin:25-jre</jib.from.image>
                          <base.image>eclipse-temurin:25-jdk</base.image>
                          <unrelated.image>eclipse-temurin:17-jdk</unrelated.image>
                      </properties>
                      <build>
                          <plugins>
                              <plugin>
                                  <groupId>com.google.cloud.tools</groupId>
                                  <artifactId>jib-maven-plugin</artifactId>
                                  <version>3.4.6</version>
                                  <configuration>
                                      <from>
                                          <image>${base.image}</image>
                                      </from>
                                  </configuration>
                              </plugin>
                          </plugins>
                      </build>
                  </project>
                  """
              )
            );
        }

        @Test
        void profilePropertiesKeepTheirOwnVariant() {
            rewriteRun(
              pomXml(
                //language=xml
                """
                  <project>
                      <groupId>org.example</groupId>
                      <artifactId>demo</artifactId>
                      <version>1.0.0</version>
                      <properties>
                          <jib.from.image>eclipse-temurin:17-jre</jib.from.image>
                      </properties>
                      <profiles>
                          <profile>
                              <id>alpine</id>
                              <properties>
                                  <jib.from.image>eclipse-temurin:17-jre-alpine</jib.from.image>
                              </properties>
                          </profile>
                      </profiles>
                  </project>
                  """,
                //language=xml
                """
                  <project>
                      <groupId>org.example</groupId>
                      <artifactId>demo</artifactId>
                      <version>1.0.0</version>
                      <properties>
                          <jib.from.image>eclipse-temurin:25-jre</jib.from.image>
                      </properties>
                      <profiles>
                          <profile>
                              <id>alpine</id>
                              <properties>
                                  <jib.from.image>eclipse-temurin:25-jre-alpine</jib.from.image>
                              </properties>
                          </profile>
                      </profiles>
                  </project>
                  """,
                spec -> spec.afterRecipe(pom -> assertThat(pom.getMarkers().findFirst(MavenResolutionResult.class)
                  .map(result -> result.getPom().getProperties().get("jib.from.image")))
                  .contains("eclipse-temurin:25-jre"))
              )
            );
        }

        @Test
        void otherPluginsAndNonJavaImages() {
            rewriteRun(
              pomXml(
                //language=xml
                """
                  <project>
                      <groupId>org.example</groupId>
                      <artifactId>demo</artifactId>
                      <version>1.0.0</version>
                      <build>
                          <plugins>
                              <plugin>
                                  <groupId>com.google.cloud.tools</groupId>
                                  <artifactId>jib-maven-plugin</artifactId>
                                  <version>3.4.6</version>
                                  <configuration>
                                      <from>
                                          <image>gcr.io/distroless/java17-debian12</image>
                                      </from>
                                  </configuration>
                              </plugin>
                              <plugin>
                                  <groupId>org.example</groupId>
                                  <artifactId>other-plugin</artifactId>
                                  <version>1.0</version>
                                  <configuration>
                                      <from>
                                          <image>eclipse-temurin:17-jre</image>
                                      </from>
                                  </configuration>
                              </plugin>
                          </plugins>
                      </build>
                  </project>
                  """
              )
            );
        }

        @Test
        void propertyOutsideJibConfiguration() {
            rewriteRun(
              pomXml(
                //language=xml
                """
                  <project>
                      <groupId>org.example</groupId>
                      <artifactId>demo</artifactId>
                      <version>1.0.0</version>
                      <properties>
                          <base.image>eclipse-temurin:17-jdk</base.image>
                      </properties>
                  </project>
                  """
              )
            );
        }
    }

    @Nested
    class Gradle {
        @Test
        void baseImage() {
            rewriteRun(
              buildGradle(
                """
                  plugins {
                      id 'java'
                      id 'com.google.cloud.tools.jib' version '3.4.5'
                  }

                  jib {
                      from {
                          image = 'eclipse-temurin:17-jre-alpine'
                      }
                      to {
                          image = 'us-central1-docker.pkg.dev/my-project/my-repo/my-java-app'
                      }
                  }
                  """,
                """
                  plugins {
                      id 'java'
                      id 'com.google.cloud.tools.jib' version '3.4.5'
                  }

                  jib {
                      from {
                          image = 'eclipse-temurin:25-jre-alpine'
                      }
                      to {
                          image = 'us-central1-docker.pkg.dev/my-project/my-repo/my-java-app'
                      }
                  }
                  """
              )
            );
        }

        @Test
        void dottedAndMethodBaseImages() {
            rewriteRun(
              buildGradle(
                """
                  jib.from.image = "openjdk:11-jdk"

                  subprojects {
                      jib {
                          from.image = 'eclipse-temurin:17'
                      }
                  }

                  project(':app') {
                      jib.from {
                          image 'amazoncorretto:17-al2023'
                      }
                  }
                  """,
                """
                  jib.from.image = "eclipse-temurin:25-jdk"

                  subprojects {
                      jib {
                          from.image = 'eclipse-temurin:25'
                      }
                  }

                  project(':app') {
                      jib.from {
                          image 'amazoncorretto:25-al2023'
                      }
                  }
                  """
              )
            );
        }

        @Test
        void asPartOfUpgradeJavaVersion() {
            rewriteRun(
              spec -> spec.recipe(new UpgradeJavaVersion(25)),
              buildGradle(
                """
                  jib {
                      from {
                          image = 'eclipse-temurin:21-jre'
                      }
                  }
                  """,
                """
                  jib {
                      from {
                          image = 'eclipse-temurin:25-jre'
                      }
                  }
                  """
              )
            );
        }

        @Test
        void imagesOutsideJibFrom() {
            rewriteRun(
              buildGradle(
                """
                  def image = 'eclipse-temurin:17-jre'

                  jib {
                      from {
                          image = "eclipse-temurin:${javaVersion}-jre"
                      }
                      to {
                          image = 'eclipse-temurin:17-jre'
                      }
                  }
                  """
              )
            );
        }

        @Test
        void alreadyAtTargetVersion() {
            rewriteRun(
              buildGradle(
                """
                  jib {
                      from {
                          image = 'eclipse-temurin:25-jre'
                      }
                  }
                  """
              )
            );
        }

        @Test
        void groovySourceOutsideBuildScripts() {
            rewriteRun(
              groovy(
                """
                  jib.from.image = 'eclipse-temurin:17-jre'
                  """
              )
            );
        }

        @Test
        void tarballBaseImage() {
            rewriteRun(
              buildGradle(
                """
                  jib.from.image = 'tar://build/eclipse-temurin:17.tar'
                  """
              )
            );
        }
    }

    @Nested
    class GradleKts {
        @Test
        void baseImage() {
            rewriteRun(
              buildGradleKts(
                """
                  plugins {
                      java
                      id("com.google.cloud.tools.jib") version "3.4.5"
                  }

                  jib {
                      from {
                          image = "eclipse-temurin:17-jre-alpine"
                      }
                      to {
                          image = "gcr.io/my-project/demo"
                      }
                  }
                  """,
                """
                  plugins {
                      java
                      id("com.google.cloud.tools.jib") version "3.4.5"
                  }

                  jib {
                      from {
                          image = "eclipse-temurin:25-jre-alpine"
                      }
                      to {
                          image = "gcr.io/my-project/demo"
                      }
                  }
                  """
              )
            );
        }

        @Test
        void configureJibExtension() {
            rewriteRun(
              buildGradleKts(
                """
                  import com.google.cloud.tools.jib.gradle.JibExtension

                  configure<JibExtension> {
                      from {
                          image = "eclipse-temurin:17-jre-alpine"
                      }
                  }
                  """,
                """
                  import com.google.cloud.tools.jib.gradle.JibExtension

                  configure<JibExtension> {
                      from {
                          image = "eclipse-temurin:25-jre-alpine"
                      }
                  }
                  """
              )
            );
        }

        @Test
        void dottedBaseImage() {
            rewriteRun(
              buildGradleKts(
                """
                  jib.from.image = "eclipse-temurin:17-jre"
                  """,
                """
                  jib.from.image = "eclipse-temurin:25-jre"
                  """
              )
            );
        }

        @Test
        void otherExtension() {
            rewriteRun(
              buildGradleKts(
                """
                  configure<OtherExtension> {
                      from {
                          image = "eclipse-temurin:17-jre"
                      }
                  }
                  """
              )
            );
        }
    }
}
