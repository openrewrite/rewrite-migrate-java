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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.Tree;
import org.openrewrite.java.marker.JavaProject;
import org.openrewrite.java.marker.JavaVersion;
import org.openrewrite.java.migrate.table.KotlinGradleJava25Prerequisites;
import static org.assertj.core.api.Assertions.assertThat;
import org.openrewrite.test.RewriteTest;
import static org.openrewrite.gradle.Assertions.buildGradleKts;
import static org.openrewrite.gradle.Assertions.settingsGradleKts;
import static org.openrewrite.kotlin.Assertions.kotlin;
import static org.openrewrite.properties.Assertions.properties;
import static org.openrewrite.test.SourceSpecs.text;
import static org.openrewrite.maven.Assertions.pomXml;

class CheckKotlinGradleJava25CompatibilityTest implements RewriteTest {
    private static final String PIPELINE = """
      type: specs.openrewrite.org/v1beta/recipe
      name: test.Java25Compatibility
      displayName: Test compatibility
      description: Exercise compatibility with real Java target and wrapper property changes.
      recipeList:
        - org.openrewrite.java.migrate.search.CheckKotlinGradleJava25Compatibility:
            minimumCompilerVersion: 2.3.0
            markCompatible: false
        - test.CompatibleJava25
      ---
      type: specs.openrewrite.org/v1beta/recipe
      name: test.CompatibleJava25
      displayName: Compatible migration
      description: Exercise both compiler and runtime settings behind the guard.
      preconditions:
        - org.openrewrite.java.migrate.search.CheckKotlinGradleJava25Compatibility:
            minimumCompilerVersion: 2.3.0
            markCompatible: true
      recipeList:
        - test.OwnedCompiler
        - org.openrewrite.gradle.UpdateJavaCompatibility:
            version: 25
        - org.openrewrite.properties.ChangePropertyValue:
            propertyKey: distributionUrl
            newValue: https://services.gradle.org/distributions/gradle-9.8.2-bin.zip
        - org.openrewrite.maven.ChangePropertyValue:
            key: maven.compiler.release
            newValue: 25
      ---
      type: specs.openrewrite.org/v1beta/recipe
      name: test.OwnedCompiler
      displayName: Upgrade an owned Kotlin compiler
      description: Existing exact-version plugin recipe behind the editable declaration guard.
      preconditions:
        - org.openrewrite.java.migrate.search.CheckKotlinGradleJava25Compatibility:
            minimumCompilerVersion: 2.3.0
            markEditableCompiler: true
      recipeList:
        - org.openrewrite.gradle.plugins.UpgradePluginVersion:
            pluginIdPattern: org.jetbrains.kotlin.*
            newVersion: 2.3.0
      """;

    @Override public void defaults(RecipeSpec spec) { spec.recipeFromYaml(PIPELINE, "test.Java25Compatibility"); }

    @Test void oldCompilerPreservesTheWholeBuild() {
        rewriteRun(
          spec -> spec.dataTable(KotlinGradleJava25Prerequisites.Row.class, rows -> assertThat(rows).singleElement().satisfies(row -> {
              assertThat(row.getCompilerVersion()).isEqualTo("1.8.20");
              assertThat(row.getManualPrerequisite()).contains("not automatically migrated to K2");
          })),
          buildGradleKts("""
            plugins { kotlin("jvm") version "1.8.20" }
            java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }
            """),
          kotlin("class Proof", s -> s.path("src/main/kotlin/Proof.kt")),
          properties("distributionUrl=old-wrapper", s -> s.path("gradle/wrapper/gradle-wrapper.properties")));
    }

    @Test void javaOnlyKotlinDslAndTransitiveStdlibRemainEligible() {
        rewriteRun(
          buildGradleKts("""
            plugins { java }
            dependencies { implementation("org.jetbrains.kotlin:kotlin-stdlib:1.8.20") }
            java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }
            """, """
            plugins { java }
            dependencies { implementation("org.jetbrains.kotlin:kotlin-stdlib:1.8.20") }
            java { toolchain { languageVersion.set(JavaLanguageVersion.of(25)) } }
            """));
    }

