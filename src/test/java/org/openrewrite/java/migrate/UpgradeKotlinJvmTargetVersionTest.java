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
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.buildGradleKts;
import static org.openrewrite.maven.Assertions.pomXml;

class UpgradeKotlinJvmTargetVersionTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UpgradeKotlinJvmTargetVersion(21));
    }

    @DocumentExample
    @Issue("https://github.com/moderneinc/customer-requests/issues/2439")
    @Test
    void upgradesKotlinMavenPluginJvmTarget() {
        rewriteRun(
          pomXml(
            //language=xml
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>org.example</groupId>
                  <artifactId>demo</artifactId>
                  <version>1.0.0</version>
                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.jetbrains.kotlin</groupId>
                              <artifactId>kotlin-maven-plugin</artifactId>
                              <version>1.9.24</version>
                              <configuration>
                                  <jvmTarget>11</jvmTarget>
                                  <args>
                                      <arg>-Xjsr305=strict</arg>
                                  </args>
                              </configuration>
                          </plugin>
                      </plugins>
                  </build>
              </project>
              """,
            //language=xml
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>org.example</groupId>
                  <artifactId>demo</artifactId>
                  <version>1.0.0</version>
                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.jetbrains.kotlin</groupId>
                              <artifactId>kotlin-maven-plugin</artifactId>
                              <version>1.9.24</version>
                              <configuration>
                                  <jvmTarget>21</jvmTarget>
                                  <args>
                                      <arg>-Xjsr305=strict</arg>
                                  </args>
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
    void upgradesGradleGroovyKotlinOptionsJvmTarget() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'org.jetbrains.kotlin.jvm' version '1.9.24'
              }
              tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile).configureEach {
                  kotlinOptions {
                      jvmTarget = '11'
                  }
              }
              """,
            """
              plugins {
                  id 'org.jetbrains.kotlin.jvm' version '1.9.24'
              }
              tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile).configureEach {
                  kotlinOptions {
                      jvmTarget = '21'
                  }
              }
              """
          )
        );
    }

    @Test
    void preservesGroovyDoubleQuoteStyle() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'org.jetbrains.kotlin.jvm' version '1.9.24'
              }
              tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile).configureEach {
                  kotlinOptions {
                      jvmTarget = "11"
                  }
              }
              """,
            """
              plugins {
                  id 'org.jetbrains.kotlin.jvm' version '1.9.24'
              }
              tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile).configureEach {
                  kotlinOptions {
                      jvmTarget = "21"
                  }
              }
              """
          )
        );
    }

    @Test
    void upgradesGradleKotlinDslCompilerOptionsJvmTarget() {
        rewriteRun(
          buildGradleKts(
            """
              import org.jetbrains.kotlin.gradle.dsl.JvmTarget

              plugins {
                  kotlin("jvm") version "1.9.24"
              }
              kotlin {
                  compilerOptions {
                      jvmTarget = JvmTarget.JVM_11
                  }
              }
              """,
            """
              import org.jetbrains.kotlin.gradle.dsl.JvmTarget

              plugins {
                  kotlin("jvm") version "1.9.24"
              }
              kotlin {
                  compilerOptions {
                      jvmTarget = JvmTarget.JVM_21
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotDowngradeGradleGroovy() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'org.jetbrains.kotlin.jvm' version '2.0.0'
              }
              tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile).configureEach {
                  kotlinOptions {
                      jvmTarget = '25'
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotChangeGradleGroovyJvmTargetOutsideKotlinBlock() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'org.jetbrains.kotlin.jvm' version '1.9.24'
              }
              ext {
                  jvmTarget = '11'
              }
              """
          )
        );
    }

    @Test
    void doNotDowngradeGradleKotlinDsl() {
        rewriteRun(
          buildGradleKts(
            """
              import org.jetbrains.kotlin.gradle.dsl.JvmTarget

              plugins {
                  kotlin("jvm") version "2.0.0"
              }
              kotlin {
                  compilerOptions {
                      jvmTarget = JvmTarget.JVM_25
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotChangeGradleKotlinDslJvmTargetOutsideKotlinBlock() {
        rewriteRun(
          buildGradleKts(
            """
              plugins {
                  kotlin("jvm") version "1.9.24"
              }
              val jvmTarget = "11"
              """
          )
        );
    }

    @Test
    void doNotDowngradeMaven() {
        rewriteRun(
          pomXml(
            //language=xml
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>org.example</groupId>
                  <artifactId>demo</artifactId>
                  <version>1.0.0</version>
                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.jetbrains.kotlin</groupId>
                              <artifactId>kotlin-maven-plugin</artifactId>
                              <version>2.0.0</version>
                              <configuration>
                                  <jvmTarget>25</jvmTarget>
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
    void doNotChangeNonKotlinMavenPlugin() {
        rewriteRun(
          pomXml(
            //language=xml
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>org.example</groupId>
                  <artifactId>demo</artifactId>
                  <version>1.0.0</version>
                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.apache.maven.plugins</groupId>
                              <artifactId>maven-compiler-plugin</artifactId>
                              <configuration>
                                  <jvmTarget>11</jvmTarget>
                              </configuration>
                          </plugin>
                      </plugins>
                  </build>
              </project>
              """
          )
        );
    }
}
