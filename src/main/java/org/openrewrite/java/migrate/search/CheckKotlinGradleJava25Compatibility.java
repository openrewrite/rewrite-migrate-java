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
package org.openrewrite.java.migrate.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.gradle.internal.ChangeStringLiteral;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.marker.JavaProject;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.tree.J;
import org.openrewrite.kotlin.tree.K;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.properties.tree.Properties;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;
import org.openrewrite.java.migrate.table.KotlinGradleJava25Prerequisites;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/** Checks build-tool and compiler runtime prerequisites, independently of Kotlin bytecode target caps. */
@Value
@EqualsAndHashCode(callSuper = false)
public class CheckKotlinGradleJava25Compatibility extends ScanningRecipe<CheckKotlinGradleJava25Compatibility.Accumulator> {
    @Option(displayName = "Minimum Kotlin compiler version", description = "A compiler version verified to compile Kotlin while Gradle runs on Java 25.", example = "2.3.0")
    String minimumCompilerVersion;

    @Option(displayName = "Mark compatible files", description = "Mark eligible files when used as a precondition. Otherwise only report manual prerequisites without changing sources.", required = false)
    @Nullable Boolean markCompatible;

    @Option(displayName = "Upgrade editable compiler declarations", description = "Upgrade only owned Kotlin JVM 2.x compiler literals inside compatible shared builds.", required = false)
    @Nullable Boolean upgradeEditableCompiler;

    @Option(displayName = "Mark alignment candidates", description = "Select modeled Kotlin modules and markerless root builds with conventional root Kotlin sources and an owned Java 25 compatible JVM compiler.", required = false)
    @Nullable Boolean markAlignmentCandidates;

    @EqualsAndHashCode.Exclude
    transient KotlinGradleJava25Prerequisites prerequisites = new KotlinGradleJava25Prerequisites(this);

    String displayName = "Check Kotlin compiler prerequisites for Java 25";
    String description = "Preserve Gradle builds compiling Kotlin with an unsupported or unresolved compiler, and report a manual prerequisite. Java-only Kotlin DSL builds are not excluded. Independent nested build boundaries are retained.";

    public static class Accumulator {
        final Set<Path> buildRoots = new HashSet<>();
        final Map<Path, Path> buildFiles = new HashMap<>();
        final Set<Path> explicitRoots = new HashSet<>();
        final Set<Path> mavenRoots = new HashSet<>();
        final Set<Path> kotlinSources = new HashSet<>();
        final Set<UUID> kotlinModules = new HashSet<>();
        final List<Compiler> compilers = new ArrayList<>();
        final Set<Path> embeddedCompilerFiles = new HashSet<>();
        final Map<Path, Map<String, String>> properties = new HashMap<>();
        final Map<Path, Map<String, Integer>> declarations = new HashMap<>();
    }

    static class Compiler {
        final Path file;
        final String id;
        final @Nullable String version;
        final @Nullable String property;
        final boolean explicitVersion;
        final boolean editable;
        final boolean applied;
        final boolean opaque;
        final UUID declarationId;
        Compiler(Path file, String id, @Nullable String version, @Nullable String property, boolean explicitVersion, boolean editable, boolean applied, boolean opaque, UUID declarationId) {
            this.file = file; this.id = id; this.version = version; this.property = property;
            this.explicitVersion = explicitVersion; this.editable = editable; this.applied = applied; this.opaque = opaque; this.declarationId = declarationId;
        }
    }

