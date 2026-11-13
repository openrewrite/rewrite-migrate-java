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

import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;

/// Upgrades the Java version of a container image reference, as found in a Dockerfile `FROM` or the
/// `image` of a CI configuration, independent of the file it was read from.
final class JavaImageReference {

    private static final String NEW_IMAGE = "eclipse-temurin";
    private static final Set<String> DEPRECATED_IMAGES = new HashSet<>(asList("openjdk", "adoptopenjdk"));
    private static final Set<String> CURRENT_IMAGES = new HashSet<>(asList(
            "eclipse-temurin", "amazoncorretto", "azul/zulu-openjdk",
            "bellsoft/liberica-openjdk-debian", "bellsoft/liberica-openjdk-alpine",
            "bellsoft/liberica-openjdk-centos", "ibm-semeru-runtimes", "sapmachine"
    ));

    private static final int OLDEST_VERSION = 8;
    /// The feature version, then any patch or update version as published by the various vendors, such as
    /// `.0.9_9` and `u392-b08` of Temurin, `.0.20.1-17.68` of Zulu or `.0.19-11` of Liberica, then the variant.
    private static final Pattern VERSIONED_TAG = Pattern.compile(
            "(\\d{1,3})(?:u\\d+(?:-b\\d+)?|(?:\\.\\d+)+(?:_\\d+)?(?:-\\d+(?:\\.\\d+)*)?)?(\\D.*)?");
    private static final Pattern MAVEN_TAG = Pattern.compile("3((?:\\.\\d+){0,2})-(eclipse-temurin|amazoncorretto)-(\\d+)(.*)");
    /// The OpenJDK based Maven images are no longer published; only those without a variant suffix have an
    /// `eclipse-temurin` based counterpart for every Java version.
    private static final Pattern DEPRECATED_MAVEN_TAG = Pattern.compile("3(?:\\.\\d+){0,2}-jdk-(\\d+)");
    /// CircleCI publishes its convenience images for a feature version as `25.0`.
    private static final Pattern CIRCLECI_TAG = Pattern.compile("(\\d+)\\.\\d+(?:\\.\\d+)?(-[a-z0-9-]+)?");
    private static final Pattern GRADLE_TAG = Pattern.compile("(?:(\\d+(?:\\.\\d+){0,2})-)?jdk(\\d+)(.*)");

    /// The oldest Gradle release able to run on each Java version, as the `gradle` images run Gradle on the
    /// JDK they ship. See https://docs.gradle.org/current/userguide/compatibility.html.
    private static final Map<Integer, int[]> OLDEST_GRADLE = new HashMap<>();

    static {
        OLDEST_GRADLE.put(11, new int[]{5, 0});
        OLDEST_GRADLE.put(17, new int[]{7, 3});
        OLDEST_GRADLE.put(18, new int[]{7, 5});
        OLDEST_GRADLE.put(19, new int[]{7, 6});
        OLDEST_GRADLE.put(20, new int[]{8, 3});
        OLDEST_GRADLE.put(21, new int[]{8, 5});
        OLDEST_GRADLE.put(22, new int[]{8, 8});
        OLDEST_GRADLE.put(23, new int[]{8, 10});
        OLDEST_GRADLE.put(24, new int[]{8, 14});
        OLDEST_GRADLE.put(25, new int[]{9, 1, 0});
        OLDEST_GRADLE.put(26, new int[]{9, 4, 0});
        OLDEST_GRADLE.put(27, new int[]{9, 8, 0});
    }

    private JavaImageReference() {
    }

    /// @param reference an image reference such as `docker.io/openjdk:11-jre@sha256:...`
    /// @return the reference to an image for `version`, or `null` where `reference` names no Java image
    /// this knows to upgrade, or one already at or beyond `version`. A digest is dropped, as it pins the old image.
    static @Nullable String upgrade(String reference, int version) {
        String[] nameAndTag = splitReference(reference);
        if (nameAndTag == null) {
            return null;
        }
        String newImageName = upgradedImageName(nameAndTag[0]);
        String newTag = upgradedTag(nameAndTag[0], nameAndTag[1], version);
        return newImageName == null || newTag == null ? null : newImageName + ':' + newTag;
    }

    static @Nullable String upgradedImageName(String imageName) {
        String repository = repository(imageName);
        if (DEPRECATED_IMAGES.contains(repository)) {
            return imageName.substring(0, imageName.length() - repository.length()) + NEW_IMAGE;
        }
        return CURRENT_IMAGES.contains(repository) || "maven".equals(repository) || "gradle".equals(repository) ||
               "cimg/openjdk".equals(repository) ? imageName : null;
    }