    @Test void supportedCompilerRemainsEligible() {
        rewriteRun(
          buildGradleKts("""
            plugins { kotlin("jvm") version "2.3.0" }
            java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }
            """, """
            plugins { kotlin("jvm") version "2.3.0" }
            java { toolchain { languageVersion.set(JavaLanguageVersion.of(25)) } }
            """),
          kotlin("class Proof", s -> s.path("src/main/kotlin/Proof.kt")));
    }

    @Test void independentNestedBuildDoesNotBlockTheJavaBuild() {
        rewriteRun(
          settingsGradleKts("rootProject.name = \"java\""),
          buildGradleKts("java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }",
            "java { toolchain { languageVersion.set(JavaLanguageVersion.of(25)) } }"),
          settingsGradleKts("rootProject.name = \"legacy\"", s -> s.path("legacy/settings.gradle.kts")),
          buildGradleKts("""
            plugins { kotlin("jvm") version "1.8.20" }
            java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }
            """, s -> s.path("legacy/build.gradle.kts")),
          kotlin("class Proof", s -> s.path("legacy/src/main/kotlin/Proof.kt")));
    }

    @Test void resourceFixturesAreNotCompiledKotlin() {
        rewriteRun(
          buildGradleKts("""
            plugins { kotlin("jvm") version "1.8.20" apply false }
            java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }
            """, """
            plugins { kotlin("jvm") version "1.8.20" apply false }
            java { toolchain { languageVersion.set(JavaLanguageVersion.of(25)) } }
            """),
          kotlin("class Fixture", s -> s.path("src/test/resources/project/src/main/kotlin/Fixture.kt")));
    }

    @Test void unresolvedCompilerRequiresAManualPrerequisite() {
        rewriteRun(
          buildGradleKts("""
            plugins { kotlin("jvm") version compilerVersion }
            java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }
            """),
          kotlin("class Proof", s -> s.path("src/main/kotlin/Proof.kt")));
    }

    @Test void editableK2MinorCompilerIsUpgradedBeforeJava25() {
        rewriteRun(
          buildGradleKts("""
            plugins { kotlin("jvm") version "2.2.21" }
            java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }
            """, """
            plugins { kotlin("jvm") version "2.3.0" }
            java { toolchain { languageVersion.set(JavaLanguageVersion.of(25)) } }
            """),
          kotlin("class Proof", s -> s.path("src/main/kotlin/Proof.kt")));
    }

    @Test void settingsOwnedLiteralCompilerIsUpgraded() {
        rewriteRun(
          settingsGradleKts("pluginManagement { plugins { kotlin(\"jvm\") version \"2.2.21\" } }",
            "pluginManagement { plugins { kotlin(\"jvm\") version \"2.3.0\" } }"),
          buildGradleKts("""
            plugins { kotlin("jvm") }
            java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }
            """, """
            plugins { kotlin("jvm") }
            java { toolchain { languageVersion.set(JavaLanguageVersion.of(25)) } }
            """),
          kotlin("class Proof", s -> s.path("src/main/kotlin/Proof.kt")));
    }

    @Test void oneLegacyChildPreservesTheSharedBuildTransactionally() {
        rewriteRun(
          settingsGradleKts("include(\"legacy\", \"modern\")"),
          buildGradleKts("plugins { java }"),
          buildGradleKts("plugins { kotlin(\"jvm\") version \"1.8.20\" }", s -> s.path("legacy/build.gradle.kts")),
          buildGradleKts("plugins { kotlin(\"jvm\") version \"2.2.21\" }", s -> s.path("modern/build.gradle.kts")),
          kotlin("class Legacy", s -> s.path("legacy/src/main/kotlin/Legacy.kt")),
          kotlin("class Modern", s -> s.path("modern/src/main/kotlin/Modern.kt")),
          properties("distributionUrl=old-wrapper", s -> s.path("gradle/wrapper/gradle-wrapper.properties")));
    }

