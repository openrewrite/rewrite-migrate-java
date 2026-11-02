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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.settingsGradle;
import static org.openrewrite.gradle.Assertions.settingsGradleKts;

class UpgradeFoojayForJava25Test implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.migrate.UpgradePluginsForJava25");
    }

    @Test
    void upgradeFoojayGroovySettings() {
        rewriteRun(
          settingsGradle(
            "plugins { id 'org.gradle.toolchains.foojay-resolver-convention' version '0.5.0' }",
            "plugins { id 'org.gradle.toolchains.foojay-resolver-convention' version '1.0.0' }"
          )
        );
    }

    @Test
    void upgradeFoojayKotlinSettings() {
        rewriteRun(
          settingsGradleKts(
            "plugins { id(\"org.gradle.toolchains.foojay-resolver-convention\") version(\"0.5.0\") }",
            "plugins { id(\"org.gradle.toolchains.foojay-resolver-convention\") version(\"1.0.0\") }"
          )
        );
    }

    @Test
    void upgradeFoojayResolver() {
        rewriteRun(
          settingsGradleKts(
            "plugins { id(\"org.gradle.toolchains.foojay-resolver\") version(\"0.9.0\") }",
            "plugins { id(\"org.gradle.toolchains.foojay-resolver\") version(\"1.0.0\") }"
          )
        );
    }

    @Test
    void preserveCurrentFoojayAndUnrelatedPlugins() {
        rewriteRun(
          settingsGradleKts(
            """
              plugins {
                  id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
                  id("com.acme.foojay-resolver") version "0.5.0"
              }
              """
          )
        );
    }
}
