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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.docker.trait.DockerFrom;
import org.openrewrite.docker.tree.Docker;
import org.openrewrite.docker.tree.Space;
import org.openrewrite.marker.Markers;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static java.util.Collections.singletonList;
import static org.openrewrite.Tree.randomId;

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

    private static final Set<String> DEPRECATED_IMAGES = new HashSet<>(Arrays.asList(
            "openjdk", "adoptopenjdk"));
    private static final Set<String> CURRENT_IMAGES = new HashSet<>(Arrays.asList(
            "eclipse-temurin", "amazoncorretto", "azul/zulu-openjdk",
            "bellsoft/liberica-openjdk-debian", "bellsoft/liberica-openjdk-alpine",
            "bellsoft/liberica-openjdk-centos", "ibm-semeru-runtimes", "sapmachine"));

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        if (version == null) {
            return TreeVisitor.noop();
        }
        return new DockerFrom.Matcher().imageName("*").asVisitor((image, ctx) -> {
            Docker.From f = image.getTree();
            String imageName = image.getImageName();
            String tag = image.getTag();
            if (imageName == null || tag == null) {
                return f;
            }

            boolean isDeprecated = DEPRECATED_IMAGES.contains(imageName);
            if (!isDeprecated && !CURRENT_IMAGES.contains(imageName)) {
                return f;
            }

            // Parse leading version number from tag
            int i = 0;
            while (i < tag.length() && Character.isDigit(tag.charAt(i))) {
                i++;
            }
            if (i == 0) {
                return f;
            }
            int oldVersion;
            try {
                oldVersion = Integer.parseInt(tag.substring(0, i));
            } catch (NumberFormatException e) {
                return f;
            }
            if (oldVersion < 8 || oldVersion >= version) {
                return f;
            }

            // Preserve suffix only when it starts with '-' (e.g. "-jdk-alpine")
            String suffix = tag.substring(i);
            String newTag = suffix.startsWith("-") ? version + suffix : version.toString();
            String newImageName = isDeprecated ? "eclipse-temurin" : null;

            Docker.Literal.QuoteStyle quoteStyle = image.getQuoteStyle();
            Docker.From result = f;

            // Handle single-content case (image:tag in one literal)
            boolean singleContent = f.getImageName().getContents().size() == 1 &&
                    f.getTag() == null && f.getDigest() == null;
            if (singleContent) {
                String imagePart = newImageName != null ? newImageName : imageName;
                Docker.ArgumentContent content = new Docker.Literal(
                        randomId(), Space.EMPTY, Markers.EMPTY,
                        imagePart + ":" + newTag, quoteStyle);
                return result.withImageName(f.getImageName().withContents(singletonList(content)));
            }

            if (newImageName != null) {
                Docker.ArgumentContent content = new Docker.Literal(
                        randomId(), Space.EMPTY, Markers.EMPTY, newImageName, quoteStyle);
                result = result.withImageName(f.getImageName().withContents(singletonList(content)));
            }

            Docker.ArgumentContent tagContent = new Docker.Literal(
                    randomId(), Space.EMPTY, Markers.EMPTY, newTag, quoteStyle);
            result = result.withTag(new Docker.Argument(
                    randomId(), Space.EMPTY, Markers.EMPTY, singletonList(tagContent)));
            return result;
        });
    }
}
