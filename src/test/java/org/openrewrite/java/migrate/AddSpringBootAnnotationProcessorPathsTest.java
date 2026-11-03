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

class AddSpringBootAnnotationProcessorPathsTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.migrate.EnableLombokAnnotationProcessor");
    }

    @DocumentExample
    @Test
    void keepConfigurationProcessorWhenLombokCreatesProcessorPaths() {
        rewriteRun(
          mavenProject("project",
            //language=xml
            pomXml(
              """
                <project>
                    <parent>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-parent</artifactId>
                        <version>3.2.0</version>
                    </parent>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <dependencies>
                        <dependency>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </dependency>
                        <dependency>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-configuration-processor</artifactId>
                            <optional>true</optional>
                        </dependency>
                    </dependencies>
                </project>
                """,
              """
                <project>
                    <parent>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-parent</artifactId>
                        <version>3.2.0</version>
                    </parent>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <dependencies>
                        <dependency>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </dependency>
                        <dependency>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-configuration-processor</artifactId>
                            <optional>true</optional>
                        </dependency>
                    </dependencies>
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
                                        <path>
                                            <groupId>org.springframework.boot</groupId>
                                            <artifactId>spring-boot-configuration-processor</artifactId>
                                            <version>3.2.0</version>
                                        </path>
                                    </annotationProcessorPaths>
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
    void leaveExistingProcessorPathsAlone() {
        rewriteRun(
          mavenProject("project",
            //language=xml
            pomXml(
              """
                <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <dependencies>
                        <dependency>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>1.18.40</version>
                        </dependency>
                        <dependency>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-configuration-processor</artifactId>
                            <version>3.2.0</version>
                            <optional>true</optional>
                        </dependency>
                    </dependencies>
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-compiler-plugin</artifactId>
                                <version>3.13.0</version>
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
          )
        );
    }

    @Test
    void noLombok() {
        rewriteRun(
          mavenProject("project",
            //language=xml
            pomXml(
              """
                <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <dependencies>
                        <dependency>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-configuration-processor</artifactId>
                            <version>3.2.0</version>
                            <optional>true</optional>
                        </dependency>
                    </dependencies>
                </project>
                """
            )
          )
        );
    }
}
