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

import java.util.ArrayList;
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class UpgradeJavaContainerImages extends Recipe {

    private static final String BITBUCKET = "bitbucket-pipelines.yml";
    private static final String GITLAB = ".gitlab-ci.yml";
    private static final String GITHUB_ACTIONS = ".github/workflows/*.yml;.github/workflows/*.yaml";
    private static final String CIRCLECI = ".circleci/config.yml";
    private static final String AZURE_PIPELINES = "**/azure-pipelines*.yml;**/azure-pipelines*.yaml";
    private static final String KUBERNETES = "**/*.yml;**/*.yaml";

    private static final String[][] IMAGES = {
            {BITBUCKET, "$.image"},
            {BITBUCKET, "$..step.image"},
            {BITBUCKET, "$..final.image"},
            {BITBUCKET, "$.definitions.services.*.image"},
            {GITLAB, "$.image"},
            {GITLAB, "$.*.image"},
            {GITLAB, "$.services[*]"},
            {GITLAB, "$.*.services[*]"},
            // Service containers are left alone, as these are not what the build runs on
            {GITHUB_ACTIONS, "$.jobs.*.container"},
            {GITHUB_ACTIONS, "$.jobs.*.container.image"},
            // The first image is the one the job runs in, the others are service containers
            {CIRCLECI, "$.jobs.*.docker[0].image"},
            {CIRCLECI, "$.executors.*.docker[0].image"},
            {AZURE_PIPELINES, "$.container"},
            {AZURE_PIPELINES, "$.container.image"},
            {AZURE_PIPELINES, "$..jobs[*].container"},
            {AZURE_PIPELINES, "$..jobs[*].container.image"},
            {AZURE_PIPELINES, "$.resources.containers[*].image"},
            {KUBERNETES, "$..containers[*].image"},
            {KUBERNETES, "$..initContainers[*].image"},
    };

    @Option(displayName = "Java version",
            description = "The Java version to upgrade to.",
            example = "25")
    Integer version;

    String displayName = "Upgrade Java container images in CI and Kubernetes files";
    String description = "Upgrade the Java version of the container images that builds run on in Bitbucket Pipelines, " +
            "GitLab CI, GitHub Actions, CircleCI and Azure Pipelines, and of the containers in Kubernetes manifests. " +
            "Images are upgraded as by `UpgradeDockerImageVersion`, which also moves deprecated `openjdk` images to " +
            "`eclipse-temurin`. Images that are not Java images are left untouched.";

    @Override
    public List<Recipe> getRecipeList() {
        List<Recipe> recipes = new ArrayList<>(IMAGES.length);
        for (String[] image : IMAGES) {
            recipes.add(new UpgradeJavaContainerImage(version, image[0], image[1]));
        }
        return recipes;
    }
}