    static @Nullable String upgradedTag(String imageName, String tag, int version) {
        String repository = repository(imageName);
        if ("maven".equals(repository)) {
            return upgradedMavenTag(tag, version);
        }
        if ("gradle".equals(repository)) {
            return upgradedGradleTag(tag, version);
        }
        if ("cimg/openjdk".equals(repository)) {
            Matcher circleCiTag = CIRCLECI_TAG.matcher(tag);
            return circleCiTag.matches() && isUpgrade(Integer.parseInt(circleCiTag.group(1)), version) ?
                    version + ".0" + (circleCiTag.group(2) == null ? "" : circleCiTag.group(2)) : null;
        }
        Matcher matcher = VERSIONED_TAG.matcher(tag);
        if (!matcher.matches() || !isUpgrade(Integer.parseInt(matcher.group(1)), version)) {
            return null;
        }
        return version + (matcher.group(2) == null ? "" : matcher.group(2));
    }

    private static @Nullable String upgradedMavenTag(String tag, int version) {
        Matcher mavenTag = MAVEN_TAG.matcher(tag);
        if (mavenTag.matches()) {
            // Old Maven releases are not rebuilt with newer JDKs, so a pinned one moves to the maintained 3.9 line
            String maven = mavenTag.group(1).isEmpty() ? "3" : "3.9";
            return isUpgrade(Integer.parseInt(mavenTag.group(3)), version) ?
                    maven + '-' + mavenTag.group(2) + '-' + version + mavenTag.group(4) : null;
        }
        Matcher deprecatedTag = DEPRECATED_MAVEN_TAG.matcher(tag);
        return deprecatedTag.matches() && isUpgrade(Integer.parseInt(deprecatedTag.group(1)), version) ?
                "3.9-eclipse-temurin-" + version : null;
    }

    private static @Nullable String upgradedGradleTag(String tag, int version) {
        Matcher gradleTag = GRADLE_TAG.matcher(tag);
        int[] oldestGradle = OLDEST_GRADLE.get(version);
        if (!gradleTag.matches() || oldestGradle == null || !isUpgrade(Integer.parseInt(gradleTag.group(2)), version)) {
            return null;
        }
        String gradleVersion = gradleTag.group(1);
        String newGradleVersion = gradleVersion == null ? "" : atLeast(gradleVersion, oldestGradle) + '-';
        return newGradleVersion + "jdk" + version + gradleTag.group(3);
    }

    /// @return `gradleVersion` if each release it may resolve to runs on the Java version `oldest` is the
    /// oldest Gradle for, else `oldest` written to the same precision, so that `8-jdk17` moves to `9-jdk25`.
    private static String atLeast(String gradleVersion, int[] oldest) {
        String[] parts = gradleVersion.split("\\.");
        StringJoiner oldestAtSamePrecision = new StringJoiner(".");
        for (int i = 0; i < parts.length; i++) {
            oldestAtSamePrecision.add(String.valueOf(i < oldest.length ? oldest[i] : 0));
        }
        for (int i = 0; i < parts.length; i++) {
            int difference = Integer.parseInt(parts[i]) - (i < oldest.length ? oldest[i] : 0);
            if (difference != 0) {
                return difference > 0 ? gradleVersion : oldestAtSamePrecision.toString();
            }
        }
        return gradleVersion;
    }

    private static boolean isUpgrade(int currentVersion, int version) {
        return OLDEST_VERSION <= currentVersion && currentVersion < version;
    }

    /// @return The registry `imageName` is pulled from, or `null` where it names no registry.
    /// A variable reference is left in the component that holds it, so `${REGISTRY}/app` is
    /// pulled from `${REGISTRY}`.
    static @Nullable String registry(String imageName) {
        int slash = imageName.indexOf('/');
        return slash >= 0 && isRegistry(imageName.substring(0, slash)) ? imageName.substring(0, slash) : null;
    }

    /// @return The path of `imageName` within its registry, as `library/ubuntu` of
    /// `docker.io/library/ubuntu`.
    static String path(String imageName) {
        String registry = registry(imageName);
        return registry == null ? imageName : imageName.substring(registry.length() + 1);
    }

    /// @return The path of `imageName` without the `library/` namespace Docker Hub keeps its official images in.
    private static String repository(String imageName) {
        String path = path(imageName);
        return path.startsWith("library/") ? path.substring("library/".length()) : path;
    }

    /// Registry-ness cannot be told from the syntax: `mcr.microsoft.com/windows/servercore` names a
    /// registry, while `redhat/ubi9-minimal` names an organisation on Docker Hub. This is the rule
    /// Docker itself settles that by; the last clause holds because a path component may not carry
    /// an uppercase character.
    private static boolean isRegistry(String component) {
        return component.indexOf('.') >= 0 ||
                component.indexOf(':') >= 0 ||
                "localhost".equals(component) ||
                !component.equals(component.toLowerCase(Locale.ROOT));
    }

    /// @return The image name and tag of `reference`, without any digest, or `null` where it has no tag.
    static String @Nullable [] splitReference(String reference) {
        int at = reference.indexOf('@');
        String withoutDigest = at == -1 ? reference : reference.substring(0, at);
        int colon = withoutDigest.indexOf(':', withoutDigest.lastIndexOf('/') + 1);
        if (colon == -1) {
            return null;
        }
        return new String[]{withoutDigest.substring(0, colon), withoutDigest.substring(colon + 1)};
    }
}
