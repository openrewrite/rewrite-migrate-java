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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;

import static java.util.Objects.requireNonNull;

/** Build-file precondition: Jakarta JAX-WS 3+ cannot supply javax.xml.ws or javax.jws types. */
@Value
@EqualsAndHashCode(callSuper = false)
public class JaxwsNamespace extends Recipe {
    private static final VersionComparator MODERN = requireNonNull(Semver.validate("[3,)", null).getValue());

    @Option(displayName = "Modern namespace", description = "Whether to match graphs using Jakarta JAX-WS or JWS API 3 or later.", example = "true")
    boolean modern;

    String displayName = "Check the JAX-WS dependency namespace";
    String description = "Select build files by their resolved JAX-WS namespace, including transitive and managed APIs.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (!(tree instanceof SourceFile)) {
                    return tree;
                }
                boolean found = tree.getMarkers().findFirst(MavenResolutionResult.class).map(m ->
                        m.getDependencies().values().stream().anyMatch(dependencies ->
                                ResolvedDependencyGraph.anyMatch(dependencies, d ->
                                        isModern(d.getGroupId(), d.getArtifactId(), d.getVersion()))) ||
                        m.getPom().getDependencyManagement().stream().anyMatch(d ->
                                isModern(d.getGroupId(), d.getArtifactId(), d.getVersion()))).orElse(false) ||
                        tree.getMarkers().findFirst(GradleProject.class).map(g ->
                                g.getConfigurations().stream().anyMatch(c ->
                                        ResolvedDependencyGraph.anyMatch(c.getDirectResolved(), d ->
                                                isModern(d.getGroupId(), d.getArtifactId(), d.getVersion())))).orElse(false);
                return found == modern ? SearchResult.found(tree) : tree;
            }
        };
    }

    private static boolean isModern(String group, String artifact, @Nullable String version) {
        return (("jakarta.xml.ws".equals(group) && "jakarta.xml.ws-api".equals(artifact)) ||
                ("jakarta.jws".equals(group) && "jakarta.jws-api".equals(artifact))) &&
               version != null && MODERN.isValid(null, version);
    }
}
