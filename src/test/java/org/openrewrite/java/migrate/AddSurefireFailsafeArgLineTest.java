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

class AddSurefireFailsafeArgLineTest implements RewriteTest {

    private static final String ARG_LINE = """
            --add-opens java.base/java.lang=ALL-UNNAMED\
             --add-opens java.base/java.util=ALL-UNNAMED\
             --add-opens java.base/java.lang.reflect=ALL-UNNAMED""";

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AddSurefireFailsafeArgLine(ARG_LINE));
    }

    @DocumentExample
    @Test
    void surefireWithNoConfiguration() {
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
                                    <argLine>--add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/java.lang.reflect=ALL-UNNAMED</argLine>
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
    void surefireWithExistingConfigurationButNoArgLine() {
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
                                    <includes>
                                        <include>**/*Test.java</include>
                                    </includes>
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
                                    <argLine>--add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/java.lang.reflect=ALL-UNNAMED</argLine>
                                    <includes>
                                        <include>**/*Test.java</include>
                                    </includes>
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
    void surefireWithExistingArgLine() {
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
                                    <argLine>-Xmx512m --add-opens java.base/java.lang=ALL-UNNAMED</argLine>
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
                                    <argLine>-Xmx512m --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/java.lang.reflect=ALL-UNNAMED</argLine>
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
    void idempotentWhenAllFlagsPresent() {
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
                                    <argLine>--add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/java.lang.reflect=ALL-UNNAMED</argLine>
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
    void failsafePlugin() {
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
                                    <argLine>--add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/java.lang.reflect=ALL-UNNAMED</argLine>
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
    void bothSurefireAndFailsafe() {
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
                            </plugin>
                            <plugin>
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-failsafe-plugin</artifactId>
                                <version>3.5.2</version>
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
                                    <argLine>--add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/java.lang.reflect=ALL-UNNAMED</argLine>
                                </configuration>
                            </plugin>
                            <plugin>
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-failsafe-plugin</artifactId>
                                <version>3.5.2</version>
                                <configuration>
                                    <argLine>--add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/java.lang.reflect=ALL-UNNAMED</argLine>
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
    void noPluginDeclared() {
        rewriteRun(
          mavenProject("project",
            pomXml(
              """
                <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                </project>
                """
            )
          )
        );
    }

    @Test
    void pluginInPluginManagement() {
        rewriteRun(
          mavenProject("project",
            pomXml(
              """
                <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <build>
                        <pluginManagement>
                            <plugins>
                                <plugin>
                                    <groupId>org.apache.maven.plugins</groupId>
                                    <artifactId>maven-surefire-plugin</artifactId>
                                    <version>3.5.2</version>
                                </plugin>
                            </plugins>
                        </pluginManagement>
                    </build>
                </project>
                """,
              """
                <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <build>
                        <pluginManagement>
                            <plugins>
                                <plugin>
                                    <groupId>org.apache.maven.plugins</groupId>
                                    <artifactId>maven-surefire-plugin</artifactId>
                                    <version>3.5.2</version>
                                    <configuration>
                                        <argLine>--add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/java.lang.reflect=ALL-UNNAMED</argLine>
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

    @Test
    void preservesArgLinePropertyReference() {
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
                                    <argLine>${argLine} -Xmx512m</argLine>
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
                                    <argLine>${argLine} -Xmx512m --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/java.lang.reflect=ALL-UNNAMED</argLine>
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
    void surefireWithImplicitGroupId() {
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
                                <artifactId>maven-surefire-plugin</artifactId>
                                <version>3.5.2</version>
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
                                <artifactId>maven-surefire-plugin</artifactId>
                                <version>3.5.2</version>
                                <configuration>
                                    <argLine>--add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/java.lang.reflect=ALL-UNNAMED</argLine>
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
