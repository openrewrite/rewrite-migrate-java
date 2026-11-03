/*
 * Copyright 2024 the original author or authors.
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
import org.openrewrite.gradle.IsBuildGradle;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.search.FindGradleProject;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.marker.Markers;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.maven.AddDependencyVisitor;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.Scope;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;
import org.openrewrite.xml.tree.Xml;

import java.time.Duration;
import java.util.*;

import static java.util.Objects.requireNonNull;

@Value
@EqualsAndHashCode(callSuper = false)
public class AddJaxwsRuntime extends Recipe {

    private static final String JAKARTA_JAXWS_API_GROUP = "jakarta.xml.ws";
    private static final String JAKARTA_JAXWS_API_ARTIFACT = "jakarta.xml.ws-api";
    private static final VersionComparator JAKARTA_NAMESPACE_API = requireNonNull(Semver.validate("[3,)", null).getValue());

    private static final String SUN_JAXWS_RUNTIME_GROUP = "com.sun.xml.ws";
    private static final String SUN_JAXWS_RUNTIME_ARTIFACT = "jaxws-rt";

    String displayName = "Use the latest JAX-WS API and runtime for Jakarta EE 8";

    String description = "Update build files to use the latest JAX-WS runtime from Jakarta EE 8 to maintain compatibility with " +
                "Java version 11 or greater. The recipe will add a JAX-WS run-time, in Gradle " +
                "`compileOnly`+`testImplementation` and Maven `provided` scope, to any project that has a transitive " +
                "dependency on the JAX-WS API. **The resulting dependencies still use the `javax` namespace, despite " +
                "the move to the Jakarta artifact**.";

    Duration estimatedEffortPerOccurrence = Duration.ofMinutes( 30 );

    Set<String> tags = new HashSet<>( Arrays.asList( "javax", "jakarta", "javaee", "jaxws", "java11" ) );

    @Override
    public List<Recipe> getRecipeList() {
        return Arrays.asList(new AddJaxwsRuntimeGradle(), new AddJaxwsRuntimeMaven());
    }

    @EqualsAndHashCode(callSuper = false)
    @Value
    public static class AddJaxwsRuntimeGradle extends Recipe {
        String displayName = "Use the latest JAX-WS API and runtime for Jakarta EE 8";

        String description = "Update Gradle build files to use the latest JAX-WS runtime from Jakarta EE 8 to maintain compatibility " +
                    "with Java version 11 or greater. The recipe will add a JAX-WS run-time, in " +
                    "`compileOnly`+`testImplementation` configurations, to any project that has a transitive dependency " +
                    "on the JAX-WS API. **The resulting dependencies still use the `javax` namespace, despite the move " +
                    "to the Jakarta artifact**.";

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor() {
            return Preconditions.check(Preconditions.and(
                    new FindGradleProject(FindGradleProject.SearchCriteria.Marker).getVisitor(),
                    new IsBuildGradle<>(true),
                    Preconditions.not(new FindJakartaNamespaceApi())), new GroovyIsoVisitor<ExecutionContext>() {
                @Override
                public G.CompilationUnit visitCompilationUnit(G.CompilationUnit cu, ExecutionContext ctx) {
                    G.CompilationUnit g = cu;

                    GradleProject gp = g.getMarkers().findFirst(GradleProject.class)
                            .orElseThrow(() -> new RuntimeException("Gradle build scripts must have a GradleProject marker"));

                    GradleDependencyConfiguration compileClasspath = gp.getConfiguration("compileClasspath");
                    GradleDependencyConfiguration runtimeClasspath = gp.getConfiguration("runtimeClasspath");
                    GradleDependencyConfiguration testCompileClasspath = gp.getConfiguration("testCompileClasspath");
                    GradleDependencyConfiguration testRuntimeClasspath = gp.getConfiguration("testRuntimeClasspath");

                    // Match the recipe's provided/compileOnly contract for main code, and actual test runtime coverage.
                    boolean mainApi = hasApi(compileClasspath) || hasApi(runtimeClasspath);
                    boolean testApi = hasApi(testCompileClasspath) || hasApi(testRuntimeClasspath);
                    boolean mainProvider = hasProvider(compileClasspath) || hasProvider(runtimeClasspath);
                    if (mainApi && !mainProvider && gp.getConfiguration("compileOnly") != null) {
                        g = addJaxWsRuntimeDependency("compileOnly", g, ctx);
                    }
                    if (testApi && !hasProvider(testRuntimeClasspath) && gp.getConfiguration("testImplementation") != null) {
                        g = addJaxWsRuntimeDependency("testImplementation", g, ctx);
                    }
                    return addCustomProviders(g, gp, ctx);
                }

                private boolean hasApi(@Nullable GradleDependencyConfiguration configuration) {
                    return configuration != null &&
                           configuration.findResolvedDependency(JAKARTA_JAXWS_API_GROUP, JAKARTA_JAXWS_API_ARTIFACT) != null;
                }

                private boolean hasProvider(@Nullable GradleDependencyConfiguration configuration) {
                    if (configuration == null) {
                        return false;
                    }
                    return isJavaxProvider(configuration.findResolvedDependency(SUN_JAXWS_RUNTIME_GROUP, SUN_JAXWS_RUNTIME_ARTIFACT)) ||
                           isJavaxProvider(configuration.findResolvedDependency("org.apache.cxf", "cxf-rt-frontend-jaxws"));
                }

                private G.CompilationUnit addCustomProviders(G.CompilationUnit g, GradleProject gp, ExecutionContext ctx) {
                    for (GradleDependencyConfiguration originalApi : getApiConfigurations(gp, JAKARTA_JAXWS_API_GROUP, JAKARTA_JAXWS_API_ARTIFACT)) {
                        if (!originalApi.isCanBeDeclared() || isStandardDeclaration(originalApi.getName())) {
                            continue;
                        }
                        GradleProject current = g.getMarkers().findFirst(GradleProject.class).orElse(gp);
                        GradleDependencyConfiguration api = current.getConfiguration(originalApi.getName());
                        if (api == null) {
                            continue;
                        }
                        List<GradleDependencyConfiguration> consumers = current.configurationsExtendingFrom(api, true);
                        List<GradleDependencyConfiguration> customClasspaths = new ArrayList<>();
                        for (GradleDependencyConfiguration consumer : consumers) {
                            if (consumer.isCanBeResolved() && !isStandardClasspath(consumer.getName())) {
                                customClasspaths.add(consumer);
                            }
                        }
                        // Legacy configurations can be both declarable and resolvable. Only use the bucket itself
                        // when it has no consumers; a bucket inherited by standard classpaths is already handled above.
                        if (consumers.isEmpty() && api.isCanBeResolved()) {
                            customClasspaths.add(api);
                        }
                        Set<String> runtimeClasspaths = new HashSet<>();
                        for (GradleDependencyConfiguration classpath : customClasspaths) {
                            if (classpath.getName().endsWith("RuntimeClasspath")) {
                                runtimeClasspaths.add(classpath.getName());
                            }
                        }
                        customClasspaths.removeIf(c -> c.getName().endsWith("CompileClasspath") &&
                                runtimeClasspaths.contains(c.getName().substring(0,
                                        c.getName().length() - "CompileClasspath".length()) + "RuntimeClasspath"));
                        for (GradleDependencyConfiguration target : customClasspaths) {
                            current = g.getMarkers().findFirst(GradleProject.class).orElse(gp);
                            GradleDependencyConfiguration currentTarget = current.getConfiguration(target.getName());
                            if (!hasApi(currentTarget) || hasProvider(currentTarget)) {
                                continue;
                            }
                            String configuration = null;
                            if (target.getName().endsWith("RuntimeClasspath")) {
                                String sourceSet = target.getName().substring(0,
                                        target.getName().length() - "RuntimeClasspath".length());
                                for (String suffix : Arrays.asList("Implementation", "RuntimeOnly")) {
                                    String name = sourceSet + suffix;
                                    GradleDependencyConfiguration candidate = current.getConfiguration(name);
                                    if (candidate != null && candidate.isCanBeDeclared() &&
                                            currentTarget.allExtendsFrom().stream().anyMatch(parent -> parent.getName().equals(name)) &&
                                            !hasCoveredConsumer(current, candidate)) {
                                        configuration = name;
                                        break;
                                    }
                                }
                            }
                            if (configuration == null) {
                                configuration = api.getName();
                            }
                            g = addJaxWsRuntimeDependency(configuration, g, ctx);
                        }
                    }
                    return g;
                }

                private boolean isStandardDeclaration(String name) {
                    return Arrays.asList("implementation", "compileOnly", "runtimeOnly", "testImplementation", "testCompileOnly", "testRuntimeOnly").contains(name);
                }

                private boolean isStandardClasspath(String name) {
                    return Arrays.asList("compileClasspath", "runtimeClasspath", "testCompileClasspath", "testRuntimeClasspath").contains(name);
                }

                private Set<GradleDependencyConfiguration> getApiConfigurations(GradleProject gp, String groupId, String artifactId) {
                    Set<GradleDependencyConfiguration> configurations = new HashSet<>();
                    for (GradleDependencyConfiguration gdc : gp.getConfigurations()) {
                        if (gdc.findRequestedDependency(groupId, artifactId) != null || gdc.findResolvedDependency(groupId, artifactId) != null) {
                            configurations.add(gdc);
                        }
                    }

                    Set<GradleDependencyConfiguration> tmpConfigurations = new HashSet<>(configurations);
                    for (GradleDependencyConfiguration tmpConfiguration : tmpConfigurations) {
                        GradleDependencyConfiguration gdc = gp.getConfiguration(tmpConfiguration.getName());
                        for (GradleDependencyConfiguration transitive : gp.configurationsExtendingFrom(gdc, true)) {
                            configurations.remove(transitive);
                        }
                    }

                    tmpConfigurations = new HashSet<>(configurations);
                    for (GradleDependencyConfiguration configuration : tmpConfigurations) {
                        GradleDependencyConfiguration gdc = gp.getConfiguration(configuration.getName());
                        for (GradleDependencyConfiguration extendsFrom : gdc.allExtendsFrom()) {
                            if (configurations.contains(extendsFrom)) {
                                configurations.remove(configuration);
                            }
                        }
                    }

                    return configurations;
                }

                private boolean hasCoveredConsumer(GradleProject project, GradleDependencyConfiguration configuration) {
                    List<GradleDependencyConfiguration> consumers = project.configurationsExtendingFrom(configuration, true);
                    if (configuration.isCanBeResolved()) {
                        consumers.add(configuration);
                    }
                    Set<String> runtimeClasspaths = new HashSet<>();
                    for (GradleDependencyConfiguration consumer : consumers) {
                        if (consumer.isCanBeResolved() && consumer.getName().endsWith("RuntimeClasspath")) {
                            runtimeClasspaths.add(consumer.getName());
                        }
                    }
                    for (GradleDependencyConfiguration consumer : consumers) {
                        // A compile-only provider does not cover the paired runtime receiving this dependency.
                        if (consumer.isCanBeResolved() &&
                                !(consumer.getName().endsWith("CompileClasspath") && runtimeClasspaths.contains(
                                        consumer.getName().substring(0, consumer.getName().length() - "CompileClasspath".length()) + "RuntimeClasspath")) &&
                                hasProvider(consumer)) {
                            return true;
                        }
                    }
                    return false;
                }

                private G.CompilationUnit addJaxWsRuntimeDependency(String apiConfiguration, G.CompilationUnit g, ExecutionContext ctx) {
                    GradleProject project = g.getMarkers().findFirst(GradleProject.class).orElseThrow(IllegalStateException::new);
                    GradleDependencyConfiguration configuration = project.getConfiguration(apiConfiguration);
                    // Every insertion target, including a shared implementation or fallback, must be safe.
                    // Preserve the graph when no existing bucket isolates the uncovered consumer.
                    if (configuration == null || !configuration.isCanBeDeclared() || hasCoveredConsumer(project, configuration)) {
                        return g;
                    }
                    return (G.CompilationUnit) new org.openrewrite.gradle.AddDependencyVisitor(SUN_JAXWS_RUNTIME_GROUP, SUN_JAXWS_RUNTIME_ARTIFACT, "2.3.x", null, apiConfiguration, null, null, null, this::isTopLevel, null)
                            .visitNonNull(g, ctx);
                }

                private boolean isTopLevel(Cursor cursor) {
                    if (cursor.getValue() instanceof J.Block) {
                        return cursor.getParentOrThrow().getValue() instanceof JavaSourceFile;
                    }
                    return cursor.getParentOrThrow().firstEnclosing(J.MethodInvocation.class) == null;
                }
            });
        }
    }

    @EqualsAndHashCode(callSuper = false)
    @Value
    public static class AddJaxwsRuntimeMaven extends Recipe {
        String displayName = "Use the latest JAX-WS API and runtime for Jakarta EE 8";

        String description = "Update maven build files to use the latest JAX-WS runtime from Jakarta EE 8 to maintain compatibility " +
                    "with Java version 11 or greater. The recipe will add a JAX-WS run-time, in `provided` scope, to any project " +
                    "that has a transitive dependency on the JAX-WS API. **The resulting dependencies still use the `javax` " +
                    "namespace, despite the move to the Jakarta artifact**.";

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor() {
            return Preconditions.check(Preconditions.not(new FindJakartaNamespaceApi()), new MavenIsoVisitor<ExecutionContext>() {
                @Override
                @SuppressWarnings({"ReassignedVariable", "ConstantConditions"})
                public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                    Xml.Document d = super.visitDocument(document, ctx);
                    MavenResolutionResult mavenModel = getResolutionResult();

                    //Find the highest scope of a transitive dependency on the JAX-WS API (if it exists at all)
                    Scope apiScope = getTransitiveDependencyScope(mavenModel, JAKARTA_JAXWS_API_GROUP, JAKARTA_JAXWS_API_ARTIFACT);
                    if (apiScope != null) {
                        if (!hasProvider(mavenModel, apiScope)) {
                            String resolvedScope = apiScope == Scope.Test ? "test" : "provided";
                            d = (Xml.Document) new AddDependencyVisitor(SUN_JAXWS_RUNTIME_GROUP, SUN_JAXWS_RUNTIME_ARTIFACT,
                                    "2.3.x", null, resolvedScope, null, null, null, null, null).visit(d, ctx);
                        }
                    }

                    return d;
                }
            });
        }

        private boolean hasProvider(MavenResolutionResult model, Scope apiScope) {
            for (Map.Entry<Scope, List<ResolvedDependency>> entry : model.getDependencies().entrySet()) {
                // A main API needs a main provider; a test-only API can use any main/test provider.
                // Main providers in provided scope satisfy the same contract as the Metro dependency we would add.
                if (apiScope != Scope.Test && entry.getKey() == Scope.Test) {
                    continue;
                }
                for (ResolvedDependency dependency : entry.getValue()) {
                    if (isJavaxProvider(dependency)) {
                        return true;
                    }
                }
            }
            return false;
        }

        /**
         * Finds the highest scope for a given group/artifact.
         *
         * @param mavenModel The maven model to search for a dependency.
         * @param groupId    The group ID of the dependency
         * @param artifactId The artifact ID of the dependency
         * @return The highest scope of the given dependency or null if the dependency does not exist.
         */
        private @Nullable Scope getTransitiveDependencyScope(MavenResolutionResult mavenModel, String groupId, String artifactId) {
            Scope maxScope = null;
            for (Map.Entry<Scope, List<ResolvedDependency>> entry : mavenModel.getDependencies().entrySet()) {
                for (ResolvedDependency dependency : entry.getValue()) {
                    if (groupId.equals(dependency.getGroupId()) && artifactId.equals(dependency.getArtifactId())) {
                        maxScope = Scope.maxPrecedence(maxScope, entry.getKey());
                        if (Scope.Compile == maxScope) {
                            return maxScope;
                        }
                        break;
                    }
                }
            }
            return maxScope;
        }
    }

    private static boolean isJavaxProvider(@Nullable ResolvedDependency dependency) {
        if (dependency == null || dependency.getClassifier() != null && !dependency.getClassifier().isEmpty() ||
            dependency.getType() != null && !"jar".equals(dependency.getType()) && !"bundle".equals(dependency.getType())) {
            return false;
        }
        String major = Semver.majorVersion(dependency.getVersion());
        return SUN_JAXWS_RUNTIME_GROUP.equals(dependency.getGroupId()) &&
               SUN_JAXWS_RUNTIME_ARTIFACT.equals(dependency.getArtifactId()) && "2".equals(major) ||
               "org.apache.cxf".equals(dependency.getGroupId()) && "cxf-rt-frontend-jaxws".equals(dependency.getArtifactId()) &&
               ("2".equals(major) || "3".equals(major));
    }

    /**
     * Version 3.0 of the JAX-WS API moved from the `javax.xml.ws` to the `jakarta.xml.ws` package, which the 2.3.x
     * runtime added by this recipe does not implement; it would pull in `javax` API jars that shadow the `jakarta` API.
     */
    private static class FindJakartaNamespaceApi extends TreeVisitor<Tree, ExecutionContext> {
        @Override
        public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
            if (!(tree instanceof SourceFile)) {
                return tree;
            }
            Markers markers = ((SourceFile) tree).getMarkers();
            boolean found = markers.findFirst(MavenResolutionResult.class).map(FindJakartaNamespaceApi::usesJakartaNamespaceApi).orElse(false) ||
                            markers.findFirst(GradleProject.class).map(FindJakartaNamespaceApi::usesJakartaNamespaceApi).orElse(false);
            return found ? SearchResult.found(tree) : tree;
        }

        private static boolean usesJakartaNamespaceApi(MavenResolutionResult mavenModel) {
            for (List<ResolvedDependency> dependencies : mavenModel.getDependencies().values()) {
                for (ResolvedDependency dependency : dependencies) {
                    if (JAKARTA_JAXWS_API_GROUP.equals(dependency.getGroupId()) &&
                        JAKARTA_JAXWS_API_ARTIFACT.equals(dependency.getArtifactId()) &&
                        JAKARTA_NAMESPACE_API.isValid(null, dependency.getVersion())) {
                        return true;
                    }
                }
            }
            return false;
        }

        private static boolean usesJakartaNamespaceApi(GradleProject gp) {
            for (GradleDependencyConfiguration gdc : gp.getConfigurations()) {
                ResolvedDependency resolved = gdc.findResolvedDependency(JAKARTA_JAXWS_API_GROUP, JAKARTA_JAXWS_API_ARTIFACT);
                if (resolved != null && JAKARTA_NAMESPACE_API.isValid(null, resolved.getVersion())) {
                    return true;
                }
            }
            return false;
        }
    }
}
