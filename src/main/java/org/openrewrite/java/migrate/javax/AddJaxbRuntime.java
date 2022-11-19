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
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.*;
import org.openrewrite.maven.tree.*;
import org.openrewrite.xml.tree.Xml;

import java.time.Duration;
import java.util.*;

@Value
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
    String runtime;

    @Override
    public String getDisplayName() {
        return "Use latest JAXB API and runtime for Jakarta EE 8";
    }

    @Override
    public String getDescription() {
        return "Update maven build files to use the latest JAXB runtime from Jakarta EE 8 to maintain compatibility " +
               "with Java version 11 or greater.  The recipe will add a JAXB run-time, in `provided` scope, to any project " +
               "that has a transitive dependency on the JAXB API. **The resulting dependencies still use the `javax` " +
               "namespace, despite the move to the Jakarta artifact**.";
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
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            @SuppressWarnings("ConstantConditions")
            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                Xml.Document d = super.visitDocument(document, ctx);

                //Normalize any existing runtimes to the one selected in this recipe.
                if ("sun".equals(runtime)) {
                    if (getRecipeList().isEmpty()) {
                        //Upgrade any previous runtimes to the most current 2.3.x version
                        doNext(new UpgradeDependencyVersion(SUN_JAXB_RUNTIME_GROUP, SUN_JAXB_RUNTIME_ARTIFACT, "2.3.x", null, null));
                    }
                    d = (Xml.Document) new ChangeDependencyGroupIdAndArtifactId(
                            GLASSFISH_JAXB_RUNTIME_GROUP, GLASSFISH_JAXB_RUNTIME_ARTIFACT,
                            SUN_JAXB_RUNTIME_GROUP, SUN_JAXB_RUNTIME_ARTIFACT, "2.3.2", null
                    ).getVisitor().visit(d, ctx);
                    d = (Xml.Document) new ChangeManagedDependencyGroupIdAndArtifactId(
                            GLASSFISH_JAXB_RUNTIME_GROUP, GLASSFISH_JAXB_RUNTIME_ARTIFACT,
                            SUN_JAXB_RUNTIME_GROUP, SUN_JAXB_RUNTIME_ARTIFACT, "2.3.2"
                    ).getVisitor().visit(d, ctx);
                } else {
                    if (getRecipeList().isEmpty()) {
                        //Upgrade any previous runtimes to the most current 2.3.x version
                        doNext(new UpgradeDependencyVersion(GLASSFISH_JAXB_RUNTIME_GROUP, GLASSFISH_JAXB_RUNTIME_ARTIFACT, "2.3.x", null, null));
                    }
                    d = (Xml.Document) new ChangeDependencyGroupIdAndArtifactId(
                            SUN_JAXB_RUNTIME_GROUP, SUN_JAXB_RUNTIME_ARTIFACT,
                            GLASSFISH_JAXB_RUNTIME_GROUP, GLASSFISH_JAXB_RUNTIME_ARTIFACT, "2.3.2", null
                    ).getVisitor().visit(d, ctx);
                    d = (Xml.Document) new ChangeManagedDependencyGroupIdAndArtifactId(
                            SUN_JAXB_RUNTIME_GROUP, SUN_JAXB_RUNTIME_ARTIFACT,
                            GLASSFISH_JAXB_RUNTIME_GROUP, GLASSFISH_JAXB_RUNTIME_ARTIFACT, "2.3.2"
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
                    AddDependencyVisitor addDependency = "sun".equals(runtime) ?
                            new AddDependencyVisitor(SUN_JAXB_RUNTIME_GROUP, SUN_JAXB_RUNTIME_ARTIFACT, "2.3.x", null, resolvedScope, null, null, null, null, null) :
                            new AddDependencyVisitor(GLASSFISH_JAXB_RUNTIME_GROUP, GLASSFISH_JAXB_RUNTIME_ARTIFACT, "2.3.x", null, resolvedScope, null, null, null, null, null);
                    return (Xml.Document) addDependency.visit(d, ctx);
                }

                return d;
            }

        };
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
}
