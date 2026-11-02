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
package org.openrewrite.java.migrate.javax;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.ExecutionContext;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.gradle.GradleParser;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.maven.tree.Dependency;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.ResolvedGroupArtifactVersion;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LegacyResolvedGraphTest {
    private static final ExecutionContext CTX = new InMemoryExecutionContext(t -> {
        throw new AssertionError(t);
    });

    private static ResolvedDependency dependency(String group, String artifact, String version, String requested,
                                                  ResolvedDependency... children) {
        return ResolvedDependency.builder()
          .gav(new ResolvedGroupArtifactVersion(null, group, artifact, version, null))
          .requested(Dependency.builder().gav(new GroupArtifactVersion(group, artifact, requested)).build())
          .type("jar").dependencies(List.of(children)).build();
    }

    private static GradleDependencyConfiguration configuration(String name, boolean resolve,
                                                                List<GradleDependencyConfiguration> parents,
                                                                List<Dependency> requested,
                                                                List<ResolvedDependency> resolved) {
        return new GradleDependencyConfiguration(name, null, true, resolve, false, true,
          parents, requested, resolved, null, null);
    }

    private static SourceFile script(Map<String, GradleDependencyConfiguration> configurations, String name) {
        SourceFile source = GradleParser.builder().build().parse(CTX,
          "plugins { id 'java' }\nconfigurations { tools }\ndependencies { " + name + " 'com.sun.xml.ws:jaxws-ri:2.3.2' }\n")
          .findFirst().orElseThrow();
        return source.withMarkers(source.getMarkers().add(GradleProject.builder()
          .name("sample").path(":sample").nameToConfiguration(configurations).build()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"4.0.5", "${unresolved}"})
    void selfResolvingConfigurationRetainsNonLegacySelection(String version) {
        var selected = dependency("com.sun.xml.ws", "jaxws-ri", version, "2.3.2");
        var tools = configuration("tools", true, List.of(), List.of(selected.getRequested()), List.of(selected));
        var source = script(Map.of("tools", tools), "tools");
        assertThat(((SourceFile) new RemoveLegacyJaxwsRi().getVisitor().visit(source, CTX)).printAll())
          .contains("jaxws-ri");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void inspectEveryRootAndTransitiveDependency(boolean legacyFirst) {
        var oldRi = dependency("com.sun.xml.ws", "jaxws-ri", "2.3.2", "2.3.2");
        var newRi = dependency("com.sun.xml.ws", "jaxws-ri", "4.0.5", "2.3.2");
        var oldAnnotation = dependency("jakarta.annotation", "jakarta.annotation-api", "1.3.5", "1.3.5");
        var newAnnotation = dependency("jakarta.annotation", "jakarta.annotation-api", "3.0.0", "3.0.0");
        var legacyRoot = dependency("example", "legacy-lib", "1", "1", oldRi, oldAnnotation);
        var modernRoot = dependency("example", "modern-lib", "1", "1", newRi, newAnnotation);
        var bucket = configuration("implementation", false, List.of(), List.of(oldRi.getRequested()), List.of());
        var consumer = configuration("compileClasspath", true, List.of(bucket), List.of(),
          legacyFirst ? List.of(legacyRoot, modernRoot) : List.of(modernRoot, legacyRoot));
        var source = script(Map.of("implementation", bucket, "compileClasspath", consumer), "implementation");
        assertThat(((SourceFile) new RemoveLegacyJaxwsRi().getVisitor().visit(source, CTX)).printAll())
          .contains("jaxws-ri");
        assertThat(new AnnotationNamespace(true).getVisitor().visit(source, CTX).getMarkers().findFirst(SearchResult.class))
          .isPresent();
        assertThat(new AnnotationNamespace(false).getVisitor().visit(source, CTX).getMarkers().findFirst(SearchResult.class))
          .isEmpty();
    }
}
