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
package org.openrewrite.java.migrate;

import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeTree;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.maven.tree.Dependency;
import org.openrewrite.maven.tree.ManagedDependency;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.Parent;
import org.openrewrite.maven.tree.Plugin;
import org.openrewrite.maven.tree.Profile;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.ResolvedManagedDependency;
import org.openrewrite.maven.tree.ResolvedPom;
import org.openrewrite.xml.ChangeTagValueVisitor;
import org.openrewrite.xml.tree.Xml;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A deliberately bounded Core-only major upgrade, not a Spring Framework migration. */
public class UpgradeStandaloneSpringCoreForJava25 extends ScanningRecipe<UpgradeStandaloneSpringCoreForJava25.State> {
    private static final String GROUP = "org.springframework";
    private static final String TARGET = "6.2.11";
    private static final Set<String> TYPES = new HashSet<>(Arrays.asList(
            "org.springframework.core.io.Resource", "org.springframework.core.io.ResourceLoader",
            "org.springframework.core.io.support.ResourcePatternResolver",
            "org.springframework.core.io.support.PathMatchingResourcePatternResolver",
            "org.springframework.core.type.AnnotationMetadata", "org.springframework.core.type.AnnotatedTypeMetadata",
            "org.springframework.core.type.ClassMetadata", "org.springframework.core.type.classreading.MetadataReader",
            "org.springframework.core.type.classreading.MetadataReaderFactory",
            "org.springframework.core.type.classreading.SimpleMetadataReaderFactory", "org.springframework.util.ClassUtils"));
    private static final Set<String> MEMBERS = new HashSet<>(Arrays.asList(
            "Resource#exists", "Resource#getFilename", "ResourceLoader#getResource", "ResourcePatternResolver#getResources",
            "PathMatchingResourcePatternResolver#<constructor>", "PathMatchingResourcePatternResolver#getResource",
            "PathMatchingResourcePatternResolver#getResources",
            "SimpleMetadataReaderFactory#<constructor>", "SimpleMetadataReaderFactory#getMetadataReader",
            "MetadataReaderFactory#getMetadataReader", "MetadataReader#getClassMetadata", "MetadataReader#getAnnotationMetadata",
            "ClassMetadata#hasSuperClass", "ClassMetadata#getSuperClassName", "ClassMetadata#isConcrete",
            "ClassMetadata#getInterfaceNames", "ClassMetadata#getClassName",
            "AnnotationMetadata#hasAnnotation", "AnnotatedTypeMetadata#getAnnotationAttributes",
            "AnnotationMetadata#getAnnotationAttributes", "ClassUtils#convertClassNameToResourcePath"));

