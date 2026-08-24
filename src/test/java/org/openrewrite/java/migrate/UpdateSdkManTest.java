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
import org.openrewrite.DocumentExample;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.semver.LatestRelease;
import org.openrewrite.test.RewriteTest;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.test.SourceSpecs.text;


class UpdateSdkManTest implements RewriteTest {

    /**
     * The nightly SDKMAN! candidates refresh drops patch versions as new ones appear, so tests that need an exact
     * version read a currently available one from the same candidate list the recipe resolves against.
     */
    private static String latestPatchVersion(String majorVersion, String distribution) {
        Pattern pattern = Pattern.compile("^" + majorVersion + "\\.\\d+\\.\\d+-" + distribution + "$");
        try (InputStream candidates = UpdateSdkMan.class.getResourceAsStream("/sdkman-java.csv");
             var reader = new BufferedReader(new InputStreamReader(candidates, StandardCharsets.UTF_8))) {
            return reader.lines()
              .filter(candidate -> pattern.matcher(candidate).matches())
              .max(new LatestRelease("-" + distribution))
              .map(candidate -> candidate.substring(0, candidate.length() - distribution.length() - 1))
              .orElseThrow(() -> new IllegalStateException(
                String.format("No %s candidate for Java %s in sdkman-java.csv", distribution, majorVersion)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @DocumentExample
    @Test
    void updateVersionExact() {
        String version = latestPatchVersion("17", "tem");
        rewriteRun(
          spec -> spec.recipe(new UpdateSdkMan(version, null)),
          text(
            """
              java=11.1.2-tem
              """,
            "java=" + version + "-tem\n",
            spec -> spec.path(".sdkmanrc")
          )
        );
    }

    @Test
    void updateVersionUsingMajorOnly() {
        rewriteRun(
          spec -> spec.recipe(new UpdateSdkMan("17", null)),
          text(
            """
              java=11.0.25-tem
              """,
            spec -> spec.path(".sdkmanrc")
              .after(sdkmanrc -> assertThat(sdkmanrc).containsOnlyOnce("java=").containsPattern("java=17").actual())
          )
        );
    }

    @Test
    void updateDistributionOnly() {
        String version = latestPatchVersion("11", "amzn");
        rewriteRun(
          spec -> spec.recipe(new UpdateSdkMan(null, "amzn")),
          text(
            "java=" + version + "-zulu\n",
            "java=" + version + "-amzn\n",
            spec -> spec.path(".sdkmanrc")
          )
        );
    }

    @Test
    void updateBoth() {
        rewriteRun(
          spec -> spec.recipe(new UpdateSdkMan("17", "graalce")),
          text(
            """
              java=11.0.25-amzn
              """,
            """
              java=17.0.9-graalce
              """,
            spec -> spec.path(".sdkmanrc")
          )
        );
    }

    @Test
    void nonExistingVersion() {
        rewriteRun(
          spec -> spec.recipe(new UpdateSdkMan("42", null)),
          text(
            """
              java=11.1.2-tem
              """,
            spec -> spec.path(".sdkmanrc")
          )
        );
    }

    @Test
    void nonExistingDist() {
        rewriteRun(
          spec -> spec.recipe(new UpdateSdkMan(null, "notreal")),
          text(
            """
              java=11.1.2-tem
              """,
            spec -> spec.path(".sdkmanrc")
          )
        );
    }

    @Test
    void emptyOptions() {
        assertThat(new UpdateSdkMan(null, null).validate(new InMemoryExecutionContext()).isInvalid()).isTrue();
    }

    @Test
    void onlyUpdateSdkManRCFiles() {
        rewriteRun(
          spec -> spec.recipe(new UpdateSdkMan("17", "tem")),
          text(
            """
              java=11.1.2-tem
              """,
            spec -> spec.path(".not-sdkmanrc")
          )
        );
    }

    @Test
    void nonNumericalVersionPart() {
        rewriteRun(
          spec -> spec.recipe(new UpdateSdkMan("17", null)),
          text(
            """
              java=11.0.25.fx-zulu
              """,
            spec -> spec
              .after(str -> assertThat(str).startsWith("java=17.0.").endsWith(".fx-zulu").actual())
              .path(".sdkmanrc")
          )
        );
    }

    @Test
    void zuluNonCrac() {
        rewriteRun(
          spec -> spec.recipe(new UpdateSdkMan("17", null)),
          text(
            """
              java=11.0.28-zulu
              """,
            spec -> spec.path(".sdkmanrc")
              .after(str -> assertThat(str)
                .startsWith("java=17.0.")
                .endsWith("-zulu")
                .doesNotContain(".crac")
                .actual())
          )
        );
    }

    @Test
    void distributionIsStillMatchedWhenTheCandidateCarriesAVendorBuild() {
        rewriteRun(
          spec -> spec.recipe(new UpdateSdkMan("17", null)),
          text(
            """
              java=11.0.28+1.1-zulu
              """,
            spec -> spec.path(".sdkmanrc")
              .after(str -> assertThat(str)
                .startsWith("java=17.0.")
                .endsWith("-zulu")
                .actual())
          )
        );
    }

    @Test
    void upgradeIfNewVersionIsStillSameVersionBasisAndSameDistribution() {
        rewriteRun(
          spec -> spec.recipe(new UpdateSdkMan("21", null)),
          text(
            """
              java=21.0.6-zulu
              """,
            spec -> spec.path(".sdkmanrc")
              .after(str -> assertThat(str)
                .startsWith("java=21.0.")
                .doesNotContain("21.0.6")
                .endsWith("-zulu")
                .actual())
          )
        );
    }

    @Test
    void upgradeIfNewVersionIsStillSameVersionBasisAndDifferentDistribution() {
        rewriteRun(
          spec -> spec.recipe(new UpdateSdkMan("21", "tem")),
          text(
            """
              java=21.0.10-jbr
              """,
            spec -> spec.path(".sdkmanrc")
              .after(str -> assertThat(str)
                .startsWith("java=21.0.")
                .endsWith("-tem")
                .actual())
          )
        );
    }

    @Test
    void minorUpgradesWithinSameMajorVersion() {
        rewriteRun(
          spec -> spec.recipe(new UpdateSdkMan("latest.patch", null)),
          text(
            """
              java=21.0.6-zulu
              """,
            spec -> spec.path(".sdkmanrc")
              .after(str -> assertThat(str)
                .startsWith("java=21.0.")
                .doesNotContain("21.0.6")
                .endsWith("-zulu")
                .actual())
          )
        );
    }

    @Test
    void minorUpgradeWithNewDistribution() {
        rewriteRun(
          spec -> spec.recipe(new UpdateSdkMan("latest.patch", "amzn")),
          text(
            """
              java=21.0.6-zulu
              """,
            spec -> spec.path(".sdkmanrc")
              .after(str -> assertThat(str)
                .startsWith("java=21.0.")
                .endsWith("-amzn")
                .actual())
          )
        );
    }

    @Test
    void minorDoesNotCrossMajorVersion() {
        rewriteRun(
          spec -> spec.recipe(new UpdateSdkMan("latest.patch", null)),
          text(
            """
              java=11.0.25-tem
              """,
            spec -> spec.path(".sdkmanrc")
              .after(str -> assertThat(str)
                .startsWith("java=11.0.")
                .endsWith("-tem")
                .actual())
          )
        );
    }

    @Test
    void doNotDowngradeVersionIfAlreadyHighEnoughSameDistribution() {
        rewriteRun(
          spec -> spec.recipe(new UpdateSdkMan("17", null)),
          text(
            """
              java=24-librca
              """,
            spec -> spec.path(".sdkmanrc")
          )
        );
    }

    @Test
    void doNotDowngradeVersionIfAlreadyHighEnoughDifferentDistribution() {
        rewriteRun(
          spec -> spec.recipe(new UpdateSdkMan("23", "zulu")),
          text(
            """
              java=24-amzn
              """,
            spec -> spec.path(".sdkmanrc")
          )
        );
    }
}
