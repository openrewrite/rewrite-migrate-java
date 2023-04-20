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
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.marker.JavaVersion;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.maven.MavenVisitor;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.search.FindTags;
import org.openrewrite.xml.tree.Xml;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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
    protected List<SourceFile> visit(List<SourceFile> before, ExecutionContext ctx) {

        GradleUpdateJavaVersionVisitor gradleUpdateJavaVersionVisitor = new GradleUpdateJavaVersionVisitor();
        MavenUpdateJavaVersionVisitor mavenUpdateVisitor = new MavenUpdateJavaVersionVisitor();

        before = ListUtils.map(before, s -> {
            s = (SourceFile) mavenUpdateVisitor.visit(s, ctx);
            s = (SourceFile) gradleUpdateJavaVersionVisitor.visit(s, ctx);
            return s;
        });

        String newVersion = this.version.toString();
        // Create a new JavaVersion marker with the new version
        Optional<JavaVersion> currentMarker = before.stream()
            .map(it -> it.getMarkers().findFirst(JavaVersion.class))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findAny();
        if (!currentMarker.isPresent() || currentMarker.get().getMajorVersion() >= version) {
            return before;
        }
        JavaVersion updatedMarker = currentMarker.get()
            .withSourceCompatibility(newVersion)
            .withTargetCompatibility(newVersion);

        return ListUtils.map(before, sourceFile -> sourceFile.getMarkers().findFirst(JavaVersion.class)
            .map(version -> (SourceFile) sourceFile.withMarkers(sourceFile.getMarkers().computeByType(version,
                (v, acc) -> updatedMarker)))
            .orElse(sourceFile));
    }

    private class GradleUpdateJavaVersionVisitor extends GroovyIsoVisitor<ExecutionContext> {
        MethodMatcher javaLanguageVersionMatcher = new MethodMatcher("org.gradle.jvm.toolchain.JavaLanguageVersion of(int)");

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method,
                                                        ExecutionContext executionContext) {
            method = super.visitMethodInvocation(method, executionContext);
            if (javaLanguageVersionMatcher.matches(method)) {
                List<Expression> args = method.getArguments();

                if (args.size() == 1 && args.get(0) instanceof J.Literal) {
                    J.Literal versionArg = (J.Literal) args.get(0);
                    if (versionArg.getValue() instanceof Integer) {
                        Integer versionNumber = (Integer)  versionArg.getValue();
                        if (versionNumber < version) {
                            return method.withArguments(
                                Collections.singletonList(versionArg.withValue(version)
                                    .withValueSource(version.toString())));
                        } else {
                            return method;
                        }
                    }
                }

                return SearchResult.found(method, "Attempted to update to Java version to " + version
                                                  + "  but was unsuccessful, please update manually");
            }
            return method;
        }
    }

    private static final List<String> JAVA_VERSION_XPATHS = Arrays.asList(
        "/project/properties/java.version",
        "/project/properties/jdk.version",
        "/project/properties/javaVersion",
        "/project/properties/jdkVersion",
        "/project/properties/maven.compiler.source",
        "/project/properties/maven.compiler.target",
        "/project/properties/maven.compiler.release",
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
                Optional<Float> maybeVersion = tag.getValue().map(Float::parseFloat);
                if (!maybeVersion.isPresent()) {
                    SearchResult.found(tag, "Attempted to update to Java version to " + version
                                               + "  but was unsuccessful, please update manually");
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
