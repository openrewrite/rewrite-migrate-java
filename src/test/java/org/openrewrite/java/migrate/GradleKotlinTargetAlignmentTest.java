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
import org.openrewrite.test.RewriteTest;
import static org.openrewrite.gradle.Assertions.buildGradle;
import static org.openrewrite.gradle.Assertions.buildGradleKts;
import static org.openrewrite.gradle.Assertions.settingsGradleKts;

class GradleKotlinTargetAlignmentTest implements RewriteTest {
    @Test void groovyEnumAndProvider() {
        rewriteRun(spec -> spec.recipe(new UpgradeKotlinJvmTargetVersion(24)),
          buildGradle("kotlin { compilerOptions { jvmTarget = JvmTarget.JVM_17 } }", "kotlin { compilerOptions { jvmTarget = JvmTarget.JVM_24 } }"));
    }
    @Test void groovyProvider() {
        rewriteRun(spec -> spec.recipe(new UpgradeKotlinJvmTargetVersion(24)),
          buildGradle("kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }", "kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_24) } }"));
    }
    @Test void legacyCompilerCap() {
        rewriteRun(spec -> spec.recipe(new UpgradeJavaVersion(25)),
          buildGradle("""
            plugins { id 'org.jetbrains.kotlin.jvm' version '1.9.22' }
            java { toolchain { languageVersion = JavaLanguageVersion.of(17) } }
            tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile) {
                kotlinOptions { jvmTarget = '17' }
            }
            """, """
            plugins { id 'org.jetbrains.kotlin.jvm' version '1.9.22' }
            java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }
            tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile) {
                kotlinOptions { jvmTarget = '21' }
            }
            """));
    }

    @Test void modernCompilerUsesUpdatedPluginRatherThanStaleModel() {
        rewriteRun(spec -> spec.recipe(new UpgradeJavaVersion(25)),
          buildGradle("""
            plugins { id 'org.jetbrains.kotlin.jvm' version '2.3.21' }
            java { toolchain { languageVersion = JavaLanguageVersion.of(24) } }
            kotlin { compilerOptions { jvmTarget = JvmTarget.JVM_17 } }
            """, """
            plugins { id 'org.jetbrains.kotlin.jvm' version '2.3.21' }
            java { toolchain { languageVersion = JavaLanguageVersion.of(25) } }
            kotlin { compilerOptions { jvmTarget = JvmTarget.JVM_25 } }
            """));
    }
    @Test void olderCompilerHasConservativeSupportedCap() {
        rewriteRun(spec -> spec.recipe(new UpgradeJavaVersion(25)),
          buildGradle("""
            plugins { id 'org.jetbrains.kotlin.jvm' version '1.8.20' }
            java { toolchain { languageVersion = JavaLanguageVersion.of(8) } }
            tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile) {
                kotlinOptions { jvmTarget = '1.8' }
            }
            """, """
            plugins { id 'org.jetbrains.kotlin.jvm' version '1.8.20' }
            java { toolchain { languageVersion = JavaLanguageVersion.of(19) } }
            tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile) {
                kotlinOptions { jvmTarget = '19' }
            }
            """));
    }
    @Test void unmodeledNestedBuildStaysUnchanged() {
        rewriteRun(spec -> spec.recipe(new UpgradeJavaVersion(25)),
          buildGradleKts("""
            plugins { kotlin("jvm") version "1.9.22" }
            java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }
            """, s -> s.path("buildSrc/build.gradle.kts")));
    }
    @Test void unrelatedEnumAndHigherTargetStayUnchanged() {
        rewriteRun(spec -> spec.recipe(new UpgradeKotlinJvmTargetVersion(24)),
          buildGradle("""
            kotlin { compilerOptions { jvmTarget = JvmTarget.JVM_25 } }
            custom { jvmTarget = JvmTarget.JVM_17 }
            """));
    }

