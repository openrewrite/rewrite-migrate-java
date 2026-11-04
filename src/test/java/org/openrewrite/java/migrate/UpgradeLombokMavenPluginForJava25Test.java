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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.maven.Assertions.pomXml;

class UpgradeLombokMavenPluginForJava25Test implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.migrate.UpgradePluginsForJava25");
    }

    @DocumentExample
    @Test
    void upgradesBundledLombokWithoutAProjectDependency() {
        rewriteRun(
          pomXml(
            pom(""),
            spec -> spec.after(actual -> assertThat(actual)
              .contains("<version>1.18.40</version>")
              .contains("<version>1.18.20.0</version>")
              .contains("<goal>delombok</goal>")
              .actual())
          )
        );
    }

    @Test
    void includedInJava25Migration() {
        rewriteRun(
          spec -> spec.recipeFromResources("org.openrewrite.java.migrate.UpgradeToJava25"),
          pomXml(pom(""), spec -> spec.after(actual -> assertThat(actual)
            .containsPattern("<version>1\\.18\\.\\d+</version>")
            .contains("<version>1.18.20.0</version>").actual()))
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"1.18.12", "1.18.38"})
    void upgradesExistingPluginDependency(String version) {
        rewriteRun(
          spec -> spec.recipeFromResources("org.openrewrite.java.migrate.UpgradeLombokMavenPluginForJava25"),
          pomXml(pom(dependency(version)), spec -> spec.after(actual -> assertThat(actual)
            .contains("<version>1.18.40</version>")
            .doesNotContain("<version>" + version + "</version>").actual()))
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"1.18.40", "1.18.48"})
    void retainsCompatiblePluginDependency(String version) {
        rewriteRun(
          spec -> spec.recipeFromResources("org.openrewrite.java.migrate.UpgradeLombokMavenPluginForJava25"),
          pomXml(pom(dependency(version)).replace("1.16.10.0", "1.18.20.0"))
        );
    }

    @Test
    void handlesPluginManagementAndProfiles() {
        rewriteRun(
          spec -> spec.recipeFromResources("org.openrewrite.java.migrate.UpgradeLombokMavenPluginForJava25"),
          pomXml(
            pom("").replace("<plugins>", "<pluginManagement><plugins>")
              .replace("</plugins>", "</plugins></pluginManagement>"),
            spec -> spec.after(actual -> assertThat(actual).contains("<version>1.18.40</version>").actual())
          ),
          pomXml(
            pom("").replace("<build>", "<profiles><profile><id>release</id><build>")
              .replace("</build>", "</build></profile></profiles>")
              .replace("delombok-example", "profile-example"),
            spec -> spec.path("profile/pom.xml")
              .after(actual -> assertThat(actual).contains("<version>1.18.40</version>").actual())
          )
        );
    }

    @Test
    void doesNotAddPluginWhenAbsent() {
        rewriteRun(
          spec -> spec.recipeFromResources("org.openrewrite.java.migrate.UpgradeLombokMavenPluginForJava25"),
          pomXml("""
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.example</groupId>
                <artifactId>without-delombok</artifactId>
                <version>1</version>
            </project>
            """)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"1.18.18", "1.18.20"})
    void detachesPluginVersionBeforeUpgradingSharedLombokProperty(String version) {
        rewriteRun(
          spec -> spec.recipeFromResources("org.openrewrite.java.migrate.UpgradeToJava25"),
          pomXml(
            pom("").replace("<build>", """
              <properties>
                  <maven.compiler.release>17</maven.compiler.release>
                  <lombok.version>%s</lombok.version>
              </properties>
              <dependencies>
                  <dependency>
                      <groupId>org.projectlombok</groupId>
                      <artifactId>lombok</artifactId>
                      <version>${lombok.version}</version>
                      <scope>provided</scope>
                  </dependency>
              </dependencies>
              <build>
              """.formatted(version)).replace("1.16.10.0", "${lombok.version}.0"),
            spec -> spec.after(actual -> assertThat(actual)
              .contains("<version>1.18.20.0</version>")
              .doesNotContain("${lombok.version}.0", "<lombok.version>" + version + "</lombok.version>")
              .contains("<artifactId>lombok</artifactId>")
              .actual())
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"${missing.version}.0", "${known}.${missing}.0", "${plugin.version}", "1.18.20.0"})
    void leavesUnresolvedExpressionsWholePropertiesAndLiteralsUnchanged(String version) {
        rewriteRun(
          spec -> spec.recipe(new ResolveLombokMavenPluginVersion()),
          pomXml(pom("").replace("<build>", """
            <properties>
                <known>1.18</known>
                <plugin.version>1.18.20.0</plugin.version>
            </properties>
            <build>
            """).replace("1.16.10.0", version))
        );
    }

    @Test
    void resolvesEachProfileInItsOwnModuleAndPropertyScope() {
        rewriteRun(
          spec -> spec.recipe(new ResolveLombokMavenPluginVersion()),
          pomXml(
            profilePom("1.18.18"),
            spec -> spec.path("first/pom.xml").after(actual -> assertThat(actual)
              .contains("<version>1.18.18.0</version>")
              .contains("<lombok.version>1.18.48</lombok.version>", "<lombok.version>1.18.18</lombok.version>")
              .doesNotContain("${lombok.version}.0").actual())
          ),
          pomXml(
            profilePom("1.18.20").replace("delombok-example", "second-example"),
            spec -> spec.path("second/pom.xml").after(actual -> assertThat(actual)
              .contains("<version>1.18.20.0</version>")
              .contains("<lombok.version>1.18.48</lombok.version>", "<lombok.version>1.18.20</lombok.version>")
              .doesNotContain("${lombok.version}.0").actual())
          )
        );
    }

    private static String profilePom(String version) {
        return pom("").replace("<build>", """
          <properties>
              <lombok.version>1.18.48</lombok.version>
          </properties>
          <profiles>
              <profile>
                  <id>release</id>
                  <properties>
                      <lombok.version>%s</lombok.version>
                  </properties>
                  <build>
          """.formatted(version))
          .replace("</build>", "</build></profile></profiles>")
          .replace("1.16.10.0", "${lombok.version}.0");
    }

    private static String dependency(String version) {
        return """
          <dependencies>
              <dependency>
                  <groupId>org.projectlombok</groupId>
                  <artifactId>lombok</artifactId>
                  <version>%s</version>
              </dependency>
          </dependencies>
          """.formatted(version);
    }

    private static String pom(String dependencies) {
        return """
          <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>com.example</groupId>
              <artifactId>delombok-example</artifactId>
              <version>1</version>
              <build>
                  <plugins>
                      <plugin>
                          <groupId>org.projectlombok</groupId>
                          <artifactId>lombok-maven-plugin</artifactId>
                          <version>1.16.10.0</version>
                          <executions>
                              <execution>
                                  <phase>generate-sources</phase>
                                  <goals>
                                      <goal>delombok</goal>
                                  </goals>
                              </execution>
                          </executions>
                          %s
                      </plugin>
                  </plugins>
              </build>
          </project>
          """.formatted(dependencies);
    }
}
