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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.docker.ChangeFrom;
import org.openrewrite.gradle.UpdateJavaCompatibility;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.marker.JavaVersion;
import org.openrewrite.java.tree.J;
import org.openrewrite.maven.UpdateMavenProjectPropertyJavaVersion;
import org.openrewrite.maven.UseMavenCompilerPluginReleaseConfiguration;

import java.time.Duration;
import java.util.*;

@EqualsAndHashCode(callSuper = false)
@Value
public class UpgradeJavaVersion extends Recipe {

    @Option(displayName = "Java version",
            description = "The Java version to upgrade to.",
            example = "11")
    Integer version;

    String displayName = "Upgrade Java version";

    String description = "Upgrade build plugin configuration to use the specified Java version. " +
            "This recipe changes `java.toolchain.languageVersion` in `build.gradle(.kts)` of gradle projects, " +
            "or maven-compiler-plugin target version and related settings. " +
            "Will not downgrade if the version is newer than the specified version.";

    @Override
    public List<Recipe> getRecipeList() {
        List<Recipe> recipes = new ArrayList<>(Arrays.asList(
                new UseMavenCompilerPluginReleaseConfiguration(version),
                new UpdateMavenProjectPropertyJavaVersion(version),
                new org.openrewrite.jenkins.UpgradeJavaVersion(version, null),
                new UpdateJavaCompatibility(version, null, null, false, null),
                new UpdateSdkMan(String.valueOf(version), null)
        ));
        recipes.addAll(createDockerImageUpgradeRecipes());
        return recipes;
    }

    private List<Recipe> createDockerImageUpgradeRecipes() {
        List<Recipe> recipes = new ArrayList<>();
        if (version == null) { // for uninitialized version
            return recipes;
        }
        // Deprecated images -> migrate to eclipse-temurin
        String[] deprecatedImages = {"openjdk", "adoptopenjdk"};
        String[] currentImages = {
                "eclipse-temurin", "amazoncorretto", "azul/zulu-openjdk",
                "bellsoft/liberica-openjdk-debian", "bellsoft/liberica-openjdk-alpine",
                "bellsoft/liberica-openjdk-centos", "ibm-semeru-runtimes", "sapmachine"
        };
        // Common tag suffixes to preserve when upgrading current images
        // Longer suffixes must come before shorter ones to match correctly
        String[] commonSuffixes = {
                "-jdk-alpine", "-jre-alpine",
                "-jdk-noble", "-jre-noble",
                "-jdk-jammy", "-jre-jammy",
                "-jdk-focal", "-jre-focal",
                "-jdk-centos7", "-jre-centos7",
                "-jdk-ubi9-minimal", "-jre-ubi9-minimal",
                "-jdk-nanoserver", "-jre-nanoserver",
                "-jdk-windowsservercore", "-jre-windowsservercore",
                "-alpine",
                "-jdk", "-jre"
        };
        for (int oldVersion = 8; oldVersion < version; oldVersion++) {
            // Deprecated images: match specific suffixes first to preserve them
            for (String image : deprecatedImages) {
                for (String suffix : commonSuffixes) {
                    recipes.add(new ChangeFrom(image, oldVersion + suffix, null, null, "eclipse-temurin", version + suffix, null, null));
                }
            }
            // Deprecated images: fall back to wildcard for remaining patterns
            for (String image : deprecatedImages) {
                recipes.add(new ChangeFrom(image, oldVersion + "*", null, null, "eclipse-temurin", version.toString(), null, null));
            }
            // Current images: match specific suffixes first to preserve them
            for (String image : currentImages) {
                for (String suffix : commonSuffixes) {
                    recipes.add(new ChangeFrom(image, oldVersion + suffix, null, null, null, version + suffix, null, null));
                }
            }
            // Current images: fall back to wildcard for remaining patterns
            for (String image : currentImages) {
                recipes.add(new ChangeFrom(image, oldVersion + "*", null, null, null, version.toString(), null, null));
            }
        }
        return recipes;
    }

    /**
     * This recipe only updates markers, so it does not correspond to human manual effort.
     *
     * @return Zero estimated time.
     */
    Duration estimatedEffortPerOccurrence = Duration.ofMinutes(0);

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        String newVersion = version.toString();
        Map<JavaVersion, JavaVersion> updatedMarkers = new HashMap<>();
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J preVisit(J tree, ExecutionContext ctx) {
                Optional<JavaVersion> maybeJavaVersion = tree.getMarkers().findFirst(JavaVersion.class);
                if (maybeJavaVersion.isPresent() && maybeJavaVersion.get().getMajorVersion() < version) {
                    return tree.withMarkers(tree.getMarkers().setByType(updatedMarkers.computeIfAbsent(maybeJavaVersion.get(),
                            m -> m.withSourceCompatibility(newVersion).withTargetCompatibility(newVersion))));
                }
                return tree;
            }
        };
    }
}
