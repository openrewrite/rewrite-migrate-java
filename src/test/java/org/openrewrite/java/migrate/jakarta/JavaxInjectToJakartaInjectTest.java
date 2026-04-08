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
package org.openrewrite.java.migrate.jakarta;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.java.Assertions.srcMainJava;
import static org.openrewrite.maven.Assertions.pomXml;

class JavaxInjectToJakartaInjectTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResource(
          "/META-INF/rewrite/jakarta-ee-9.yml",
          "org.openrewrite.java.migrate.jakarta.JavaxInjectMigrationToJakartaInject");
    }

    @DocumentExample
    @Test
    void projectWithJavaxInject() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>demo</artifactId>
                  <version>0.0.1-SNAPSHOT</version>
                  <dependencies>
                      <dependency>
                            <groupId>javax.inject</groupId>
                            <artifactId>javax.inject</artifactId>
                            <version>1</version>
                      </dependency>
                  </dependencies>
              </project>
              """,
            spec -> spec.after(actual -> assertThat(actual)
              .contains("<groupId>jakarta.inject</groupId>")
              .contains("<artifactId>jakarta.inject-api</artifactId>")
              .containsPattern("<version>\\d+\\.\\d+\\.\\d+.*</version>")
              .actual())
          )
        );
    }

    @Test
    void addsJakartaInjectApiDependencyWhenNoExplicitInjectDependencyDeclared() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion()
            .classpathFromResources(new InMemoryExecutionContext(), "javax.inject-1")),
          mavenProject(
            "Sample",
            srcMainJava(
              //language=java
              java(
                """
                  import javax.inject.Inject;
                  public class TestApplication {
                      @Inject
                      private String name;
                  }
                  """,
                """
                  import jakarta.inject.Inject;
                  public class TestApplication {
                      @Inject
                      private String name;
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
                  return assertThat( pom )
                          .containsPattern(
                                  "<groupId>jakarta\\.inject</groupId>\\s*" +
                                          "<artifactId>jakarta\\.inject-api</artifactId>\\s*" +
                                          "<version>2\\.0\\.\\d+</version>" ).actual();
              })
            )
          )
        );
    }
}
