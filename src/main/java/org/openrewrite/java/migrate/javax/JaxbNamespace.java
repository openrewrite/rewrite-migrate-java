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
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/** Build-file precondition: Jakarta JAXB 3+ cannot supply javax.xml.bind types. */
@Value
@EqualsAndHashCode(callSuper = false)
public class JaxbNamespace extends ScanningRecipe<JaxbNamespace.Sources> {
    private static final VersionComparator MODERN = requireNonNull(Semver.validate("[3,)", null).getValue());

    @Option(displayName = "Modern namespace", description = "Whether to match graphs using Jakarta JAXB API 3 or later.", example = "true")
    boolean modern;

    @Option(displayName = "Require legacy runtime", description = "Match only graphs with known javax JAXB consumers and no existing direct javax JAXB API.", example = "true")
    boolean runtimeRequired;

    @Option(displayName = "Source usage", description = "When set, select modules with or without javax JAXB source uses.", required = false)
    @Nullable Boolean sourceUsage;

    static class Sources {
        final Set<Path> builds = new HashSet<>();
        final Set<Path> uses = new HashSet<>();
    }

    @Override
    public Sources getInitialValue(ExecutionContext ctx) {
        return new Sources();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Sources sources) {
        if (sourceUsage == null) {
            return TreeVisitor.noop();
        }
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree preVisit(Tree tree, ExecutionContext ctx) {
                stopAfterPreVisit();
                if (tree instanceof SourceFile) {
                    SourceFile file = (SourceFile) tree;
                    if (file.getMarkers().findFirst(MavenResolutionResult.class).isPresent() ||
                        file.getMarkers().findFirst(GradleProject.class).isPresent()) {
                        sources.builds.add(file.getSourcePath());
                    }
                    if (tree instanceof JavaSourceFile) {
                        JavaSourceFile cu = (JavaSourceFile) tree;
                        if (cu.getImports().stream().anyMatch(i -> "javax.xml.bind".equals(i.getTypeName()) ||
                                i.getTypeName().startsWith("javax.xml.bind.")) ||
                            new UsesType<>("javax.xml.bind..*", false).visit(cu, ctx) != cu) {
                            sources.uses.add(cu.getSourcePath());
                        }
                    }
                }
                return tree;
            }
        };
    }

    private static boolean usesSources(Path build, Sources sources) {
        for (Path source : sources.uses) {
            Path nearest = null;
            for (Path candidate : sources.builds) {
                Path root = candidate.getParent();
                if ((root == null || source.startsWith(root)) &&
                    (nearest == null || candidate.getNameCount() > nearest.getNameCount())) {
                    nearest = candidate;
                }
            }
            if (build.equals(nearest)) {
                return true;
            }
        }
        return false;
    }

    String displayName = "Check the JAXB dependency namespace";
    String description = "Select build files by their resolved JAXB namespace, including transitive and managed APIs.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Sources sources) {
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
                boolean legacy = tree.getMarkers().findFirst(MavenResolutionResult.class).map(m ->
                        m.getDependencies().values().stream().anyMatch(ds ->
                                ResolvedDependencyGraph.anyMatch(ds, d -> isLegacy(d.getGroupId(), d.getArtifactId(), d.getVersion())))).orElse(false) ||
                        tree.getMarkers().findFirst(GradleProject.class).map(g ->
                                g.getConfigurations().stream().anyMatch(c -> ResolvedDependencyGraph.anyMatch(c.getDirectResolved(),
                                        d -> isLegacy(d.getGroupId(), d.getArtifactId(), d.getVersion())))).orElse(false);
                boolean existing = tree.getMarkers().findFirst(MavenResolutionResult.class).map(m ->
                        m.getPom().getRequested().getDependencies().stream().anyMatch(d ->
                                "javax.xml.bind".equals(d.getGroupId()) && "jaxb-api".equals(d.getArtifactId()))).orElse(false) ||
                        tree.getMarkers().findFirst(GradleProject.class).map(g ->
                                g.getConfigurations().stream().anyMatch(c -> c.findRequestedDependency("javax.xml.bind", "jaxb-api") != null)).orElse(false);
                return found == modern && (!runtimeRequired || legacy && !existing) &&
                       (sourceUsage == null || sourceUsage == usesSources(((SourceFile) tree).getSourcePath(), sources)) ? SearchResult.found(tree) : tree;
            }
        };
    }

    private static boolean isLegacy(String group, String artifact, @Nullable String version) {
        if (version == null) {
            return false;
        }
        // These binaries load javax.xml.bind even when application sources do not import it.
        return (("org.glassfish.jaxb".equals(group) && "jaxb-runtime".equals(artifact) ||
                 "com.sun.xml.bind".equals(group) && "jaxb-impl".equals(artifact) ||
                 "com.sun.xml.ws".equals(group) && "jaxws-rt".equals(artifact) ||
                 "javax.xml.ws".equals(group) && "jaxws-api".equals(artifact) ||
                 "jakarta.xml.ws".equals(group) && "jakarta.xml.ws-api".equals(artifact)) && version.startsWith("2.")) ||
               ("org.apache.cxf".equals(group) && ("cxf-rt-databinding-jaxb".equals(artifact) || "cxf-rt-frontend-jaxws".equals(artifact)) && version.startsWith("3."));
    }

    private static boolean isModern(String group, String artifact, @Nullable String version) {
        return "jakarta.xml.bind".equals(group) && "jakarta.xml.bind-api".equals(artifact) &&
               version != null && MODERN.isValid(null, version);
    }
}
