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
package org.openrewrite.java.migrate.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.java.marker.JavaProject;
import org.openrewrite.marker.Markers;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.maven.tree.MavenResolutionResult;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@EqualsAndHashCode(callSuper = false)
@Value
public class ModuleOrParentHasDependency extends ScanningRecipe<ModuleOrParentHasDependency.Accumulator> {

    @Option(displayName = "Group pattern",
            description = "Group glob pattern used to match dependencies.",
            example = "org.projectlombok")
    String groupIdPattern;

    @Option(displayName = "Artifact pattern",
            description = "Artifact glob pattern used to match dependencies.",
            example = "lombok")
    String artifactIdPattern;

    String displayName = "Module or its reactor parent has dependency";

    String description = "Marks all files in modules that have a matching dependency, and the in-reactor parent poms " +
            "those modules inherit from. Intended as a precondition for recipes that configure a module through its " +
            "parent's `pluginManagement`, which a per-module precondition would otherwise keep from being visited.";

    public static class Accumulator {
        Set<JavaProject> modules = new HashSet<>();
        Set<Path> parentPoms = new HashSet<>();
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof SourceFile && hasDependency(tree.getMarkers())) {
                    tree.getMarkers().findFirst(JavaProject.class).ifPresent(acc.modules::add);
                    tree.getMarkers().findFirst(MavenResolutionResult.class).ifPresent(mrr -> {
                        for (MavenResolutionResult p = mrr; p.parentPomIsProjectPom() && p.getParent() != null; p = p.getParent()) {
                            Path parentPath = p.getParent().getPom().getRequested().getSourcePath();
                            if (parentPath == null) {
                                break;
                            }
                            acc.parentPoms.add(parentPath);
                        }
                    });
                }
                return tree;
            }
        };
    }

    private boolean hasDependency(Markers markers) {
        Optional<MavenResolutionResult> mrr = markers.findFirst(MavenResolutionResult.class);
        if (mrr.isPresent()) {
            return !mrr.get().findDependencies(groupIdPattern, artifactIdPattern, null).isEmpty();
        }
        Optional<GradleProject> gp = markers.findFirst(GradleProject.class);
        if (gp.isPresent()) {
            for (GradleDependencyConfiguration configuration : gp.get().getConfigurations()) {
                if (configuration.findRequestedDependency(groupIdPattern, artifactIdPattern) != null ||
                    configuration.findResolvedDependency(groupIdPattern, artifactIdPattern) != null) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                assert tree != null;
                Optional<JavaProject> jp = tree.getMarkers().findFirst(JavaProject.class);
                if (jp.isPresent() && acc.modules.contains(jp.get())) {
                    return SearchResult.found(tree, "Module has dependency");
                }
                if (tree instanceof SourceFile && acc.parentPoms.contains(((SourceFile) tree).getSourcePath())) {
                    return SearchResult.found(tree, "Parent of a module with the dependency");
                }
                return tree;
            }
        };
    }
}
