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
package org.openrewrite.java.migrate.jakarta;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Option;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.maven.tree.MavenResolutionResult;

import java.util.Collection;
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class DoesNotHaveProvidedApiDependency extends Recipe {
    String displayName = "Build does not declare a provided API dependency";

    String description = "Find build files without an explicitly provided or compile-only API, " +
            "so adding a compile dependency does not promote a container-provided API to runtime scope.";

    @Option(displayName = "API coordinates",
            description = "Legacy and Jakarta groupId:artifactId coordinates whose provided scope must be preserved.",
            example = "javax.enterprise:cdi-api, jakarta.enterprise:jakarta.enterprise.cdi-api")
    List<String> coordinates;

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree preVisit(Tree tree, ExecutionContext ctx) {
                stopAfterPreVisit();
                MavenResolutionResult maven = tree.getMarkers().findFirst(MavenResolutionResult.class).orElse(null);
                if (maven != null && maven.getDependencies().values().stream().flatMap(Collection::stream)
                        .anyMatch(d -> d.getDepth() == 0 &&
                                "provided".equals(maven.getPom().getValue(d.getRequested().getScope())) &&
                                coordinates.contains(d.getGroupId() + ":" + d.getArtifactId()))) {
                    return tree;
                }
                GradleProject gradle = tree.getMarkers().findFirst(GradleProject.class).orElse(null);
                if (gradle != null) {
                    GradleDependencyConfiguration compileOnly = gradle.getConfiguration("compileOnly");
                    if (compileOnly != null) {
                        for (String coordinate : coordinates) {
                            String[] ga = coordinate.split(":", 2);
                            if (ga.length == 2 && compileOnly.findRequestedDependency(ga[0], ga[1]) != null) {
                                return tree;
                            }
                        }
                    }
                }
                return SearchResult.found(tree);
            }
        };
    }

}