    @Test void opaqueManagedK2CompilerIsPreserved() {
        rewriteRun(
          spec -> spec.dataTable(KotlinGradleJava25Prerequisites.Row.class, rows -> assertThat(rows).singleElement().satisfies(row -> assertThat(row.getCompilerVersion()).isEqualTo("2.0.0"))),
          buildGradleKts("""
            val compilerVersion: String by project
            plugins { kotlin("jvm") version compilerVersion }
            java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }
            """),
          properties("compilerVersion=2.0.0", s -> s.path("gradle.properties")),
          kotlin("class Proof", s -> s.path("src/main/kotlin/Proof.kt")));
    }

    @Test void unparsedKotlinSourceStillPreservesItsBuild() {
        rewriteRun(
          buildGradleKts("plugins { kotlin(\"jvm\") version \"1.8.20\" }"),
          text("class Proof", s -> s.path("src/main/kotlin/Proof.kt")),
          properties("distributionUrl=old-wrapper", s -> s.path("gradle/wrapper/gradle-wrapper.properties")));
    }

    @Test void unparsedBuildWithActualKotlinRequiresAPrerequisite() {
        rewriteRun(
          spec -> spec.dataTable(KotlinGradleJava25Prerequisites.Row.class, rows -> assertThat(rows).singleElement().satisfies(row -> assertThat(row.getCompilerVersion()).isEqualTo("unknown"))),
          text("plugins { kotlin(\"jvm\") version managedVersion }", s -> s.path("build.gradle.kts")),
          kotlin("class Proof", s -> s.path("src/main/kotlin/Proof.kt")),
          properties("distributionUrl=old-wrapper", s -> s.path("gradle/wrapper/gradle-wrapper.properties")));
    }

    @Test void futureCompilerIsNotDowngraded() {
        rewriteRun(
          buildGradleKts("""
            plugins { kotlin("jvm") version "2.4.0" }
            java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }
            """, """
            plugins { kotlin("jvm") version "2.4.0" }
            java { toolchain { languageVersion.set(JavaLanguageVersion.of(25)) } }
            """),
          kotlin("class Proof", s -> s.path("src/main/kotlin/Proof.kt")));
    }

    @Test void embeddedKotlinDslCompilerRemainsEligible() {
        rewriteRun(
          buildGradleKts("""
            plugins { `kotlin-dsl` }
            java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }
            """, """
            plugins { `kotlin-dsl` }
            java { toolchain { languageVersion.set(JavaLanguageVersion.of(25)) } }
            """),
          text("plugins {}", s -> s.path("src/main/kotlin/buildlogic.gradle.kts")));
    }

    @Test void unrelatedMavenProjectIsNotPreservedByGradlePolicy() {
        rewriteRun(
          buildGradleKts("plugins { kotlin(\"jvm\") version \"1.8.20\" }"),
          kotlin("class Proof", s -> s.path("src/main/kotlin/Proof.kt")),
          pomXml("""
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>example</groupId><artifactId>java-child</artifactId><version>1</version>
                <properties><maven.compiler.release>17</maven.compiler.release></properties>
            </project>
            """, """
            <project>
                <modelVersion>4.0.0</modelVersion>
                <groupId>example</groupId><artifactId>java-child</artifactId><version>1</version>
                <properties><maven.compiler.release>25</maven.compiler.release></properties>
            </project>
            """, s -> s.path("maven-child/pom.xml")));
    }

    @Test void productionJava25CompositePreservesLegacyBuild() {
        JavaProject module = new JavaProject(Tree.randomId(), "legacy", null);
        rewriteRun(
          spec -> spec.recipeFromResources("org.openrewrite.java.migrate.UpgradeToJava25")
            .allSources(source -> source.markers(module, new JavaVersion(Tree.randomId(), "", "", "17", "17")))
            .dataTable(KotlinGradleJava25Prerequisites.Row.class, rows -> assertThat(rows).hasSize(1)),
          buildGradleKts("""
            plugins { kotlin("jvm") version "1.8.20" }
            java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }
            """),
          kotlin("class Proof", s -> s.path("src/main/kotlin/Proof.kt")),
          properties("distributionUrl=old-wrapper", s -> s.path("gradle/wrapper/gradle-wrapper.properties")));
    }

