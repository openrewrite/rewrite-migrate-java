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
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static java.util.regex.Pattern.quote;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.gradle.Assertions.buildGradle;

class UpgradeThirdPartyGradlePluginsForJava25Test implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.migrate.UpgradeThirdPartyGradlePluginsForJava25")
          .allSources(source -> source.markers(GradlePluginPortal.project()));
    }

    @DocumentExample
    @Test
    void upgradeGradle9PluginsAndJava25ToolsTogether() {
        rewriteRun(
          spec -> spec.recipeFromResources("org.openrewrite.java.migrate.UpgradePluginsForJava25"),
          buildGradle(
            """
              plugins {
                  id 'java'
                  id 'com.diffplug.spotless' version '6.17.0'
                  id 'com.github.spotbugs' version '4.8.0'
              }

              spotless {
                  java {
                      googleJavaFormat()
                  }
              }
              """,
            spec -> spec.after(actual -> assertThat(actual)
              .containsPattern("id 'com.diffplug.spotless' version '6\\.25\\.\\d+'")
              .containsPattern("id 'com.github.spotbugs' version '5\\.2\\.\\d+'")
              .contains("googleJavaFormat('1.27.0')")
              .containsPattern("spotbugs \\{\n    toolVersion = '4\\.9\\.\\d+'\n}")
              .actual())
          )
        );
    }

    @ParameterizedTest
    @CsvSource({
      "net.ltgt.errorprone, 3.1.0, 4.1.",
      "org.gradle.test-retry, 1.6.2, 1.6.",
      "com.bmuschko.docker-java-application, 9.4.0, 10.0.",
      "com.bmuschko.docker-spring-boot-application, 9.4.0, 10.0.",
    })
    void upgradePluginBrokenOnJava25(String pluginId, String oldVersion, String newVersionPrefix) {
        rewriteRun(
          buildGradle(
            "plugins { id '%s' version '%s' }".formatted(pluginId, oldVersion),
            spec -> spec.after(actual -> assertThat(actual)
              .containsPattern("id '%s' version '%s[\\d.]*'".formatted(quote(pluginId), quote(newVersionPrefix)))
              .doesNotContain("version '%s'".formatted(oldVersion))
              .actual())
          )
        );
    }

    @Test
    void upgradeSpotBugsToolVersion() {
        rewriteRun(
          buildGradle(
            """
              spotbugs {
                  toolVersion = '4.8.3'
              }
              """,
            spec -> spec.after(actual -> assertThat(actual)
              .containsPattern("toolVersion = '4\\.9\\.\\d+'")
              .actual())
          )
        );
    }

    @Test
    void upgradeGoogleJavaFormat() {
        rewriteRun(
          buildGradle(
            """
              spotless {
                  java {
                      googleJavaFormat('1.17.0')
                  }
              }
              """,
            """
              spotless {
                  java {
                      googleJavaFormat('1.27.0')
                  }
              }
              """
          )
        );
    }

    @Test
    void doNotDowngradeNewerPlugins() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'net.ltgt.errorprone' version '5.0.0'
                  id 'org.gradle.test-retry' version '2.0.0'
                  id 'com.bmuschko.docker-java-application' version '11.0.0'
              }
              """
          )
        );
    }
}
