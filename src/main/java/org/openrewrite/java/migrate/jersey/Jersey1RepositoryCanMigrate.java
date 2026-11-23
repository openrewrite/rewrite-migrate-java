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
package org.openrewrite.java.migrate.jersey;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.Pom;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.Scope;

import java.util.*;

import static java.util.Collections.newSetFromMap;

/**
 * Jersey 1 and Jersey 2 share the {@code javax.ws.rs} packages, but not their implementations, so a module can only
 * move to Jersey 2 once nothing else it depends on still needs Jersey 1 at runtime.
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class Jersey1RepositoryCanMigrate extends ScanningRecipe<Jersey1RepositoryCanMigrate.Accumulator> {

    String displayName = "Find repositories that can migrate from Jersey 1";

    String description = "Marks every file of a repository whose Maven or Gradle build declares a Jersey 1 " +
            "(`com.sun.jersey`) dependency itself. A repository is left alone as a whole when one of its modules gets " +
            "Jersey 1 through a library that is not Jersey, such as Dropwizard before 0.8, Eureka 1 or Hadoop, since " +
            "that library keeps using Jersey 1; when a module declares Jersey 1 as `provided` or `compileOnly`, since " +
            "the container or the code being instrumented supplies it; and when it uses the Jersey 1 Guice integration, " +
            "for which Jersey 2 has no counterpart.";

    public static class Accumulator {
        final Set<String> projectCoordinates = new HashSet<>();
        final List<ResolvedDependency> directDependencies = new ArrayList<>();
        boolean declaresJersey1;
        boolean blocked;

        @Nullable
        Boolean canMigrate;

        boolean canMigrate() {
            if (canMigrate == null) {
                canMigrate = declaresJersey1 && !blocked && !reachedThroughAnotherLibrary();
            }
            return canMigrate;
        }

        private boolean reachedThroughAnotherLibrary() {
            for (ResolvedDependency direct : directDependencies) {
                if (isJersey1(direct) || isProject(direct)) {
                    continue;
                }
                if (anyTransitive(direct, this)) {
                    return true;
                }
            }
            return false;
        }

        private boolean isProject(ResolvedDependency dependency) {
            return projectCoordinates.contains(dependency.getGroupId() + ":" + dependency.getArtifactId());
        }
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree preVisit(Tree tree, ExecutionContext ctx) {
                stopAfterPreVisit();
                tree.getMarkers().findFirst(MavenResolutionResult.class).ifPresent(maven -> scanMaven(maven, acc));
                tree.getMarkers().findFirst(GradleProject.class).ifPresent(gradle -> scanGradle(gradle, acc));
                return tree;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree preVisit(Tree tree, ExecutionContext ctx) {
                stopAfterPreVisit();
                return acc.canMigrate() ? SearchResult.found(tree) : tree;
            }
        };
    }

    private static void scanMaven(MavenResolutionResult maven, Accumulator acc) {
        for (Pom pom : maven.getProjectPoms().values()) {
            acc.projectCoordinates.add(pom.getGroupId() + ":" + pom.getArtifactId());
        }
        acc.projectCoordinates.add(maven.getPom().getGroupId() + ":" + maven.getPom().getArtifactId());
        for (Map.Entry<Scope, List<ResolvedDependency>> byScope : maven.getDependencies().entrySet()) {
            for (ResolvedDependency dependency : byScope.getValue()) {
                if (dependency.getDepth() != 0) {
                    continue;
                }
                acc.directDependencies.add(dependency);
                if (isJersey1(dependency)) {
                    String scope = maven.getPom().getValue(dependency.getRequested().getScope());
                    if ("provided".equals(scope) || "system".equals(scope)) {
                        acc.blocked = true;
                    } else {
                        acc.declaresJersey1 = true;
                    }
                }
                if (isJersey1Guice(dependency)) {
                    acc.blocked = true;
                }
            }
        }
    }

    private static void scanGradle(GradleProject gradle, Accumulator acc) {
        if (gradle.getGroup() != null) {
            acc.projectCoordinates.add(gradle.getGroup() + ":" + gradle.getName());
        }
        for (GradleDependencyConfiguration configuration : gradle.getConfigurations()) {
            String name = configuration.getName();
            boolean compileOnly = name.endsWith("ompileOnly") || "providedCompile".equals(name) ||
                    "providedRuntime".equals(name);
            for (org.openrewrite.maven.tree.Dependency requested : configuration.getRequested()) {
                if (requested.getGroupId() != null && requested.getGroupId().startsWith("com.sun.jersey")) {
                    if (compileOnly) {
                        acc.blocked = true;
                    } else {
                        acc.declaresJersey1 = true;
                    }
                    if ("jersey-guice".equals(requested.getArtifactId())) {
                        acc.blocked = true;
                    }
                }
            }
            if (configuration.isCanBeResolved()) {
                acc.directDependencies.addAll(configuration.getDirectResolved());
            }
        }
    }

    static boolean isJersey1(ResolvedDependency dependency) {
        return dependency.getGroupId().startsWith("com.sun.jersey");
    }

    private static boolean isJersey1Guice(ResolvedDependency dependency) {
        return "com.sun.jersey.contribs".equals(dependency.getGroupId()) &&
                "jersey-guice".equals(dependency.getArtifactId());
    }

    private static boolean anyTransitive(ResolvedDependency root, Accumulator acc) {
        Set<ResolvedDependency> visited = newSetFromMap(new IdentityHashMap<>());
        Deque<ResolvedDependency> pending = new ArrayDeque<>(root.getDependencies());
        while (!pending.isEmpty()) {
            ResolvedDependency dependency = pending.removeFirst();
            if (!visited.add(dependency)) {
                continue;
            }
            if (isJersey1(dependency)) {
                return true;
            }
            if (acc.isProject(dependency)) {
                // Modules of this repository are checked as modules of their own.
                continue;
            }
            pending.addAll(dependency.getDependencies());
        }
        return false;
    }
}
