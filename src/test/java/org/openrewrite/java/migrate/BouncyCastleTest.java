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
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.DocumentExample;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.Scope;
import org.openrewrite.test.RewriteTest;

import static java.util.function.UnaryOperator.identity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.maven.Assertions.pomXml;

class BouncyCastleTest implements RewriteTest {

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

    @ValueSource(strings = {"bcprov", "bcprov-ext", "bcutil", "bcpkix", "bcmail", "bcjmail", "bcpg", "bctls"})
    @ParameterizedTest
    void jdk15onToJdk18on(String artifactBaseName) {
        runBouncyCastleArtifactUpgradeRecipe(
          "/META-INF/rewrite/bouncycastle-jdk18on.yml",
          "org.openrewrite.java.migrate.BounceCastleFromJdk15OntoJdk18On",
          artifactBaseName,
          "jdk15on",
          "1.70",
          "jdk18on"
        );
    }

    @ValueSource(strings = {"bcprov", "bcprov-ext", "bcutil", "bcpkix", "bcmail", "bcjmail", "bcpg", "bctls"})
    @ParameterizedTest
    void jdk15to18ToJdk18on(String artifactBaseName) {
        runBouncyCastleArtifactUpgradeRecipe(
          "/META-INF/rewrite/bouncycastle-jdk18on.yml",
          "org.openrewrite.java.migrate.BounceCastleFromJdk15OntoJdk18On",
          artifactBaseName,
          "jdk15to18",
          "1.70",
          "jdk18on"
        );
    }

    @ValueSource(strings = {"bcprov", "bcprov-ext", "bcutil", "bcpkix", "bcmail", "bcpg", "bctls"})
    @ParameterizedTest
    void jdk14ToJdk18on(String artifactBaseName) {
        runBouncyCastleArtifactUpgradeRecipe(
          "/META-INF/rewrite/bouncycastle-jdk18on.yml",
          "org.openrewrite.java.migrate.BounceCastleFromJdk15OntoJdk18On",
          artifactBaseName,
          "jdk14",
          "1.70",
          "jdk18on"
        );
    }

    @ValueSource(strings = {"bcprov", "bcprov-ext", "bcmail", "bcpg"})
    @ParameterizedTest
    void jdk15ToJdk18on(String artifactBaseName) {
        runBouncyCastleArtifactUpgradeRecipe(
          "/META-INF/rewrite/bouncycastle-jdk18on.yml",
          "org.openrewrite.java.migrate.BounceCastleFromJdk15OntoJdk18On",
          artifactBaseName,
          "jdk15",
          "1.46",
          "jdk18on"
        );
    }

    @ValueSource(strings = {"bcprov", "bcmail", "bcpg"})
    @ParameterizedTest
    void jdk15PlusToJdk18on(String artifactBaseName) {
        runBouncyCastleArtifactUpgradeRecipe(
          "/META-INF/rewrite/bouncycastle-jdk18on.yml",
          "org.openrewrite.java.migrate.BounceCastleFromJdk15OntoJdk18On",
          artifactBaseName,
          "jdk15+",
          "1.46",
          "jdk18on"
        );
    }

    @ValueSource(strings = {"bcprov", "bcprov-ext", "bcmail", "bcpg"})
    @ParameterizedTest
    void jdk16ToJdk18on(String artifactBaseName) {
        runBouncyCastleArtifactUpgradeRecipe(
          "/META-INF/rewrite/bouncycastle-jdk18on.yml",
          "org.openrewrite.java.migrate.BounceCastleFromJdk15OntoJdk18On",
          artifactBaseName,
          "jdk16",
          "1.46",
          "jdk18on"
        );
    }

    @ValueSource(strings = {"bcprov", "bcpg"})
    @ParameterizedTest
    void jdk12ToJdk18on(String artifactBaseName) {
        runBouncyCastleArtifactUpgradeRecipe(
          "/META-INF/rewrite/bouncycastle-jdk18on.yml",
          "org.openrewrite.java.migrate.BounceCastleFromJdk15OntoJdk18On",
          artifactBaseName,
          "jdk12",
          "130",
          "jdk18on"
        );
    }

    @ValueSource(strings = {"bcprov", "bcprov-ext", "bcutil", "bcpkix", "bcmail", "bcjmail", "bcpg", "bctls"})
    @ParameterizedTest
    void jdk15onToJdk15To18(String artifactBaseName) {
        runBouncyCastleArtifactUpgradeRecipe(
          "/META-INF/rewrite/bouncycastle-jdk15to18.yml",
          "org.openrewrite.java.migrate.BouncyCastleFromJdk15OnToJdk15to18",
          artifactBaseName,
          "jdk15on",
          "1.70",
          "jdk15to18"
        );
    }

    @ValueSource(strings = {"bcprov", "bcprov-ext", "bcutil", "bcpkix", "bcmail", "bcpg", "bctls"})
    @ParameterizedTest
    void jdk14ToJdk15to18(String artifactBaseName) {
        runBouncyCastleArtifactUpgradeRecipe(
          "/META-INF/rewrite/bouncycastle-jdk15to18.yml",
          "org.openrewrite.java.migrate.BouncyCastleFromJdk15OnToJdk15to18",
          artifactBaseName,
          "jdk14",
          "1.70",
          "jdk15to18"
        );
    }

