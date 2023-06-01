/*
 * Copyright 2021 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
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
import org.openrewrite.*;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.search.FindGradleProject;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.Scope;
import org.openrewrite.xml.tree.Xml;

import java.time.Duration;
import java.util.*;

@EqualsAndHashCode(callSuper = true)
public class AddJaxbRuntime extends Recipe {

    private static final String JAKARTA_API_GROUP = "jakarta.xml.bind";
    private static final String JAKARTA_API_ARTIFACT = "jakarta.xml.bind-api";

    private static final String SUN_JAXB_RUNTIME_GROUP = "com.sun.xml.bind";
    private static final String SUN_JAXB_RUNTIME_ARTIFACT = "jaxb-impl";

    private static final String GLASSFISH_JAXB_RUNTIME_GROUP = "org.glassfish.jaxb";
    private static final String GLASSFISH_JAXB_RUNTIME_ARTIFACT = "jaxb-runtime";

    @Option(displayName = "JAXB run-time",
            description = "Which implementation of the JAXB run-time that will be added to maven projects that have transitive dependencies on the JAXB API",
            valid = {"glassfish", "sun"},
            example = "glassfish")
    private final String runtime;

    private final AddJaxbRuntimeGradle addJaxbRuntimeGradle;
    private final AddJaxbRuntimeMaven addJaxbRuntimeMaven;

    public AddJaxbRuntime(String runtime) {
        this.runtime = runtime;
        this.addJaxbRuntimeGradle = new AddJaxbRuntimeGradle(runtime);
        this.addJaxbRuntimeMaven = new AddJaxbRuntimeMaven(runtime);
    }

    @Override
    public String getDisplayName() {
        return "Use latest JAXB API and runtime for Jakarta EE 8";
    }

    @Override
    public String getDescription() {
        return "Update build files to use the latest JAXB runtime from Jakarta EE 8 to maintain compatibility with " +
                "Java version 11 or greater. The recipe will add a JAXB run-time, in Gradle " +
                "`compileOnly`+`testImplementation` and Maven `provided` scope, to any project that has a transitive " +
                "dependency on the JAXB API. **The resulting dependencies still use the `javax` namespace, despite " +
                "the move to the Jakarta artifact**.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(30);
    }

    @Override
    public Set<String> getTags() {
        return new HashSet<>(Arrays.asList("javax", "jakarta", "javaee", "jaxb", "glassfish", "java11"));
    }

    @Override
    public boolean causesAnotherCycle() {
        return true;
    }

    @Override
    public List<Recipe> getRecipeList() {
        return Arrays.asList(addJaxbRuntimeGradle, addJaxbRuntimeMaven);
    }

    @Value
    @EqualsAndHashCode(callSuper = true)
    private static class AddJaxbRuntimeGradle extends Recipe {
        String runtime;

        @Override
        public String getDisplayName() {
            return "Use latest JAXB API and runtime for Jakarta EE 8";
        }

        @Override
        public String getDescription() {
            return "Update Gradle build files to use the latest JAXB runtime from Jakarta EE 8 to maintain compatibility " +
                    "with Java version 11 or greater.  The recipe will add a JAXB run-time, in " +
                    "`compileOnly`+`testImplementation` configurations, to any project that has a transitive dependency " +
                    "on the JAXB API. **The resulting dependencies still use the `javax` namespace, despite the move to " +
                    "the Jakarta artifact**.";
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor() {
            return Preconditions.check(new FindGradleProject(FindGradleProject.SearchCriteria.Marker).getVisitor(), new GroovyIsoVisitor<ExecutionContext>() {
                @Override
                public G.CompilationUnit visitCompilationUnit(G.CompilationUnit cu, ExecutionContext ctx) {
                    G.CompilationUnit g = cu;
                    if ("sun".equals(runtime)) {
                        if (getAfterVisit().isEmpty()) {
                            // Upgrade any previous runtimes to the most current 2.3.x version
                            doAfterVisit(new org.openrewrite.gradle.UpgradeDependencyVersion(SUN_JAXB_RUNTIME_GROUP, SUN_JAXB_RUNTIME_ARTIFACT, "2.3.x", null).getVisitor());
                        }
                        g = (G.CompilationUnit) new org.openrewrite.gradle.ChangeDependency(GLASSFISH_JAXB_RUNTIME_GROUP, GLASSFISH_JAXB_RUNTIME_ARTIFACT,
                                SUN_JAXB_RUNTIME_GROUP, SUN_JAXB_RUNTIME_ARTIFACT, "2.3.x", null
                        ).getVisitor().visit(g, ctx);
                    } else {
                        if (getAfterVisit().isEmpty()) {
                            // Upgrade any previous runtimes to the most current 2.3.x version
                            doAfterVisit(new org.openrewrite.gradle.UpgradeDependencyVersion(GLASSFISH_JAXB_RUNTIME_GROUP, GLASSFISH_JAXB_RUNTIME_ARTIFACT, "2.3.x", null).getVisitor());
                        }
                        g = (G.CompilationUnit) new org.openrewrite.gradle.ChangeDependency(SUN_JAXB_RUNTIME_GROUP, SUN_JAXB_RUNTIME_ARTIFACT,
                                GLASSFISH_JAXB_RUNTIME_GROUP, GLASSFISH_JAXB_RUNTIME_ARTIFACT, "2.3.x", null
                        ).getVisitor().visit(g, ctx);
                    }
                    maybeAddRuntimeDependency(g);
                    return g;
                }

                private void maybeAddRuntimeDependency(G.CompilationUnit g) {
                    Optional<GradleProject> maybeGp = g.getMarkers().findFirst(GradleProject.class);
                    if (!maybeGp.isPresent()) {
                        return;
                    }

                    GradleProject gp = maybeGp.get();
                    Set<String> apiConfigurations = getTransitiveDependencyConfiguration(gp, JAKARTA_API_GROUP, JAKARTA_API_ARTIFACT);
                    if (apiConfigurations.isEmpty()) {
                        return;
                    }
                    Set<String> runtimeConfigurations = "sun".equals(runtime) ?
                            getTransitiveDependencyConfiguration(gp, SUN_JAXB_RUNTIME_GROUP, SUN_JAXB_RUNTIME_ARTIFACT) :
                            getTransitiveDependencyConfiguration(gp, GLASSFISH_JAXB_RUNTIME_GROUP, GLASSFISH_JAXB_RUNTIME_ARTIFACT);

                    if (runtimeConfigurations.isEmpty()) {
                        if (gp.getConfiguration("compileOnly") != null) {
                            doAfterVisit(addDependency("compileOnly"));
                        }
                        if (gp.getConfiguration("testImplementation") != null) {
                            doAfterVisit(addDependency("testImplementation"));
                        }
                    } else {
                        for (String apiConfiguration : apiConfigurations) {
                            GradleDependencyConfiguration apiGdc = gp.getConfiguration(apiConfiguration);
                            List<GradleDependencyConfiguration> apiTransitives = gp.configurationsExtendingFrom(apiGdc, true);
                            for (String runtimeConfiguration : runtimeConfigurations) {
                                GradleDependencyConfiguration runtimeGdc = gp.getConfiguration(runtimeConfiguration);
                                List<GradleDependencyConfiguration> runtimeTransitives = gp.configurationsExtendingFrom(runtimeGdc, true);
                                if (apiTransitives.stream().noneMatch(runtimeTransitives::contains)) {
                                    doAfterVisit(addDependency(apiConfiguration));
                                }
                            }
                        }
                    }
                }

                private Set<String> getTransitiveDependencyConfiguration(GradleProject gp, String groupId, String artifactId) {
                    Set<String> configurations = new HashSet<>();
                    for (GradleDependencyConfiguration gdc : gp.getConfigurations()) {
                        if (gdc.findRequestedDependency(groupId, artifactId) != null || gdc.findResolvedDependency(groupId, artifactId) != null) {
                            configurations.add(gdc.getName());
                        }
                    }

                    Set<String> tmpConfigurations = new HashSet<>(configurations);
                    for (String tmpConfiguration : tmpConfigurations) {
                        GradleDependencyConfiguration gdc = gp.getConfiguration(tmpConfiguration);
                        for (GradleDependencyConfiguration transitive : gp.configurationsExtendingFrom(gdc, true)) {
                            configurations.remove(transitive.getName());
                        }
                    }

                    tmpConfigurations = new HashSet<>(configurations);
                    for (String configuration : tmpConfigurations) {
                        GradleDependencyConfiguration gdc = gp.getConfiguration(configuration);
                        for (GradleDependencyConfiguration extendsFrom : gdc.allExtendsFrom()) {
                            if (configurations.contains(extendsFrom.getName())) {
                                configurations.remove(configuration);
                            }
                        }
                    }

                    return configurations;
                }

                private org.openrewrite.gradle.AddDependencyVisitor addDependency(String configuration) {
                    return "sun".equals(runtime) ?
                            new org.openrewrite.gradle.AddDependencyVisitor(SUN_JAXB_RUNTIME_GROUP, SUN_JAXB_RUNTIME_ARTIFACT, "2.3.x", null, configuration, null, null, null) :
                            new org.openrewrite.gradle.AddDependencyVisitor(GLASSFISH_JAXB_RUNTIME_GROUP, GLASSFISH_JAXB_RUNTIME_ARTIFACT, "2.3.x", null, configuration, null, null, null);
                }
            });
        }
    }

    @Value
    @EqualsAndHashCode(callSuper = true)
    private static class AddJaxbRuntimeMaven extends Recipe {
        String runtime;

        @Override
        public String getDisplayName() {
            return "Use latest JAXB API and runtime for Jakarta EE 8";
        }

        @Override
        public String getDescription() {
            return "Update Maven build files to use the latest JAXB runtime from Jakarta EE 8 to maintain compatibility " +
                    "with Java version 11 or greater.  The recipe will add a JAXB run-time, in `provided` scope, to any project " +
                    "that has a transitive dependency on the JAXB API. **The resulting dependencies still use the `javax` " +
                    "namespace, despite the move to the Jakarta artifact**.";
        }

        @Override
        public TreeVisitor<?, ExecutionContext> getVisitor() {
            return new MavenIsoVisitor<ExecutionContext>() {
                @SuppressWarnings("ConstantConditions")
                @Override
                public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                    Xml.Document d = super.visitDocument(document, ctx);

                    //Normalize any existing runtimes to the one selected in this recipe.
                    if ("sun".equals(runtime)) {
                        if (getAfterVisit().isEmpty()) {
                            //Upgrade any previous runtimes to the most current 2.3.x version
                            doAfterVisit(new org.openrewrite.maven.UpgradeDependencyVersion(SUN_JAXB_RUNTIME_GROUP, SUN_JAXB_RUNTIME_ARTIFACT, "2.3.x", null, null, null).getVisitor());
                        }
                        d = (Xml.Document) new org.openrewrite.maven.ChangeDependencyGroupIdAndArtifactId(
                                GLASSFISH_JAXB_RUNTIME_GROUP, GLASSFISH_JAXB_RUNTIME_ARTIFACT,
                                SUN_JAXB_RUNTIME_GROUP, SUN_JAXB_RUNTIME_ARTIFACT, "2.3.x", null
                        ).getVisitor().visit(d, ctx);
                        d = (Xml.Document) new org.openrewrite.maven.ChangeManagedDependencyGroupIdAndArtifactId(
                                GLASSFISH_JAXB_RUNTIME_GROUP, GLASSFISH_JAXB_RUNTIME_ARTIFACT,
                                SUN_JAXB_RUNTIME_GROUP, SUN_JAXB_RUNTIME_ARTIFACT, "2.3.x"
                        ).getVisitor().visit(d, ctx);
                    } else {
                        if (getAfterVisit().isEmpty()) {
                            //Upgrade any previous runtimes to the most current 2.3.x version
                            doAfterVisit(new org.openrewrite.maven.UpgradeDependencyVersion(GLASSFISH_JAXB_RUNTIME_GROUP, GLASSFISH_JAXB_RUNTIME_ARTIFACT, "2.3.x", null, null, null).getVisitor());
                        }
                        d = (Xml.Document) new org.openrewrite.maven.ChangeDependencyGroupIdAndArtifactId(
                                SUN_JAXB_RUNTIME_GROUP, SUN_JAXB_RUNTIME_ARTIFACT,
                                GLASSFISH_JAXB_RUNTIME_GROUP, GLASSFISH_JAXB_RUNTIME_ARTIFACT, "2.3.x", null
                        ).getVisitor().visit(d, ctx);
                        d = (Xml.Document) new org.openrewrite.maven.ChangeManagedDependencyGroupIdAndArtifactId(
                                SUN_JAXB_RUNTIME_GROUP, SUN_JAXB_RUNTIME_ARTIFACT,
                                GLASSFISH_JAXB_RUNTIME_GROUP, GLASSFISH_JAXB_RUNTIME_ARTIFACT, "2.3.x"
                        ).getVisitor().visit(d, ctx);
                    }
                    if (d != document) {
                        //If changes have been made, update the maven model, next cycle, runtime will be added.
                        return d;
                    }
                    return maybeAddRuntimeDependency(d, ctx);
                }

                @SuppressWarnings("ConstantConditions")
                private Xml.Document maybeAddRuntimeDependency(Xml.Document d, ExecutionContext ctx) {

                    MavenResolutionResult mavenModel = getResolutionResult();

                    Scope apiScope = getTransitiveDependencyScope(mavenModel, JAKARTA_API_GROUP, JAKARTA_API_ARTIFACT);
                    Scope runtimeScope = "sun".equals(runtime) ?
                            getTransitiveDependencyScope(mavenModel, SUN_JAXB_RUNTIME_GROUP, SUN_JAXB_RUNTIME_ARTIFACT) :
                            getTransitiveDependencyScope(mavenModel, GLASSFISH_JAXB_RUNTIME_GROUP, GLASSFISH_JAXB_RUNTIME_ARTIFACT);

                    if (apiScope != null && (runtimeScope == null || !apiScope.isInClasspathOf(runtimeScope))) {
                        String resolvedScope = apiScope == Scope.Test ? "test" : "provided";
                        org.openrewrite.maven.AddDependencyVisitor addDependency = "sun".equals(runtime) ?
                                new org.openrewrite.maven.AddDependencyVisitor(SUN_JAXB_RUNTIME_GROUP, SUN_JAXB_RUNTIME_ARTIFACT, "2.3.x", null, resolvedScope, null, null, null, null, null) :
                                new org.openrewrite.maven.AddDependencyVisitor(GLASSFISH_JAXB_RUNTIME_GROUP, GLASSFISH_JAXB_RUNTIME_ARTIFACT, "2.3.x", null, resolvedScope, null, null, null, null, null);
                        return (Xml.Document) addDependency.visit(d, ctx);
                    }

                    return d;
                }

                /**
                 * Finds the highest scope for a given group/artifact.
                 *
                 * @param mavenModel The maven model to search for a dependency.
                 * @param groupId The group ID of the dependency
                 * @param artifactId The artifact ID of the dependency
                 * @return The highest scope of the given dependency or null if the dependency does not exist.
                 */
                @Nullable
                private Scope getTransitiveDependencyScope(MavenResolutionResult mavenModel, String groupId, String artifactId) {
                    Scope maxScope = null;
                    for (Map.Entry<Scope, List<ResolvedDependency>> entry : mavenModel.getDependencies().entrySet()) {
                        for (ResolvedDependency dependency : entry.getValue()) {
                            if (groupId.equals(dependency.getGroupId()) && artifactId.equals(dependency.getArtifactId())) {
                                maxScope = Scope.maxPrecedence(maxScope, entry.getKey());
                                if (Scope.Compile.equals(maxScope)) {
                                    return maxScope;
                                }
                                break;
                            }
                        }
                    }
                    return maxScope;
                }
            };
        }
    }
}
