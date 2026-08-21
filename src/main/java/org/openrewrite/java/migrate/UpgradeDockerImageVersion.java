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
import java.util.UUID;
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

    private static final String FROM_REPLACEMENTS = "fromReplacements";

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

                ArgPlan plan = planUpgrades(file, defaults);
                getCursor().putMessage(FROM_REPLACEMENTS, plan.getFromReplacements());
                Docker.File f = super.visitFile(file, ctx);

                Map<String, String> upgrades = plan.getArgUpgrades();
                if (upgrades.isEmpty()) {
                    return f;
                }
                return f.withGlobalArgs(ListUtils.map(f.getGlobalArgs(), arg -> {
                    String upgraded = upgrades.get(arg.getName().getText());
                    return upgraded == null ? arg : arg.withValue(withText(requireNonNull(arg.getValue()), upgraded));
                }));
            }

            @Override
            public Docker.From visitFrom(Docker.From from, ExecutionContext ctx) {
                if (containsVariable(from.getImageName()) || containsVariable(from.getTag())) {
                    Map<UUID, Docker.From> replacements = getCursor().getNearestMessage(FROM_REPLACEMENTS, emptyMap());
                    return replacements.getOrDefault(from.getId(), from);
                }

                DockerFrom image = new DockerFrom(getCursor());
                String newTag = upgradedTag(image.getTag().orElse(""));
                if (newTag == null) {
                    return from;
                }
                String imageName = image.getImageName().orElse("");
                if (DEPRECATED_IMAGES.contains(imageName)) {
                    return image.withImageReference(NEW_IMAGE + ":" + newTag);
                }
                if (CURRENT_IMAGES.contains(imageName)) {
                    return image.withTag(newTag).withDigest(null);
                }
                return from;
            }
        };
    }

    /**
     * An argument is shared by every `FROM` that reads it, so which ones may be rewritten is only known once the whole
     * file has been read.
     */
    private ArgPlan planUpgrades(Docker.File file, Map<String, String> defaults) {
        Map<String, String> upgrades = new HashMap<>();
        Map<UUID, Docker.From> replacements = new HashMap<>();
        new DockerIsoVisitor<Integer>() {
            @Override
            public Docker.From visitFrom(Docker.From from, Integer p) {
                if (containsVariable(from.getImageName()) || containsVariable(from.getTag())) {
                    Docker.From planned = planFrom(from, defaults, upgrades);
                    if (planned != from) {
                        replacements.put(from.getId(), planned);
                    }
                }
                return from;
            }
        }.visit(file, 0);
        return new ArgPlan(upgrades, replacements);
    }

    private Docker.From planFrom(Docker.From from, Map<String, String> defaults, Map<String, String> upgrades) {
        String imageVariable = soleVariable(from.getImageName());
        String tagVariable = from.getTag() == null ? null : leadingVariable(from.getTag());
        String imageName = imageVariable == null ? from.getImageName().getText() : defaults.get(imageVariable);
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
            tagVariable = imageVariable;
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
                from = from.withImageName(withText(from.getImageName(), newImageName));
            } else {
                upgrades.put(imageVariable, newImageName);
            }
        }
        return from.withDigest(null);
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

    private static boolean containsVariable(Docker.@Nullable Argument argument) {
        return argument != null && argument.hasEnvironmentVariables();
    }

    private static @Nullable String soleVariable(Docker.Argument argument) {
        Docker.EnvironmentVariable variable = sole(argument.getContents(), Docker.EnvironmentVariable.class);
        return variable == null ? null : variable.getName();
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

    private static <T> @Nullable T sole(List<? extends Docker.ArgumentContent> contents, Class<T> type) {
        if (contents.size() == 1 && type.isInstance(contents.get(0))) {
            return type.cast(contents.get(0));
        }
        return null;
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

    private static Docker.Argument withText(Docker.Argument argument, String text) {
        Docker.Literal literal = requireNonNull(sole(argument.getContents(), Docker.Literal.class));
        return argument.withContents(singletonList(literal.withText(text)));
    }

    @Value
    private static class ArgPlan {
        Map<String, String> argUpgrades;
        Map<UUID, Docker.From> fromReplacements;
    }
}
