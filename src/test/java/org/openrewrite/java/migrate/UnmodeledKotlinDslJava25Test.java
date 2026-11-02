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
import org.openrewrite.java.marker.JavaProject;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.gradle.Assertions.buildGradleKts;

class UnmodeledKotlinDslJava25Test implements RewriteTest {
    @Test
    void preserveUnmodeledNestedToolchains() {
        rewriteRun(
          spec -> spec.recipeFromResources("org.openrewrite.java.migrate.UpgradeBuildToJava25"),
          buildGradleKts(
            "java { toolchain { languageVersion.set(JavaLanguageVersion.of(8)) } }",
            spec -> spec.path("integration-test/buildSrc/build.gradle.kts")),
          buildGradleKts(
            "java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }",
            spec -> spec.path("integration-test/buildSrc/src/main/kotlin/java-conventions.gradle.kts")),
          buildGradleKts(
            "java { toolchain { languageVersion.set(JavaLanguageVersion.of(24)) } }",
            spec -> spec.path("build.gradle.kts"))
        );
    }

    @Test
    void modeledJavaKotlinDslStillUpgrades() {
        rewriteRun(
          spec -> spec.recipeFromResources("org.openrewrite.java.migrate.UpgradeBuildToJava25"),
          buildGradleKts(
            "java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }",
            "java { toolchain { languageVersion.set(JavaLanguageVersion.of(25)) } }",
            spec -> spec.markers(new JavaProject(Tree.randomId(), "java-app", null)))
        );
    }
}
