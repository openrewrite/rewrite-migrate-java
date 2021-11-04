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
import org.openrewrite.SourceFile;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.AddDependencyVisitor;
import org.openrewrite.maven.RemoveDependency;
import org.openrewrite.maven.tree.Maven;
import org.openrewrite.maven.tree.Pom;
import org.openrewrite.maven.tree.Scope;

import java.util.List;
import java.util.Set;

@Value
@EqualsAndHashCode(callSuper = true)
public class AddJaxwsRuntime extends Recipe {

    private static final String JAKARTA_JAXWS_API_GROUP = "jakarta.xml.ws";
    private static final String JAKARTA_JAXWS_API_ARTIFACT = "jakarta.xml.ws-api";

    private static final String SUN_JAXWS_RUNTIME_GROUP = "com.sun.xml.ws";
    private static final String SUN_JAXWS_RUNTIME_ARTIFACT = "jaxws-rt";

    @Override
    public String getDisplayName() {
        return "Add JAX-WS run-time dependency to a Maven project";
    }

    @Override
    public String getDescription() {
        return "This recipe will add a JAX-WS run-time dependency to any maven project that has a transitive dependency on JAX-WS APIs.";
    }

    @Override
    protected List<SourceFile> visit(List<SourceFile> before, ExecutionContext ctx) {
        // remove legacy jaxws-ri library (in favor of the jakarta runtime)
        doNext(new RemoveDependency("com.sun.xml.ws", "jaxws-ri", null));

        return ListUtils.map(before, s -> {
            if (s instanceof Maven) {
                Maven mavenSource = (Maven) s;
                // Find the highest scope of a transitive dependency on the JAX-WS API (if it exists at all)
                Scope apiScope = getDependencyScope(mavenSource.getModel(), JAKARTA_JAXWS_API_GROUP, JAKARTA_JAXWS_API_ARTIFACT);
                // Find the highest scope of a transitive dependency on the JAX-WS runtime (if it exists at all)
                Scope runtimeScope = getDependencyScope(mavenSource.getModel(), SUN_JAXWS_RUNTIME_GROUP, SUN_JAXWS_RUNTIME_ARTIFACT);

                if (!Scope.None.equals(apiScope) && (Scope.None.equals(runtimeScope) || !apiScope.isInClasspathOf(runtimeScope))) {
                    // If the API is present and there is not a matching runtime in the transitive scope of the api, add the runtime.
                    String resolvedScope = apiScope == Scope.Test ? "test" : "provided";
                    // TODO: auto-format after changes
                    return (SourceFile) new AddDependencyVisitor(
                            SUN_JAXWS_RUNTIME_GROUP, SUN_JAXWS_RUNTIME_ARTIFACT, "2.3.2",
                            null, resolvedScope, null, null, null,
                            null, null).visit(mavenSource, ctx);
                }
            }
            return s;
        });
    }

    /**
     * Finds the highest scope for a given group/artifact.
     *
     * @param pom The pom to search for a dependency.
     * @param groupId The group ID of the dependency
     * @param artifactId The artifact ID of the dependency
     * @return The highest scope of the given dependency or null if the dependency does not exist.
     */
    private Scope getDependencyScope(Pom pom, String groupId, String artifactId) {
        Scope scope = Scope.Compile;
        Set<Pom.Dependency> dependencies = pom.getDependencies(scope);
        for (Pom.Dependency dependency : dependencies) {
            if (groupId.equals(dependency.getGroupId()) && artifactId.equals(dependency.getArtifactId())) {
                return scope;
            }
        }

        scope = Scope.Provided;
        dependencies = pom.getDependencies(scope);
        for (Pom.Dependency dependency : dependencies) {
            if (groupId.equals(dependency.getGroupId()) && artifactId.equals(dependency.getArtifactId())) {
                return scope;
            }
        }

        scope = Scope.Runtime;
        dependencies = pom.getDependencies(scope);
        for (Pom.Dependency dependency : dependencies) {
            if (groupId.equals(dependency.getGroupId()) && artifactId.equals(dependency.getArtifactId())) {
                return scope;
            }
        }

        scope = Scope.Test;
        dependencies = pom.getDependencies(scope);
        for (Pom.Dependency dependency : dependencies) {
            if (groupId.equals(dependency.getGroupId()) && artifactId.equals(dependency.getArtifactId())) {
                return scope;
            }
        }

        return Scope.None;
    }
}
