/*
 * Copyright 2025 the original author or authors.
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
import org.openrewrite.ScanningRecipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.marker.JavaProject;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.Scope;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Value
@EqualsAndHashCode(callSuper = false)
public class HasNoOldKotlinDependency extends ScanningRecipe<HasNoOldKotlinDependency.Accumulator> {
    String displayName = "Project has no Kotlin <2.3 dependency";

    String description = "Marks all sources in projects that do not use Kotlin <2.3. " +
            "This is useful as a precondition for recipes that should only apply when Kotlin <2.3 is not present.";

    @Value
    public static class Accumulator {
        Set<JavaProject> projectsWithOldKotlin;
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator(new HashSet<>());
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree preVisit(Tree tree, ExecutionContext ctx) {
                stopAfterPreVisit();
                tree.getMarkers().findFirst(JavaProject.class)
                        .filter(jp -> !acc.getProjectsWithOldKotlin().contains(jp))
                        .ifPresent(jp -> tree.getMarkers().findFirst(MavenResolutionResult.class)
                                .ifPresent(maven -> {
                                    if (hasOldKotlinDependency(maven)) {
                                        acc.getProjectsWithOldKotlin().add(jp);
                                    }
                                }));
                return tree;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree preVisit(Tree tree, ExecutionContext ctx) {
                stopAfterPreVisit();
                return tree.getMarkers().findFirst(JavaProject.class)
                        .filter(jp -> !acc.getProjectsWithOldKotlin().contains(jp))
                        .map(__ -> SearchResult.found(tree))
                        .orElse(tree);
            }
        };
    }

    private static boolean hasOldKotlinDependency(MavenResolutionResult maven) {
        for (Map.Entry<Scope, List<ResolvedDependency>> entry : maven.getDependencies().entrySet()) {
            for (ResolvedDependency dep : entry.getValue()) {
                if ("org.jetbrains.kotlin".equals(dep.getGroupId()) &&
                    dep.getArtifactId().startsWith("kotlin-stdlib") &&
                    isVersionLessThan(dep.getVersion(), 2, 3)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isVersionLessThan(String version, int major, int minor) {
        try {
            String[] parts = version.split("[.-]");
            int v0 = Integer.parseInt(parts[0]);
            int v1 = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            return v0 < major || (v0 == major && v1 < minor);
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