    @Test void dynamicLegacyTargetUsesModernVersionString() {
        rewriteRun(spec -> spec.recipe(new UpgradeJavaVersion(25)),
          buildGradleKts("""
            plugins { kotlin("jvm") version "1.9.22" }
            val jvmVersion = JavaLanguageVersion.of(17)
            java { toolchain.languageVersion.set(jvmVersion) }
            tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
                kotlinOptions { jvmTarget = "1.$jvmVersion" }
            }
            """, """
            plugins { kotlin("jvm") version "1.9.22" }
            val jvmVersion = JavaLanguageVersion.of(21)
            java { toolchain.languageVersion.set(jvmVersion) }
            tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
                kotlinOptions { jvmTarget = "$jvmVersion" }
            }
            """));
    }
    @Test void unknownDynamicTargetIsPreserved() {
        rewriteRun(spec -> spec.recipe(new UpgradeKotlinJvmTargetVersion(25)),
          buildGradleKts("""
            val jvmVersion = providers.gradleProperty("target")
            tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
                kotlinOptions { jvmTarget = "1.$jvmVersion" }
            }
            """));
    }

    @Test void explicitReceiverDoesNotSupplyKotlinCompilerVersion() {
        rewriteRun(spec -> spec.recipe(new UpgradeJavaVersion(25)),
          buildGradle("""
            plugins { custom.id('org.jetbrains.kotlin.jvm').version('1.9.22') }
            java { toolchain { languageVersion = JavaLanguageVersion.of(17) } }
            """, """
            plugins { custom.id('org.jetbrains.kotlin.jvm').version('1.9.22') }
            java { toolchain { languageVersion = JavaLanguageVersion.of(25) } }
            """));
    }

    @Test void customNonStringPluginDslIsNotACompiler() {
        rewriteRun(spec -> spec.recipe(new UpgradeJavaVersion(25)),
          buildGradle("""
            plugins { custom.id(123).version(456) }
            java { toolchain { languageVersion = JavaLanguageVersion.of(17) } }
            """, """
            plugins { custom.id(123).version(456) }
            java { toolchain { languageVersion = JavaLanguageVersion.of(25) } }
            """));
    }

    @Test void unresolvedCompilerVersionDoesNotAssumeNewBytecodeSupport() {
        rewriteRun(spec -> spec.recipe(new UpgradeJavaVersion(25)),
          buildGradle("""
            plugins { id 'org.jetbrains.kotlin.jvm' version kotlinVersion }
            java { toolchain { languageVersion = JavaLanguageVersion.of(17) } }
            """));
    }
    @Test void peerLegacyReleaseHonorsCap() {
        rewriteRun(spec -> spec.recipe(new UpgradeJavaVersion(25)),
          buildGradle("""
            plugins { id 'org.jetbrains.kotlin.jvm' version '1.9.22' }
            java { toolchain { languageVersion = JavaLanguageVersion.of(17) } }
            tasks.withType(JavaCompile) { options.release = 17 }
            kotlin { compilerOptions { jvmTarget = JvmTarget.JVM_17 } }
            """, """
            plugins { id 'org.jetbrains.kotlin.jvm' version '1.9.22' }
            java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }
            tasks.withType(JavaCompile) { options.release = 21 }
            kotlin { compilerOptions { jvmTarget = JvmTarget.JVM_21 } }
            """));
    }
    @Test void peerExplicitCustomPluginDoesNotCapJava() {
        rewriteRun(spec -> spec.recipe(new UpgradeGradleJavaVersion(25)),
          buildGradle("""
            plugins { custom.id('org.jetbrains.kotlin.jvm').version('1.8.20') }
            java { toolchain { languageVersion = JavaLanguageVersion.of(17) } }
            """, """
            plugins { custom.id('org.jetbrains.kotlin.jvm').version('1.8.20') }
            java { toolchain { languageVersion = JavaLanguageVersion.of(25) } }
            """));
    }
    @Test void peerVersionlessSiblingUsesKnownCompiler() {
        rewriteRun(spec -> spec.recipe(new UpgradeGradleJavaVersion(25)),
          buildGradle("""
            plugins {
                id 'org.jetbrains.kotlin.jvm' version '1.9.22'
                id 'org.jetbrains.kotlin.plugin.spring'
            }
            java { toolchain { languageVersion = JavaLanguageVersion.of(17) } }
            """, """
            plugins {
                id 'org.jetbrains.kotlin.jvm' version '1.9.22'
                id 'org.jetbrains.kotlin.plugin.spring'
            }
            java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }
            """));
    }
    @Test void peerVersionlessJvmDoesNotAssumeCompilerSupport() {
        rewriteRun(spec -> spec.recipe(new UpgradeGradleJavaVersion(25)),
          buildGradle("""
            plugins { id 'org.jetbrains.kotlin.jvm' }
            java { toolchain { languageVersion = JavaLanguageVersion.of(17) } }
            kotlin { compilerOptions { jvmTarget = JvmTarget.JVM_17 } }
            """));
    }

