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

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;

@EqualsAndHashCode(callSuper = false)
@Value
public class UpgradeDockerImageVersion extends Recipe {

    @Option(displayName = "Java version",
            description = "The Java version to upgrade to.",
            example = "11")
    Integer version;

    private static final String NEW_IMAGE = "eclipse-temurin";
    private static final Set<String> DEPRECATED_IMAGES = new HashSet<>(asList("openjdk", "adoptopenjdk"));
    private static final Set<String> CURRENT_IMAGES = new HashSet<>(asList(
            "eclipse-temurin", "amazoncorretto", "azul/zulu-openjdk",
            "bellsoft/liberica-openjdk-debian", "bellsoft/liberica-openjdk-alpine",
            "bellsoft/liberica-openjdk-centos", "ibm-semeru-runtimes", "sapmachine"
    ));

    private static final int OLDEST_VERSION = 8;
    private static final Pattern VERSIONED_TAG = Pattern.compile("(\\d{1,3})(\\D.*)?");

    String displayName = "Upgrade Docker image Java version";
    String description = "Upgrade Docker image tags to use the specified Java version. " +
            "Updates common Java Docker images including eclipse-temurin, amazoncorretto, azul/zulu-openjdk, " +
            "and others. Also migrates deprecated images (openjdk, adoptopenjdk) to eclipse-temurin, " +
            "preserving any tag suffix such as `-jre-alpine`. Image references built from build arguments or " +
            "environment variables are left untouched, as their value can not be determined statically.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        if (version == null) {
            return TreeVisitor.noop();
        }
        return new DockerFrom.Matcher().asVisitor((image, ctx) -> {
            String imageName = image.getImageName().orElse("");
            String tag = image.getTag().orElse("");
            if (containsVariable(imageName) || containsVariable(tag)) {
                return image.getTree();
            }

            Matcher matcher = VERSIONED_TAG.matcher(tag);
            if (!matcher.matches()) {
                return image.getTree();
            }
            int currentVersion = Integer.parseInt(matcher.group(1));
            if (currentVersion < OLDEST_VERSION || version <= currentVersion) {
                return image.getTree();
            }

            String newTag = version + (matcher.group(2) == null ? "" : matcher.group(2));
            if (DEPRECATED_IMAGES.contains(imageName)) {
                return image.withImageReference(NEW_IMAGE + ":" + newTag +
                        image.getDigest().map(digest -> "@" + digest).orElse(""));
            }
            if (CURRENT_IMAGES.contains(imageName)) {
                return image.withTag(newTag);
            }
            return image.getTree();
        });
    }

    private static boolean containsVariable(String imageReferencePart) {
        return imageReferencePart.indexOf('$') != -1;
    }
}
