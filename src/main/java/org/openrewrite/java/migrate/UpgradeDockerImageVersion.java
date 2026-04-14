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

    private static final String[] DEPRECATED_IMAGES = {"openjdk", "adoptopenjdk"};
    private static final String[] CURRENT_IMAGES = {
            "eclipse-temurin", "amazoncorretto", "azul/zulu-openjdk",
            "bellsoft/liberica-openjdk-debian", "bellsoft/liberica-openjdk-alpine",
            "bellsoft/liberica-openjdk-centos", "ibm-semeru-runtimes", "sapmachine"
    };

    String displayName = "Upgrade Docker image Java version";
    String description = "Upgrade Docker image tags to use the specified Java version. " +
            "Updates common Java Docker images including eclipse-temurin, amazoncorretto, azul/zulu-openjdk, " +
            "and others. Also migrates deprecated images (openjdk, adoptopenjdk) to eclipse-temurin. " +
            "Uses a single `ChangeFrom` glob capture per (image, oldVersion) to preserve any tag suffix.";

    @Override
    public List<Recipe> getRecipeList() {
        List<Recipe> recipes = new ArrayList<>();
        if (version == null) {
            return recipes;
        }
        String newVer = version.toString();
        for (int oldVersion = 8; oldVersion < version; oldVersion++) {
            String oldTag = oldVersion + "*";
            String newTag = newVer + "$1";
            for (String image : DEPRECATED_IMAGES) {
                recipes.add(new ChangeFrom(image, oldTag, null, null, "eclipse-temurin", newTag, null, null));
            }
            for (String image : CURRENT_IMAGES) {
                recipes.add(new ChangeFrom(image, oldTag, null, null, null, newTag, null, null));
            }
        }
        return recipes;
    }
}
