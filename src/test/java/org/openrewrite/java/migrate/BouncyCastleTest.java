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
package org.openrewrite.java.migrate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openrewrite.DocumentExample;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.Scope;
import org.openrewrite.test.RewriteTest;

import java.util.Arrays;
import java.util.List;

import static java.util.function.UnaryOperator.identity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.maven.Assertions.pomXml;

class BouncyCastleTest implements RewriteTest {

    static List<String> artifactBaseNames() {
        return Arrays.asList("bcprov", "bcutil", "bcpkix", "bcmail", "bcjmail", "bcpg", "bctls");
    }

    @DocumentExample
    @Test
    void document() {
        rewriteRun(
          spec -> spec.recipeFromResource(
            "/META-INF/rewrite/bouncycastle-jdk18on.yml",
            "org.openrewrite.java.migrate.BounceCastleFromJdk15OntoJdk18On"),
          mavenProject("project",
            //language=xml
            pomXml(
              """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <dependencies>
                    <dependency>
                      <groupId>org.bouncycastle</groupId>
                      <artifactId>bcprov-jdk15on</artifactId>
                      <version>1.70</version>
                    </dependency>
                    <dependency>
                      <groupId>org.bouncycastle</groupId>
                      <artifactId>bcpkix-jdk15on</artifactId>
                      <version>1.70</version>
                    </dependency>
                  </dependencies>
                </project>
                """,
              spec -> spec
                .after(identity())
                .afterRecipe(doc -> assertThat(doc.getMarkers().findFirst(MavenResolutionResult.class)
                  .get().getDependencies().get(Scope.Compile))
                  .filteredOn(rd -> rd.getDepth() == 0)
                  .satisfiesExactlyInAnyOrder(
                    rd -> {
                        assertThat(rd.getGroupId()).isEqualTo("org.bouncycastle");
                        assertThat(rd.getArtifactId()).isEqualTo("bcprov-jdk18on");
                    },
                    rd -> {
                        assertThat(rd.getGroupId()).isEqualTo("org.bouncycastle");
                        assertThat(rd.getArtifactId()).isEqualTo("bcpkix-jdk18on");
                    }))
            )
          )
        );
    }

    @MethodSource("artifactBaseNames")
    @ParameterizedTest
    void jdk15onToJdk18on(String artifactBaseName) {
        runBouncyCastleArtifactUpgradeRecipe(
          "/META-INF/rewrite/bouncycastle-jdk18on.yml",
          "org.openrewrite.java.migrate.BounceCastleFromJdk15OntoJdk18On",
          artifactBaseName,
          "jdk15on",
          "jdk18on"
        );
    }

    @MethodSource("artifactBaseNames")
    @ParameterizedTest
    void jdk15to18ToJdk18on(String artifactBaseName) {
        runBouncyCastleArtifactUpgradeRecipe(
          "/META-INF/rewrite/bouncycastle-jdk18on.yml",
          "org.openrewrite.java.migrate.BounceCastleFromJdk15OntoJdk18On",
          artifactBaseName,
          "jdk15to18",
          "jdk18on"
        );
    }

    @MethodSource("artifactBaseNames")
    @ParameterizedTest
    void jdk15onToJdk15To18(String artifactBaseName) {
        runBouncyCastleArtifactUpgradeRecipe(
          "/META-INF/rewrite/bouncycastle-jdk15to18.yml",
          "org.openrewrite.java.migrate.BouncyCastleFromJdk15OnToJdk15to18",
          artifactBaseName,
          "jdk15on",
          "jdk15to18"
        );
    }

    void runBouncyCastleArtifactUpgradeRecipe(
      String yamlFile,
      String recipe,
      String baseArtifactId,
      String originalArtifactSuffix,
      String expectedArtifactSuffix) {
        rewriteRun(
          recipeSpec -> recipeSpec.recipeFromResource(yamlFile, recipe),
          mavenProject("project",
            //language=xml
            pomXml(
              """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <dependencies>
                    <dependency>
                      <groupId>org.bouncycastle</groupId>
                      <artifactId>%s-%s</artifactId>
                      <version>1.70</version>
                    </dependency>
                  </dependencies>
                </project>
                """.formatted(baseArtifactId, originalArtifactSuffix),
              spec -> spec
                .after(identity())
                .afterRecipe(doc -> assertThat(doc.getMarkers().findFirst(MavenResolutionResult.class)
                  .get().getDependencies().get(Scope.Compile))
                  .filteredOn(rd -> rd.getDepth() == 0)
                  .singleElement()
                  .satisfies(rd -> {
                      assertThat(rd.getGroupId()).isEqualTo("org.bouncycastle");
                      assertThat(rd.getArtifactId()).isEqualTo(String.format("%s-%s", baseArtifactId, expectedArtifactSuffix));
                  }))
            )
          )
        );
    }
}
