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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.regex.Pattern;

import static org.openrewrite.maven.Assertions.pomXml;

class UpdateJakartaFacesApiTest {

    @Nested
    class Faces3 implements RewriteTest {
        @Override
        public void defaults(RecipeSpec spec) {
            spec.recipeFromResources("org.openrewrite.java.migrate.jakarta.UpdateJakartaFacesApi3");
        }

        @DocumentExample
        @Test
        void migrateSunFacesJsfApi() {
            rewriteRun(
              //language=xml
              pomXml(
                """
                  <project>
                      <groupId>com.example</groupId>
                      <artifactId>jsf-app</artifactId>
                      <version>1.0.0</version>
                      <dependencies>
                          <dependency>
                              <groupId>com.sun.faces</groupId>
                              <artifactId>jsf-api</artifactId>
                              <version>2.2.20</version>
                          </dependency>
                      </dependencies>
                  </project>
                  """,
                spec -> spec.after(actual -> {
                    String version = Pattern.compile("<version>(3\\.0\\.\\d+)</version>")
                      .matcher(actual).results().reduce((a, b) -> b).orElseThrow().group(1);
                    return """
                      <project>
                          <groupId>com.example</groupId>
                          <artifactId>jsf-app</artifactId>
                          <version>1.0.0</version>
                          <dependencies>
                              <dependency>
                                  <groupId>jakarta.faces</groupId>
                                  <artifactId>jakarta.faces-api</artifactId>
                                  <version>%s</version>
                              </dependency>
                          </dependencies>
                      </project>
                      """.formatted(version);
                })
              )
            );
        }

        @Test
        void migrateSunFacesJsfImpl() {
            rewriteRun(
              //language=xml
              pomXml(
                """
                  <project>
                      <groupId>com.example</groupId>
                      <artifactId>jsf-app</artifactId>
                      <version>1.0.0</version>
                      <dependencies>
                          <dependency>
                              <groupId>com.sun.faces</groupId>
                              <artifactId>jsf-impl</artifactId>
                              <version>2.2.20</version>
                          </dependency>
                      </dependencies>
                  </project>
                  """,
                spec -> spec.after(actual -> {
                    String version = Pattern.compile("<version>(3\\.0\\.\\d+)</version>")
                      .matcher(actual).results().reduce((a, b) -> b).orElseThrow().group(1);
                    return """
                      <project>
                          <groupId>com.example</groupId>
                          <artifactId>jsf-app</artifactId>
                          <version>1.0.0</version>
                          <dependencies>
                              <dependency>
                                  <groupId>org.glassfish</groupId>
                                  <artifactId>jakarta.faces</artifactId>
                                  <version>%s</version>
                              </dependency>
                          </dependencies>
                      </project>
                      """.formatted(version);
                })
              )
            );
        }

        @Test
        void migrateGlassfishJavaxFaces() {
            rewriteRun(
              //language=xml
              pomXml(
                """
                  <project>
                      <groupId>com.example</groupId>
                      <artifactId>jsf-app</artifactId>
                      <version>1.0.0</version>
                      <dependencies>
                          <dependency>
                              <groupId>org.glassfish</groupId>
                              <artifactId>javax.faces</artifactId>
                              <version>2.3.9</version>
                          </dependency>
                      </dependencies>
                  </project>
                  """,
                spec -> spec.after(actual -> {
                    String version = Pattern.compile("<version>(3\\.0\\.\\d+)</version>")
                      .matcher(actual).results().reduce((a, b) -> b).orElseThrow().group(1);
                    return """
                      <project>
                          <groupId>com.example</groupId>
                          <artifactId>jsf-app</artifactId>
                          <version>1.0.0</version>
                          <dependencies>
                              <dependency>
                                  <groupId>org.glassfish</groupId>
                                  <artifactId>jakarta.faces</artifactId>
                                  <version>%s</version>
                              </dependency>
                          </dependencies>
                      </project>
                      """.formatted(version);
                })
              )
            );
        }
    }

    @Nested
    class Faces4 implements RewriteTest {
        @Override
        public void defaults(RecipeSpec spec) {
            spec.recipeFromResources("org.openrewrite.java.migrate.jakarta.UpdateJakartaFacesApi4");
        }

        @Test
        void upgradeGlassfishJakartaFaces() {
            rewriteRun(
              //language=xml
              pomXml(
                """
                  <project>
                      <groupId>com.example</groupId>
                      <artifactId>jsf-app</artifactId>
                      <version>1.0.0</version>
                      <dependencies>
                          <dependency>
                              <groupId>org.glassfish</groupId>
                              <artifactId>jakarta.faces</artifactId>
                              <version>3.0.3</version>
                          </dependency>
                      </dependencies>
                  </project>
                  """,
                spec -> spec.after(actual -> {
                    String version = Pattern.compile("<version>(4\\.0\\.\\d+)</version>")
                      .matcher(actual).results().reduce((a, b) -> b).orElseThrow().group(1);
                    return """
                      <project>
                          <groupId>com.example</groupId>
                          <artifactId>jsf-app</artifactId>
                          <version>1.0.0</version>
                          <dependencies>
                              <dependency>
                                  <groupId>org.glassfish</groupId>
                                  <artifactId>jakarta.faces</artifactId>
                                  <version>%s</version>
                              </dependency>
                          </dependencies>
                      </project>
                      """.formatted(version);
                })
              )
            );
        }
    }
}
