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
package org.openrewrite.java.migrate.lombok;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.maven.Assertions.pomXml;

class LombokBestPracticesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResource("/META-INF/rewrite/lombok.yml",
          "org.openrewrite.java.migrate.lombok.LombokBestPractices");
    }

    @DocumentExample
    @Test
    void providedScope() {
        rewriteRun(
          pomXml(
            //language=xml
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>example</artifactId>
                  <version>1.0.0</version>
                  <dependencies>
                      <dependency>
                          <groupId>org.projectlombok</groupId>
                          <artifactId>lombok</artifactId>
                          <version>1.18.6</version>
                      </dependency>
                      <dependency>
                          <groupId>org.projectlombok</groupId>
                          <artifactId>lombok-mapstruct-binding</artifactId>
                          <version>0.2.0</version>
                      </dependency>
                  </dependencies>
              </project>
              """,
            spec -> spec.after(pom -> {
                Matcher version = Pattern.compile("1.[1-9]\\d+(.\\d+)?").matcher(pom);
                assertThat(version.find()).isTrue();
                //language=xml
                return """
                  <project>
                      <modelVersion>4.0.0</modelVersion>
                      <groupId>com.example</groupId>
                      <artifactId>example</artifactId>
                      <version>1.0.0</version>
                      <dependencies>
                          <dependency>
                              <groupId>org.projectlombok</groupId>
                              <artifactId>lombok</artifactId>
                              <version>%s</version>
                              <scope>provided</scope>
                          </dependency>
                          <dependency>
                              <groupId>org.projectlombok</groupId>
                              <artifactId>lombok-mapstruct-binding</artifactId>
                              <version>0.2.0</version>
                              <scope>provided</scope>
                          </dependency>
                      </dependencies>
                  </project>
                  """.formatted(version.group(0));
            })
          )
        );
    }

    @Test
    void excludeTransitiveLombok() {
        //language=xml
        rewriteRun(
          mavenProject(
            "parent",
            pomXml(
              """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>com.example</groupId>
                    <artifactId>parent</artifactId>
                    <version>1.0.0</version>
                    <modules>
                        <module>library</module>
                        <module>project</module>
                    </modules>
                </project>
                """
            ),
            mavenProject("library",
              pomXml(
                """
                  <project>
                      <modelVersion>4.0.0</modelVersion>
                      <parent>
                        <groupId>com.example</groupId>
                        <artifactId>parent</artifactId>
                        <version>1.0.0</version>
                      </parent>
                      <artifactId>library</artifactId>
                      <dependencies>
                          <dependency>
                              <groupId>org.projectlombok</groupId>
                              <artifactId>lombok</artifactId>
                              <version>1.18.6</version>
                          </dependency>
                      </dependencies>
                  </project>
                  """,
                SourceSpec::skip // Keep as is, such that we trigger exclude below
              )
            ),
            mavenProject("project",
              pomXml(
                """
                  <project>
                      <modelVersion>4.0.0</modelVersion>
                      <parent>
                        <groupId>com.example</groupId>
                        <artifactId>parent</artifactId>
                        <version>1.0.0</version>
                      </parent>
                      <artifactId>project</artifactId>
                      <dependencies>
                          <dependency>
                              <groupId>com.example</groupId>
                              <artifactId>library</artifactId>
                              <version>1.0.0</version>
                          </dependency>
                      </dependencies>
                  </project>
                  """,
                """
                  <project>
                      <modelVersion>4.0.0</modelVersion>
                      <parent>
                        <groupId>com.example</groupId>
                        <artifactId>parent</artifactId>
                        <version>1.0.0</version>
                      </parent>
                      <artifactId>project</artifactId>
                      <dependencies>
                          <dependency>
                              <groupId>com.example</groupId>
                              <artifactId>library</artifactId>
                              <version>1.0.0</version>
                              <exclusions>
                                  <exclusion>
                                      <groupId>org.projectlombok</groupId>
                                      <artifactId>lombok</artifactId>
                                  </exclusion>
                              </exclusions>
                          </dependency>
                      </dependencies>
                  </project>
                  """
              )
            )
          )
        );
    }
}
