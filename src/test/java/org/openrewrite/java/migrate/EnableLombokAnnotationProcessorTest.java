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
package org.openrewrite.java.migrate;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.maven.Assertions.pomXml;

class EnableLombokAnnotationProcessorTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.migrate.EnableLombokAnnotationProcessor");
    }

    @Test
    void doesNotAddLombokWhenNotPresentViaUpgradeToJava25() {
        rewriteRun(
          spec -> spec.recipeFromResources("org.openrewrite.java.migrate.UpgradeToJava25"),
          mavenProject("project",
            pomXml(
              //language=xml
              """
                <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <properties>
                        <maven.compiler.release>17</maven.compiler.release>
                    </properties>
                    <dependencies>
                        <dependency>
                            <groupId>com.google.guava</groupId>
                            <artifactId>guava</artifactId>
                            <version>33.0.0-jre</version>
                        </dependency>
                    </dependencies>
                </project>
                """,
              //language=xml
              """
                <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <properties>
                        <maven.compiler.release>25</maven.compiler.release>
                    </properties>
                    <dependencies>
                        <dependency>
                            <groupId>com.google.guava</groupId>
                            <artifactId>guava</artifactId>
                            <version>33.0.0-jre</version>
                        </dependency>
                    </dependencies>
                </project>
                """
            )
          )
        );
    }

    @Test
    void doesNotAddLombokToModuleWithoutLombok() {
        rewriteRun(
          mavenProject("module-with-lombok",
            pomXml(
              //language=xml
              """
                <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>module-with-lombok</artifactId>
                    <version>1</version>
                    <dependencies>
                        <dependency>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>1.18.40</version>
                        </dependency>
                    </dependencies>
                </project>
                """,
              spec -> spec.after(actual -> actual)
            )
          ),
          mavenProject("module-without-lombok",
            pomXml(
              //language=xml
              """
                <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>module-without-lombok</artifactId>
                    <version>1</version>
                    <dependencies>
                        <dependency>
                            <groupId>com.google.guava</groupId>
                            <artifactId>guava</artifactId>
                            <version>33.0.0-jre</version>
                        </dependency>
                    </dependencies>
                </project>
                """
            )
          )
        );
    }

    @Test
    void doesNotAddLombokWhenNotPresent() {
        rewriteRun(
          mavenProject("project",
            pomXml(
              //language=xml
              """
                <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <dependencies>
                        <dependency>
                            <groupId>com.google.guava</groupId>
                            <artifactId>guava</artifactId>
                            <version>33.0.0-jre</version>
                        </dependency>
                    </dependencies>
                </project>
                """
            )
          )
        );
    }
}
