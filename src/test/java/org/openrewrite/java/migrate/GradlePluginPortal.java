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

import org.openrewrite.Tree;
import org.openrewrite.gradle.marker.GradleBuildscript;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.marker.GradleSettings;
import org.openrewrite.maven.tree.MavenRepository;

import java.util.List;

/// Markers that resolve plugins from the Gradle Plugin Portal and dependencies from Maven Central, as a real
/// Gradle build would, so that version selectors such as `1.0.x` can be resolved without running the Tooling API.
final class GradlePluginPortal {
    private static final MavenRepository REPOSITORY = new MavenRepository(
      "Gradle Central Plugin Repository", "https://plugins.gradle.org/m2", "true", "false", true, null, null, null, null);

    private GradlePluginPortal() {
    }

    static GradleBuildscript buildscript() {
        return GradleBuildscript.builder().mavenRepositories(List.of(REPOSITORY)).build();
    }

    static GradleProject project() {
        return GradleProject.builder()
          .id(Tree.randomId())
          .buildscript(buildscript())
          .mavenRepositories(List.of(MavenRepository.MAVEN_CENTRAL))
          .build();
    }

    static GradleSettings settings() {
        return GradleSettings.builder().buildscript(buildscript()).build();
    }
}
