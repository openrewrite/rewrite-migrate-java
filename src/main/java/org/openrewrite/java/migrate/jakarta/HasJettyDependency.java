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
import org.openrewrite.ScanningRecipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.marker.SearchResult;

import java.util.concurrent.atomic.AtomicBoolean;

@Value
@EqualsAndHashCode(callSuper = false)
public class HasJettyDependency extends ScanningRecipe<AtomicBoolean> {
    String displayName = "Build uses Jetty before version 12";

    String description = "Mark the source set when a Maven or Gradle module depends on Jetty 8 through 11. " +
            "This permits updating the Java baseline in parent build files as well as the module using Jetty.";

    @Override
    public AtomicBoolean getInitialValue(ExecutionContext ctx) {
        return new AtomicBoolean();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(AtomicBoolean usesJetty) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree preVisit(Tree tree, ExecutionContext ctx) {
                stopAfterPreVisit();
                tree.getMarkers().findFirst(MavenResolutionResult.class).ifPresent(model -> {
                    if (model.getDependencies().values().stream().flatMap(java.util.Collection::stream)
                            .anyMatch(d -> isLegacyJetty(d.getGroupId(), d.getVersion()))) {
                        usesJetty.set(true);
                    }
                });
                tree.getMarkers().findFirst(GradleProject.class).ifPresent(model -> {
                    if (model.getConfigurations().stream().flatMap(c -> c.getDirectResolved().stream())
                            .anyMatch(d -> isLegacyJetty(d.getGroupId(), d.getVersion()))) {
                        usesJetty.set(true);
                    }
                });
                return tree;
            }
        };
    }

    private static boolean isLegacyJetty(String groupId, String version) {
        return groupId != null && ("org.eclipse.jetty".equals(groupId) || groupId.startsWith("org.eclipse.jetty.")) &&
                version != null && version.matches("(?:8|9|10|11)(?:\\..*)?");
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(AtomicBoolean usesJetty) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree preVisit(Tree tree, ExecutionContext ctx) {
                stopAfterPreVisit();
                return usesJetty.get() ? SearchResult.found(tree) : tree;
            }
        };
    }
}
