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
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.docker.DockerIsoVisitor;
import org.openrewrite.docker.trait.DockerFrom;
import org.openrewrite.docker.tree.Docker;
import org.openrewrite.internal.ListUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

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

    private static final String ARG_DEFAULTS = "argDefaults";
    private static final String ARG_UPGRADES = "argUpgrades";

    String displayName = "Upgrade Docker image Java version";
    String description = "Upgrade Docker image tags to use the specified Java version. " +
            "Updates common Java Docker images including eclipse-temurin, amazoncorretto, azul/zulu-openjdk, " +
            "and others. Also migrates deprecated images (openjdk, adoptopenjdk) to eclipse-temurin, " +
            "preserving any tag suffix such as `-jre-alpine`. When a `FROM` is built from a build argument, the " +
            "default value of the corresponding global `ARG` is upgraded instead, such that `ARG java_version=17` " +
            "used as `FROM eclipse-temurin:${java_version}` becomes `ARG java_version=25`. Image references built " +
            "from arguments without a default value are left untouched, as their value can not be determined " +
            "statically. A digest pin is dropped when the tag is upgraded, as the stale digest would otherwise " +
            "keep resolving to the old image.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        if (version == null) {
            return TreeVisitor.noop();
        }
        return new DockerIsoVisitor<ExecutionContext>() {

            @Override
            public Docker.File visitFile(Docker.File file, ExecutionContext ctx) {
                Map<String, String> defaults = new HashMap<>();
                for (Docker.Arg arg : file.getGlobalArgs()) {
                    Docker.Literal literalDefault = literalDefault(arg);
                    if (literalDefault != null) {
                        defaults.put(arg.getName().getText(), literalDefault.getText());
                    }
                }
                Map<String, String> upgrades = new HashMap<>();
                getCursor().putMessage(ARG_DEFAULTS, defaults);
                getCursor().putMessage(ARG_UPGRADES, upgrades);

                Docker.File f = super.visitFile(file, ctx);
                if (upgrades.isEmpty()) {
                    return f;
                }
                return f.withGlobalArgs(ListUtils.map(f.getGlobalArgs(), arg -> {
                    String upgraded = upgrades.get(arg.getName().getText());
                    Docker.Literal literalDefault = literalDefault(arg);
                    if (upgraded == null || literalDefault == null) {
                        return arg;
                    }
                    return arg.withValue(requireNonNull(arg.getValue())
                            .withContents(singletonList(literalDefault.withText(upgraded))));
                }));
            }

            @Override
            public Docker.From visitFrom(Docker.From from, ExecutionContext ctx) {
                DockerFrom image = new DockerFrom(getCursor());
                String imageName = image.getImageName().orElse("");
                String tag = image.getTag().orElse("");
                if (!containsVariable(imageName) && !containsVariable(tag)) {
                    return upgradeLiteralFrom(image, imageName, tag);
                }
                return upgradeThroughArgs(from,
                        getCursor().getNearestMessage(ARG_DEFAULTS, emptyMap()),
                        getCursor().getNearestMessage(ARG_UPGRADES, new HashMap<>()));
            }

            private Docker.From upgradeLiteralFrom(DockerFrom image, String imageName, String tag) {
                String newTag = upgradedTag(tag);
                if (newTag == null) {
                    return image.getTree();
                }
                if (DEPRECATED_IMAGES.contains(imageName)) {
                    return image.withImageReference(NEW_IMAGE + ":" + newTag);
                }
                if (CURRENT_IMAGES.contains(imageName)) {
                    return image.withTag(newTag).withDigest(null);
                }
                return image.getTree();
            }

            /**
             * Upgrade a {@code FROM} whose image name or tag is built from a build argument, by rewriting the
             * default value of the global {@code ARG} that supplies it. Only arguments that carry a literal
             * default are resolvable; anything else is left untouched.
             */
            private Docker.From upgradeThroughArgs(Docker.From from, Map<String, String> defaults, Map<String, String> upgrades) {
                String imageVariable = soleVariable(from.getImageName());
                String imageName = imageVariable == null ?
                        literalText(from.getImageName()) :
                        defaults.get(imageVariable);
                if (imageName == null) {
                    return from;
                }

                String tagVariable;
                String tag;
                if (from.getTag() == null) {
                    // A single argument holding the whole `name:tag` reference, as in `FROM ${BASE_IMAGE}`
                    String[] reference = splitReference(imageName);
                    if (imageVariable == null || reference == null) {
                        return from;
                    }
                    imageName = reference[0];
                    tag = reference[1];
                    tagVariable = imageVariable;
                } else {
                    tagVariable = leadingVariable(from.getTag());
                    tag = tagVariable == null ? literalText(from.getTag()) : defaults.get(tagVariable);
                }
                if (tag == null) {
                    return from;
                }

                String newImageName = upgradedImageName(imageName);
                String newTag = upgradedTag(tag);
                if (newImageName == null || newTag == null) {
                    return from;
                }

                if (tagVariable != null && tagVariable.equals(imageVariable)) {
                    upgrades.put(imageVariable, newImageName + ":" + newTag);
                    return from;
                }
                if (tagVariable == null) {
                    from = new DockerFrom(getCursor()).withTag(newTag);
                } else {
                    upgrades.put(tagVariable, newTag);
                }
                if (!newImageName.equals(imageName)) {
                    if (imageVariable == null) {
                        from = withImageName(from, newImageName);
                    } else {
                        upgrades.put(imageVariable, newImageName);
                    }
                }
                return from.withDigest(null);
            }
        };
    }

    private @Nullable String upgradedImageName(String imageName) {
        if (DEPRECATED_IMAGES.contains(imageName)) {
            return NEW_IMAGE;
        }
        return CURRENT_IMAGES.contains(imageName) ? imageName : null;
    }

    private @Nullable String upgradedTag(String tag) {
        Matcher matcher = VERSIONED_TAG.matcher(tag);
        if (!matcher.matches()) {
            return null;
        }
        int currentVersion = Integer.parseInt(matcher.group(1));
        if (currentVersion < OLDEST_VERSION || version <= currentVersion) {
            return null;
        }
        return version + (matcher.group(2) == null ? "" : matcher.group(2));
    }

    private static boolean containsVariable(String imageReferencePart) {
        return imageReferencePart.indexOf('$') != -1;
    }

    private static Docker.@Nullable Literal literalDefault(Docker.Arg arg) {
        Docker.Argument value = arg.getValue();
        return value == null ? null : sole(value.getContents(), Docker.Literal.class);
    }

    private static @Nullable String literalText(Docker.Argument argument) {
        Docker.Literal literal = sole(argument.getContents(), Docker.Literal.class);
        return literal == null ? null : literal.getText();
    }

    private static @Nullable String soleVariable(Docker.Argument argument) {
        Docker.EnvironmentVariable variable = sole(argument.getContents(), Docker.EnvironmentVariable.class);
        return variable == null ? null : variable.getName();
    }

    /**
     * The name of the variable a tag starts with, as in the {@code JAVA_VERSION} of {@code ${JAVA_VERSION}-jre},
     * or null when the tag does not start with a variable or holds a further variable we can not resolve.
     */
    private static @Nullable String leadingVariable(Docker.Argument argument) {
        List<Docker.ArgumentContent> contents = argument.getContents();
        if (contents.isEmpty() || !(contents.get(0) instanceof Docker.EnvironmentVariable)) {
            return null;
        }
        for (int i = 1; i < contents.size(); i++) {
            if (!(contents.get(i) instanceof Docker.Literal)) {
                return null;
            }
        }
        return ((Docker.EnvironmentVariable) contents.get(0)).getName();
    }

    private static <T> @Nullable T sole(List<? extends Docker.ArgumentContent> contents, Class<T> type) {
        if (contents.size() == 1 && type.isInstance(contents.get(0))) {
            return type.cast(contents.get(0));
        }
        return null;
    }

    /**
     * Splits an image reference into its name and tag, dropping any digest; null when there is no tag.
     */
    private static String @Nullable [] splitReference(String reference) {
        int at = reference.indexOf('@');
        String withoutDigest = at == -1 ? reference : reference.substring(0, at);
        int colon = withoutDigest.indexOf(':', withoutDigest.lastIndexOf('/') + 1);
        if (colon == -1) {
            return null;
        }
        return new String[]{withoutDigest.substring(0, colon), withoutDigest.substring(colon + 1)};
    }

    private static Docker.From withImageName(Docker.From from, String imageName) {
        Docker.Argument argument = from.getImageName();
        Docker.Literal literal = requireNonNull(sole(argument.getContents(), Docker.Literal.class));
        return from.withImageName(argument.withContents(singletonList(literal.withText(imageName))));
    }
}
