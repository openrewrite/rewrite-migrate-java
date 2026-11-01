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
import org.openrewrite.Tree;
import org.openrewrite.gradle.marker.GradleBuildscript;
import org.openrewrite.gradle.marker.GradlePluginDescriptor;
import org.openrewrite.gradle.marker.GradleProject;

import java.util.Collections;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradle;

class UpgradeNebulaOptionalBaseForJava25Test implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipeFromResources("org.openrewrite.java.migrate.UpgradePluginsForJava25");
    }

    @Test
    void migrateLegacyOptionalBase() {
        rewriteRun(
          buildGradle(
            "plugins { id 'nebula.optional-base' version '7.0.0' }",
            "plugins { id 'com.netflix.nebula.optional-base' version '9.0.0' }",
            spec -> spec.markers(GradleProject.builder().id(Tree.randomId())
              .plugins(Collections.singletonList(new GradlePluginDescriptor("nebula.plugin.extraconfigurations.OptionalBasePlugin", "nebula.optional-base")))
              .buildscript(GradleBuildscript.builder().build()).build())
          )
        );
    }

    @Test
    void upgradeNamespacedOptionalBase() {
        rewriteRun(
          buildGradle(
            "plugins { id 'com.netflix.nebula.optional-base' version '8.0.0' }",
            "plugins { id 'com.netflix.nebula.optional-base' version '9.0.0' }"
          )
        );
    }

    @Test
    void preserveNewerOptionalBase() {
        rewriteRun(buildGradle("plugins { id 'com.netflix.nebula.optional-base' version '10.0.1' }"));
    }


}