    @Test void unappliedKotlinCompilerDoesNotCapJavaOnlyBuild() {
        rewriteRun(spec -> spec.recipe(new UpgradeJavaVersion(25)),
          buildGradle("""
            plugins {
                id 'java'
                id 'org.jetbrains.kotlin.jvm' version '1.9.22' apply false
            }
            sourceCompatibility = 17
            targetCompatibility = 17
            """, """
            plugins {
                id 'java'
                id 'org.jetbrains.kotlin.jvm' version '1.9.22' apply false
            }
            sourceCompatibility = 25
            targetCompatibility = 25
            """));
    }
    @Test void unappliedKotlinDslCompilerDoesNotCapJavaOnlyBuild() {
        rewriteRun(spec -> spec.recipe(new UpgradeJavaVersion(25)),
          buildGradleKts("""
            plugins {
                java
                kotlin("jvm") version "1.9.22" apply false
            }
            java {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
            """, """
            plugins {
                java
                kotlin("jvm") version "1.9.22" apply false
            }
            java {
                sourceCompatibility = JavaVersion.VERSION_25
                targetCompatibility = JavaVersion.VERSION_25
            }
            """));
    }
    @Test void javascriptKotlinPluginDoesNotSupplyJvmCompiler() {
        rewriteRun(spec -> spec.recipe(new UpgradeJavaVersion(25)),
          buildGradle("""
            plugins {
                id 'java'
                id 'org.jetbrains.kotlin.js' version '1.9.22'
            }
            sourceCompatibility = 17
            targetCompatibility = 17
            """, """
            plugins {
                id 'java'
                id 'org.jetbrains.kotlin.js' version '1.9.22'
            }
            sourceCompatibility = 25
            targetCompatibility = 25
            """));
    }
    @Test void serializationPluginAloneDoesNotSupplyJvmCompiler() {
        rewriteRun(spec -> spec.recipe(new UpgradeJavaVersion(25)),
          buildGradleKts("""
            plugins {
                java
                kotlin("plugin.serialization") version "1.9.22"
            }
            java { toolchain.languageVersion.set(JavaLanguageVersion.of(17)) }
            """, """
            plugins {
                java
                kotlin("plugin.serialization") version "1.9.22"
            }
            java { toolchain.languageVersion.set(JavaLanguageVersion.of(25)) }
            """));
    }

