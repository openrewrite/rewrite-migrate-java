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
package org.openrewrite.java.migrate.j2ee;

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
public class UpdateJaxbRuntimeToJakartaEE8 extends Recipe {

    private static final String LEGACY_JAVA_JAXB_API_GROUP = "javax.xml.bind";
    private static final String LEGACY_JAVA_JAXB_API_ARTIFACT = "jaxb-api";

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
        return "Update maven build files to use the latest JAXB API from Jakarta EE8 and add a compatible runtime " +
               "dependency to maintain compatibility with Java versions greater than Java 8. This recipe will change " +
               "existing dependencies on `javax.xml.bind:jax-api` to `jakarta.xml.bind:jakarta.xml.bind-api`. The " +
               "recipe will also add a JAXB run-time, in `provided` scope, to any project that has a transitive dependency " +
               "on the JAXB API. **The resulting dependencies still use the `javax` namespace, despite the move " +
               "to the Jakarta artifact.**";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(30);
    }

    @Override
    public Set<String> getTags() {
        return new HashSet<>(Arrays.asList("javax", "jakarta", "j2ee", "jaxb", "glassfish", "java11"));
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            @SuppressWarnings("ConstantConditions")
            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                //remove legacy jaxb-core, regardless of which runtime is being used.
                doAfterVisit(new RemoveDependency("com.sun.xml.bind", "jaxb-core", null));
                Xml.Document d = super.visitDocument(document, ctx);

                d = (Xml.Document) new ChangeDependencyGroupIdAndArtifactId(
                        LEGACY_JAVA_JAXB_API_GROUP, LEGACY_JAVA_JAXB_API_ARTIFACT,
                        JAKARTA_API_GROUP, JAKARTA_API_ARTIFACT, "2.3.2", null
                ).getVisitor().visit(d, ctx);
                d = (Xml.Document) new ChangeManagedDependencyGroupIdAndArtifactId(
                        LEGACY_JAVA_JAXB_API_GROUP, LEGACY_JAVA_JAXB_API_ARTIFACT,
                        JAKARTA_API_GROUP, JAKARTA_API_ARTIFACT, "2.3.2"
                ).getVisitor().visit(d, ctx);

                //Normalize any existing runtimes to the one selected in this recipe.
                if ("sun".equals(runtime)) {
                    d = (Xml.Document) new ChangeDependencyGroupIdAndArtifactId(
                            GLASSFISH_JAXB_RUNTIME_GROUP, GLASSFISH_JAXB_RUNTIME_ARTIFACT,
                            SUN_JAXB_RUNTIME_GROUP, SUN_JAXB_RUNTIME_ARTIFACT, "2.3.2", null
                    ).getVisitor().visit(d, ctx);
                    d = (Xml.Document) new ChangeManagedDependencyGroupIdAndArtifactId(
                            GLASSFISH_JAXB_RUNTIME_GROUP, GLASSFISH_JAXB_RUNTIME_ARTIFACT,
                            SUN_JAXB_RUNTIME_GROUP, SUN_JAXB_RUNTIME_ARTIFACT, "2.3.2"
                    ).getVisitor().visit(d, ctx);
                } else {
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
                    //If any changes to dependencies were made, return now to allow the maven model to be updated.
                    //The logic for adding missing dependencies will happen in the next cycle.
                    return d;
                }
                d = maybeAddRuntimeDependency(d, ctx);
                if (d != document) {
                    doAfterVisit(new RemoveRedundantDependencyVersions("org.glassfish.jaxb", "*", true));
                    doAfterVisit(new RemoveRedundantDependencyVersions("com.sun.xml.bind", "*", true));
                    doAfterVisit(new RemoveRedundantDependencyVersions("jakarta.xml.bind", "*", true));
                }
                return d;
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
                            new AddDependencyVisitor(SUN_JAXB_RUNTIME_GROUP, SUN_JAXB_RUNTIME_ARTIFACT, "2.3.2", null, resolvedScope, null, null, null, null, null) :
                            new AddDependencyVisitor(GLASSFISH_JAXB_RUNTIME_GROUP, GLASSFISH_JAXB_RUNTIME_ARTIFACT, "2.3.2", null, resolvedScope, null, null, null, null, null);
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
