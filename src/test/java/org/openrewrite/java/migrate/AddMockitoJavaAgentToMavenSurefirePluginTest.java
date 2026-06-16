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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.maven.Assertions.pomXml;

class AddMockitoJavaAgentToMavenSurefirePluginTest implements RewriteTest {

  @Override
  public void defaults(RecipeSpec spec) {
    spec.recipe(new AddMockitoJavaAgentToMavenSurefirePlugin());
  }

    @DocumentExample
    @Test
    void addsMockitoAgentArgAndPropertiesGoalToMavenPlugins() {
    rewriteRun(
      mavenProject("test-project",
        pomXml(
          """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>org.sample</groupId>
              <artifactId>test</artifactId>
              <version>${revision}</version>

              <parent>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-parent</artifactId>
                <version>3.5.4</version>
                <relativePath/>
              </parent>

              <dependencies>
                <dependency>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-test</artifactId>
                  <scope>test</scope>
                </dependency>
              </dependencies>
              <build>
                <plugins>
                  <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-dependency-plugin</artifactId>
                  </plugin>
                  <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-surefire-plugin</artifactId>
                  </plugin>
                </plugins>
              </build>
            </project>
            """,
          """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>org.sample</groupId>
              <artifactId>test</artifactId>
              <version>${revision}</version>

              <parent>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-parent</artifactId>
                <version>3.5.4</version>
                <relativePath/>
              </parent>
              <properties>
                <argLine></argLine>
              </properties>

              <dependencies>
                <dependency>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-test</artifactId>
                  <scope>test</scope>
                </dependency>
              </dependencies>
              <build>
                <plugins>
                  <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-dependency-plugin</artifactId>
                    <executions>
                      <execution>
                        <goals>
                          <goal>properties</goal>
                        </goals>
                      </execution>
                    </executions>
                  </plugin>
                  <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-surefire-plugin</artifactId>
                    <configuration>
                      <!--suppress MavenModelInspection -->
                      <argLine>@{argLine} -javaagent:${org.mockito:mockito-core:jar}</argLine>
                    </configuration>
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
  void addsByteBuddyAgentArgWithSurefirePluginAndNoExistingConfigurationWithOlderMockitoVersion() {
    rewriteRun(
      mavenProject("test-project",
        pomXml(
          """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>org.sample</groupId>
              <artifactId>test</artifactId>
              <version>${revision}</version>

              <parent>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-parent</artifactId>
                <version>3.3.13</version>
                <relativePath/>
              </parent>
              <properties>
                <argLine>custom value</argLine>
              </properties>

              <dependencies>
                <dependency>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-test</artifactId>
                  <scope>test</scope>
                </dependency>
              </dependencies>
              <build>
                <plugins>
                  <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-dependency-plugin</artifactId>
                    <executions>
                      <execution>
                        <configuration>
                          <foobar />
                        </configuration>
                      </execution>
                    </executions>
                  </plugin>
                  <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-surefire-plugin</artifactId>
                  </plugin>
                </plugins>
              </build>
            </project>
            """,
          """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>org.sample</groupId>
              <artifactId>test</artifactId>
              <version>${revision}</version>

              <parent>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-parent</artifactId>
                <version>3.3.13</version>
                <relativePath/>
              </parent>
              <properties>
                <argLine>custom value</argLine>
              </properties>

              <dependencies>
                <dependency>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-test</artifactId>
                  <scope>test</scope>
                </dependency>
              </dependencies>
              <build>
                <plugins>
                  <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-dependency-plugin</artifactId>
                    <executions>
                      <execution>
                        <configuration>
                          <foobar />
                        </configuration>
                        <goals>
                          <goal>properties</goal>
                        </goals>
                      </execution>
                    </executions>
                  </plugin>
                  <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-surefire-plugin</artifactId>
                    <configuration>
                      <!--suppress MavenModelInspection -->
                      <argLine>@{argLine} -javaagent:${net.bytebuddy:byte-buddy-agent:jar}</argLine>
                    </configuration>
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
  void addsMockitoAgentArgToExistingConfigurationWithNoArgLineTag() {
    rewriteRun(
      mavenProject("test-project",
        pomXml(
          """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>org.sample</groupId>
              <artifactId>test</artifactId>
              <version>${revision}</version>

              <parent>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-parent</artifactId>
                <version>3.5.4</version>
                <relativePath/>
              </parent>
              <properties>
                <customProperty>some-property</customProperty>
              </properties>

              <dependencies>
                <dependency>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-test</artifactId>
                  <scope>test</scope>
                </dependency>
              </dependencies>
              <build>
                <plugins>
                  <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-dependency-plugin</artifactId>
                    <executions>
                      <execution>
                        <goals>
                          <goal>properties</goal>
                        </goals>
                      </execution>
                    </executions>
                  </plugin>
                  <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-surefire-plugin</artifactId>
                    <configuration>
                      <systemPropertyVariables>
                        <propertyName>foobar</propertyName>
                      </systemPropertyVariables>
                    </configuration>
                  </plugin>
                </plugins>
              </build>
            </project>
            """,
          """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>org.sample</groupId>
              <artifactId>test</artifactId>
              <version>${revision}</version>

              <parent>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-parent</artifactId>
                <version>3.5.4</version>
                <relativePath/>
              </parent>
              <properties>
                <argLine></argLine>
                <customProperty>some-property</customProperty>
              </properties>

              <dependencies>
                <dependency>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-test</artifactId>
                  <scope>test</scope>
                </dependency>
              </dependencies>
              <build>
                <plugins>
                  <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-dependency-plugin</artifactId>
                    <executions>
                      <execution>
                        <goals>
                          <goal>properties</goal>
                        </goals>
                      </execution>
                    </executions>
                  </plugin>
                  <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-surefire-plugin</artifactId>
                    <configuration>
                      <systemPropertyVariables>
                        <propertyName>foobar</propertyName>
                      </systemPropertyVariables>
                      <!--suppress MavenModelInspection -->
                      <argLine>@{argLine} -javaagent:${org.mockito:mockito-core:jar}</argLine>
                    </configuration>
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
  void addsMockitoAgentArgToExistingArgLineTagWithNoValue() {
    rewriteRun(
      mavenProject("test-project",
        pomXml(
          """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>org.sample</groupId>
              <artifactId>test</artifactId>
              <version>${revision}</version>

              <parent>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-parent</artifactId>
                <version>3.5.4</version>
                <relativePath/>
              </parent>

              <dependencies>
                <dependency>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-test</artifactId>
                  <scope>test</scope>
                </dependency>
              </dependencies>
              <build>
                <plugins>
                  <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-dependency-plugin</artifactId>
                    <executions>
                      <execution>
                        <goals>
                          <goal>properties</goal>
                        </goals>
                      </execution>
                    </executions>
                  </plugin>
                  <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-surefire-plugin</artifactId>
                    <configuration>
                      <argLine />
                    </configuration>
                  </plugin>
                </plugins>
              </build>
            </project>
            """,
          """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>org.sample</groupId>
              <artifactId>test</artifactId>
              <version>${revision}</version>

              <parent>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-parent</artifactId>
                <version>3.5.4</version>
                <relativePath/>
              </parent>
              <properties>
                <argLine></argLine>
              </properties>

              <dependencies>
                <dependency>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-test</artifactId>
                  <scope>test</scope>
                </dependency>
              </dependencies>
              <build>
                <plugins>
                  <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-dependency-plugin</artifactId>
                    <executions>
                      <execution>
                        <goals>
                          <goal>properties</goal>
                        </goals>
                      </execution>
                    </executions>
                  </plugin>
                  <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-surefire-plugin</artifactId>
                    <configuration>
                      <!--suppress MavenModelInspection -->
                      <argLine>@{argLine} -javaagent:${org.mockito:mockito-core:jar}</argLine>
                    </configuration>
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
  void addsMockitoAgentArgPreservingExistingArgLineArguments() {
    rewriteRun(
      mavenProject("test-project",
        pomXml(
          """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>org.sample</groupId>
              <artifactId>test</artifactId>
              <version>${revision}</version>

              <parent>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-parent</artifactId>
                <version>3.5.4</version>
                <relativePath/>
              </parent>

              <dependencies>
                <dependency>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-test</artifactId>
                  <scope>test</scope>
                </dependency>
              </dependencies>
              <build>
                <plugins>
                  <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-dependency-plugin</artifactId>
                    <executions>
                      <execution>
                        <goals>
                          <goal>properties</goal>
                        </goals>
                      </execution>
                    </executions>
                  </plugin>
                  <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-surefire-plugin</artifactId>
                    <configuration>
                      <systemPropertyVariables>
                        <propertyName>foobar</propertyName>
                      </systemPropertyVariables>
                      <argLine>-Xmx256m</argLine>
                    </configuration>
                  </plugin>
                </plugins>
              </build>
            </project>
            """,
          """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>org.sample</groupId>
              <artifactId>test</artifactId>
              <version>${revision}</version>

              <parent>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-parent</artifactId>
                <version>3.5.4</version>
                <relativePath/>
              </parent>
              <properties>
                <argLine></argLine>
              </properties>

              <dependencies>
                <dependency>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-test</artifactId>
                  <scope>test</scope>
                </dependency>
              </dependencies>
              <build>
                <plugins>
                  <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-dependency-plugin</artifactId>
                    <executions>
                      <execution>
                        <goals>
                          <goal>properties</goal>
                        </goals>
                      </execution>
                    </executions>
                  </plugin>
                  <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-surefire-plugin</artifactId>
                    <configuration>
                      <systemPropertyVariables>
                        <propertyName>foobar</propertyName>
                      </systemPropertyVariables>
                      <!--suppress MavenModelInspection -->
                      <argLine>-Xmx256m -javaagent:${org.mockito:mockito-core:jar}</argLine>
                    </configuration>
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
  void onlyAddsConfigurationToPomWithMockitoInMultiModuleProject() {
    rewriteRun(
      mavenProject("test-project",
        pomXml(
          """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>org.sample</groupId>
              <artifactId>test</artifactId>
              <version>1.0</version>

              <modules>
                <module>test-module1</module>
              </modules>

              <parent>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-parent</artifactId>
                <version>3.5.4</version>
                <relativePath/>
              </parent>

            </project>
            """,
          spec -> spec.path("pom.xml")
        ),
        pomXml(
          """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>org.sample</groupId>
              <artifactId>test-module1</artifactId>
              <version>${revision}</version>

              <parent>
                <groupId>org.sample</groupId>
                <artifactId>test</artifactId>
                <version>1.0</version>
                <relativePath>../pom.xml</relativePath>
              </parent>

              <dependencies>
                <dependency>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-test</artifactId>
                  <scope>test</scope>
                </dependency>
              </dependencies>
              <build>
                <plugins>
                  <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-dependency-plugin</artifactId>
                  </plugin>
                  <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-surefire-plugin</artifactId>
                  </plugin>
                </plugins>
              </build>
            </project>
            """,
          """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>org.sample</groupId>
              <artifactId>test-module1</artifactId>
              <version>${revision}</version>

              <parent>
                <groupId>org.sample</groupId>
                <artifactId>test</artifactId>
                <version>1.0</version>
                <relativePath>../pom.xml</relativePath>
              </parent>
              <properties>
                <argLine></argLine>
              </properties>

              <dependencies>
                <dependency>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-test</artifactId>
                  <scope>test</scope>
                </dependency>
              </dependencies>
              <build>
                <plugins>
                  <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-dependency-plugin</artifactId>
                    <executions>
                      <execution>
                        <goals>
                          <goal>properties</goal>
                        </goals>
                      </execution>
                    </executions>
                  </plugin>
                  <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-surefire-plugin</artifactId>
                    <configuration>
                      <!--suppress MavenModelInspection -->
                      <argLine>@{argLine} -javaagent:${org.mockito:mockito-core:jar}</argLine>
                    </configuration>
                  </plugin>
                </plugins>
              </build>
            </project>
            """,
          spec -> spec.path("test-module1/pom.xml")
        )
      )
    );
  }

  @Test
  void addsMavenSurefireAndDependencyPluginsWhenAbsent() {
    rewriteRun(
      mavenProject("test-project",
        pomXml(
          """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>org.sample</groupId>
              <artifactId>test</artifactId>
              <version>${revision}</version>

              <parent>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-parent</artifactId>
                <version>3.5.4</version>
                <relativePath/>
              </parent>

              <dependencies>
                <dependency>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-test</artifactId>
                  <scope>test</scope>
                </dependency>
              </dependencies>
            </project>
            """,
          """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>org.sample</groupId>
              <artifactId>test</artifactId>
              <version>${revision}</version>

              <parent>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-parent</artifactId>
                <version>3.5.4</version>
                <relativePath/>
              </parent>
              <properties>
                <argLine></argLine>
              </properties>

              <dependencies>
                <dependency>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-test</artifactId>
                  <scope>test</scope>
                </dependency>
              </dependencies>
              <build>
                <plugins>
                  <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-dependency-plugin</artifactId>
                    <executions>
                      <execution>
                        <goals>
                          <goal>properties</goal>
                        </goals>
                      </execution>
                    </executions>
                  </plugin>
                  <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-surefire-plugin</artifactId>
                    <configuration>
                      <!--suppress MavenModelInspection -->
                      <argLine>@{argLine} -javaagent:${org.mockito:mockito-core:jar}</argLine>
                    </configuration>
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
  void addsGoalsTagWithPropertiesGoalToExistingMavenDependencyPluginWhenMissing() {
    rewriteRun(
      mavenProject("test-project",
        pomXml(
          """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>org.sample</groupId>
              <artifactId>test</artifactId>
              <version>${revision}</version>

              <parent>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-parent</artifactId>
                <version>3.5.4</version>
                <relativePath/>
              </parent>

              <dependencies>
                <dependency>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-test</artifactId>
                  <scope>test</scope>
                </dependency>
              </dependencies>
              <build>
                <plugins>
                  <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-dependency-plugin</artifactId>
                    <executions>
                      <execution>
                        <configuration>
                          <foobar />
                        </configuration>
                      </execution>
                    </executions>
                  </plugin>
                  <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-surefire-plugin</artifactId>
                    <configuration>
                      <!--suppress MavenModelInspection -->
                      <argLine>@{argLine} -javaagent:${org.mockito:mockito-core:jar}</argLine>
                    </configuration>
                  </plugin>
                </plugins>
              </build>
            </project>
            """,
          """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>org.sample</groupId>
              <artifactId>test</artifactId>
              <version>${revision}</version>

              <parent>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-parent</artifactId>
                <version>3.5.4</version>
                <relativePath/>
              </parent>
              <properties>
                <argLine></argLine>
              </properties>

              <dependencies>
                <dependency>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-test</artifactId>
                  <scope>test</scope>
                </dependency>
              </dependencies>
              <build>
                <plugins>
                  <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-dependency-plugin</artifactId>
                    <executions>
                      <execution>
                        <configuration>
                          <foobar />
                        </configuration>
                        <goals>
                          <goal>properties</goal>
                        </goals>
                      </execution>
                    </executions>
                  </plugin>
                  <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-surefire-plugin</artifactId>
                    <configuration>
                      <!--suppress MavenModelInspection -->
                      <argLine>@{argLine} -javaagent:${org.mockito:mockito-core:jar}</argLine>
                    </configuration>
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
  void addsPropertiesGoalToExistingGoalsSectionInMavenDependencyPlugin() {
    rewriteRun(
      mavenProject("test-project",
        pomXml(
          """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>org.sample</groupId>
              <artifactId>test</artifactId>
              <version>${revision}</version>

              <parent>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-parent</artifactId>
                <version>3.5.4</version>
                <relativePath/>
              </parent>

              <dependencies>
                <dependency>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-test</artifactId>
                  <scope>test</scope>
                </dependency>
              </dependencies>
              <build>
                <plugins>
                  <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-dependency-plugin</artifactId>
                    <executions>
                      <execution>
                        <configuration>
                          <foobar />
                        </configuration>
                        <goals>
                          <goal>analyize</goal>
                        </goals>
                      </execution>
                    </executions>
                  </plugin>
                  <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-surefire-plugin</artifactId>
                    <configuration>
                      <!--suppress MavenModelInspection -->
                      <argLine>@{argLine} -javaagent:${org.mockito:mockito-core:jar}</argLine>
                    </configuration>
                  </plugin>
                </plugins>
              </build>
            </project>
            """,
          """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>org.sample</groupId>
              <artifactId>test</artifactId>
              <version>${revision}</version>

              <parent>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-parent</artifactId>
                <version>3.5.4</version>
                <relativePath/>
              </parent>
              <properties>
                <argLine></argLine>
              </properties>

              <dependencies>
                <dependency>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-test</artifactId>
                  <scope>test</scope>
                </dependency>
              </dependencies>
              <build>
                <plugins>
                  <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-dependency-plugin</artifactId>
                    <executions>
                      <execution>
                        <configuration>
                          <foobar />
                        </configuration>
                        <goals>
                          <goal>analyize</goal>
                          <goal>properties</goal>
                        </goals>
                      </execution>
                    </executions>
                  </plugin>
                  <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-surefire-plugin</artifactId>
                    <configuration>
                      <!--suppress MavenModelInspection -->
                      <argLine>@{argLine} -javaagent:${org.mockito:mockito-core:jar}</argLine>
                    </configuration>
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
  void makesNoChangeWhenMockitoAgentFlagAlreadyExists() {
    rewriteRun(
      mavenProject("test-project",
        pomXml(
          """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>org.sample</groupId>
              <artifactId>test</artifactId>
              <version>${revision}</version>

              <parent>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-parent</artifactId>
                <version>3.5.4</version>
                <relativePath/>
              </parent>
              <properties>
                <argLine></argLine>
              </properties>

              <dependencies>
                <dependency>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-test</artifactId>
                  <scope>test</scope>
                </dependency>
              </dependencies>
              <build>
                <plugins>
                  <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-dependency-plugin</artifactId>
                    <executions>
                      <execution>
                        <goals>
                          <goal>properties</goal>
                        </goals>
                      </execution>
                    </executions>
                  </plugin>
                  <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-surefire-plugin</artifactId>
                    <configuration>
                      <!--suppress MavenModelInspection -->
                      <argLine>@{argLine} -javaagent:${org.mockito:mockito-core:jar}</argLine>
                      <systemPropertyVariables>
                        <jacoco-agent.destfile>${project.build.directory}/jacoco.exec</jacoco-agent.destfile>
                      </systemPropertyVariables>
                    </configuration>
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
  void makesNoChangeWhenMockitoAgentFlagAlreadyExistsUsingSingleLineArgline() {
    rewriteRun(
      mavenProject("test-project",
        pomXml(
          """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>org.sample</groupId>
              <artifactId>test</artifactId>
              <version>${revision}</version>

              <parent>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-parent</artifactId>
                <version>3.5.4</version>
                <relativePath/>
              </parent>
              <properties>
                <argLine/>
              </properties>

              <dependencies>
                <dependency>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-test</artifactId>
                  <scope>test</scope>
                </dependency>
              </dependencies>
              <build>
                <plugins>
                  <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-dependency-plugin</artifactId>
                    <executions>
                      <execution>
                        <goals>
                          <goal>properties</goal>
                        </goals>
                      </execution>
                    </executions>
                  </plugin>
                  <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-surefire-plugin</artifactId>
                    <configuration>
                      <!--suppress MavenModelInspection -->
                      <argLine>@{argLine} -javaagent:${org.mockito:mockito-core:jar}</argLine>
                      <systemPropertyVariables>
                        <jacoco-agent.destfile>${project.build.directory}/jacoco.exec</jacoco-agent.destfile>
                      </systemPropertyVariables>
                    </configuration>
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
  void makesNoChangeWhenMockitoCoreDependencyIsNotOnClasspath() {
    rewriteRun(
      mavenProject("test-project",
        pomXml(
          """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>org.sample</groupId>
              <artifactId>test</artifactId>
              <version>${revision}</version>

              <parent>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-parent</artifactId>
                <version>3.5.4</version>
                <relativePath/>
              </parent>

              <build>
                <plugins>
                  <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-dependency-plugin</artifactId>
                  </plugin>
                  <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-surefire-plugin</artifactId>
                    <configuration>
                      <systemPropertyVariables>
                        <jacoco-agent.destfile>${project.build.directory}/jacoco.exec</jacoco-agent.destfile>
                      </systemPropertyVariables>
                    </configuration>
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
  void makesNoChangesWhenParentPomManagesSurefirePluginAndHasAgentConfiguration() {
    rewriteRun(
      mavenProject("test-project",
        pomXml(
          """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>org.sample</groupId>
              <artifactId>test</artifactId>
              <version>1.0</version>

              <modules>
                <module>test-module1</module>
              </modules>

              <parent>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-parent</artifactId>
                <version>3.5.4</version>
                <relativePath/>
              </parent>

              <build>
                <pluginManagement>
                  <plugins>
                    <plugin>
                      <groupId>org.apache.maven.plugins</groupId>
                      <artifactId>maven-surefire-plugin</artifactId>
                        <configuration>
                          <!--suppress MavenModelInspection -->
                          <argLine>@{argLine} -javaagent:${org.mockito:mockito-core:jar}</argLine>
                          <systemPropertyVariables>
                            <jacoco-agent.destfile>${project.build.directory}/jacoco.exec</jacoco-agent.destfile>
                          </systemPropertyVariables>
                        </configuration>
                      </plugin>
                  </plugins>
                </pluginManagement>
              </build>
            </project>
            """,
          spec -> spec.path("pom.xml")
        ),
        pomXml(
          """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>org.sample</groupId>
              <artifactId>test-module1</artifactId>
              <version>${revision}</version>

              <parent>
                <groupId>org.sample</groupId>
                <artifactId>test</artifactId>
                <version>1.0</version>
                <relativePath>../pom.xml</relativePath>
              </parent>

              <dependencies>
                <dependency>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-test</artifactId>
                  <scope>test</scope>
                </dependency>
              </dependencies>
              <build>
                <plugins>
                  <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-dependency-plugin</artifactId>
                  </plugin>
                  <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-surefire-plugin</artifactId>
                  </plugin>
                </plugins>
              </build>
            </project>
            """,
          spec -> spec.path("test-module1/pom.xml")
        )
      )
    );
  }

  @Test
  void updatesIndividualPomsWhenParentPomManagesSurefirePluginWithoutAgentConfiguration() {
    rewriteRun(
      mavenProject("test-project",
        pomXml(
          """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>org.sample</groupId>
              <artifactId>test</artifactId>
              <version>1.0</version>

              <modules>
                <module>test-module1</module>
              </modules>

              <parent>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-parent</artifactId>
                <version>3.5.4</version>
                <relativePath/>
              </parent>

              <build>
                <pluginManagement>
                  <plugins>
                    <plugin>
                      <groupId>org.apache.maven.plugins</groupId>
                      <artifactId>maven-dependency-plugin</artifactId>
                      <executions>
                        <execution>
                          <goals>
                            <goal>properties</goal>
                          </goals>
                        </execution>
                      </executions>
                    </plugin>
                    <plugin>
                      <groupId>org.apache.maven.plugins</groupId>
                      <artifactId>maven-surefire-plugin</artifactId>
                        <configuration>
                          <systemPropertyVariables>
                            <jacoco-agent.destfile>${project.build.directory}/jacoco.exec</jacoco-agent.destfile>
                          </systemPropertyVariables>
                        </configuration>
                      </plugin>
                  </plugins>
                </pluginManagement>
              </build>
            </project>
            """,
          spec -> spec.path("pom.xml")
        ),
        pomXml(
          """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>org.sample</groupId>
              <artifactId>test-module1</artifactId>
              <version>${revision}</version>

              <parent>
                <groupId>org.sample</groupId>
                <artifactId>test</artifactId>
                <version>1.0</version>
                <relativePath>../pom.xml</relativePath>
              </parent>

              <dependencies>
                <dependency>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-test</artifactId>
                  <scope>test</scope>
                </dependency>
              </dependencies>
              <build>
                <plugins>
                  <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-surefire-plugin</artifactId>
                  </plugin>
                </plugins>
              </build>
            </project>
            """,
          """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>org.sample</groupId>
              <artifactId>test-module1</artifactId>
              <version>${revision}</version>

              <parent>
                <groupId>org.sample</groupId>
                <artifactId>test</artifactId>
                <version>1.0</version>
                <relativePath>../pom.xml</relativePath>
              </parent>
              <properties>
                <argLine></argLine>
              </properties>

              <dependencies>
                <dependency>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-test</artifactId>
                  <scope>test</scope>
                </dependency>
              </dependencies>
              <build>
                <plugins>
                  <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-surefire-plugin</artifactId>
                    <configuration>
                      <!--suppress MavenModelInspection -->
                      <argLine>@{argLine} -javaagent:${org.mockito:mockito-core:jar}</argLine>
                    </configuration>
                  </plugin>
                  <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-dependency-plugin</artifactId>
                    <executions>
                      <execution>
                        <goals>
                          <goal>properties</goal>
                        </goals>
                      </execution>
                    </executions>
                  </plugin>
                </plugins>
              </build>
            </project>
            """,
          spec -> spec.path("test-module1/pom.xml")
        )
      )
    );
  }

  @Test
  void augmentsSurefirePluginDeclaredInPluginManagement() {
    rewriteRun(
      mavenProject("test-project",
        pomXml(
          """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>org.sample</groupId>
              <artifactId>test</artifactId>
              <version>${revision}</version>

              <parent>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-parent</artifactId>
                <version>3.5.4</version>
                <relativePath/>
              </parent>

              <dependencies>
                <dependency>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-test</artifactId>
                  <scope>test</scope>
                </dependency>
              </dependencies>
              <build>
                <plugins>
                  <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-dependency-plugin</artifactId>
                    <executions>
                      <execution>
                        <goals>
                          <goal>properties</goal>
                        </goals>
                      </execution>
                    </executions>
                  </plugin>
                </plugins>
                <pluginManagement>
                  <plugins>
                    <plugin>
                      <groupId>org.apache.maven.plugins</groupId>
                      <artifactId>maven-surefire-plugin</artifactId>
                    </plugin>
                  </plugins>
                </pluginManagement>
              </build>
            </project>
            """,
          """
            <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>org.sample</groupId>
              <artifactId>test</artifactId>
              <version>${revision}</version>

              <parent>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-parent</artifactId>
                <version>3.5.4</version>
                <relativePath/>
              </parent>
              <properties>
                <argLine></argLine>
              </properties>

              <dependencies>
                <dependency>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-test</artifactId>
                  <scope>test</scope>
                </dependency>
              </dependencies>
              <build>
                <plugins>
                  <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-dependency-plugin</artifactId>
                    <executions>
                      <execution>
                        <goals>
                          <goal>properties</goal>
                        </goals>
                      </execution>
                    </executions>
                  </plugin>
                </plugins>
                <pluginManagement>
                  <plugins>
                    <plugin>
                      <groupId>org.apache.maven.plugins</groupId>
                      <artifactId>maven-surefire-plugin</artifactId>
                      <configuration>
                        <!--suppress MavenModelInspection -->
                        <argLine>@{argLine} -javaagent:${org.mockito:mockito-core:jar}</argLine>
                      </configuration>
                    </plugin>
                  </plugins>
                </pluginManagement>
              </build>
            </project>
            """
        )
      )
    );
  }
}
