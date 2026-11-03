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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.xml.tree.Xml;

import static java.util.function.UnaryOperator.identity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.maven.Assertions.pomXml;

class BouncyCastleMultiModuleMigrationTest implements RewriteTest {
    @ParameterizedTest
    @CsvSource({
      "8,bcprov,jdk18on",
      "8,bcpkix,jdk18on",
      "7,bcprov,jdk15to18",
      "7,bcpkix,jdk15to18"
    })
    void keepManagedAndVersionlessChildCoordinatesTogether(int javaVersion, String artifact, String target) {
        rewriteRun(
          spec -> spec.recipeFromResources("org.openrewrite.java.migrate.UpgradeToJava" + javaVersion),
          mavenProject("parent",
            pomXml(
              """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>parent</artifactId>
                  <version>1</version>
                  <packaging>pom</packaging>
                  <modules><module>child</module></modules>
                  <properties><maven.compiler.release>11</maven.compiler.release></properties>
                  <dependencyManagement>
                    <dependencies>
                      <dependency>
                        <groupId>org.bouncycastle</groupId>
                        <artifactId>%s-jdk15on</artifactId>
                        <version>1.70</version>
                      </dependency>
                    </dependencies>
                  </dependencyManagement>
                </project>
                """.formatted(artifact),
              spec -> spec.after(identity()).afterRecipe(doc -> {
                  Xml.Tag dependency = doc.getRoot().getChild("dependencyManagement").orElseThrow()
                    .getChild("dependencies").orElseThrow().getChild("dependency").orElseThrow();
                  assertThat(dependency.getChildValue("artifactId")).contains(artifact + "-" + target);
                  assertThat(dependency.getChildValue("version")).isPresent();
              })
            ),
            mavenProject("child",
              pomXml(
                """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                      <groupId>com.example</groupId>
                      <artifactId>parent</artifactId>
                      <version>1</version>
                    </parent>
                    <artifactId>child</artifactId>
                    <dependencies>
                      <dependency>
                        <groupId>org.bouncycastle</groupId>
                        <artifactId>%s-jdk15on</artifactId>
                      </dependency>
                    </dependencies>
                  </project>
                  """.formatted(artifact),
                spec -> spec.after(identity()).afterRecipe(doc -> {
                    Xml.Tag dependency = doc.getRoot().getChild("dependencies").orElseThrow()
                      .getChild("dependency").orElseThrow();
                    assertThat(dependency.getChildValue("artifactId")).contains(artifact + "-" + target);
                    assertThat(dependency.getChild("version")).isEmpty();
                })
              )
            )
          )
        );
    }
}
