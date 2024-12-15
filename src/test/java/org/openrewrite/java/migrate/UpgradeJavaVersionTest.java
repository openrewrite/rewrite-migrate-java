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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.config.CompositeRecipe;
import org.openrewrite.java.marker.JavaVersion;
import org.openrewrite.java.migrate.search.AboutJavaVersion;
import org.openrewrite.test.RewriteTest;

import java.util.List;
import java.util.UUID;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.version;
import static org.openrewrite.maven.Assertions.pomXml;

class UpgradeJavaVersionTest implements RewriteTest {
    @Nested
    class Maven {
        @DocumentExample
        @Test
        void mavenUpgradeFromJava8ToJava17ViaProperties() {
            rewriteRun(
              spec -> spec.recipe(new UpgradeJavaVersion(17)),
              pomXml(
                //language=xml
                """
                  <project>
                    <modelVersion>4.0.0</modelVersion>

                    <properties>
                      <java.version>1.8</java.version>
                      <maven.compiler.source>1.8</maven.compiler.source>
                      <maven.compiler.target>1.8</maven.compiler.target>
                    </properties>

                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                  </project>
                  """,
                //language=xml
                """
                  <project>
                    <modelVersion>4.0.0</modelVersion>

                    <properties>
                      <java.version>17</java.version>
                      <maven.compiler.source>17</maven.compiler.source>
                      <maven.compiler.target>17</maven.compiler.target>
                    </properties>

                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                  </project>
                  """,
                spec -> spec.markers(new JavaVersion(UUID.randomUUID(), "", "", "11.0.15+10", "11.0.15+10"))
              )
            );
        }

        @Test
        void mavenUpgradeFromJava8ToJava17ViaConfiguration() {
            rewriteRun(
              spec -> spec.recipe(new UpgradeJavaVersion(17)),
              //language=xml
              pomXml(
                //language=xml
                """
                  <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <build>
                      <plugins>
                        <plugin>
                          <groupId>org.apache.maven.plugins</groupId>
                          <artifactId>maven-compiler-plugin</artifactId>
                          <version>3.8.0</version>
                          <configuration>
                            <source>1.8</source>
                            <target>1.8</target>
                          </configuration>
                        </plugin>
                      </plugins>
                    </build>
                  </project>
                  """,
                //language=xml
                """
                  <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <build>
                      <plugins>
                        <plugin>
                          <groupId>org.apache.maven.plugins</groupId>
                          <artifactId>maven-compiler-plugin</artifactId>
                          <version>3.8.0</version>
                          <configuration>
                            <release>17</release>
                          </configuration>
                        </plugin>
                      </plugins>
                    </build>
                  </project>
                  """,
                spec -> spec.markers(new JavaVersion(UUID.randomUUID(), "", "", "11.0.15+10", "11.0.15+10"))
              )
            );
        }
    }

    @SuppressWarnings("GroovyAssignabilityCheck")
    @Nested
    class Gradle {
        @Test
        void gradleUpgradeFromJava11ToJava17() {
            rewriteRun(
              spec -> spec.recipe(new UpgradeJavaVersion(17)),
              buildGradle(
                //language=groovy
                """
                  java {
                    toolchain {
                      languageVersion = JavaLanguageVersion.of(11)
                    }
                  }
                  """,
                //language=groovy
                """
                  java {
                    toolchain {
                      languageVersion = JavaLanguageVersion.of(17)
                    }
                  }
                  """,
                spec -> spec.markers(new JavaVersion(UUID.randomUUID(), "", "", "11.0.15+10", "11.0.15+10"))
              )
            );
        }

        @Test
        void gradleSourceTargetFromJava11ToJava17() {
            rewriteRun(
              spec -> spec.recipe(new UpgradeJavaVersion(17)),
              buildGradle(
                //language=groovy
                """
                  java {
                    sourceCompatibility = 11
                    targetCompatibility = 11
                  }
                  """,
                //language=groovy
                """
                  java {
                    sourceCompatibility = 17
                    targetCompatibility = 17
                  }
                  """,
                spec -> spec.markers(new JavaVersion(UUID.randomUUID(), "", "", "11.0.15+10", "11.0.15+10"))
              )
            );
        }