    @Override public String getDisplayName() { return "Upgrade standalone Spring Core metadata readers for Java 25"; }
    @Override public String getDescription() {
        return "Explicitly upgrade standalone Spring Core 5.3 OSS dependencies to 6.2.11 only when the complete Maven graph contains " +
               "no other Spring family and typed Java usage is limited to private implementation metadata/resource APIs. " +
               "Refuse framework management, unknown graphs, mixed versions, other APIs and exposed Spring signatures. Pin local dependencies without changing shared properties.";
    }
    public static class State {
        final Map<Path, Module> modules = new HashMap<>();
        final Map<Path, Usage> sources = new HashMap<>();
    }
    private static class Usage {
        final boolean safe;
        final boolean usesSpring;
        Usage(boolean safe, boolean usesSpring) { this.safe = safe; this.usesSpring = usesSpring; }
    }
    private static class Module {
        boolean safe;
        boolean usesSpring;
        Module(boolean safe) { this.safe = safe; }
    }
    @Override public State getInitialValue(ExecutionContext ctx) { return new State(); }
    @Override public TreeVisitor<?, ExecutionContext> getScanner(State state) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof SourceFile) {
                    SourceFile source = (SourceFile) tree;
                    if ("pom.xml".equals(source.getSourcePath().getFileName().toString())) {
                        state.modules.put(directory(source.getSourcePath()), new Module(source.getMarkers()
                                .findFirst(MavenResolutionResult.class).map(m -> source instanceof Xml.Document && standalone(m) && localVersions((Xml.Document) source, m.getPom())).orElse(false)));
                    } else {
                        Module facts = new Module(true);
                        String path = source.getSourcePath().toString();
                        if (source instanceof J.CompilationUnit) inspect((J.CompilationUnit) source, facts);
                        else if (path.endsWith(".java") || path.endsWith(".kt") || path.endsWith(".groovy")) facts.safe = false;
                        state.sources.put(source.getSourcePath(), new Usage(facts.safe, facts.usesSpring));
                    }
                }
                return tree;
            }
        };
    }
    @Override public TreeVisitor<?, ExecutionContext> getVisitor(State state) {
        for (Map.Entry<Path, Usage> source : state.sources.entrySet()) {
            Module module = owner(source.getKey(), state.modules);
            if (module == null) continue;
            module.safe &= source.getValue().safe;
            module.usesSpring |= source.getValue().usesSpring;
        }
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                Module module = state.modules.get(directory(document.getSourcePath()));
                return module != null && module.safe && module.usesSpring ? super.visitDocument(document, ctx) : document;
            }
            @Override public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = super.visitTag(tag, ctx);
                if (!isDependencyTag(GROUP, "spring-core") && !isDependencyTag(GROUP, "spring-jcl")) return t;
                // No profiles or dependency management are eligible. Only the declaration-local version changes.
                Optional<Xml.Tag> version = t.getChild("version");
                if (!version.isPresent()) return t;
                Xml.Tag updated = (Xml.Tag) new ChangeTagValueVisitor<>(version.get(), TARGET)
                        .visitNonNull(t, ctx, getCursor().getParentOrThrow());
                if (updated != t) maybeUpdateModel();
                return updated;
            }
        };
    }
    private static Path directory(Path path) { return path.getParent() == null ? Paths.get("") : path.getParent().normalize(); }
    private static @Nullable Module owner(Path source, Map<Path, Module> modules) {
        Path best = null;
        for (Path root : modules.keySet()) {
            if ((root.toString().isEmpty() || source.startsWith(root)) && (best == null || root.toString().length() > best.toString().length())) best = root;
        }
        return best == null ? null : modules.get(best);
    }
    private static boolean spring(@Nullable String group) { return group != null && (group.startsWith("org.springframework") || group.startsWith("io.spring")); }
    private static boolean old(@Nullable String version) {
        if (version == null || !version.matches("5\\.3\\.[0-9]+")) return false;
        try { return Integer.parseInt(version.substring(4)) <= 39; } catch (NumberFormatException e) { return false; }
    }
    private static boolean localVersions(Xml.Document document, ResolvedPom pom) {
        Optional<Xml.Tag> dependencies = document.getRoot().getChild("dependencies");
        if (!dependencies.isPresent()) return false;
        for (Xml.Tag dependency : dependencies.get().getChildren()) {
            String group = pom.getValue(dependency.getChildValue("groupId").orElse(null));
            if (group == null || group.contains("${")) return false;
            if (spring(group) && (!old(pom.getValue(dependency.getChildValue("version").orElse(null))) ||
                dependency.getChild("systemPath").isPresent())) return false;
        }
        return true;
    }
    private static boolean standalone(MavenResolutionResult m) {
        ResolvedPom pom = m.getPom();
        if (m.getDependencies().isEmpty() || !m.getModules().isEmpty() || !pom.getSubprojects().isEmpty() ||
            frameworkPlugin(pom.getPlugins(), pom) || frameworkPlugin(pom.getPluginManagement(), pom)) return false;
        for (MavenResolutionResult p = m; p != null; p = p.getParent()) {
            Parent parent = p.getPom().getRequested().getParent();
            if (spring(p.getPom().getGroupId()) || parent != null &&
                !("org.codehaus.mojo".equals(p.getPom().getValue(parent.getGroupId())) &&
                  "mojo-parent".equals(p.getPom().getValue(parent.getArtifactId())) &&
                  "87".equals(p.getPom().getValue(parent.getVersion())))) return false;
        }
        for (ManagedDependency d : pom.getRequested().getDependencyManagement()) {
            if (spring(pom.getValue(d.getGroupId())) || d instanceof ManagedDependency.Imported &&
                !("org.junit".equals(pom.getValue(d.getGroupId())) && "junit-bom".equals(pom.getValue(d.getArtifactId())) &&
                  "5.11.4".equals(pom.getValue(d.getVersion())))) return false;
        }
        for (ResolvedManagedDependency d : pom.getDependencyManagement()) {
            if (spring(d.getGroupId()) || d.getBomGav() != null &&
                !("org.junit".equals(d.getBomGav().getGroupId()) && "junit-bom".equals(d.getBomGav().getArtifactId()) &&
                  "5.11.4".equals(d.getBomGav().getVersion()))) return false;
        }
        List<ResolvedDependency> graph = new ArrayList<>();
        for (List<ResolvedDependency> scope : m.getDependencies().values()) for (ResolvedDependency d : scope) flatten(d, graph);
        for (Profile profile : pom.getRequested().getProfiles()) {
            if (!profile.getDependencies().isEmpty() || !profile.getDependencyManagement().isEmpty() ||
                frameworkPlugin(profile.getPlugins(), pom) || frameworkPlugin(profile.getPluginManagement(), pom)) return false;
            for (String key : profile.getProperties().keySet()) {
                for (ResolvedDependency dependency : graph) {
                    Dependency requested = dependency.getRequested();
                    if (references(requested.getGroupId(), key, pom, new HashSet<>()) || references(requested.getArtifactId(), key, pom, new HashSet<>()) ||
                        references(requested.getVersion(), key, pom, new HashSet<>())) return false;
                }
            }
        }
        String coreVersion = null;
        boolean jcl = false;
        for (ResolvedDependency d : graph) {
            if (d.getGroupId() == null || d.getArtifactId() == null || d.getVersion() == null ||
                d.getGroupId().contains("${") || d.getArtifactId().contains("${") || d.getVersion().contains("${")) return false;
            if (!spring(d.getGroupId())) continue;
            if ("spring-jcl".equals(d.getArtifactId())) jcl = true;
            if (!GROUP.equals(d.getGroupId()) || !("spring-core".equals(d.getArtifactId()) || "spring-jcl".equals(d.getArtifactId())) || !old(d.getVersion()) ||
                !"jar".equals(d.getType()) || d.getClassifier() != null ||
                !d.getVersion().equals(pom.getValue(d.getRequested().getVersion()))) return false;
            if ("spring-core".equals(d.getArtifactId()) && d.getDepth() == 0) coreVersion = d.getVersion();
        }
        if (coreVersion == null || !jcl) return false;
        for (ResolvedDependency d : graph) if (spring(d.getGroupId()) && !coreVersion.equals(d.getVersion())) return false;
        boolean localCore = false;
        for (Dependency d : pom.getRequested().getDependencies()) {
            String group = pom.getValue(d.getGroupId());
            if (group == null || group.contains("${")) return false;
            if (spring(group)) {
                String artifact = pom.getValue(d.getArtifactId());
                if (!GROUP.equals(group) || !("spring-core".equals(artifact) || "spring-jcl".equals(artifact)) ||
                    !coreVersion.equals(pom.getValue(d.getVersion()))) return false;
                if ("spring-core".equals(artifact)) localCore = true;
            }
        }
        return localCore;
    }
    private static boolean frameworkPlugin(List<Plugin> plugins, ResolvedPom pom) {
        for (Plugin plugin : plugins) {
            String group = plugin.getGroupId();
            if (group == null) continue; // Maven's default plugin group is org.apache.maven.plugins.
            group = pom.getValue(group);
            if (group == null || group.contains("${") || spring(group)) return true;
        }
        return false;
    }
    private static final Pattern PROPERTY = Pattern.compile("\\$\\{([^}]+)}");
    private static boolean references(@Nullable String value, String key, ResolvedPom pom, Set<String> visited) {
        if (value == null) return false;
        Matcher matcher = PROPERTY.matcher(value);
        while (matcher.find()) {
            String property = matcher.group(1);
            if (key.equals(property)) return true;
            if (visited.add(property)) {
                String raw = pom.getRequested().getProperties().get(property);
                if (raw == null) raw = pom.getProperties().get(property);
                if (references(raw, key, pom, visited)) return true;
            }
        }
        return false;
    }
    private static void flatten(ResolvedDependency dependency, List<ResolvedDependency> graph) {
        graph.add(dependency);
        for (ResolvedDependency child : dependency.getDependencies()) flatten(child, graph);
    }
    private static boolean springType(@Nullable JavaType type) {
        return springType(type, Collections.newSetFromMap(new IdentityHashMap<JavaType, Boolean>()));
    }
    private static boolean springType(@Nullable JavaType type, Set<JavaType> visited) {
        if (type == null || !visited.add(type)) return false;
        if (type instanceof JavaType.Array) return springType(((JavaType.Array) type).getElemType(), visited);
        JavaType.FullyQualified fq = TypeUtils.asFullyQualified(type);
        if (fq != null && spring(fq.getFullyQualifiedName())) return true;
        if (type instanceof JavaType.Parameterized) for (JavaType t : ((JavaType.Parameterized) type).getTypeParameters()) if (springType(t, visited)) return true;
        if (type instanceof JavaType.GenericTypeVariable) for (JavaType t : ((JavaType.GenericTypeVariable) type).getBounds()) if (springType(t, visited)) return true;
        return false;
    }
    private static boolean springBounds(@Nullable List<J.TypeParameter> parameters) {
        if (parameters != null) for (J.TypeParameter parameter : parameters) {
            if (parameter.getBounds() != null) for (TypeTree bound : parameter.getBounds()) if (springType(bound.getType())) return true;
        }
        return false;
    }
    private static void inspect(J.CompilationUnit cu, Module module) {
        for (JavaType type : cu.getTypesInUse().getTypesInUse()) {
            JavaType.FullyQualified fq = TypeUtils.asFullyQualified(type);
            if (fq != null && spring(fq.getFullyQualifiedName())) {
                module.usesSpring = true;
                if (!TYPES.contains(fq.getFullyQualifiedName())) module.safe = false;
            }
        }
        for (J.Import imp : cu.getImports()) {
            String name = imp.getTypeName();
            if (spring(name) && (!TYPES.contains(name) || imp.isStatic())) module.safe = false;
        }
        for (JavaType.Method method : cu.getTypesInUse().getUsedMethods()) {
            String type = method.getDeclaringType().getFullyQualifiedName();
            if (spring(type) && (!TYPES.contains(type) || !MEMBERS.contains(type.substring(type.lastIndexOf('.') + 1) + "#" + method.getName()))) module.safe = false;
        }
        new JavaIsoVisitor<Integer>() {
            @Override public J.Literal visitLiteral(J.Literal literal, Integer ctx) {
                if (literal.getValue() instanceof String && ((String) literal.getValue()).contains("org.springframework")) module.safe = false;
                return super.visitLiteral(literal, ctx);
            }
            @Override public J.FieldAccess visitFieldAccess(J.FieldAccess field, Integer ctx) {
                JavaType.Variable variable = field.getName().getFieldType();
                JavaType.FullyQualified owner = variable == null ? null : TypeUtils.asFullyQualified(variable.getOwner());
                if (owner != null && spring(owner.getFullyQualifiedName()) &&
                    !("org.springframework.core.io.support.ResourcePatternResolver".equals(owner.getFullyQualifiedName()) &&
                      "CLASSPATH_ALL_URL_PREFIX".equals(field.getSimpleName()))) module.safe = false;
                return super.visitFieldAccess(field, ctx);
            }
            @Override public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration c, Integer ctx) {
                if (springBounds(c.getTypeParameters())) module.safe = false;
                if (c.getPrimaryConstructor() != null) for (Statement parameter : c.getPrimaryConstructor()) {
                    if (parameter instanceof J.VariableDeclarations && springType(((J.VariableDeclarations) parameter).getType())) module.safe = false;
                }
                if (c.getExtends() != null && springType(c.getExtends().getType())) module.safe = false;
                if (c.getImplements() != null) for (TypeTree t : c.getImplements()) if (springType(t.getType())) module.safe = false;
                return super.visitClassDeclaration(c, ctx);
            }
            @Override public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, Integer ctx) {
                JavaType.Method mt = method.getMethodType();
                if (mt == null) { module.safe = false; }
                else if (!method.getLeadingAnnotations().isEmpty()) {
                    if (springType(mt.getReturnType()) || springBounds(method.getTypeParameters())) module.safe = false;
                    for (JavaType t : mt.getParameterTypes()) if (springType(t)) module.safe = false;
                }
                if (mt != null && !method.hasModifier(J.Modifier.Type.Private)) {
                    if (springType(mt.getReturnType()) || springBounds(method.getTypeParameters())) module.safe = false;
                    for (JavaType t : mt.getParameterTypes()) if (springType(t)) module.safe = false;
                    for (JavaType t : mt.getThrownExceptions()) if (springType(t)) module.safe = false;
                }
                return super.visitMethodDeclaration(method, ctx);
            }
            @Override public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations v, Integer ctx) {
                if (springType(v.getType()) && getCursor().getParentTreeCursor().getValue() instanceof J.Block &&
                    getCursor().getParentTreeCursor().getParentTreeCursor().getValue() instanceof J.ClassDeclaration) module.safe = false;
                return super.visitVariableDeclarations(v, ctx);
            }
            @Override public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Integer ctx) {
                if (method.getMethodType() == null && method.getSelect() != null && springType(method.getSelect().getType())) module.safe = false;
                return super.visitMethodInvocation(method, ctx);
            }
        }.visit(cu, 0);
    }
}
