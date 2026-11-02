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
import org.openrewrite.*;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.java.marker.JavaProject;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.Scope;

import java.nio.file.Path;
import java.util.*;

/** Supply the namespace required by application sources, not by unrelated configurations or modules. */
@Value
@EqualsAndHashCode(callSuper = false)
public class AddScopedJavaxJaxwsDependencies extends ScanningRecipe<AddScopedJavaxJaxwsDependencies.Uses> {
    @Option(displayName = "API", description = "The legacy API needed by sources: XML-WS or JWS.", example = "ws", valid = {"ws", "jws"})
    String api;

    String displayName = "Supply a legacy JAX-WS API in its source scope";
    String description = "Keep modern Jakarta APIs intact and supply a javax-compatible API only to source sets that use it, " +
                         "reusing an existing equivalent coordinate when its current scope is insufficient.";

    static class Uses {
        final Map<Path, SourceFile> builds = new LinkedHashMap<>();
        final Map<Path, SourceUse> sources = new LinkedHashMap<>();
    }

    static class SourceUse {
        final SourceFile source;
        final @Nullable String sourceSet;

        SourceUse(SourceFile source, @Nullable String sourceSet) {
            this.source = source;
            this.sourceSet = sourceSet;
        }
    }

    @Override
    public Uses getInitialValue(ExecutionContext ctx) {
        return new Uses();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Uses uses) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree preVisit(Tree tree, ExecutionContext ctx) {
                stopAfterPreVisit();
                if (!(tree instanceof SourceFile)) {
                    return tree;
                }
                SourceFile file = (SourceFile) tree;
                String name = file.getSourcePath().getFileName().toString();
                if ("pom.xml".equals(name) || "build.gradle".equals(name) || "build.gradle.kts".equals(name)) {
                    // An unmodelled child build is still a module boundary; never spill its uses into a parent.
                    uses.builds.put(file.getSourcePath(), file);
                } else if (file instanceof JavaSourceFile) {
                    JavaSourceFile cu = (JavaSourceFile) file;
                    String pkg = "ws".equals(api) ? "javax.xml.ws" : "javax.jws";
                    if (cu.getImports().stream().anyMatch(i -> i.getTypeName().equals(pkg) || i.getTypeName().startsWith(pkg + ".")) ||
                        new UsesType<>(pkg + "..*", false).visit(cu, ctx) != cu) {
                        uses.sources.put(file.getSourcePath(), new SourceUse(file,
                                file.getMarkers().findFirst(JavaSourceSet.class).map(JavaSourceSet::getName).orElse(null)));
                    }
                }
                return tree;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Uses uses) {
        Map<Path, Set<String>> required = new LinkedHashMap<>();
        for (SourceUse use : uses.sources.values()) {
            Path build = owner(use.source, uses.builds);
            if (build == null) {
                continue;
            }
            Path parent = build.getParent();
            Path relative = parent == null ? use.source.getSourcePath() : parent.relativize(use.source.getSourcePath());
            // An unmodelled buildSrc/included build is not application source of its enclosing build.
            if (relative.startsWith("buildSrc") || relative.startsWith("build-logic")) {
                continue;
            }
            String sourceSet = use.sourceSet != null ? use.sourceSet : relative.startsWith("src/test") ? "test" : "main";
            required.computeIfAbsent(build, key -> new LinkedHashSet<>()).add(sourceSet);
        }
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (!(tree instanceof SourceFile)) {
                    return tree;
                }
                SourceFile file = (SourceFile) tree;
                Set<String> sourceSets = required.get(file.getSourcePath());
                if (sourceSets == null || new JaxwsNamespace(true).getVisitor().visit(file, ctx) == file) {
                    return tree;
                }
                MavenResolutionResult maven = file.getMarkers().findFirst(MavenResolutionResult.class).orElse(null);
                if (maven != null) {
                    return supplyMaven(file, maven, sourceSets.contains("main") ? "main" : "test", ctx);
                }
                GradleProject gradle = file.getMarkers().findFirst(GradleProject.class).orElse(null);
                if (gradle != null) {
                    // Main first: a production insertion can also cover test source sets via the model's inheritance.
                    List<String> ordered = new ArrayList<>(sourceSets);
                    ordered.sort(Comparator.comparing(s -> !"main".equals(s)));
                    for (String sourceSet : ordered) {
                        file = supplyGradle(file, sourceSet, ctx);
                    }
                }
                return file;
            }
        };
    }

    private static @Nullable Path owner(SourceFile source, Map<Path, SourceFile> builds) {
        Path nearest = null;
        JavaProject project = source.getMarkers().findFirst(JavaProject.class).orElse(null);
        for (Map.Entry<Path, SourceFile> entry : builds.entrySet()) {
            Path build = entry.getKey();
            Path root = build.getParent();
            if (root != null && !source.getSourcePath().startsWith(root)) {
                continue;
            }
            JavaProject buildProject = entry.getValue().getMarkers().findFirst(JavaProject.class).orElse(null);
            if (project != null && buildProject != null && !project.getId().equals(buildProject.getId())) {
                continue;
            }
            if (nearest == null || build.getNameCount() > nearest.getNameCount()) {
                nearest = build;
            }
        }
        return nearest;
    }

    private boolean legacy(ResolvedDependency dependency) {
        return JaxwsNamespace.isLegacy(api, dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion());
    }

    private boolean available(Collection<ResolvedDependency> dependencies) {
        return ResolvedDependencyGraph.anyMatch(dependencies, this::legacy);
    }

    private boolean usableLegacy(ResolvedDependency candidate, List<ResolvedDependency> candidates) {
        // Gradle can resolve different API versions in different source sets. Do not promote a
        // legacy Jakarta GAV when that same GAV supplies modern types elsewhere in this project.
        return legacy(candidate) && candidates.stream().noneMatch(other ->
                candidate.getGroupId().equals(other.getGroupId()) && candidate.getArtifactId().equals(other.getArtifactId()) &&
                JaxwsNamespace.isModern(other.getGroupId(), other.getArtifactId(), other.getVersion()));
    }

    private static List<ResolvedDependency> graph(Collection<ResolvedDependency> roots) {
        List<ResolvedDependency> result = new ArrayList<>();
        Set<ResolvedDependency> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        Deque<ResolvedDependency> pending = new ArrayDeque<>(roots);
        while (!pending.isEmpty()) {
            ResolvedDependency dependency = pending.removeFirst();
            if (seen.add(dependency)) {
                result.add(dependency);
                pending.addAll(dependency.getDependencies());
            }
        }
        return result;
    }

    private SourceFile supplyMaven(SourceFile file, MavenResolutionResult model, String sourceSet, ExecutionContext ctx) {
        if (available(model.getDependencies().getOrDefault("main".equals(sourceSet) ? Scope.Compile : Scope.Test, Collections.emptyList())) ||
            "main".equals(sourceSet) && model.getDependencies().getOrDefault(Scope.Provided, Collections.emptyList()).stream()
                    .anyMatch(d -> declaredScope(model, d) == Scope.Provided && available(Collections.singletonList(d)))) {
            return file;
        }
        List<ResolvedDependency> candidates = new ArrayList<>();
        for (List<ResolvedDependency> dependencies : model.getDependencies().values()) {
            candidates.addAll(graph(dependencies));
        }
        ResolvedDependency existing = candidates.stream().filter(d -> usableLegacy(d, candidates))
                .filter(d -> model.getPom().getRequested().getDependencies().stream().anyMatch(requested ->
                        d.getGroupId().equals(model.getPom().getValue(requested.getGroupId())) &&
                        d.getArtifactId().equals(model.getPom().getValue(requested.getArtifactId()))))
                .findFirst().orElse(null);
        String scope = "main".equals(sourceSet) ? "compile" : "test";
        if (existing != null) {
            final ResolvedDependency direct = existing;
            org.openrewrite.maven.tree.Dependency requested = model.getPom().getRequested().getDependencies().stream()
                    .filter(d -> direct.getGroupId().equals(model.getPom().getValue(d.getGroupId())) &&
                            direct.getArtifactId().equals(model.getPom().getValue(d.getArtifactId())))
                    .findFirst().get();
            SourceFile promoted = (SourceFile) new org.openrewrite.maven.ChangeDependencyScope(
                    requested.getGroupId(), requested.getArtifactId(), scope).getVisitor().visitNonNull(file, ctx);
            return promoted == file ? file : (SourceFile) new org.openrewrite.maven.UpdateMavenModel<ExecutionContext>()
                    .visitNonNull(promoted, ctx);
        }
        existing = candidates.stream().filter(d -> usableLegacy(d, candidates)).findFirst().orElse(null);
        return (SourceFile) new org.openrewrite.maven.AddDependencyVisitor(
                existing == null ? fallbackGroup() : existing.getGroupId(),
                existing == null ? fallbackArtifact() : existing.getArtifactId(),
                existing == null ? fallbackVersion() : existing.getVersion(), null, scope, null, null, null, null, null)
                .visitNonNull(file, ctx);
    }

    private static Scope declaredScope(MavenResolutionResult model, ResolvedDependency dependency) {
        String scope = dependency.getRequested() == null ? null : model.getPom().getValue(dependency.getRequested().getScope());
        return scope == null ? model.getPom().getManagedScope(dependency.getGroupId(), dependency.getArtifactId(),
                dependency.getType(), dependency.getClassifier()) : Scope.fromName(scope);
    }

    private SourceFile supplyGradle(SourceFile file, String sourceSet, ExecutionContext ctx) {
        GradleProject model = file.getMarkers().findFirst(GradleProject.class).get();
        String prefix = "main".equals(sourceSet) ? "" : sourceSet;
        String compile = prefix.isEmpty() ? "compileClasspath" : prefix + "CompileClasspath";
        GradleDependencyConfiguration classpath = model.getConfiguration(compile);
        if (classpath != null && available(classpath.getDirectResolved())) {
            return file;
        }
        String target = prefix.isEmpty() ? "implementation" : prefix + "Implementation";
        // Do not invent custom source-set configurations absent from the model.
        if (!prefix.isEmpty() && !"test".equals(sourceSet) && model.getConfiguration(target) == null) {
            return file;
        }
        List<ResolvedDependency> candidates = new ArrayList<>();
        for (GradleDependencyConfiguration configuration : model.getConfigurations()) {
            if (JaxwsNamespace.isApplicationConfiguration(configuration.getName())) {
                candidates.addAll(graph(configuration.getDirectResolved()));
            }
        }
        for (ResolvedDependency candidate : candidates) {
            if (!usableLegacy(candidate, candidates)) {
                continue;
            }
            for (GradleDependencyConfiguration configuration : model.getConfigurations()) {
                if (!JaxwsNamespace.isApplicationConfiguration(configuration.getName()) ||
                    configuration.findRequestedDependency(candidate.getGroupId(), candidate.getArtifactId()) == null) {
                    continue;
                }
                String name = configuration.getName();
                if (name.equals(target) || name.equals(prefix.isEmpty() ? "compileOnly" : prefix + "CompileOnly") ||
                    prefix.isEmpty() && ("api".equals(name) || "compile".equals(name))) {
                    return file;
                }
                // Moving from test to production covers the original test consumer; moving runtimeOnly
                // to implementation preserves runtime availability. Other custom scopes stay intact.
                if (prefix.isEmpty() && ("testImplementation".equals(name) || "runtimeOnly".equals(name))) {
                    SourceFile promoted = promoteGradle(file, candidate, name, target, ctx);
                    if (promoted != file) {
                        return promoted;
                    }
                }
            }
        }
        ResolvedDependency existing = candidates.stream().filter(d -> usableLegacy(d, candidates)).findFirst().orElse(null);
        return (SourceFile) new org.openrewrite.gradle.AddDependencyVisitor(
                existing == null ? fallbackGroup() : existing.getGroupId(),
                existing == null ? fallbackArtifact() : existing.getArtifactId(),
                existing == null ? fallbackVersion() : existing.getVersion(), null, target, null, null, null,
                cursor -> cursor.getParentOrThrow().firstEnclosing(org.openrewrite.java.tree.J.MethodInvocation.class) == null, null)
                .visitNonNull(file, ctx);
    }

    private SourceFile promoteGradle(SourceFile file, ResolvedDependency dependency, String oldConfiguration,
                                     String target, ExecutionContext ctx) {
        return (SourceFile) new org.openrewrite.java.JavaIsoVisitor<ExecutionContext>() {
            @Override
            public org.openrewrite.java.tree.J.MethodInvocation visitMethodInvocation(
                    org.openrewrite.java.tree.J.MethodInvocation method, ExecutionContext context) {
                org.openrewrite.java.tree.J.MethodInvocation m = super.visitMethodInvocation(method, context);
                if (!new org.openrewrite.gradle.trait.GradleDependency.Matcher().configuration(oldConfiguration)
                        .groupId(dependency.getGroupId()).artifactId(dependency.getArtifactId()).get(getCursor()).isPresent()) {
                    return m;
                }
                boolean dependencies = false;
                for (Cursor parent = getCursor().getParent(); parent != null; parent = parent.getParent()) {
                    if (parent.getValue() instanceof org.openrewrite.java.tree.J.MethodInvocation) {
                        org.openrewrite.java.tree.J.MethodInvocation enclosing = parent.getValue();
                        if (dependencies || !"dependencies".equals(enclosing.getSimpleName()) || enclosing.getSelect() != null) {
                            return m;
                        }
                        dependencies = true;
                    }
                }
                if (!dependencies) {
                    return m;
                }
                JavaSourceSet.markDirty(context, file);
                return m.withName(m.getName().withSimpleName(target));
            }
        }.visitNonNull(file, ctx);
    }

    private String fallbackGroup() {
        return "ws".equals(api) ? "javax.xml.ws" : "javax.jws";
    }

    private String fallbackArtifact() {
        return "ws".equals(api) ? "jaxws-api" : "javax.jws-api";
    }

    private String fallbackVersion() {
        return "ws".equals(api) ? "2.3.1" : "1.1";
    }
}
