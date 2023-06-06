/*
 * Copyright 2021 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
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
import org.openrewrite.*;
import org.openrewrite.gradle.IsBuildGradle;
import org.openrewrite.gradle.UpdateJavaCompatibility;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.marker.JavaVersion;
import org.openrewrite.maven.MavenVisitor;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

import java.util.*;
import java.util.stream.Collectors;


@Value
@EqualsAndHashCode(callSuper = true)
public class UpgradeJavaVersion extends Recipe {
    @Override
    public String getDisplayName() {
        return "Upgrade Java version";
    }

    @Override
    public String getDescription() {
        return "Upgrade build plugin configuration to use the specified Java version. " +
                "This recipe changes `java.toolchain.languageVersion` in `build.gradle(.kts)` of gradle projects, " +
                "or maven-compiler-plugin target version and related settings. " +
                "Will not downgrade if the version is newer than the specified version.";
    }

    @Option(displayName = "Java version",
            description = "The Java version to upgrade to.",
            example = "11")
    Integer version;

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        String newVersion = version.toString();
        Map<JavaVersion, JavaVersion> updatedMarkers = new HashMap<>();

        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (!(tree instanceof SourceFile)) {
                    return tree;
                }
                SourceFile source = (SourceFile) tree;

                Optional<JavaVersion> maybeJavaVersion = source.getMarkers().findFirst(JavaVersion.class);
                if (maybeJavaVersion.isPresent() && maybeJavaVersion.get().getMajorVersion() >= version) {
                    // No change if try to downgrade java version, or on same java version.
                    return source;
                }

                if (source instanceof G.CompilationUnit && new IsBuildGradle<ExecutionContext>().visit(source, ctx) != source) {
                    source = (SourceFile) new UpdateJavaCompatibility(version, null, null).getVisitor().visitNonNull(source, ctx);
                } else if (source instanceof Xml.Document) {
                    source = (SourceFile) new MavenUpdateJavaVersionVisitor().visitNonNull(source, ctx);
                }

                if (maybeJavaVersion.isPresent()) {
                    source =
                        source.withMarkers(source.getMarkers().setByType(updatedMarkers.computeIfAbsent(maybeJavaVersion.get(),
                        m -> m.withSourceCompatibility(newVersion).withTargetCompatibility(newVersion))));
                }

                return source;
            }
        };
    }

    private static final List<String> JAVA_VERSION_XPATHS = Arrays.asList(
            "/project/properties/java.version",
            "/project/properties/jdk.version",
            "/project/properties/javaVersion",
            "/project/properties/jdkVersion",
            "/project/properties/maven.compiler.source",
            "/project/properties/maven.compiler.target",
            "/project/properties/maven.compiler.release",
            "/project/properties/release.version",
            "/project/build/plugins/plugin[artifactId='maven-compiler-plugin']/configuration/source",
            "/project/build/plugins/plugin[artifactId='maven-compiler-plugin']/configuration/target",
            "/project/build/plugins/plugin[artifactId='maven-compiler-plugin']/configuration/release");

    private static final List<XPathMatcher> JAVA_VERSION_XPATH_MATCHERS =
            JAVA_VERSION_XPATHS.stream().map(XPathMatcher::new).collect(Collectors.toList());


    private class MavenUpdateJavaVersionVisitor extends MavenVisitor<ExecutionContext> {
        @Override
        public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
            tag = (Xml.Tag) super.visitTag(tag, ctx);

            if (JAVA_VERSION_XPATH_MATCHERS.stream().anyMatch(matcher -> matcher.matches(getCursor()))) {
                Optional<Float> maybeVersion = tag.getValue().flatMap(
                        value -> {
                            try {
                                return Optional.of(Float.parseFloat(value));
                            } catch (NumberFormatException e) {
                                return Optional.empty();
                            }
                        }
                );

                if (!maybeVersion.isPresent()) {
                    return tag;
                }
                float currentVersion = maybeVersion.get();
                if (currentVersion >= version) {
                    return tag;
                }
                return tag.withValue(String.valueOf(version));
            }

            return tag;
        }
    }
}
