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
package org.openrewrite.java.migrate.maven;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.maven.Assertions.pomXml;

class AddAnnotationProcessorToEffectivePomTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AddAnnotationProcessorToEffectivePom(
          "org.projectlombok",
          "lombok",
          "1.18.40"
        ));
    }

    @DocumentExample
    @Test
    void addToSingleModule() {
        rewriteRun(
          pomXml(
            //language=xml
            """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <build>
                      <plugins>
                          <plugin>
                              <groupId>org.apache.maven.plugins</groupId>
                              <artifactId>maven-compiler-plugin</artifactId>
                              <configuration>
                                  <annotationProcessorPaths/>
                              </configuration>
                          </plugin>
                      </plugins>
                  </build>
              </project>
              """,
            //language=xml
            """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
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
                                          <version>1.18.40</version>
                                      </path>
                                  </annotationProcessorPaths>
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
    void doesNotDuplicateWhenAlreadyInOwnXml() {
        rewriteRun(
          pomXml(
            //language=xml
            """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
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
                                          <version>1.18.40</version>
                                      </path>
                                  </annotationProcessorPaths>
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
    void addToParentPluginManagementInMultiModule() {
        rewriteRun(
          //language=xml
          pomXml(
            """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>parent</artifactId>
                  <version>1</version>
                  <packaging>pom</packaging>
                  <modules>
                      <module>child</module>
                  </modules>
              </project>
              """,
            //language=xml
            """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>parent</artifactId>
                  <version>1</version>
                  <packaging>pom</packaging>
                  <modules>
                      <module>child</module>
                  </modules>
                  <build>
                      <pluginManagement>
                          <plugins>
                              <plugin>
                                  <groupId>org.apache.maven.plugins</groupId>
                                  <artifactId>maven-compiler-plugin</artifactId>
                                  <configuration>
                                      <annotationProcessorPaths>
                                          <path>
                                              <groupId>org.projectlombok</groupId>
                                              <artifactId>lombok</artifactId>
                                              <version>1.18.40</version>
                                          </path>
                                      </annotationProcessorPaths>
                                  </configuration>
                              </plugin>
                          </plugins>
                      </pluginManagement>
                  </build>
              </project>
              """
          ),
          mavenProject("child",
            //language=xml
            pomXml(
              """
                <project>
                    <parent>
                        <groupId>com.example</groupId>
                        <artifactId>parent</artifactId>
                        <version>1</version>
                    </parent>
                    <artifactId>child</artifactId>
                </project>
                """
            )
          )
        );
    }

    /**
     * Tests that the recipe does not add an annotation processor that is already present
     * in the effective POM via an ancestor, even when the intermediate parent POM does not
     * have it in its own XML. In this 3-level hierarchy:
     * <ul>
     *   <li>Grandparent defines lombok in {@code pluginManagement}</li>
     *   <li>Intermediate parent inherits it via effective POM but has no own configuration</li>
     *   <li>Child depends on the intermediate parent</li>
     * </ul>
     * The intermediate parent should NOT have lombok added again.
     */
    @Test
    void doesNotDuplicateWhenAlreadyInEffectivePomViaAncestor() {
        rewriteRun(
          //language=xml
          pomXml(
            // Grandparent already has lombok configured in pluginManagement
            """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>grandparent</artifactId>
                  <version>1</version>
                  <packaging>pom</packaging>
                  <modules>
                      <module>parent-module</module>
                  </modules>
                  <build>
                      <pluginManagement>
                          <plugins>
                              <plugin>
                                  <groupId>org.apache.maven.plugins</groupId>
                                  <artifactId>maven-compiler-plugin</artifactId>
                                  <configuration>
                                      <annotationProcessorPaths>
                                          <path>
                                              <groupId>org.projectlombok</groupId>
                                              <artifactId>lombok</artifactId>
                                              <version>1.18.40</version>
                                          </path>
                                      </annotationProcessorPaths>
                                  </configuration>
                              </plugin>
                          </plugins>
                      </pluginManagement>
                  </build>
              </project>
              """
          ),
          mavenProject("parent-module",
            //language=xml
            pomXml(
              // Intermediate parent: inherits lombok from grandparent via effective POM,
              // but does NOT have it in its own XML
              """
                <project>
                    <parent>
                        <groupId>com.example</groupId>
                        <artifactId>grandparent</artifactId>
                        <version>1</version>
                    </parent>
                    <artifactId>parent-module</artifactId>
                    <packaging>pom</packaging>
                    <modules>
                        <module>child-module</module>
                    </modules>
                </project>
                """
            )
          ),
          mavenProject("child-module",
            //language=xml
            pomXml(
              """
                <project>
                    <parent>
                        <groupId>com.example</groupId>
                        <artifactId>parent-module</artifactId>
                        <version>1</version>
                    </parent>
                    <artifactId>child-module</artifactId>
                </project>
                """
            )
          )
        );
    }
}
