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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.docker.ChangeFrom;

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = false)
@Value
public class UpgradeDockerImageVersion extends Recipe {

    @Option(displayName = "Java version",
            description = "The Java version to upgrade to.",
            example = "11")
    Integer version;

    String displayName = "Upgrade Docker image Java version";
    String description = "Upgrade Docker image tags to use the specified Java version. " +
            "Updates common Java Docker images including eclipse-temurin, amazoncorretto, azul/zulu-openjdk, " +
            "and others. Also migrates deprecated images (openjdk, adoptopenjdk) to eclipse-temurin.";

    @Override
    public List<Recipe> getRecipeList() {
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
                // Alpine
                "-jdk-alpine", "-jre-alpine",
                // Ubuntu LTS
                "-jdk-noble", "-jre-noble",
                "-jdk-jammy", "-jre-jammy",
                "-jdk-focal", "-jre-focal",
                "-jdk-bionic", "-jre-bionic",
                // Debian
                "-jdk-slim-bookworm", "-jre-slim-bookworm",
                "-jdk-slim-bullseye", "-jre-slim-bullseye",
                "-jdk-slim-buster", "-jre-slim-buster",
                "-jdk-bookworm", "-jre-bookworm",
                "-jdk-bullseye", "-jre-bullseye",
                "-jdk-buster", "-jre-buster",
                // Other Linux
                "-jdk-centos7", "-jre-centos7",
                "-jdk-ubi9-minimal", "-jre-ubi9-minimal",
                // Windows
                "-jdk-nanoserver", "-jre-nanoserver",
                "-jdk-windowsservercore", "-jre-windowsservercore",
                // Generic suffixes (must come last)
                "-alpine", "-slim",
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
}
