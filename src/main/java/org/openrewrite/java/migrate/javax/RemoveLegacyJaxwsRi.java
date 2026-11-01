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
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.maven.tree.Dependency;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.ResolvedManagedDependency;
import org.openrewrite.semver.Semver;
import org.openrewrite.xml.RemoveContentVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.Optional;

@Value
@EqualsAndHashCode(callSuper = false)
public class RemoveLegacyJaxwsRi extends Recipe {
    String displayName = "Remove legacy JAX-WS RI declarations";
    String description = "Remove only positively identified javax-era JAX-WS RI dependencies, preserving modern and unresolved versions.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree == null) {
                    return null;
                }
                if (tree instanceof Xml.Document && tree.getMarkers().findFirst(MavenResolutionResult.class).isPresent()) {
                    return new MavenIsoVisitor<ExecutionContext>() {
                        @Override
                        public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                            Xml.Tag t = super.visitTag(tag, ctx);
                            if (isDependencyTag("com.sun.xml.ws", "jaxws-ri") ||
                                isManagedDependencyTag("com.sun.xml.ws", "jaxws-ri")) {
                                String version = tag.getChildValue("version").map(getResolutionResult().getPom()::getValue).orElse(null);
                                if (version == null) {
                                    ResolvedDependency dependency = findDependency(tag);
                                    ResolvedManagedDependency managed = findManagedDependency(tag);
                                    version = dependency != null ? dependency.getVersion() : managed != null ? managed.getVersion() : null;
                                }
                                if (isLegacy(version)) {
                                    doAfterVisit(new RemoveContentVisitor<>(t, true, true));
                                    maybeUpdateModel();
                                }
                            }
                            return t;
                        }
                    }.visit(tree, ctx);
                }
                Optional<GradleProject> project = tree.getMarkers().findFirst(GradleProject.class);
                if (project.isPresent()) {
                    for (GradleDependencyConfiguration configuration : project.get().getConfigurations()) {
                        Dependency requested = configuration.findRequestedDependency("com.sun.xml.ws", "jaxws-ri");
                        if (requested == null) {
                            continue;
                        }
                        // Declarable buckets often have no resolved dependencies of their own.
                        // Consult their consuming classpaths before falling back to a literal version.
                        String version = requested.getVersion();
                        for (GradleDependencyConfiguration consumer : project.get().configurationsExtendingFrom(configuration, true)) {
                            ResolvedDependency resolved = consumer.findResolvedDependency("com.sun.xml.ws", "jaxws-ri");
                            if (resolved != null) {
                                version = resolved.getVersion();
                                if (!isLegacy(version)) {
                                    break;
                                }
                            }
                        }
                        if (isLegacy(version)) {
                            tree = new org.openrewrite.gradle.RemoveDependency("com.sun.xml.ws", "jaxws-ri", configuration.getName())
                                    .getVisitor().visit(tree, ctx);
                        }
                    }
                }
                return tree;
            }
        };
    }

    private static boolean isLegacy(@Nullable String version) {
        if (version == null) {
            return false;
        }
        String major = Semver.majorVersion(version);
        return "1".equals(major) || "2".equals(major);
    }
}
