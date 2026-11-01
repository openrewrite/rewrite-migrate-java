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
package org.openrewrite.java.migrate.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.Tree;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.java.marker.JavaProject;
import org.openrewrite.java.marker.JavaVersion;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.test.SourceSpecs.text;

class ModuleHasKotlinSourceTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ModuleHasKotlinSource(false));
    }

    @Test
    void partialBuildModelsKeepExistingNonKotlinBehavior() {
        rewriteRun(
          spec -> spec.recipe(new ModuleHasKotlinSource(true)),
          text("plugins {}", "~~(No module, so vacuously has no Kotlin source)~~>plugins {}",
            spec -> spec.path("gradle-model/build.gradle.kts")
              .markers(GradleProject.builder().id(Tree.randomId()).build())),
          text("plugins {}", "~~(No module, so vacuously has no Kotlin source)~~>plugins {}",
            spec -> spec.path("version-model/build.gradle.kts")
              .markers(new JavaVersion(Tree.randomId(), "", "", "17", "17")))
        );
    }

    @Test
    void modeledKotlinModuleDoesNotEnterNonKotlinUpgrade() {
        JavaProject module = new JavaProject(Tree.randomId(), "modern-kotlin", null);
        rewriteRun(
          spec -> spec.recipe(new ModuleHasKotlinSource(true)),
          text("plugins {}", spec -> spec.path("build.gradle.kts").markers(module)),
          text("class Example", spec -> spec.path("src/main/kotlin/Example.kt").markers(module))
        );
    }

    @Test
    void unmodeledKotlinDslIsNotKnownToBeNonKotlin() {
        rewriteRun(
          spec -> spec.recipe(new ModuleHasKotlinSource(true)),
          text("plugins {}", spec -> spec.path("integration-test/buildSrc/build.gradle.kts")),
          text("plugins {}", spec -> spec.path("integration-test/buildSrc/src/main/kotlin/example.gradle.kts")),
          text("plugins {}", spec -> spec.path("build.gradle.kts")),
          text("plugins {}", "~~(No module, so vacuously has no Kotlin source)~~>plugins {}",
            spec -> spec.path("build.gradle"))
        );
    }

    @Test
    void precompiledGradleScriptsAreKotlinSources() {
        JavaProject module = new JavaProject(Tree.randomId(), "buildSrc", null);
        rewriteRun(
          text("plugins {}", "~~(Module has Kotlin source)~~>plugins {}", spec -> spec.path("integration-test/buildSrc/build.gradle.kts").markers(module)),
          text("plugins {}", "~~(Module has Kotlin source)~~>plugins {}", spec -> spec.path("integration-test/buildSrc/src/main/kotlin/example.gradle.kts").markers(module))
        );
    }

    @Test
    void buildAndSettingsScriptsDoNotMakeJavaProjectsKotlinModules() {
        JavaProject module = new JavaProject(Tree.randomId(), "app", null);
        rewriteRun(
          text("plugins {}", spec -> spec.path("build.gradle.kts").markers(module)),
          text("rootProject.name = \"app\"", spec -> spec.path("settings.gradle.kts").markers(module))
        );
    }

    @Test
    void resourceFixturesAreNotCompiledKotlinSources() {
        JavaProject module = new JavaProject(Tree.randomId(), "app", null);
        rewriteRun(
          text("plugins {}", spec -> spec.path("build.gradle.kts").markers(module)),
          text("plugins {}", spec -> spec.path("src/test/resources/project/src/main/kotlin/example.gradle.kts").markers(module))
        );
    }
}
