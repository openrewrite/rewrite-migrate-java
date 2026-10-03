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
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.maven.tree.MavenResolutionResult;

import java.util.Collection;

@Value
@EqualsAndHashCode(callSuper = false)
public class DoesNotHaveProvidedCdiApi extends Recipe {
    String displayName = "Build does not declare a provided CDI API";

    String description = "Find build files without an explicitly provided or compile-only CDI API, " +
            "so adding a compile dependency does not promote a container-provided API to runtime scope.";

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
                                isCdiApi(d.getGroupId(), d.getArtifactId()))) {
                    return tree;
                }
                GradleProject gradle = tree.getMarkers().findFirst(GradleProject.class).orElse(null);
                if (gradle != null) {
                    GradleDependencyConfiguration compileOnly = gradle.getConfiguration("compileOnly");
                    if (compileOnly != null &&
                            (compileOnly.findRequestedDependency("javax.enterprise", "cdi-api") != null ||
                             compileOnly.findRequestedDependency("jakarta.enterprise", "jakarta.enterprise.cdi-api") != null)) {
                        return tree;
                    }
                }
                return SearchResult.found(tree);
            }
        };
    }

    private static boolean isCdiApi(String groupId, String artifactId) {
        return "javax.enterprise".equals(groupId) && "cdi-api".equals(artifactId) ||
                "jakarta.enterprise".equals(groupId) && "jakarta.enterprise.cdi-api".equals(artifactId);
    }
}
