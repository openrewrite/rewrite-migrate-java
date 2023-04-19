/*
 * Copyright 2021 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.migrate;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.marker.JavaVersion;
import org.openrewrite.java.migrate.search.AboutJavaVersion;
import org.openrewrite.test.RewriteTest;

import java.util.UUID;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.maven.Assertions.pomXml;

class UpgradeJavaVersionTest implements RewriteTest {

    @Test
    void mavenUpgradeFromJava8ToJava17() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeJavaVersion(17)),
          //language=xml
          pomXml(
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
              """, spec -> spec.markers(new JavaVersion(UUID.randomUUID(), "", "", "1.8.0+x", ""))
          )
        );
    }

    @Test
    void gradleUpgradeFromJava11ToJava17() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeJavaVersion(17)),
          buildGradle(
            """
              java {
                toolchain {
                  languageVersion = JavaLanguageVersion.of(11)
                }
              }
              """,
            """
              java {
                toolchain {
                  languageVersion = JavaLanguageVersion.of(17)
                }
              }
              """
            ,spec -> spec.markers(new JavaVersion(UUID.randomUUID(), "", "", "11.0.15+10", "11.0.15+10"))
          )
        );
    }

    @Test
    void gradleNoChangeIfUpgradeFromJava11ToJava8() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeJavaVersion(8)),
          buildGradle(
            """
              java {
                toolchain {
                  languageVersion = JavaLanguageVersion.of(11)
                }
              }
              """,spec -> spec.markers(new JavaVersion(UUID.randomUUID(), "", "", "11.0.15+10", "11.0.15+10"))
          )
        );
    }

    @Test
    void upgradeJavaVersionTo17From11() {
        rewriteRun(
          spec -> spec.recipe(new UpgradeJavaVersion(17).doNext(new AboutJavaVersion(null))),
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
          spec -> spec.recipe(new UpgradeJavaVersion(11).doNext(new AboutJavaVersion(null))),
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
}