    @Test void explicitlyAppliedCompilerStillBoundsBothTargets() {
        rewriteRun(spec -> spec.recipe(new UpgradeJavaVersion(25)),
          buildGradle("""
            plugins { id 'org.jetbrains.kotlin.jvm' version '1.9.22' apply true }
            java { toolchain { languageVersion = JavaLanguageVersion.of(17) } }
            kotlin { compilerOptions { jvmTarget = JvmTarget.JVM_17 } }
            """, """
            plugins { id 'org.jetbrains.kotlin.jvm' version '1.9.22' apply true }
            java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }
            kotlin { compilerOptions { jvmTarget = JvmTarget.JVM_21 } }
            """));
    }
    @Test void managedUnappliedCompilerDoesNotCapJavaOnlyBuild() {
        rewriteRun(spec -> spec.recipe(new UpgradeJavaVersion(25)),
          settingsGradleKts("""
            pluginManagement {
                plugins { kotlin("jvm") version "1.9.22" }
            }
            """),
          buildGradleKts("""
            plugins { java }
            java { toolchain.languageVersion.set(JavaLanguageVersion.of(17)) }
            """, """
            plugins { java }
            java { toolchain.languageVersion.set(JavaLanguageVersion.of(25)) }
            """));
    }
    @Test void versionlessExplicitApplicationRemainsConservative() {
        rewriteRun(spec -> spec.recipe(new UpgradeJavaVersion(25)),
          buildGradle("""
            plugins { id 'org.jetbrains.kotlin.jvm' apply true }
            java { toolchain { languageVersion = JavaLanguageVersion.of(17) } }
            kotlin { compilerOptions { jvmTarget = JvmTarget.JVM_17 } }
            """));
    }

    @Test void ownedPluginVersionVariableAlignsTargets() {
        rewriteRun(spec -> spec.recipe(new UpgradeJavaVersion(25)),
          buildGradleKts("""
            plugins {
                val kotlinVersion = "2.3.0"
                kotlin("jvm") version kotlinVersion
                kotlin("plugin.spring") version kotlinVersion
            }
            java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }
            kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }
            """, """
            plugins {
                val kotlinVersion = "2.3.0"
                kotlin("jvm") version kotlinVersion
                kotlin("plugin.spring") version kotlinVersion
            }
            java { toolchain { languageVersion.set(JavaLanguageVersion.of(25)) } }
            kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_25) } }
            """));
    }
    @Test void ownedLegacyPluginVersionVariableKeepsCompilerCap() {
        rewriteRun(spec -> spec.recipe(new UpgradeJavaVersion(25)),
          buildGradleKts("""
            plugins { val compilerVersion = "1.9.22"; kotlin("jvm") version compilerVersion }
            java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }
            """, """
            plugins { val compilerVersion = "1.9.22"; kotlin("jvm") version compilerVersion }
            java { toolchain { languageVersion.set(JavaLanguageVersion.of(21)) } }
            """));
    }
    @Test void reassignedPluginVersionVariableRemainsUnknown() {
        rewriteRun(spec -> spec.recipe(new UpgradeJavaVersion(25)),
          buildGradleKts("""
            plugins {
                var compilerVersion = "2.3.0"
                compilerVersion = "1.8.20"
                kotlin("jvm") version compilerVersion
            }
            java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }
            """));
    }
    @Test void collidingPluginVersionVariablesRemainUnknown() {
        rewriteRun(spec -> spec.recipe(new UpgradeJavaVersion(25)),
          buildGradleKts("""
            plugins {
                val compilerVersion = "2.3.0"
                class Other { val compilerVersion = "1.8.20" }
                kotlin("jvm") version compilerVersion
            }
            java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }
            """));
    }

    @Test void pluginVersionVariableFromAnotherScopeRemainsUnknown() {
        rewriteRun(spec -> spec.recipe(new UpgradeJavaVersion(25)),
          buildGradleKts("""
            val compilerVersion = "2.3.0"
            plugins { kotlin("jvm") version compilerVersion }
            java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }
            """));
    }
    @Test void unappliedPluginVersionVariableDoesNotCapJava() {
        rewriteRun(spec -> spec.recipe(new UpgradeJavaVersion(25)),
          buildGradleKts("""
            plugins { val compilerVersion = "1.8.20"; kotlin("jvm") version compilerVersion apply false }
            java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }
            """, """
            plugins { val compilerVersion = "1.8.20"; kotlin("jvm") version compilerVersion apply false }
            java { toolchain { languageVersion.set(JavaLanguageVersion.of(25)) } }
            """));
    }
}