    @ValueSource(strings = {"bcprov", "bcprov-ext", "bcmail", "bcpg"})
    @ParameterizedTest
    void jdk15ToJdk15to18(String artifactBaseName) {
        runBouncyCastleArtifactUpgradeRecipe(
          "/META-INF/rewrite/bouncycastle-jdk15to18.yml",
          "org.openrewrite.java.migrate.BouncyCastleFromJdk15OnToJdk15to18",
          artifactBaseName,
          "jdk15",
          "1.46",
          "jdk15to18"
        );
    }

    @ValueSource(strings = {"bcprov", "bcmail", "bcpg"})
    @ParameterizedTest
    void jdk15PlusToJdk15to18(String artifactBaseName) {
        runBouncyCastleArtifactUpgradeRecipe(
          "/META-INF/rewrite/bouncycastle-jdk15to18.yml",
          "org.openrewrite.java.migrate.BouncyCastleFromJdk15OnToJdk15to18",
          artifactBaseName,
          "jdk15+",
          "1.46",
          "jdk15to18"
        );
    }

    @ValueSource(strings = {"bcprov", "bcprov-ext", "bcmail", "bcpg"})
    @ParameterizedTest
    void jdk16ToJdk15to18(String artifactBaseName) {
        runBouncyCastleArtifactUpgradeRecipe(
          "/META-INF/rewrite/bouncycastle-jdk15to18.yml",
          "org.openrewrite.java.migrate.BouncyCastleFromJdk15OnToJdk15to18",
          artifactBaseName,
          "jdk16",
          "1.46",
          "jdk15to18"
        );
    }

    @ValueSource(strings = {"bcprov", "bcpg"})
    @ParameterizedTest
    void jdk12ToJdk15to18(String artifactBaseName) {
        runBouncyCastleArtifactUpgradeRecipe(
          "/META-INF/rewrite/bouncycastle-jdk15to18.yml",
          "org.openrewrite.java.migrate.BouncyCastleFromJdk15OnToJdk15to18",
          artifactBaseName,
          "jdk12",
          "130",
          "jdk15to18"
        );
    }

    @ValueSource(strings = {"jdk14", "jdk15", "jdk15+", "jdk15on", "jdk16"})
    @ParameterizedTest
    void bctspToBcpkixJdk18on(String originalArtifactSuffix) {
        runBouncyCastleDependencyChangeRecipe(
          "/META-INF/rewrite/bouncycastle-jdk18on.yml",
          "org.openrewrite.java.migrate.BounceCastleFromJdk15OntoJdk18On",
          "bctsp-" + originalArtifactSuffix,
          "1.46",
          "bcpkix-jdk18on"
        );
    }

    @ValueSource(strings = {"jdk14", "jdk15", "jdk15+", "jdk15on", "jdk16"})
    @ParameterizedTest
    void bctspToBcpkixJdk15to18(String originalArtifactSuffix) {
        runBouncyCastleDependencyChangeRecipe(
          "/META-INF/rewrite/bouncycastle-jdk15to18.yml",
          "org.openrewrite.java.migrate.BouncyCastleFromJdk15OnToJdk15to18",
          "bctsp-" + originalArtifactSuffix,
          "1.46",
          "bcpkix-jdk15to18"
        );
    }

    @Test
    void bctspAlongsideBcpkixCollapsesToASingleDependency() {
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
                      <artifactId>bctsp-jdk15on</artifactId>
                      <version>1.46</version>
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
                .after(actual -> {
                    assertThat(actual).containsOnlyOnce("<artifactId>bcpkix-jdk18on</artifactId>");
                    return actual;
                })
                .afterRecipe(doc -> assertThat(doc.getMarkers().findFirst(MavenResolutionResult.class)
                  .get().getDependencies().get(Scope.Compile))
                  .filteredOn(rd -> rd.getDepth() == 0)
                  .singleElement()
                  .satisfies(rd -> {
                      assertThat(rd.getGroupId()).isEqualTo("org.bouncycastle");
                      assertThat(rd.getArtifactId()).isEqualTo("bcpkix-jdk18on");
                  }))
            )
          )
        );
    }

    void runBouncyCastleArtifactUpgradeRecipe(
      String yamlFile,
      String recipe,
      String baseArtifactId,
      String originalArtifactSuffix,
      String originalVersion,
      String expectedArtifactSuffix) {
        runBouncyCastleDependencyChangeRecipe(
          yamlFile,
          recipe,
          "%s-%s".formatted(baseArtifactId, originalArtifactSuffix),
          originalVersion,
          "%s-%s".formatted(baseArtifactId, expectedArtifactSuffix)
        );
    }

    void runBouncyCastleDependencyChangeRecipe(
      String yamlFile,
      String recipe,
      String originalArtifactId,
      String originalVersion,
      String expectedArtifactId) {
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
                      <artifactId>%s</artifactId>
                      <version>%s</version>
                    </dependency>
                  </dependencies>
                </project>
                """.formatted(originalArtifactId, originalVersion),
              spec -> spec
                .after(identity())
                .afterRecipe(doc -> assertThat(doc.getMarkers().findFirst(MavenResolutionResult.class)
                  .get().getDependencies().get(Scope.Compile))
                  .filteredOn(rd -> rd.getDepth() == 0)
                  .singleElement()
                  .satisfies(rd -> {
                      assertThat(rd.getGroupId()).isEqualTo("org.bouncycastle");
                      assertThat(rd.getArtifactId()).isEqualTo(expectedArtifactId);
                  }))
            )
          )
        );
    }
}
