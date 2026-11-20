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
import static org.openrewrite.gradle.Assertions.buildGradleKts;

class UpgradeThirdPartyGradlePluginsForJava25Test implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.migrate.UpgradeThirdPartyGradlePluginsForJava25")
          .allSources(source -> source.markers(GradlePluginPortal.project()));
    }

    @DocumentExample
    @Test
    void upgradeAlongsideGradle9Wrapper() {
        rewriteRun(
          spec -> spec.recipeFromResources("org.openrewrite.java.migrate.UpgradePluginsForJava25"),
          buildGradle(
            """
              plugins {
                  id 'java'
                  id 'com.diffplug.spotless' version '6.17.0'
                  id 'com.github.ben-manes.versions' version '0.51.0'
                  id 'org.sonarqube' version '4.4.1.3373'
              }
              """,
            spec -> spec.after(actual -> assertThat(actual)
              .containsPattern("id 'com.diffplug.spotless' version '6\\.25\\.\\d+'")
              .containsPattern("id 'com.github.ben-manes.versions' version '0\\.52\\.\\d+'")
              .containsPattern("id 'org.sonarqube' version '6\\.2\\.[\\d.]+'")
              .actual())
          )
        );
    }

    @ParameterizedTest
    @CsvSource({
      "com.diffplug.spotless, 6.17.0, 6.25.",
      "com.github.spotbugs, 4.8.0, 5.2.",
      "net.ltgt.errorprone, 3.1.0, 4.1.",
      "org.sonarqube, 4.4.1.3373, 6.2.",
      "com.github.ben-manes.versions, 0.51.0, 0.52.",
      "com.adarshr.test-logger, 3.2.0, 4.0.",
      "org.gradle.test-retry, 1.6.2, 1.6.",
      "info.solidsoft.pitest, 1.15.0, 1.19.",
      "org.checkerframework, 0.6.30, 0.6.",
      "com.github.jk1.dependency-license-report, 2.1, 2.",
      "io.qameta.allure, 2.12.0, 3.0.",
      "me.champeau.jmh, 0.7.0, 0.7.",
      "io.freefair.lombok, 8.6, 8.14.",
      "io.freefair.aspectj.post-compile-weaving, 8.13.1, 8.14.",
      "com.google.protobuf, 0.9.2, 0.9.4",
      "com.gorylenko.gradle-git-properties, 2.4.1, 2.5.",
      "com.google.cloud.tools.jib, 3.4.5, 3.5.",
      "org.asciidoctor.jvm.convert, 3.3.2, 4.0.",
      "io.github.gradle-nexus.publish-plugin, 1.3.0, 2.0.",
      "com.vanniktech.maven.publish, 0.29.0, 0.32.",
      "org.unbroken-dome.test-sets, 4.0.0, 4.1.",
      "com.github.davidmc24.gradle.plugin.avro, 1.2.0, 1.2.",
      "org.graalvm.buildtools.native, 0.9.20, 0.9.",
      "nu.studer.jooq, 8.2, 8.2.",
      "com.bmuschko.docker-java-application, 9.4.0, 10.0.",
      "com.bmuschko.docker-spring-boot-application, 9.4.0, 10.0.",
    })
    void upgradePluginBrokenOnGradle9OrJava25(String pluginId, String oldVersion, String newVersionPrefix) {
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
    void upgradeKotlinDsl() {
        rewriteRun(
          buildGradleKts(
            """
              plugins {
                  id("com.google.protobuf") version "0.9.2"
                  id("com.adarshr.test-logger") version "3.2.0"
              }
              """,
            spec -> spec.after(actual -> assertThat(actual)
              .contains("id(\"com.google.protobuf\") version \"0.9.4\"")
              .containsPattern("id\\(\"com.adarshr.test-logger\"\\) version \"4\\.0\\.\\d+\"")
              .actual())
          )
        );
    }

    @Test
    void doNotDowngradeNewerPlugins() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'com.diffplug.spotless' version '7.0.0'
                  id 'com.github.spotbugs' version '6.2.4'
                  id 'com.google.protobuf' version '0.9.5'
                  id 'io.freefair.lombok' version '9.0.0'
              }
              """
          )
        );
    }

    @Test
    void leavePluginsThatWorkOnGradle9Alone() {
        rewriteRun(
          buildGradle(
            """
              plugins {
                  id 'com.diffplug.spotless-changelog' version '3.0.0'
                  id 'com.bmuschko.docker-remote-api' version '9.4.0'
                  id 'com.palantir.git-version' version '3.0.0'
              }
              """
          )
        );
    }
}
