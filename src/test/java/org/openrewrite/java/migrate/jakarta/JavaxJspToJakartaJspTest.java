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
package org.openrewrite.java.migrate.jakarta;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.toolingapi.Assertions.withToolingApi;
import static org.openrewrite.java.Assertions.*;
import static org.openrewrite.maven.Assertions.pomXml;

class JavaxJspToJakartaJspTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion()
            .classpathFromResources(new InMemoryExecutionContext(), "javax.servlet.jsp-api-2.3.3"))
          .recipeFromResource(
            "/META-INF/rewrite/jakarta-ee-9.yml",
            "org.openrewrite.java.migrate.jakarta.JavaxJspToJakartaJsp");
    }

    @DocumentExample
    @Test
    void switchesJavaxJspApiDependencyToJakartaJspApiDependency() {
        rewriteRun(
          mavenProject(
            "Sample",
            //language=xml
            pomXml(
              """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>demo</artifactId>
                    <version>0.0.1-SNAPSHOT</version>
                    <dependencies>
                        <dependency>
                            <groupId>javax.servlet.jsp</groupId>
                            <artifactId>javax.servlet.jsp-api</artifactId>
                            <version>2.3.3</version>
                        </dependency>
                    </dependencies>
                </project>
                """,
              spec -> spec.after(pom -> {
                  assertThat(pom)
                    .doesNotContain("javax.servlet.jsp")
                    .containsPattern(
                      "<groupId>jakarta\\.servlet\\.jsp</groupId>\\s*" +
                      "<artifactId>jakarta\\.servlet\\.jsp-api</artifactId>\\s*" +
                      "<version>3\\.0\\.\\d+</version>");
                  return pom;
              })
            )
          ),
          srcMainJava(
            //language=java
            java(
              """
                import javax.servlet.jsp.PageContext;
                public class TestApplication {
                }
                """,
              """
                import jakarta.servlet.jsp.PageContext;
                public class TestApplication {
                }
                """
            )
          )
        );
    }

    @Test
    void addsJakartaJspApiDependencyIfJavaxJspApiOnlyExistsInTransitive() {
        rewriteRun(
          mavenProject(
            "Sample",
            srcMainJava(
              //language=java
              java(
                """
                  import javax.servlet.jsp.PageContext;
                  public class TestApplication {
                  }
                  """,
                """
                  import jakarta.servlet.jsp.PageContext;
                  public class TestApplication {
                  }
                  """
              )
            ),
            //language=xml
            pomXml(
              """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>demo</artifactId>
                    <version>0.0.1-SNAPSHOT</version>
                    <dependencies>
                        <dependency>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot</artifactId>
                            <version>2.1.9.RELEASE</version>
                        </dependency>
                    </dependencies>
                </project>
                """,
              spec -> spec.after(pom -> {
                  assertThat(pom)
                    .containsPattern(
                      "<groupId>jakarta\\.servlet\\.jsp</groupId>\\s*" +
                      "<artifactId>jakarta\\.servlet\\.jsp-api</artifactId>\\s*" +
                      "<version>3\\.0\\.\\d+</version>");
                  return pom;
              })
            )
          )
        );
    }

    @Test
    void upgradesJakartaJspApiDependencyIfAlreadyExistingAtLowerVersion() {
        rewriteRun(
          mavenProject(
            "Sample",
            //language=xml
            pomXml(
              """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>demo</artifactId>
                    <version>0.0.1-SNAPSHOT</version>
                    <dependencies>
                        <dependency>
                            <groupId>jakarta.servlet.jsp</groupId>
                            <artifactId>jakarta.servlet.jsp-api</artifactId>
                            <version>2.3.6</version>
                        </dependency>
                    </dependencies>
                </project>
                """,
              spec -> spec.after(pom -> {
                  assertThat(pom)
                    .containsPattern(
                      "<groupId>jakarta\\.servlet\\.jsp</groupId>\\s*" +
                      "<artifactId>jakarta\\.servlet\\.jsp-api</artifactId>\\s*" +
                      "<version>3\\.0\\.\\d+</version>");
                  return pom;
              })
            )
          )
        );
    }

    @Test
    void addsJakartaJspApiDependencyInGradleIfJavaxJspApiOnlyExistsInTransitive() {
        rewriteRun(
          spec -> spec.beforeRecipe(withToolingApi()),
          mavenProject(
            "Sample",
            srcMainJava(
              //language=java
              java(
                """
                  import javax.servlet.jsp.PageContext;
                  public class TestApplication {
                  }
                  """,
                """
                  import jakarta.servlet.jsp.PageContext;
                  public class TestApplication {
                  }
                  """
              )
            ),
            //language=groovy
            buildGradle(
              """
                plugins {
                    id "java-library"
                }

                repositories {
                    mavenCentral()
                }
                """,
              spec -> spec.after(gradle -> {
                  assertThat(gradle)
                    .contains("jakarta.servlet.jsp:jakarta.servlet.jsp-api:3.0.");
                  return gradle;
              })
            )
          )
        );
    }

    @Test
    void noChangeIfAlreadyOnJakartaJspApi() {
        rewriteRun(
          mavenProject(
            "Sample",
            srcMainJava(
              //language=java
              java(
                """
                  import jakarta.servlet.jsp.PageContext;
                  public class TestApplication {
                  }
                  """
              )
            ),
            //language=xml
            pomXml(
              """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>demo</artifactId>
                    <version>0.0.1-SNAPSHOT</version>
                    <dependencies>
                        <dependency>
                            <groupId>jakarta.servlet.jsp</groupId>
                            <artifactId>jakarta.servlet.jsp-api</artifactId>
                            <version>3.0.0</version>
                        </dependency>
                    </dependencies>
                </project>
                """
            )
          )
        );
    }
}
