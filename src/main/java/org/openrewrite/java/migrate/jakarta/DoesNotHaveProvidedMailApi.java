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
public class DoesNotHaveProvidedMailApi extends Recipe {
    String displayName = "Build does not declare a provided Mail API";

    String description = "Find build files without an explicitly provided or compile-only Mail API, " +
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
                                isMailApi(d.getGroupId(), d.getArtifactId()))) {
                    return tree;
                }
                GradleProject gradle = tree.getMarkers().findFirst(GradleProject.class).orElse(null);
                if (gradle != null) {
                    GradleDependencyConfiguration compileOnly = gradle.getConfiguration("compileOnly");
                    if (compileOnly != null &&
                            (compileOnly.findRequestedDependency("javax.mail", "mail") != null ||
                             compileOnly.findRequestedDependency("javax.mail", "javax.mail-api") != null ||
                             compileOnly.findRequestedDependency("com.sun.mail", "javax.mail") != null ||
                             compileOnly.findRequestedDependency("com.sun.mail", "jakarta.mail") != null ||
                             compileOnly.findRequestedDependency("jakarta.mail", "jakarta.mail-api") != null)) {
                        return tree;
                    }
                }
                return SearchResult.found(tree);
            }
        };
    }

    private static boolean isMailApi(String groupId, String artifactId) {
        return "javax.mail".equals(groupId) && ("mail".equals(artifactId) || "javax.mail-api".equals(artifactId)) ||
                "com.sun.mail".equals(groupId) && ("javax.mail".equals(artifactId) || "jakarta.mail".equals(artifactId)) ||
                "jakarta.mail".equals(groupId) && "jakarta.mail-api".equals(artifactId);
    }
}
