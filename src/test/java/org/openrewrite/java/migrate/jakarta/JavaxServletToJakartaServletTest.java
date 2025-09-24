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
package org.openrewrite.java.migrate.jakarta;

import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.*;
import static org.openrewrite.maven.Assertions.pomXml;

class JavaxServletToJakartaServletTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion()
            .classpathFromResources(new InMemoryExecutionContext(), "javax.servlet-api-4.0.1"))
          .recipeFromResource(
            "/META-INF/rewrite/jakarta-ee-9.yml",
            "org.openrewrite.java.migrate.jakarta.JavaxServletToJakartaServlet");
    }

    @Test
    void switchesJavaxServletApiDependencyToJakartaServletApiDependency() {
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
                            <groupId>javax.servlet</groupId>
                            <artifactId>javax.servlet-api</artifactId>
                            <version>4.0.1</version>
                        </dependency>
                    </dependencies>
                </project>
                """,
              spec -> spec.after(pom -> assertThat(pom)
                .contains("<groupId>jakarta.servlet</groupId>")
                .contains("<artifactId>jakarta.servlet-api</artifactId>")
                .containsPattern("<version>5.0.\\d+</version>")
                .doesNotContain("<groupId>javax.servlet</groupId>")
                .doesNotContain("<artifactId>javax.servlet-api</artifactId>")
                .actual())
            )
          ),
          srcMainJava(
            //language=java
            java(
              """
                import javax.servlet.Filter;
                public class TestApplication {
                }
                """,
              """
                import jakarta.servlet.Filter;
                public class TestApplication {
                }
                """
            )
          )
        );
    }

    @Test
    void addsJakartaServletApiDependencyIfJavaxServletApiOnlyExistsInTransitive() {
        rewriteRun(
          mavenProject(
            "Sample",
            srcMainJava(
              //language=java
              java(
                """
                  import javax.servlet.Filter;
                  public class TestApplication {
                  }
                  """,
                """
                  import jakarta.servlet.Filter;
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
              spec -> spec.after(pom -> assertThat(pom)
                .contains("<groupId>jakarta.servlet</groupId>")
                .contains("<artifactId>jakarta.servlet-api</artifactId>")
                .containsPattern("<version>5.0.\\d+</version>")
                .actual())
            )
          )
        );
    }

    @Test
    void upgradesJakartaServletApiDependencyIfAlreadyExistingAtLowerVersion() {
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
                            <groupId>jakarta.servlet</groupId>
                            <artifactId>jakarta.servlet-api</artifactId>
                            <version>4.0.4</version>
                        </dependency>
                    </dependencies>
                </project>
                """,
              spec -> spec.after(pom -> assertThat(pom)
                .containsPattern("<version>5.0.\\d+</version>")
                .actual())
            )
          )
        );
    }

    @Test
    void ignoresJakartaServletApiDependencyIfAlreadyExisting() {
        rewriteRun(
          mavenProject(
            "Sample",
            srcMainJava(
              //language=java
              java(
                """
                  import javax.servlet.Filter;
                  public class TestApplication {
                  }
                  """,
                """
                  import jakarta.servlet.Filter;
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
                            <groupId>jakarta.servlet</groupId>
                            <artifactId>jakarta.servlet-api</artifactId>
                            <version>5.0.0</version>
                        </dependency>
                    </dependencies>
                </project>
                """
            )
          )
        );
    }
}