        @Test
        void gradleSourceTargetFromJava11ToJava21ThroughEnum() {
            rewriteRun(
              spec -> spec.recipe(new UpgradeJavaVersion(21)),
              buildGradle(
                //language=groovy
                """
                  java {
                    sourceCompatibility = JavaVersion.VERSION_11
                    targetCompatibility = JavaVersion.VERSION_11
                  }
                  """,
                //language=groovy
                """
                  java {
                    sourceCompatibility = JavaVersion.VERSION_21
                    targetCompatibility = JavaVersion.VERSION_21
                  }
                  """,
                spec -> spec.markers(new JavaVersion(UUID.randomUUID(), "", "", "11.0.15+10", "11.0.15+10"))
              )
            );
        }

        @Test
        void gradleNoChangeIfUpgradeFromJava11ToJava8() {
            rewriteRun(
              spec -> spec.recipe(new UpgradeJavaVersion(8)),
              buildGradle(
                //language=groovy
                """
                  java {
                    toolchain {
                      languageVersion = JavaLanguageVersion.of(11)
                    }
                  }
                  """,
                spec -> spec.markers(new JavaVersion(UUID.randomUUID(), "", "", "11.0.15+10", "11.0.15+10"))
              )
            );
        }
    }

    @Nested
    class Markers {
        @Test
        void upgradeJavaVersionTo17From11() {
            rewriteRun(
              spec -> spec.recipe(new CompositeRecipe(List.of(new UpgradeJavaVersion(17), new AboutJavaVersion(null)))),
              java(
                //language=java
                """
                  class Test {
                  }
                  """,
                //language=java
                """
                  /*~~(Java version: 17)~~>*/class Test {
                  }
                  """,
                spec -> spec.markers(new JavaVersion(UUID.randomUUID(), "", "", "11.0.15+10", "11.0.15+10"))
              )
            );
        }

        @Test
        void upgradeJavaVersionTo11From8() {
            rewriteRun(
              spec -> spec.recipe(new CompositeRecipe(List.of(new UpgradeJavaVersion(11), new AboutJavaVersion(null)))),
              java(
                //language=java
                """
                  class Test {
                  }
                  """,
                //language=java
                """
                  /*~~(Java version: 11)~~>*/class Test {
                  }
                  """,
                spec -> spec.markers(new JavaVersion(UUID.randomUUID(), "", "", "1.8.0+x", ""))
              )
            );
        }

        @Test
        void upgradeAllThatNeedUpgrading() {
            rewriteRun(
              spec -> spec.recipes(new UpgradeJavaVersion(11), new AboutJavaVersion(null)),
              version(
                java(
                  //language=java
                  """
                    class Test {
                    }
                    """,
                  //language=java
                  """
                    /*~~(Java version: 11)~~>*/class Test {
                    }
                    """
                ),
                8
              ),
              version(
                //language=java
                java(
                  """
                    class Test2 {
                    }
                    """,
                  """
                    /*~~(Java version: 11)~~>*/class Test2 {
                    }
                    """
                ),
                11
              ),
              version(
                java(
                  //language=java
                  """
                    class Test3 {
                    }
                    """,
                  //language=java
                  """
                    /*~~(Java version: 17)~~>*/class Test3 {
                    }
                    """
                ),
                17
              )
            );
        }

        @Test
        void upgradeAllThatNeedUpgradingNewestFirst() {
            rewriteRun(
              spec -> spec.recipes(new UpgradeJavaVersion(11), new AboutJavaVersion(null)),
              version(
                java(
                  //language=java
                  """
                    class Test {
                    }
                    """,
                  //language=java
                  """
                    /*~~(Java version: 17)~~>*/class Test {
                    }
                    """
                ),
                17
              ),
              version(
                java(
                  //language=java
                  """
                    class Test2 {
                    }
                    """,
                  """
                    /*~~(Java version: 11)~~>*/class Test2 {
                    }
                    """
                ),
                11
              ),
              version(
                java(
                  //language=java
                  """
                    class Test3 {
                    }
                    """,
                  //language=java
                  """
                    /*~~(Java version: 11)~~>*/class Test3 {
                    }
                    """
                ),
                8
              )
            );
        }
    }
}
