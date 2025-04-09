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
package org.openrewrite.java.migrate.jacoco;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.regex.Pattern;

import static org.openrewrite.maven.Assertions.pomXml;

class UpgradeJaCoCoTest implements RewriteTest {
    private static final Pattern JACOCO_VERSION_PATTERN = Pattern.compile("<jacoco.version>(0\\.8\\.\\d\\d+)</jacoco.version>");

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.migrate.jacoco.UpgradeJaCoCo");
    }

    @DocumentExample
    @Test
    void pluginWithProperty() {
        rewriteRun(
          //language=xml
          pomXml(
            """
              <project>
                <properties>
                  <jacoco.version>0.8.1</jacoco.version>
                </properties>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.jacoco</groupId>
                      <artifactId>jacoco-maven-plugin</artifactId>
                      <version>${jacoco.version}</version>
                    </plugin>
                  </plugins>
                </build>
              </project>
              """,
            spec -> spec.after(pomXml -> String.format("""
              <project>
                <properties>
                  <jacoco.version>%s</jacoco.version>
                </properties>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.jacoco</groupId>
                      <artifactId>jacoco-maven-plugin</artifactId>
                      <version>${jacoco.version}</version>
                    </plugin>
                  </plugins>
                </build>
              </project>
              """, JACOCO_VERSION_PATTERN.matcher(pomXml).results().findFirst().get().group(1)))
          )
        );
    }

    @Test
    void pluginAndDepWithProperty() {
        rewriteRun(
          //language=xml
          pomXml(
            """
              <project>
                <properties>
                  <jacoco.version>0.8.1</jacoco.version>
                </properties>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencies>
                  <dependency>
                    <groupId>org.jacoco</groupId>
                    <artifactId>org.jacoco.agent</artifactId>
                    <classifier>runtime</classifier>
                    <scope>test</scope>
                    <version>${jacoco.version}</version>
                  </dependency>
                </dependencies>
                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.jacoco</groupId>
                      <artifactId>jacoco-maven-plugin</artifactId>
                      <version>${jacoco.version}</version>
                    </plugin>
                  </plugins>
                </build>
              </project>
              """,
            spec -> spec.after(pomXml -> String.format("""
              <project>
                <properties>
                  <jacoco.version>%s</jacoco.version>
                </properties>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencies>
                  <dependency>
                    <groupId>org.jacoco</groupId>
                    <artifactId>org.jacoco.agent</artifactId>
                    <classifier>runtime</classifier>
                    <scope>test</scope>
                    <version>${jacoco.version}</version>
                  </dependency>
                </dependencies>
                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.jacoco</groupId>
                      <artifactId>jacoco-maven-plugin</artifactId>
                      <version>${jacoco.version}</version>
                    </plugin>
                  </plugins>
                </build>
              </project>
              """, JACOCO_VERSION_PATTERN.matcher(pomXml).results().findFirst().get().group(1))
            )
          )
        );
    }

    @Test
    void pluginAndDepAndDepMgmt() {
        Pattern versionPattern = Pattern.compile("<version>(0\\.8\\.\\d\\d+)</version>");
        rewriteRun(
          //language=xml
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>org.jacoco</groupId>
                      <artifactId>org.jacoco.agent</artifactId>
                      <version>0.8.1</version>
                      <classifier>runtime</classifier>
                      <scope>test</scope>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
                <dependencies>
                  <dependency>
                    <groupId>org.jacoco</groupId>
                    <artifactId>org.jacoco.agent</artifactId>
                    <classifier>runtime</classifier>
                    <scope>test</scope>
                  </dependency>
                </dependencies>
                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.jacoco</groupId>
                      <artifactId>jacoco-maven-plugin</artifactId>
                      <version>0.8.1</version>
                    </plugin>
                  </plugins>
                </build>
              </project>
              """,
            spec -> spec.after(pomXml -> String.format("""
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>org.jacoco</groupId>
                      <artifactId>org.jacoco.agent</artifactId>
                      <version>%s</version>
                      <classifier>runtime</classifier>
                      <scope>test</scope>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
                <dependencies>
                  <dependency>
                    <groupId>org.jacoco</groupId>
                    <artifactId>org.jacoco.agent</artifactId>
                    <classifier>runtime</classifier>
                    <scope>test</scope>
                  </dependency>
                </dependencies>
                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.jacoco</groupId>
                      <artifactId>jacoco-maven-plugin</artifactId>
                      <version>%1$s</version>
                    </plugin>
                  </plugins>
                </build>
              </project>
              """, versionPattern.matcher(pomXml).results().findFirst().get().group(1))
            )
          )
        );
    }

    @Test
    void pluginAndDepAndDepMgmtWithProperty() {
        rewriteRun(
          //language=xml
          pomXml(
            """
              <project>
                <properties>
                  <jacoco.version>0.8.1</jacoco.version>
                </properties>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>org.jacoco</groupId>
                      <artifactId>org.jacoco.agent</artifactId>
                      <version>${jacoco.version}</version>
                      <classifier>runtime</classifier>
                      <scope>test</scope>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
                <dependencies>
                  <dependency>
                    <groupId>org.jacoco</groupId>
                    <artifactId>org.jacoco.agent</artifactId>
                    <classifier>runtime</classifier>
                    <scope>test</scope>
                  </dependency>
                </dependencies>
                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.jacoco</groupId>
                      <artifactId>jacoco-maven-plugin</artifactId>
                      <version>${jacoco.version}</version>
                    </plugin>
                  </plugins>
                </build>
              </project>
              """,
            spec -> spec.after(pomXml -> String.format("""
              <project>
                <properties>
                  <jacoco.version>%s</jacoco.version>
                </properties>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>org.jacoco</groupId>
                      <artifactId>org.jacoco.agent</artifactId>
                      <version>${jacoco.version}</version>
                      <classifier>runtime</classifier>
                      <scope>test</scope>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
                <dependencies>
                  <dependency>
                    <groupId>org.jacoco</groupId>
                    <artifactId>org.jacoco.agent</artifactId>
                    <classifier>runtime</classifier>
                    <scope>test</scope>
                  </dependency>
                </dependencies>
                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.jacoco</groupId>
                      <artifactId>jacoco-maven-plugin</artifactId>
                      <version>${jacoco.version}</version>
                    </plugin>
                  </plugins>
                </build>
              </project>
              """, JACOCO_VERSION_PATTERN.matcher(pomXml).results().findFirst().get().group(1))
            )
          )
        );
    }
}
