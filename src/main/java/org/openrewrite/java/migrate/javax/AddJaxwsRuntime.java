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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.*;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.Scope;
import org.openrewrite.xml.tree.Xml;

import java.time.Duration;
import java.util.*;

@Value
@EqualsAndHashCode(callSuper = true)
public class AddJaxwsRuntime extends Recipe {

    private static final String LEGACY_JAVA_JAXWS_API_GROUP = "javax.xml.ws";
    private static final String LEGACY_JAVA_JAXWS_API_ARTIFACT = "jaxws-api";

    private static final String JAKARTA_JAXWS_API_GROUP = "jakarta.xml.ws";
    private static final String JAKARTA_JAXWS_API_ARTIFACT = "jakarta.xml.ws-api";

    private static final String SUN_JAXWS_RUNTIME_GROUP = "com.sun.xml.ws";
    private static final String SUN_JAXWS_RUNTIME_ARTIFACT = "jaxws-rt";

    @Override
    public String getDisplayName() {
        return "Use the latest JAX-WS API and runtime for Jakarta EE 8";
    }

    @Override
    public String getDescription() {
        return "Update maven build files to use the latest JAX-WS runtime from Jakarta EE 8 to maintain compatibility " +
               "with Java version 11 or greater.  The recipe will add a JAX-WS run-time, in `provided` scope, to any project " +
               "that has a transitive dependency on the JAX-WS API. **The resulting dependencies still use the `javax` " +
               "namespace, despite the move to the Jakarta artifact**.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(30);
    }

    @Override
    public Set<String> getTags() {
        return new HashSet<>(Arrays.asList("javax", "jakarta", "javaee", "jaxws", "java11"));
    }


    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            @SuppressWarnings({"ReassignedVariable", "ConstantConditions"})
            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                Xml.Document d = super.visitDocument(document, ctx);
                MavenResolutionResult mavenModel = getResolutionResult();

                //Find the highest scope of a transitive dependency on the JAX-WS API (if it exists at all)
                Scope apiScope = getTransitiveDependencyScope(mavenModel, JAKARTA_JAXWS_API_GROUP, JAKARTA_JAXWS_API_ARTIFACT);
                //Find the highest scope of a transitive dependency on the JAX-WS runtime (if it exists at all)
                Scope runtimeScope = getTransitiveDependencyScope(mavenModel, SUN_JAXWS_RUNTIME_GROUP, SUN_JAXWS_RUNTIME_ARTIFACT);

                if (apiScope != null && (runtimeScope == null || !apiScope.isInClasspathOf(runtimeScope))) {
                    String resolvedScope = apiScope == Scope.Test ? "test" : "provided";
                    d = (Xml.Document) new AddDependencyVisitor(SUN_JAXWS_RUNTIME_GROUP, SUN_JAXWS_RUNTIME_ARTIFACT,
                            "2.3.x", null, resolvedScope, null, null, null, null, null).visit(d, ctx);
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
