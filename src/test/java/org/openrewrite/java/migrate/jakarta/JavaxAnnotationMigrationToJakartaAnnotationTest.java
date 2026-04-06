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
import org.openrewrite.java.JavaParser;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;
import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.java.Assertions.srcMainJava;
import static org.openrewrite.maven.Assertions.pomXml;

class JavaxAnnotationMigrationToJakartaAnnotationTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResource(
          "/META-INF/rewrite/jakarta-ee-9.yml",
          "org.openrewrite.java.migrate.jakarta.JavaxAnnotationMigrationToJakartaAnnotation");
    }

    @DocumentExample
    @Test
    void migratesExplicitDependency() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion()
            .dependsOn("package javax.annotation; public @interface Nonnull {}")),
          mavenProject("my-project",
            srcMainJava(
              //language=java
              java(
                """
                  import javax.annotation.Nonnull;

                  class A {
                      @Nonnull
                      String name;
                  }
                  """,
                """
                  import jakarta.annotation.Nonnull;

                  class A {
                      @Nonnull
                      String name;
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
                            <groupId>javax.annotation</groupId>
                            <artifactId>javax.annotation-api</artifactId>
                            <version>1.3.2</version>
                        </dependency>
                    </dependencies>
                </project>
                """,
              spec -> spec.after(pom -> assertThat(pom)
                    .contains("<groupId>jakarta.annotation</groupId>")
                    .contains("<artifactId>jakarta.annotation-api</artifactId>")
                    .containsPattern("<version>2\\.0\\.\\d+</version>")
                    .actual())
            )
          )
        );
    }

    @Test
    void addsDependencyWhenNoExplicitAnnotationDependency() {
        rewriteRun(
          spec -> spec.parser(JavaParser.fromJavaVersion()
            .dependsOn("package javax.annotation; public @interface Nonnull {}")),
          mavenProject("my-project",
            srcMainJava(
              //language=java
              java(
                """
                  import javax.annotation.Nonnull;

                  class A {
                      @Nonnull
                      String name;
                  }
                  """,
                """
                  import jakarta.annotation.Nonnull;

                  class A {
                      @Nonnull
                      String name;
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
                </project>
                """,
              spec -> spec.after(pom -> assertThat(pom)
                    .containsPattern(
                      "<groupId>jakarta\\.annotation</groupId>\\s*" +
                      "<artifactId>jakarta\\.annotation-api</artifactId>\\s*" +
                      "<version>2\\.0\\.\\d+</version>")
                    .actual())
            )
          )
        );
    }
}
