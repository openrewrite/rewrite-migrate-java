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
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.test.SourceSpecs.text;


class UpdateSdkManTest implements RewriteTest {

    @DocumentExample
    @Test
    void updateVersionExact() {
        rewriteRun(
          spec -> spec.recipe(new UpdateSdkMan("17.0.14", null)),
          text(
            """
              java=11.1.2-tem
              """,
            """
              java=17.0.14-tem
              """,
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
        rewriteRun(
          spec -> spec.recipe(new UpdateSdkMan(null, "amzn")),
          text(
            """
              java=11.0.26-tem
              """,
            """
              java=11.0.26-amzn
              """,
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
            """
              java=17.0.16.fx-zulu
              """,
            spec -> spec.path(".sdkmanrc")
          )
        );
    }

    @Test
    void zuluNonCrac() {
        rewriteRun(
          spec -> spec.recipe(new UpdateSdkMan("17", null)),
          text(
            """
              java=11.0.26-zulu
              """,
            """
              java=17.0.16-zulu
              """,
            spec -> spec.path(".sdkmanrc")
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
            """
              java=21.0.8-zulu
              """,
            spec -> spec.path(".sdkmanrc")
          )
        );
    }

    @Test
    void upgradeIfNewVersionIsStillSameVersionBasisAndDifferentDistribution() {
        rewriteRun(
          spec -> spec.recipe(new UpdateSdkMan("23", "amzn")),
          text(
            """
              java=23.0.1-open
              """,
            """
              java=23.0.2-amzn
              """,
            spec -> spec.path(".sdkmanrc")
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
