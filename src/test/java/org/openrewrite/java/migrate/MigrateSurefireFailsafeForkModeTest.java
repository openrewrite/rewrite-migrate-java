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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.maven.Assertions.pomXml;

class MigrateSurefireFailsafeForkModeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new MigrateSurefireFailsafeForkMode());
    }

    @DocumentExample
    @Test
    void once() {
        rewriteRun(
          mavenProject("project",
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
                                <artifactId>maven-surefire-plugin</artifactId>
                                <version>3.5.2</version>
                                <configuration>
                                    <forkMode>once</forkMode>
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
                                <artifactId>maven-surefire-plugin</artifactId>
                                <version>3.5.2</version>
                                <configuration>
                                    <forkCount>1</forkCount>
                                    <reuseForks>true</reuseForks>
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
    void alwaysOnFailsafe() {
        rewriteRun(
          mavenProject("project",
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
                                <artifactId>maven-failsafe-plugin</artifactId>
                                <version>3.5.2</version>
                                <configuration>
                                    <forkMode>always</forkMode>
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
                                <artifactId>maven-failsafe-plugin</artifactId>
                                <version>3.5.2</version>
                                <configuration>
                                    <forkCount>1</forkCount>
                                    <reuseForks>false</reuseForks>
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
    void never() {
        rewriteRun(
          mavenProject("project",
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
                                <artifactId>maven-surefire-plugin</artifactId>
                                <version>3.5.2</version>
                                <configuration>
                                    <forkMode>never</forkMode>
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
                                <artifactId>maven-surefire-plugin</artifactId>
                                <version>3.5.2</version>
                                <configuration>
                                    <forkCount>0</forkCount>
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
    void perthreadWithThreadCount() {
        rewriteRun(
          mavenProject("project",
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
                                <artifactId>maven-surefire-plugin</artifactId>
                                <version>3.5.2</version>
                                <configuration>
                                    <forkMode>perthread</forkMode>
                                    <threadCount>4</threadCount>
                                    <parallel>classes</parallel>
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
                                <artifactId>maven-surefire-plugin</artifactId>
                                <version>3.5.2</version>
                                <configuration>
                                    <forkCount>4</forkCount>
                                    <reuseForks>false</reuseForks>
                                    <threadCount>4</threadCount>
                                    <parallel>classes</parallel>
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
    void perthreadWithoutThreadCountIsLeftUntouched() {
        rewriteRun(
          mavenProject("project",
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
                                <artifactId>maven-surefire-plugin</artifactId>
                                <version>3.5.2</version>
                                <configuration>
                                    <forkMode>perthread</forkMode>
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
    void unrecognizedValueIsLeftUntouched() {
        rewriteRun(
          mavenProject("project",
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
                                <artifactId>maven-surefire-plugin</artifactId>
                                <version>3.5.2</version>
                                <configuration>
                                    <forkMode>pertest</forkMode>
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
    void redundantForkModeAlongsideForkCountIsRemoved() {
        rewriteRun(
          mavenProject("project",
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
                                <artifactId>maven-surefire-plugin</artifactId>
                                <version>3.5.2</version>
                                <configuration>
                                    <forkMode>always</forkMode>
                                    <forkCount>2</forkCount>
                                    <reuseForks>true</reuseForks>
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
                                <artifactId>maven-surefire-plugin</artifactId>
                                <version>3.5.2</version>
                                <configuration>
                                    <forkCount>2</forkCount>
                                    <reuseForks>true</reuseForks>
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
    void unrelatedPluginIsLeftUntouched() {
        rewriteRun(
          mavenProject("project",
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
                                <version>3.14.0</version>
                                <configuration>
                                    <forkMode>once</forkMode>
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
}
