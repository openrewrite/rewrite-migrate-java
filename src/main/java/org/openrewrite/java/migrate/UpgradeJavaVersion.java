/*
 * Copyright 2024 the original author or authors.
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
import org.openrewrite.gradle.UpdateJavaCompatibility;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.marker.JavaVersion;
import org.openrewrite.java.tree.J;
import org.openrewrite.maven.UpdateMavenProjectPropertyJavaVersion;
import org.openrewrite.maven.UseMavenCompilerPluginReleaseConfiguration;

import java.time.Duration;
import java.util.*;

@Value
@EqualsAndHashCode(callSuper = false)
public class UpgradeJavaVersion extends Recipe {

    @Option(displayName = "Java version",
            description = "The Java version to upgrade to.",
            example = "11")
    Integer version;

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

    @Override
    public List<Recipe> getRecipeList() {
        return Arrays.asList(
                new UseMavenCompilerPluginReleaseConfiguration(version),
                new UpdateMavenProjectPropertyJavaVersion(version),
                new org.openrewrite.jenkins.UpgradeJavaVersion(version, null),
                new UpdateJavaCompatibility(version, null, null, false, null),
                new UpdateSdkMan(String.valueOf(version), null)
        );
    }

    /**
     * This recipe only updates markers, so it does not correspond to human manual effort.
     *
     * @return Zero estimated time.
     */
    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(0);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        String newVersion = version.toString();
        Map<JavaVersion, JavaVersion> updatedMarkers = new HashMap<>();
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J preVisit(J tree, ExecutionContext ctx) {
                Optional<JavaVersion> maybeJavaVersion = tree.getMarkers().findFirst(JavaVersion.class);
                if (maybeJavaVersion.isPresent() && maybeJavaVersion.get().getMajorVersion() < version) {
                    return tree.withMarkers(tree.getMarkers().setByType(updatedMarkers.computeIfAbsent(maybeJavaVersion.get(),
                            m -> m.withSourceCompatibility(newVersion).withTargetCompatibility(newVersion))));
                }
                return tree;
            }
        };
    }
}
