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
      recipeList:
        - org.openrewrite.java.migrate.search.CheckKotlinGradleJava25Compatibility:
            minimumCompilerVersion: 2.3.0
            upgradeEditableCompiler: true
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

    @Test void pluginBlockOwnedCompilerLiteralIsEditable() {
        rewriteRun(
          buildGradleKts("""
            plugins {
                val compilerVersion = "2.2.21"
                kotlin("jvm") version compilerVersion
            }
            java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }
            """, """
            plugins {
                val compilerVersion = "2.3.0"
                kotlin("jvm") version compilerVersion
            }
            java { toolchain { languageVersion.set(JavaLanguageVersion.of(25)) } }
            """),
          kotlin("class Proof", s -> s.path("src/main/kotlin/Proof.kt")));
    }

    @Test void reassignedCompilerVariableIsOpaque() {
        rewriteRun(
          buildGradleKts("""
            var compilerVersion = "2.2.21"
            compilerVersion = "1.8.20"
            plugins { kotlin("jvm") version compilerVersion }
            java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }
            """),
          kotlin("class Proof", s -> s.path("src/main/kotlin/Proof.kt")));
    }

    @Test void unusedLegacyJsDeclarationIsNotCrossedToK2() {
        rewriteRun(
          settingsGradleKts("""
            pluginManagement {
                plugins {
                    kotlin("jvm") version "2.2.21"
                    kotlin("js") version "1.9.22"
                }
            }
            """, """
            pluginManagement {
                plugins {
                    kotlin("jvm") version "2.3.0"
                    kotlin("js") version "1.9.22"
                }
            }
            """),
          buildGradleKts("""
            plugins { kotlin("jvm") }
            java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }
            """, """
            plugins { kotlin("jvm") }
            java { toolchain { languageVersion.set(JavaLanguageVersion.of(25)) } }
            """),
          kotlin("class Proof", s -> s.path("src/main/kotlin/Proof.kt")));
    }

    @Test void customReceiverWithTheSamePluginIdIsNotACompilerDeclaration() {
        rewriteRun(
          settingsGradleKts("""
            pluginManagement {
                plugins {
                    kotlin("jvm") version "2.2.21"
                    custom.id("org.jetbrains.kotlin.jvm").version("1.9.22")
                }
            }
            """, """
            pluginManagement {
                plugins {
                    kotlin("jvm") version "2.3.0"
                    custom.id("org.jetbrains.kotlin.jvm").version("1.9.22")
                }
            }
            """),
          buildGradleKts("plugins { kotlin(\"jvm\") }"),
          kotlin("class Proof", s -> s.path("src/main/kotlin/Proof.kt")));
    }

    @Test void appliedLegacyCompilerNeedsAPrerequisiteBeforeSourcesExist() {
        rewriteRun(
          buildGradleKts("plugins { kotlin(\"jvm\") version \"1.8.20\" }"),
          properties("distributionUrl=old-wrapper", s -> s.path("gradle/wrapper/gradle-wrapper.properties")));
    }

    @Test void anAppliedLegacyJsChildPreservesTheSharedBuild() {
        rewriteRun(
          settingsGradleKts("include(\"jvm\", \"js\")"),
          buildGradleKts("plugins { java }"),
          buildGradleKts("plugins { kotlin(\"jvm\") version \"2.2.21\" }", s -> s.path("jvm/build.gradle.kts")),
          buildGradleKts("plugins { kotlin(\"js\") version \"1.9.22\" }", s -> s.path("js/build.gradle.kts")),
          properties("distributionUrl=old-wrapper", s -> s.path("gradle/wrapper/gradle-wrapper.properties")));
    }
    @Test void independentBuildsOwnSameNamedCompilerVariablesSeparately() {
        String before = """
          plugins {
              val compilerVersion = "2.2.21"
              kotlin("jvm") version compilerVersion
          }
          java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }
          """;
        String after = before.replace("2.2.21", "2.3.0").replace("of(17)", "of(25)");
        rewriteRun(
          settingsGradleKts("rootProject.name = \"outer\""),
          settingsGradleKts("rootProject.name = \"nested\"", s -> s.path("nested/settings.gradle.kts")),
          buildGradleKts(before, after),
          buildGradleKts(before, after, s -> s.path("nested/build.gradle.kts")),
          kotlin("class OuterProof", s -> s.path("src/main/kotlin/OuterProof.kt")),
          kotlin("class NestedProof", s -> s.path("nested/src/main/kotlin/NestedProof.kt")));
    }

    @Test void sharedBuildVariableCollisionsStillRequireAManualDecision() {
        String build = """
          plugins {
              val compilerVersion = "2.2.21"
              kotlin("jvm") version compilerVersion
          }
          java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }
          """;
        rewriteRun(
          settingsGradleKts("include(\"child\")"),
          buildGradleKts(build),
          buildGradleKts(build, s -> s.path("child/build.gradle.kts")),
          kotlin("class OuterProof", s -> s.path("src/main/kotlin/OuterProof.kt")),
          kotlin("class ChildProof", s -> s.path("child/src/main/kotlin/ChildProof.kt")));
    }

    @Test void productionJava25AlignsMarkerlessOwnedRootCompiler() {
        String before = """
          plugins {
              val kotlinVersion = "2.2.0"
              kotlin("jvm") version kotlinVersion
              kotlin("plugin.spring") version kotlinVersion
          }
          java { toolchain { languageVersion = JavaLanguageVersion.of(17) } }
          """;
        rewriteRun(spec -> spec.recipeFromResources("org.openrewrite.java.migrate.UpgradeToJava25")
            .cycles(3).expectedCyclesThatMakeChanges(2),
          buildGradleKts(before, before.replace("2.2.0", "2.3.0").replace("of(17)", "of(25)")),
          kotlin("class Proof", source -> source.path("src/main/kotlin/Proof.kt")));
    }

    @Test void alignmentKeepsMarkerlessJavaOnlyRootAndIndependentNestedBuild() {
        rewriteRun(spec -> spec.recipeFromResources("org.openrewrite.java.migrate.AlignGradleKotlinTargetsForJava25"),
          settingsGradleKts("includeBuild(\"nested\")"),
          buildGradleKts("plugins { java }; java { toolchain { languageVersion = JavaLanguageVersion.of(17) } }"),
          settingsGradleKts("rootProject.name = \"nested\"", source -> source.path("nested/settings.gradle.kts")),
          buildGradleKts("plugins { kotlin(\"jvm\") version \"2.3.0\" }; java { toolchain { languageVersion = JavaLanguageVersion.of(17) } }",
            source -> source.path("nested/build.gradle.kts")),
          kotlin("class Proof", source -> source.path("nested/src/main/kotlin/Proof.kt")));
    }

    @Test void alignmentDoesNotAssumeOpaqueMarkerlessCompiler() {
        rewriteRun(spec -> spec.recipeFromResources("org.openrewrite.java.migrate.AlignGradleKotlinTargetsForJava25"),
          buildGradleKts("""
            val compilerVersion: String by project
            plugins { kotlin("jvm") version compilerVersion }
            java { toolchain { languageVersion = JavaLanguageVersion.of(17) } }
            """),
          properties("compilerVersion=2.3.0", source -> source.path("gradle.properties")),
          kotlin("class Proof", source -> source.path("src/main/kotlin/Proof.kt")));
    }

    @Test void alignmentRetainsModeledLegacyCompilerCap() {
        JavaProject module = new JavaProject(Tree.randomId(), "legacy", null);
        String before = """
          plugins { kotlin("jvm") version "1.8.20" }
          java { toolchain { languageVersion = JavaLanguageVersion.of(17) } }
          """;
        rewriteRun(spec -> spec.recipeFromResources("org.openrewrite.java.migrate.AlignGradleKotlinTargetsForJava25")
            .allSources(source -> source.markers(module)),
          buildGradleKts(before, before.replace("of(17)", "of(19)")),
          kotlin("class Proof", source -> source.path("src/main/kotlin/Proof.kt")));
    }
    @Test void peerAlignmentDoesNotTreatUnmodeledBuildSrcAsRootKotlinSources() {
        rewriteRun(spec -> spec.recipeFromResources("org.openrewrite.java.migrate.AlignGradleKotlinTargetsForJava25"),
          buildGradleKts("""
            plugins { kotlin("jvm") version "2.3.0" }
            java { toolchain { languageVersion = JavaLanguageVersion.of(17) } }
            """),
          kotlin("class BuildLogic", source -> source.path("buildSrc/src/main/kotlin/BuildLogic.kt")));
    }

    @Test void alignmentMatchesModeledProjectValuesAcrossDifferentMarkerIds() {
        JavaProject build = new JavaProject(Tree.randomId(), "legacy", new JavaProject.Publication("g", "a", "1"));
        JavaProject source = new JavaProject(Tree.randomId(), "legacy", new JavaProject.Publication("g", "a", "1"));
        String before = "plugins { kotlin(\"jvm\") version \"1.8.20\" }; java { toolchain { languageVersion = JavaLanguageVersion.of(17) } }";
        rewriteRun(spec -> spec.recipeFromResources("org.openrewrite.java.migrate.AlignGradleKotlinTargetsForJava25"),
          buildGradleKts(before, before.replace("of(17)", "of(19)"), file -> file.markers(build)),
          kotlin("class Proof", file -> file.path("src/main/kotlin/Proof.kt").markers(source)));
    }

    @Test void alignmentDistinguishesModeledProjectPublications() {
        JavaProject build = new JavaProject(Tree.randomId(), "legacy", new JavaProject.Publication("g", "a", "1"));
        JavaProject source = new JavaProject(Tree.randomId(), "legacy", new JavaProject.Publication("g", "other", "1"));
        rewriteRun(spec -> spec.recipeFromResources("org.openrewrite.java.migrate.AlignGradleKotlinTargetsForJava25"),
          buildGradleKts("plugins { kotlin(\"jvm\") version \"1.8.20\" }; java { toolchain { languageVersion = JavaLanguageVersion.of(17) } }",
            file -> file.markers(build)),
          kotlin("class Proof", file -> file.path("src/main/kotlin/Proof.kt").markers(source)));
    }
}
