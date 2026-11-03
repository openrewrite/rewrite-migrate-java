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

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.maven.tree.MavenResolutionResult;

/** The external Karaf harness looks up the Maven coordinate, not just javax annotation classes. */
public class CommonAnnotationsCoordinateCanMigrate extends Recipe {
    @Override
    public String getDisplayName() {
        return "Check whether the Common Annotations coordinate can migrate";
    }

    @Override
    public String getDescription() {
        return "Preserve an existing javax.annotation API coordinate in Maven modules resolving the external " +
               "Karaf 4.4.11 test harness, whose versionAsInProject lookup requires that coordinate. " +
               "Other modules continue migrating to the equivalent Jakarta EE 8 API.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        // This precondition also applies to Gradle build files; only the known Maven harness is excluded.
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree preVisit(Tree tree, ExecutionContext ctx) {
                stopAfterPreVisit();
                MavenResolutionResult model = tree.getMarkers().findFirst(MavenResolutionResult.class).orElse(null);
                if (model != null && requiresJavaxCoordinate(model)) {
                    return tree;
                }
                return SearchResult.found(tree);
            }
        };
    }

    private static boolean requiresJavaxCoordinate(MavenResolutionResult model) {
        if (model.findDependencies("javax.annotation", "javax.annotation-api", null).isEmpty()) {
            return false;
        }
        // Do not infer a binary contract for a common module being built from source in this reactor.
        boolean localHarness = model.getProjectPoms().values().stream().anyMatch(p ->
                "org.apache.karaf.itests".equals(p.getGroupId()) && "common".equals(p.getArtifactId()) &&
                "4.4.11".equals(p.getVersion()));
        return !localHarness && model.getDependencies().values().stream().anyMatch(dependencies ->
                ResolvedDependencyGraph.anyMatch(dependencies, d ->
                        "org.apache.karaf.itests".equals(d.getGroupId()) && "common".equals(d.getArtifactId()) &&
                        "4.4.11".equals(d.getVersion())));
    }
}
