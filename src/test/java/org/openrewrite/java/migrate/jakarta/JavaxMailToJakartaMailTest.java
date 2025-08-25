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
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.*;
import static org.openrewrite.maven.Assertions.pomXml;

class JavaxMailToJakartaMailTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion()
            .classpath("mail-1.4.7", "javax.mail-api-1.6.2"))
          .recipeFromResource(
            "/META-INF/rewrite/jakarta-ee-9.yml",
            "org.openrewrite.java.migrate.jakarta.JavaxMailToJakartaMail");
    }

    @Test
    void switchesJavaxMailApiDependencyToJakartaMailApiDependency() {
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
                            <groupId>javax.mail</groupId>
                            <artifactId>javax.mail-api</artifactId>
                            <version>1.4.7</version>
                        </dependency>
                    </dependencies>
                </project>
                """,
              spec -> spec.after(pom -> assertThat(pom)
                .contains("<groupId>jakarta.mail</groupId>")
                .contains("<artifactId>jakarta.mail-api</artifactId>")
                .containsPattern("<version>2.0.\\d+</version>")
                .doesNotContain("<groupId>javax.mail</groupId>")
                .doesNotContain("<artifactId>javax.mail-api</artifactId>")
                .actual())
            )
          ),
          srcMainJava(
            //language=java
            java(
              """
                import javax.mail.Session;
                public class TestApplication {
                }
                """,
              """
                import jakarta.mail.Session;
                public class TestApplication {
                }
                """
            )
          )
        );
    }

    @Test
    void switchesJavaxMailDependencyToJakartaMailApiDependency() {
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
                            <groupId>javax.mail</groupId>
                            <artifactId>mail</artifactId>
                            <version>1.3.3</version>
                        </dependency>
                    </dependencies>
                </project>
                """,
              spec -> spec.after(pom -> assertThat(pom)
                .contains("<groupId>jakarta.mail</groupId>")
                .contains("<artifactId>jakarta.mail-api</artifactId>")
                .containsPattern("<version>2.0.\\d+</version>")
                .doesNotContain("<groupId>javax.mail</groupId>")
                .doesNotContain("<artifactId>mail</artifactId>")
                .actual())
            )
          ),
          srcMainJava(
            //language=java
            java(
              """
                import javax.mail.Session;
                public class TestApplication {
                }
                """,
              """
                import jakarta.mail.Session;
                public class TestApplication {
                }
                """
            )
          )
        );
    }

    @Test
    void addsJakartaMailApiDependencyIfJavaxMailApiOnlyExistingInTransitive() {
        rewriteRun(
          mavenProject(
            "Sample",
            srcMainJava(
              //language=java
              java(
                """
                  import javax.mail.Session;
                  public class TestApplication {
                  }
                  """,
                """
                  import jakarta.mail.Session;
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
                            <artifactId>spring-boot-autoconfigure</artifactId>
                            <version>2.1.0.RELEASE</version>
                        </dependency>
                    </dependencies>
                </project>
                """,
              spec -> spec.after(pom -> assertThat(pom)
                .contains("<groupId>jakarta.mail</groupId>")
                .contains("<artifactId>jakarta.mail-api</artifactId>")
                .containsPattern("<version>2.0.\\d+</version>")
                .actual())
            )
          )
        );
    }

    @Test
    void upgradesJakartaMailApiDependencyIfAlreadyExistingAtALowerVersion() {
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
                            <groupId>jakarta.mail</groupId>
                            <artifactId>jakarta.mail-api</artifactId>
                            <version>1.6.8</version>
                        </dependency>
                    </dependencies>
                </project>
                """,
              spec -> spec.after(pom -> assertThat(pom)
                .containsPattern("<version>2.0.\\d+</version>")
                .actual())
            )
          )
        );
    }

    @Test
    void ignoresJakartaMailApiDependencyIfAlreadyExisting() {
        rewriteRun(
          mavenProject(
            "Sample",
            srcMainJava(
              //language=java
              java(
                """
                  import javax.mail.Session;
                  public class TestApplication {
                  }
                  """,
                """
                  import jakarta.mail.Session;
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
                            <groupId>jakarta.mail</groupId>
                            <artifactId>jakarta.mail-api</artifactId>
                            <version>2.0.2</version>
                        </dependency>
                    </dependencies>
                </project>
                """
            )
          )
        );
    }
}