    @Test void productionJava25CompositeRetainsEditableK2MinorUpgrade() {
        JavaProject module = new JavaProject(Tree.randomId(), "modern", null);
        rewriteRun(
          spec -> spec.recipeFromResources("org.openrewrite.java.migrate.UpgradeToJava25")
            .allSources(source -> source.markers(module, new JavaVersion(Tree.randomId(), "", "", "17", "17"))),
          buildGradleKts("""
            plugins { kotlin("jvm") version "2.2.21" }
            java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }
            """, """
            plugins { kotlin("jvm") version "2.3.0" }
            java { toolchain { languageVersion.set(JavaLanguageVersion.of(25)) } }
            """),
          kotlin("class Proof", s -> s.path("src/main/kotlin/Proof.kt")));
    }

    @Test void unusedOldPluginDoesNotBlockEmbeddedCompiler() {
        rewriteRun(
          buildGradleKts("""
            plugins { `kotlin-dsl`; kotlin("jvm") version "1.8.20" apply false }
            java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }
            """, """
            plugins { `kotlin-dsl`; kotlin("jvm") version "1.8.20" apply false }
            java { toolchain { languageVersion.set(JavaLanguageVersion.of(25)) } }
            """),
          text("plugins {}", s -> s.path("src/main/kotlin/buildlogic.gradle.kts")));
    }

    @Test void unusedOldSettingsDefaultDoesNotOverrideAppliedCompiler() {
        rewriteRun(
          settingsGradleKts("pluginManagement { plugins { kotlin(\"jvm\") version \"1.8.20\" } }"),
          buildGradleKts("""
            plugins { kotlin("jvm") version "2.3.0" }
            java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }
            """, """
            plugins { kotlin("jvm") version "2.3.0" }
            java { toolchain { languageVersion.set(JavaLanguageVersion.of(25)) } }
            """),
          kotlin("class Proof", s -> s.path("src/main/kotlin/Proof.kt")));
    }

    @Test void appliedCompilerInheritsOldSettingsDefault() {
        rewriteRun(
          settingsGradleKts("pluginManagement { plugins { kotlin(\"jvm\") version \"1.8.20\" } }"),
          buildGradleKts("""
            plugins { kotlin("jvm") }
            java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }
            """),
          kotlin("class Proof", s -> s.path("src/main/kotlin/Proof.kt")));
    }

    @Test void uniquelyOwnedCompilerLiteralVariableIsEditable() {
        rewriteRun(
          buildGradleKts("""
            val compilerVersion = "2.2.21"
            plugins { kotlin("jvm") version compilerVersion }
            java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }
            """, """
            val compilerVersion = "2.3.0"
            plugins { kotlin("jvm") version compilerVersion }
            java { toolchain { languageVersion.set(JavaLanguageVersion.of(25)) } }
            """),
          kotlin("class Proof", s -> s.path("src/main/kotlin/Proof.kt")));
    }

    @Test void collidingVariableNamesRequireAManualDecision() {
        rewriteRun(
          buildGradleKts("""
            val compilerVersion = "2.2.21"
            plugins { kotlin("jvm") version compilerVersion }
            class Other { val compilerVersion = "unrelated" }
            java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }
            """),
          kotlin("class Proof", s -> s.path("src/main/kotlin/Proof.kt")));
    }

    @Test void providerVersionIsOpaqueEvenWithAMatchingDefaultProperty() {
        rewriteRun(
          buildGradleKts("""
            val compilerVersion = providers.gradleProperty("externalCompiler").get()
            plugins { kotlin("jvm") version compilerVersion }
            java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }
            """),
          properties("compilerVersion=2.4.0", s -> s.path("gradle.properties")),
          kotlin("class Proof", s -> s.path("src/main/kotlin/Proof.kt")));
    }
}
