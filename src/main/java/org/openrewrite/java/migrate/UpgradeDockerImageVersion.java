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
import org.openrewrite.docker.trait.ImageName;
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
                    String value = arg.getValue() == null ? null : arg.getValue().getText();
                    if (value != null) {
                        defaults.put(arg.getName().getText(), value);
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
                    String name = arg.getName().getText();
                    String upgraded = upgrades.get(name);
                    // A name may be declared more than once; only the declaration the default was read from moves
                    if (upgraded == null || arg.getValue() == null || !defaults.get(name).equals(arg.getValue().getText())) {
                        return arg;
                    }
                    return arg.withValue(withText(arg.getValue(), upgraded));
                }));
            }

            @Override
            public Docker.From visitFrom(Docker.From from, ExecutionContext ctx) {
                if (containsVariable(from.getImageName()) || containsVariable(from.getTag())) {
                    return upgradeThroughArgs(from,
                            getCursor().getNearestMessage(ARG_DEFAULTS, emptyMap()),
                            getCursor().getNearestMessage(ARG_UPGRADES, new HashMap<>()));
                }

                DockerFrom image = new DockerFrom(getCursor());
                String newTag = upgradedTag(image.getTag().orElse(""));
                if (newTag == null) {
                    return from;
                }
                String imageName = image.getImageName().orElse("");
                String newImageName = upgradedImageName(imageName);
                if (newImageName == null) {
                    return from;
                }
                if (!newImageName.equals(imageName)) {
                    return image.withImageReference(newImageName + ":" + newTag);
                }
                return image.withTag(newTag).withDigest(null);
            }
        };
    }

    private Docker.From upgradeThroughArgs(Docker.From from, Map<String, String> defaults, Map<String, String> upgrades) {
        String imageVariable = soleVariable(from.getImageName());
        String tagVariable = from.getTag() == null ? null : leadingVariable(from.getTag());
        String imageName = imageVariable == null ?
                from.getImageName().getTextWithVariables() :
                defaults.get(imageVariable);
        if (imageName == null) {
            return from;
        }

        String tag;
        boolean wholeReference = from.getTag() == null;
        if (wholeReference) {
            // A single argument holding the whole reference, as in `FROM ${BASE_IMAGE}`
            String[] reference = imageVariable == null ? null : splitReference(imageName);
            if (reference == null) {
                return from;
            }
            imageName = reference[0];
            tag = reference[1];
        } else {
            tag = tagVariable == null ? from.getTag().getText() : defaults.get(tagVariable);
        }
        if (tag == null) {
            return from;
        }

        String newImageName = upgradedImageName(imageName);
        String newTag = upgradedTag(tag);
        if (newImageName == null || newTag == null) {
            return from;
        }

        if (wholeReference) {
            upgrades.put(requireNonNull(imageVariable), newImageName + ":" + newTag);
            return from.withDigest(null);
        }
        if (tagVariable == null) {
            from = from.withTag(withText(requireNonNull(from.getTag()), newTag));
        } else {
            upgrades.put(tagVariable, newTag);
        }
        if (!newImageName.equals(imageName)) {
            if (imageVariable == null) {
                from = from.withImageName(withRepository(from.getImageName(), imageName, newImageName));
            } else {
                upgrades.put(imageVariable, newImageName);
            }
        }
        return from.withDigest(null);
    }

    private @Nullable String upgradedImageName(String imageName) {
        ImageName parsed = ImageName.parse(imageName);
        String path = parsed.getPath();
        if (DEPRECATED_IMAGES.contains(path)) {
            String registry = parsed.getRegistry();
            return registry == null ? NEW_IMAGE : registry + '/' + NEW_IMAGE;
        }
        return CURRENT_IMAGES.contains(path) ? imageName : null;
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

    private static boolean containsVariable(Docker.@Nullable Argument argument) {
        return argument != null && argument.hasEnvironmentVariables();
    }

    private static @Nullable String soleVariable(Docker.Argument argument) {
        List<Docker.ArgumentContent> contents = argument.getContents();
        return contents.size() == 1 && contents.get(0) instanceof Docker.EnvironmentVariable ?
                ((Docker.EnvironmentVariable) contents.get(0)).getName() : null;
    }

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

    private static String @Nullable [] splitReference(String reference) {
        int at = reference.indexOf('@');
        String withoutDigest = at == -1 ? reference : reference.substring(0, at);
        int colon = withoutDigest.indexOf(':', withoutDigest.lastIndexOf('/') + 1);
        if (colon == -1) {
            return null;
        }
        return new String[]{withoutDigest.substring(0, colon), withoutDigest.substring(colon + 1)};
    }

    /// The registry an image is pulled from is left as written, which may be a variable, so only the trailing
    /// repository is rewritten.
    private static Docker.Argument withRepository(Docker.Argument imageName, String from, String to) {
        String oldPath = ImageName.parse(from).getPath();
        String newPath = ImageName.parse(to).getPath();
        return imageName.withContents(ListUtils.mapLast(imageName.getContents(), content -> {
            if (!(content instanceof Docker.Literal)) {
                return content;
            }
            Docker.Literal literal = (Docker.Literal) content;
            String text = literal.getText();
            return text.endsWith(oldPath) ?
                    literal.withText(text.substring(0, text.length() - oldPath.length()) + newPath) :
                    literal;
        }));
    }

    private static Docker.Argument withText(Docker.Argument argument, String text) {
        return argument.withContents(ListUtils.mapLast(argument.getContents(),
                content -> content instanceof Docker.Literal ? ((Docker.Literal) content).withText(text) : content));
    }

}
