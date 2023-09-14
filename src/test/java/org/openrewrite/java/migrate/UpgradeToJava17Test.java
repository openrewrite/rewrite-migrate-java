/*
 * Copyright 2023 the original author or authors.
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
import org.openrewrite.DocumentExample;
import org.openrewrite.config.Environment;
import org.openrewrite.java.marker.JavaVersion;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.*;
import static org.openrewrite.maven.Assertions.pomXml;

class UpgradeToJava17Test implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(Environment.builder()
          .scanRuntimeClasspath("org.openrewrite.java.migrate")
          .build()
          .activateRecipes("org.openrewrite.java.migrate.UpgradeToJava17"));
    }

    @DocumentExample
    @Test
    void upgradeFromJava8ToJava17() {
        rewriteRun(
          version(
            mavenProject("project",
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
                  """
              ),
              //language=java
              srcMainJava(
                java(
                  """
                    package com.abc;

                    class A {
                       public String test() {
                           return String.format("Hello %s", "world");
                       }
                    }
                    """,
                  """
                    package com.abc;

                    class A {
                       public String test() {
                           return "Hello %s".formatted("world");
                       }
                    }
                    """
                )
              )
            ),
            8)
        );
    }

    @Test
    void referenceToJavaVersionProperty() {
        rewriteRun(
          version(
            mavenProject("project",
              //language=xml
              pomXml(
                """
                  <project>
                    <modelVersion>4.0.0</modelVersion>

                    <properties>
                      <java.version>1.8</java.version>
                      <maven.compiler.source>${java.version}</maven.compiler.source>
                      <maven.compiler.target>${java.version}</maven.compiler.target>
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
                      <maven.compiler.source>${java.version}</maven.compiler.source>
                      <maven.compiler.target>${java.version}</maven.compiler.target>
                    </properties>

                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                  </project>
                  """
              ),
              srcMainJava(
                java(
                  //language=java
                  """
                    package com.abc;

                    class A {
                       public String test() {
                           return String.format("Hello %s", "world");
                       }
                    }
                    """,
                  //language=java
                  """
                    package com.abc;

                    class A {
                       public String test() {
                           return "Hello %s".formatted("world");
                       }
                    }
                    """,
                  spec -> spec.afterRecipe(cu ->
                    assertThat(cu.getMarkers().findFirst(JavaVersion.class).map(JavaVersion::getSourceCompatibility).get())
                      .isEqualTo("17"))
                )
              )
            ),
            8)
        );
    }

    @Test
    void needToUpgradeMavenCompilerPluginToSupportReleaseTag() {
        rewriteRun(
          version(
            mavenProject("project",
              //language=xml
              pomXml(
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
                          <version>3.3.0</version>
                          <configuration>
                            <source>1.8</source>
                            <target>1.8</target>
                          </configuration>
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
                    <build>
                      <plugins>
                        <plugin>
                          <groupId>org.apache.maven.plugins</groupId>
                          <artifactId>maven-compiler-plugin</artifactId>
                          <version>3.6.2</version>
                          <configuration>
                            <release>17</release>
                          </configuration>
                        </plugin>
                      </plugins>
                    </build>
                  </project>
                  """
              )
            ),
            8)
        );
    }

    @Test
    void notNeedToUpgradeMavenCompilerPluginToSupportReleaseTag() {
        rewriteRun(
          version(
            mavenProject("project",
              //language=xml
              pomXml(
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
                  """
              )
            ),
            8)
        );
    }
}
