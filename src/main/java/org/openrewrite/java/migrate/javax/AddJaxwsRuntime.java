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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.search.FindGradleProject;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.maven.AddDependencyVisitor;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.Scope;
import org.openrewrite.xml.tree.Xml;

import java.time.Duration;
import java.util.*;

@Value
@EqualsAndHashCode(callSuper = false)
public class AddJaxwsRuntime extends Recipe {

    private static final String JAKARTA_JAXWS_API_GROUP = "jakarta.xml.ws";
    private static final String JAKARTA_JAXWS_API_ARTIFACT = "jakarta.xml.ws-api";

    private static final String SUN_JAXWS_RUNTIME_GROUP = "com.sun.xml.ws";
    private static final String SUN_JAXWS_RUNTIME_ARTIFACT = "jaxws-rt";

    String displayName = "Use the latest JAX-WS API and runtime for Jakarta EE 8";

    String description = "Update build files to use the latest JAX-WS runtime from Jakarta EE 8 to maintain compatibility with " +
                "Java version 11 or greater. The recipe will add a JAX-WS run-time, in Gradle " +
                "`compileOnly`+`testImplementation` and Maven `provided` scope, to any project that has a transitive " +
                "dependency on the JAX-WS API. **The resulting dependencies still use the `javax` namespace, despite " +
                "the move to the Jakarta artifact**.";

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(30);
    }

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
                    "with Java version 11 or greater.  The recipe will add a JAX-WS run-time, in " +
                    "`compileOnly`+`testImplementation` configurations, to any project that has a transitive dependency " +
                    "on the JAX-WS API. **The resulting dependencies still use the `javax` namespace, despite the move " +
                    "to the Jakarta artifact**.";

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor() {
            return Preconditions.check(new FindGradleProject(FindGradleProject.SearchCriteria.Marker).getVisitor(), new GroovyIsoVisitor<ExecutionContext>() {
                @Override
                public G.CompilationUnit visitCompilationUnit(G.CompilationUnit cu, ExecutionContext ctx) {
                    G.CompilationUnit g = cu;

                    GradleProject gp = g.getMarkers().findFirst(GradleProject.class)
                            .orElseThrow(() -> new RuntimeException("Gradle build scripts must have a GradleProject marker"));

                    Set<GradleDependencyConfiguration> apiConfigurations = getTransitiveDependencyConfiguration(gp, JAKARTA_JAXWS_API_GROUP, JAKARTA_JAXWS_API_ARTIFACT);

                    if (!apiConfigurations.isEmpty()) {
                        Set<GradleDependencyConfiguration> runtimeConfigurations = getTransitiveDependencyConfiguration(gp, SUN_JAXWS_RUNTIME_GROUP, SUN_JAXWS_RUNTIME_ARTIFACT);
                        if (runtimeConfigurations.isEmpty()) {
                            if (gp.getConfiguration("compileOnly") != null) {
                                g = addJaxWsRuntimeDependency("compileOnly", g, ctx);
                            }
                            if (gp.getConfiguration("testImplementation") != null) {
                                g = addJaxWsRuntimeDependency("testImplementation", g, ctx);
                            }
                        } else {
                            for (GradleDependencyConfiguration apiConfiguration : apiConfigurations) {
                                List<GradleDependencyConfiguration> apiTransitives = gp.configurationsExtendingFrom(apiConfiguration, true);
                                for (GradleDependencyConfiguration runtimeConfiguration : runtimeConfigurations) {
                                    List<GradleDependencyConfiguration> runtimeTransitives = gp.configurationsExtendingFrom(runtimeConfiguration, true);
                                    if (apiTransitives.stream().noneMatch(runtimeTransitives::contains) && apiConfiguration.isCanBeDeclared()) {
                                        g = addJaxWsRuntimeDependency(apiConfiguration.getName(), g, ctx);
                                    }
                                }
                            }
                        }
                    }

                    return g;
                }

                private Set<GradleDependencyConfiguration> getTransitiveDependencyConfiguration(GradleProject gp, String groupId, String artifactId) {
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

                private G.CompilationUnit addJaxWsRuntimeDependency(String apiConfiguration, G.CompilationUnit g, ExecutionContext ctx) {
                    return (G.CompilationUnit) new org.openrewrite.gradle.AddDependencyVisitor(SUN_JAXWS_RUNTIME_GROUP, SUN_JAXWS_RUNTIME_ARTIFACT, "2.3.x", null, apiConfiguration, null, null, null, null, null)
                            .visitNonNull(g, ctx);
                }
            });
        }
    }

    @EqualsAndHashCode(callSuper = false)
    @Value
    public static class AddJaxwsRuntimeMaven extends Recipe {
        String displayName = "Use the latest JAX-WS API and runtime for Jakarta EE 8";

        String description = "Update maven build files to use the latest JAX-WS runtime from Jakarta EE 8 to maintain compatibility " +
                    "with Java version 11 or greater.  The recipe will add a JAX-WS run-time, in `provided` scope, to any project " +
                    "that has a transitive dependency on the JAX-WS API. **The resulting dependencies still use the `javax` " +
                    "namespace, despite the move to the Jakarta artifact**.";

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor() {
            return new MavenIsoVisitor<ExecutionContext>() {
                @Override
                @SuppressWarnings({"ReassignedVariable", "ConstantConditions"})
                public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                    Xml.Document d = super.visitDocument(document, ctx);
                    MavenResolutionResult mavenModel = getResolutionResult();

                    //Find the highest scope of a transitive dependency on the JAX-WS API (if it exists at all)
                    Scope apiScope = getTransitiveDependencyScope(mavenModel, JAKARTA_JAXWS_API_GROUP, JAKARTA_JAXWS_API_ARTIFACT);
                    if (apiScope != null) {
                        //Find the highest scope of a transitive dependency on the JAX-WS runtime (if it exists at all)
                        Scope runtimeScope = getTransitiveDependencyScope(mavenModel, SUN_JAXWS_RUNTIME_GROUP, SUN_JAXWS_RUNTIME_ARTIFACT);

                        if (runtimeScope == null || !apiScope.isInClasspathOf(runtimeScope)) {
                            String resolvedScope = apiScope == Scope.Test ? "test" : "provided";
                            d = (Xml.Document) new AddDependencyVisitor(SUN_JAXWS_RUNTIME_GROUP, SUN_JAXWS_RUNTIME_ARTIFACT,
                                    "2.3.x", null, resolvedScope, null, null, null, null, null).visit(d, ctx);
                        }
                    }

                    return d;
                }
            };
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
}
