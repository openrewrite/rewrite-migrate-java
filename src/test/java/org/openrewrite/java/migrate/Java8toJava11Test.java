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
import org.openrewrite.DocumentExample;
import org.openrewrite.config.Environment;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.java.Assertions.version;
import static org.openrewrite.maven.Assertions.pomXml;

class Java8toJava11Test implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(Environment.builder()
          .scanRuntimeClasspath("org.openrewrite.java.migrate")
          .build()
          .activateRecipes("org.openrewrite.java.migrate.UpgradePluginsForJava11"));
    }

    @DocumentExample
    @Test
    void needToJaxb2MavenPlugin() {
        rewriteRun(
          version(
            mavenProject("project",
              //language=xml
              pomXml(
                """
                  <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <build>
                      <plugins>
                        <plugin>
                          <groupId>org.codehaus.mojo</groupId>
                          <artifactId>jaxb2-maven-plugin</artifactId>
                          <version>2.3.1</version>
                        </plugin>
                      </plugins>
                    </build>
                  </project>
                  """,
                after -> after.after(pomXml -> pomXml)
                  .afterRecipe(doc -> assertThat(doc.getMarkers().findFirst(MavenResolutionResult.class))
                    .hasValueSatisfying(mrr -> mrr.getPom().getPlugins().getFirst().getVersion().startsWith("2.5.")))
              )
            ),
            8)
        );
    }

    @Test
    void compilerPluginVersionAddedWhenMissing() {
        rewriteRun(
          version(
            mavenProject("project",
              //language=xml
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
                          <artifactId>maven-compiler-plugin</artifactId>
                          <configuration>
                            <source>1.5</source>
                            <target>1.5</target>
                          </configuration>
                        </plugin>
                      </plugins>
                    </build>
                  </project>
                  """,
                spec -> spec.after(pomXml -> {
                    assertThat(pomXml).contains("<version>3.");
                    return pomXml;
                })
              )
            ),
            8)
        );
    }

    @Test
    void compilerPluginVersionAddedAndReleaseSetForFullMigration() {
        rewriteRun(
          spec -> spec.recipe(Environment.builder()
            .scanRuntimeClasspath("org.openrewrite.java.migrate")
            .build()
            .activateRecipes("org.openrewrite.java.migrate.Java8toJava11")),
          version(
            mavenProject("project",
              //language=xml
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
                          <artifactId>maven-compiler-plugin</artifactId>
                          <configuration>
                            <source>1.5</source>
                            <target>1.5</target>
                          </configuration>
                        </plugin>
                      </plugins>
                    </build>
                  </project>
                  """,
                spec -> spec.after(pomXml -> {
                    assertThat(pomXml)
                      .contains("<release>11</release>")
                      .contains("<version>3.")
                      .doesNotContain("<source>")
                      .doesNotContain("<target>");
                    return pomXml;
                })
              )
            ),
            8)
        );
    }

    @Test
    void noChangeOnCorrectJaxb2MavenPluginVersion() {
        rewriteRun(
          version(
            mavenProject("project",
              //language=xml
              pomXml(
                """
                  <project>
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <properties>
                      <maven.compiler.release>11</maven.compiler.release>
                    </properties>
                    <build>
                      <plugins>
                        <plugin>
                          <groupId>org.codehaus.mojo</groupId>
                          <artifactId>jaxb2-maven-plugin</artifactId>
                          <version>2.5.0</version>
                        </plugin>
                      </plugins>
                    </build>
                  </project>
                  """
              )
            ),
            11)
        );
    }
}