    static class Blocked {
        final Path file;
        final String version;
        Blocked(Path file, String version) { this.file = file; this.version = version; }
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) { return new Accumulator(); }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (!(tree instanceof SourceFile)) return tree;
                SourceFile source = (SourceFile) tree;
                Path path = source.getSourcePath().normalize();
                String name = path.getFileName().toString();
                Path parent = parent(path);
                if ("settings.gradle".equals(name) || "settings.gradle.kts".equals(name)) acc.explicitRoots.add(parent);
                if ("pom.xml".equals(name)) acc.mavenRoots.add(parent);
                if (path.toString().replace('\\', '/').endsWith("gradle/wrapper/gradle-wrapper.properties")) {
                    acc.explicitRoots.add(parent(parent(parent(path))));
                }
                if ("gradle.properties".equals(name) && source instanceof Properties.File) {
                    Map<String, String> values = new HashMap<>();
                    for (Properties.Content content : ((Properties.File) source).getContent()) {
                        if (content instanceof Properties.Entry) {
                            Properties.Entry entry = (Properties.Entry) content;
                            values.put(entry.getKey(), entry.getValue().getText());
                        }
                    }
                    acc.properties.put(parent, values);
                }
                if (compiledKotlin(source)) acc.kotlinSources.add(path);
                // Preserve the existing modeled-module predicate; retain only the marker identity.
                if (path.toString().endsWith(".kt") || compiledKotlin(source)) {
                    source.getMarkers().findFirst(JavaProject.class).ifPresent(project -> acc.kotlinModules.add(project.getId()));
                }
                if ("build.gradle".equals(name) || "build.gradle.kts".equals(name)) {
                    acc.buildRoots.add(parent);
                    acc.buildFiles.put(parent, path);
                }
                if (("build.gradle".equals(name) || "build.gradle.kts".equals(name) ||
                     "settings.gradle".equals(name) || "settings.gradle.kts".equals(name)) &&
                    (source instanceof G.CompilationUnit || source instanceof K.CompilationUnit)) {
                    scanCompiler(source, acc, ctx);
                }
                return tree;
            }
        };
    }

    private static void scanCompiler(SourceFile source, Accumulator acc, ExecutionContext ctx) {
        Map<String, Set<String>> literals = new HashMap<>();
        Map<String, Integer> declarations = new HashMap<>();
        Set<String> writes = new HashSet<>();
        new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, ExecutionContext ctx) {
                declarations.merge(variable.getSimpleName(), 1, Integer::sum);
                if (topLevelVariable(getCursor()) && variable.getInitializer() instanceof J.Literal && ((J.Literal) variable.getInitializer()).getValue() instanceof String) {
                    literals.computeIfAbsent(variable.getSimpleName(), k -> new HashSet<>()).add((String) ((J.Literal) variable.getInitializer()).getValue());
                }
                return super.visitVariable(variable, ctx);
            }
            @Override public J.Assignment visitAssignment(J.Assignment assignment, ExecutionContext ctx) {
                if (assignment.getVariable() instanceof J.Identifier) writes.add(((J.Identifier) assignment.getVariable()).getSimpleName());
                return super.visitAssignment(assignment, ctx);
            }
            @Override public J.AssignmentOperation visitAssignmentOperation(J.AssignmentOperation assignment, ExecutionContext ctx) {
                if (assignment.getVariable() instanceof J.Identifier) writes.add(((J.Identifier) assignment.getVariable()).getSimpleName());
                return super.visitAssignmentOperation(assignment, ctx);
            }
        }.visit(source, ctx);
        acc.declarations.put(source.getSourcePath().normalize(), declarations);
        new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                J.MethodInvocation id = m;
                if ("version".equals(m.getSimpleName()) && m.getSelect() instanceof J.MethodInvocation) id = (J.MethodInvocation) m.getSelect();
                else if (getCursor().getParentTreeCursor().getValue() instanceof J.MethodInvocation &&
                         "version".equals(((J.MethodInvocation) getCursor().getParentTreeCursor().getValue()).getSimpleName())) return m;
                if (id.getSelect() != null || !("id".equals(id.getSimpleName()) || "kotlin".equals(id.getSimpleName())) ||
                    id.getArguments().size() != 1 || !(id.getArguments().get(0) instanceof J.Literal) || !plugins(getCursor())) return m;
                Object value = ((J.Literal) id.getArguments().get(0)).getValue();
                if (!(value instanceof String)) return m;
                String plugin = "kotlin".equals(id.getSimpleName()) ? "org.jetbrains.kotlin." + value : (String) value;
                if ("org.gradle.kotlin.kotlin-dsl".equals(plugin) && id == m) acc.embeddedCompilerFiles.add(source.getSourcePath().normalize());
                if (!("org.jetbrains.kotlin.jvm".equals(plugin) || "org.jetbrains.kotlin.android".equals(plugin) || "org.jetbrains.kotlin.multiplatform".equals(plugin) || "org.jetbrains.kotlin.js".equals(plugin))) return m;
                String version = null;
                String property = null;
                if (id != m && m.getArguments().size() == 1) {
                    if (m.getArguments().get(0) instanceof J.Literal && ((J.Literal) m.getArguments().get(0)).getValue() instanceof String) {
                        version = (String) ((J.Literal) m.getArguments().get(0)).getValue();
                    } else if (m.getArguments().get(0) instanceof J.Identifier) {
                        property = ((J.Identifier) m.getArguments().get(0)).getSimpleName();
                        Set<String> values = literals.get(property);
                        if (values != null && values.size() == 1 && !writes.contains(property)) version = values.iterator().next();
                    }
                }
                acc.compilers.add(new Compiler(source.getSourcePath().normalize(), plugin, version, property, id != m,
                        id != m && (m.getArguments().size() == 1 && m.getArguments().get(0) instanceof J.Literal || property != null && version != null),
                        applied(getCursor()), id != m && version == null, m.getId()));
                return m;
            }
            @Override
            public J.Identifier visitIdentifier(J.Identifier identifier, ExecutionContext ctx) {
                if ("kotlin-dsl".equals(identifier.getSimpleName()) && plugins(getCursor())) acc.embeddedCompilerFiles.add(source.getSourcePath().normalize());
                return super.visitIdentifier(identifier, ctx);
            }
        }.visit(source, ctx);
    }

    private static boolean topLevelVariable(Cursor cursor) {
        Iterator<Object> path = cursor.getParentOrThrow().getPath();
        boolean lambdaNeedsOwner = false;
        while (path.hasNext()) {
            Object value = path.next();
            if (value instanceof J.ClassDeclaration || value instanceof J.MethodDeclaration) return false;
            if (value instanceof J.Lambda) {
                if (lambdaNeedsOwner) return false;
                lambdaNeedsOwner = true;
            }
            if (value instanceof J.MethodInvocation) {
                J.MethodInvocation method = (J.MethodInvocation) value;
                if (method.getSelect() != null || !("plugins".equals(method.getSimpleName()) || "pluginManagement".equals(method.getSimpleName()))) return false;
                lambdaNeedsOwner = false;
            }
        }
        return !lambdaNeedsOwner;
    }

    private static boolean plugins(Cursor cursor) {
        Iterator<Object> parents = cursor.getParentOrThrow().getPath();
        boolean found = false;
        while (parents.hasNext()) {
            Object value = parents.next();
            if (!(value instanceof J.MethodInvocation)) continue;
            J.MethodInvocation m = (J.MethodInvocation) value;
            if (!found && ("version".equals(m.getSimpleName()) || "apply".equals(m.getSimpleName()))) continue;
            if (!found && "plugins".equals(m.getSimpleName()) && m.getSelect() == null) { found = true; continue; }
            if (found && "pluginManagement".equals(m.getSimpleName()) && m.getSelect() == null) continue;
            return false;
        }
        return found;
    }

    private static boolean applied(Cursor cursor) {
        Iterator<Object> path = cursor.getParentOrThrow().getPath();
        while (path.hasNext()) {
            Object value = path.next();
            if (value instanceof J.MethodInvocation) {
                J.MethodInvocation method = (J.MethodInvocation) value;
                if ("pluginManagement".equals(method.getSimpleName())) return false;
                if ("apply".equals(method.getSimpleName()) && method.getArguments().size() == 1 &&
                    method.getArguments().get(0) instanceof J.Literal &&
                    Boolean.FALSE.equals(((J.Literal) method.getArguments().get(0)).getValue())) return false;
            }
        }
        return true;
    }

    private static boolean compiledKotlin(SourceFile source) {
        String path = source.getSourcePath().toString().replace('\\', '/');
        if (path.matches(".*(?:^|/)src/[^/]+/resources/.*")) return false;
        if (path.matches("(?:.*/)?src/[^/]+/kotlin/.*")) return path.endsWith(".kt") || path.endsWith(".gradle.kts");
        return source instanceof K.CompilationUnit && path.endsWith(".kt") &&
                (source.getMarkers().findFirst(JavaProject.class).isPresent() || source.getMarkers().findFirst(JavaSourceSet.class).isPresent());
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        Set<Path> roots = new HashSet<>(acc.explicitRoots);
        roots.addAll(acc.mavenRoots);
        for (Path build : acc.buildRoots) if (owner(build, roots) == null) roots.add(build);
        Map<Path, List<Compiler>> byBuild = new HashMap<>();
        for (Compiler compiler : acc.compilers) {
            Path root = owner(compiler.file, roots);
            if (root != null) byBuild.computeIfAbsent(root, k -> new ArrayList<>()).add(compiler);
        }
        Set<Path> kotlinBuilds = new HashSet<>();
        for (Path source : acc.kotlinSources) {
            Path root = owner(source, roots);
            if (root != null) kotlinBuilds.add(root);
        }
        Set<Path> kotlinSourceBuilds = new HashSet<>(kotlinBuilds);
        // An applied compiler can fail during configuration even before it has source files.
        for (Compiler compiler : acc.compilers) if (compiler.applied) {
            Path root = owner(compiler.file, roots);
            if (root != null) kotlinBuilds.add(root);
        }
        VersionComparator versions = Objects.requireNonNull(Semver.validate("latest.release", null).getValue());
        Map<Path, Blocked> blocked = new HashMap<>();
        Map<Path, Set<UUID>> literalDeclarations = new HashMap<>();
        Map<Path, Set<String>> literalVariables = new HashMap<>();
        for (Path root : kotlinBuilds) {
            List<Compiler> compilers = byBuild.getOrDefault(root, Collections.emptyList());
            boolean active = false;
            for (Compiler compiler : compilers) if (compiler.applied) active = true;
            Set<Compiler> neededCompilers = new HashSet<>();
            if (!active) {
                boolean embedded = false;
                for (Path file : acc.embeddedCompilerFiles) if (root.equals(owner(file, roots))) embedded = true;
                if (!embedded) for (Path build : acc.buildRoots) if (root.equals(owner(build, roots))) {
                    Path file = acc.buildFiles.get(build);
                    blocked.put(root, new Blocked(file, "unknown"));
                    break;
                }
            }
            for (Compiler compiler : compilers) {
                if (!compiler.applied) continue;
                String version = resolve(compiler, root, acc);
                boolean editable = editable(compiler, acc, root, roots);
                boolean opaque = compiler.opaque;
                Set<Compiler> writers = new HashSet<>();
                if (editable) writers.add(compiler);
                if (version == null && !compiler.explicitVersion) {
                    Set<String> inherited = new HashSet<>();
                    for (Compiler other : compilers) if (other.id.equals(compiler.id)) {
                        String known = resolve(other, root, acc);
                        if (known != null) inherited.add(known);
                    }
                    if (inherited.size() == 1) {
                        version = inherited.iterator().next();
                        opaque = true;
                        for (Compiler definition : compilers) if (definition.id.equals(compiler.id) && version.equals(resolve(definition, root, acc)) && !definition.opaque) opaque = false;
                        for (Compiler other : compilers) if (other.id.equals(compiler.id) && version.equals(resolve(other, root, acc)) && editable(other, acc, root, roots)) {
                            editable = true;
                            writers.add(other);
                        }
                    }
                }
                if (opaque || version == null || !version.matches("\\d+\\.\\d+\\.\\d+(?:[-+][0-9A-Za-z.-]+)?") ||
                    versions.compare(null, version, minimumCompilerVersion) < 0 && !(version.startsWith("2.") && minimumCompilerVersion.startsWith("2.") && editable)) {
                    blocked.put(root, new Blocked(compiler.file, version == null ? "unknown" : version));
                    break;
                }
                if (editable && version != null && versions.compare(null, version, minimumCompilerVersion) < 0) neededCompilers.addAll(writers);
            }
            if (!blocked.containsKey(root)) for (Compiler compiler : neededCompilers) {
                if (compiler.property == null) literalDeclarations.computeIfAbsent(compiler.file, k -> new HashSet<>()).add(compiler.declarationId);
                else literalVariables.computeIfAbsent(compiler.file, k -> new HashSet<>()).add(compiler.property);
            }
        }
        Set<Path> ownedCompilerRoots = new HashSet<>();
        boolean rootKotlinSources = acc.kotlinSources.stream().anyMatch(path ->
                path.toString().replace('\\', '/').matches("src/[^/]+/kotlin/.*"));
        for (Path root : kotlinSourceBuilds) {
            // buildSrc and other unmodeled nested build logic do not establish root source ownership.
            if (!root.toString().isEmpty() || !rootKotlinSources || blocked.containsKey(root)) continue;
            for (Compiler compiler : byBuild.getOrDefault(root, Collections.emptyList())) {
                String known = resolve(compiler, root, acc);
                if (compiler.applied && compiler.explicitVersion && !compiler.opaque &&
                    "org.jetbrains.kotlin.jvm".equals(compiler.id) && editable(compiler, acc, root, roots) &&
                    known != null && versions.compare(null, known, minimumCompilerVersion) >= 0) {
                    ownedCompilerRoots.add(root);
                }
            }
        }
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (!(tree instanceof SourceFile)) return tree;
                SourceFile source = (SourceFile) tree;
                Path root = owner(source.getSourcePath().normalize(), roots);
                Blocked refusal = blocked.get(root);
                if (Boolean.TRUE.equals(markAlignmentCandidates)) {
                    boolean modeledKotlin = source.getMarkers().findFirst(JavaProject.class)
                            .map(project -> acc.kotlinModules.contains(project.getId())).orElse(false);
                    Path path = source.getSourcePath().normalize();
                    boolean ownedRoot = ownedCompilerRoots.contains(root) && path.getParent() == null &&
                            ("build.gradle".equals(path.toString()) || "build.gradle.kts".equals(path.toString()));
                    return modeledKotlin || ownedRoot ? SearchResult.found(source) : source;
                }
                if (Boolean.TRUE.equals(upgradeEditableCompiler)) {
                    if (refusal != null) return source;
                    Set<UUID> declarations = literalDeclarations.getOrDefault(source.getSourcePath().normalize(), Collections.emptySet());
                    Set<String> variables = literalVariables.getOrDefault(source.getSourcePath().normalize(), Collections.emptySet());
                    if (declarations.isEmpty() && variables.isEmpty()) return source;
                    return new JavaIsoVisitor<ExecutionContext>() {
                        @Override public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                            J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                            if (declarations.contains(m.getId()) && m.getArguments().size() == 1 && m.getArguments().get(0) instanceof J.Literal) {
                                return m.withArguments(Collections.singletonList(ChangeStringLiteral.withStringValue((J.Literal) m.getArguments().get(0), minimumCompilerVersion)));
                            }
                            return m;
                        }
                        @Override public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, ExecutionContext ctx) {
                            J.VariableDeclarations.NamedVariable v = super.visitVariable(variable, ctx);
                            if (variables.contains(v.getSimpleName()) && v.getInitializer() instanceof J.Literal) {
                                return v.withInitializer(ChangeStringLiteral.withStringValue((J.Literal) v.getInitializer(), minimumCompilerVersion));
                            }
                            return v;
                        }
                    }.visit(source, ctx);
                }
                if (Boolean.TRUE.equals(markCompatible)) return refusal == null ? SearchResult.found(source) : source;
                if (refusal != null && source.getSourcePath().normalize().equals(refusal.file)) {
                    prerequisites.insertRow(ctx, new KotlinGradleJava25Prerequisites.Row(refusal.file.toString(), refusal.version,
                            "The Kotlin compiler cannot be assumed to configure and compile on the Java 25/Gradle 9 runtime.",
                            "Keep this build unchanged; explicitly select a compatible Kotlin compiler (at least " + minimumCompilerVersion + ") before retrying Java 25. Kotlin 1.x is not automatically migrated to K2."));
                }
                return source;
            }
        };
    }

    private static boolean editable(Compiler compiler, Accumulator acc, Path root, Set<Path> roots) {
        if (!compiler.editable || !"org.jetbrains.kotlin.jvm".equals(compiler.id)) return false;
        if (compiler.property == null) return true;
        int count = 0;
        for (Map.Entry<Path, Map<String, Integer>> declarations : acc.declarations.entrySet()) {
            if (root.equals(owner(declarations.getKey(), roots))) count += declarations.getValue().getOrDefault(compiler.property, 0);
        }
        return count == 1;
    }

    private static @Nullable String resolve(Compiler compiler, Path root, Accumulator acc) {
        if (compiler.version != null) return compiler.version;
        if (compiler.property != null) {
            Map<String, String> local = acc.properties.get(parent(compiler.file));
            if (local != null && local.containsKey(compiler.property)) return local.get(compiler.property);
            return acc.properties.getOrDefault(root, Collections.emptyMap()).get(compiler.property);
        }
        return null;
    }

    private static Path parent(Path path) { return path.getParent() == null ? Paths.get("") : path.getParent(); }
    private static @Nullable Path owner(Path path, Set<Path> roots) {
        Path owner = null;
        for (Path root : roots) if ((root.toString().isEmpty() || path.startsWith(root)) &&
                (owner == null || root.toString().length() > owner.toString().length())) owner = root;
        return owner;
    }
}
