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
import org.junit.jupiter.params.provider.CsvSource;
import org.openrewrite.semver.LatestRelease;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.maven.Assertions.pomXml;

class MavenBuildPluginsForJava25Test implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.migrate.UpgradePluginsForJava25");
    }

    @ParameterizedTest
    @CsvSource({"maven-plugin-plugin,3.15.1,3.15.2", "maven-shade-plugin,3.6.0,3.6.1"})
    void upgradePluginsThatReadJava25Classes(String artifact, String oldVersion, String minimum) {
        rewriteRun(pomXml(pom(artifact, oldVersion, false), spec -> spec.after(after -> {
            assertCompatible(after, artifact, minimum);
            return after;
        })));
    }

    @ParameterizedTest
    @CsvSource({"maven-plugin-plugin,3.15.1,3.15.2", "maven-shade-plugin,3.6.0,3.6.1"})
    void upgradeManagedPluginsThatReadJava25Classes(String artifact, String oldVersion, String minimum) {
        rewriteRun(pomXml(pom(artifact, oldVersion, true), spec -> spec.after(after -> {
            assertCompatible(after, artifact, minimum);
            return after;
        })));
    }

    @ParameterizedTest
    @CsvSource({"maven-plugin-plugin,4.0.0-beta-1", "maven-shade-plugin,4.0.0-beta-1"})
    void preserveNewerMajorPluginLines(String artifact, String version) {
        rewriteRun(pomXml(pom(artifact, version, false)));
    }

    @Test
    void doesNotIntroducePluginsIntoPlainProjects() {
        rewriteRun(pomXml("""
          <project>
              <modelVersion>4.0.0</modelVersion>
              <groupId>example</groupId>
              <artifactId>plain</artifactId>
              <version>1</version>
          </project>
          """));
    }

    private static void assertCompatible(String pom, String artifact, String minimum) {
        Matcher matcher = Pattern.compile("<artifactId>" + artifact + "</artifactId>\\s*<version>([^<]+)</version>").matcher(pom);
        assertThat(matcher.find()).isTrue();
        assertThat(new LatestRelease(null).compare(null, matcher.group(1), minimum)).isGreaterThanOrEqualTo(0);
    }

    private static String pom(String artifact, String version, boolean managed) {
        String plugin = "<plugin><groupId>org.apache.maven.plugins</groupId><artifactId>" + artifact +
                        "</artifactId><version>" + version + "</version></plugin>";
        String plugins = "<plugins>" + plugin + "</plugins>";
        return "<project><modelVersion>4.0.0</modelVersion><groupId>example</groupId><artifactId>build</artifactId>" +
               "<version>1</version><build>" + (managed ? "<pluginManagement>" + plugins + "</pluginManagement>" : plugins) +
               "</build></project>";
    }
}
