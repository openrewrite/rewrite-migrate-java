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
import org.openrewrite.java.migrate.MavenUtils;
import org.openrewrite.maven.AddDependencyVisitor;
import org.openrewrite.maven.RemoveDependency;
import org.openrewrite.maven.tree.GroupArtifactVersion;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.Scope;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.openrewrite.java.migrate.MavenUtils.getMavenModel;
import static org.openrewrite.java.migrate.MavenUtils.isMavenSource;

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
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(5);
    }

    @Override
    protected List<SourceFile> visit(List<SourceFile> before, ExecutionContext ctx) {
        //remove legacy jaxws-ri library (in favor of the jakarta runtime)
        doNext(new RemoveDependency(SUN_JAXWS_RUNTIME_GROUP, "jaxws-ri", null));

        //Collect a map of gav coordinates to pom models for any maven files in the source set (other visitors may have
        //made changes to those models)
        Map<GroupArtifactVersion, MavenResolutionResult> gavToModel = before.stream()
                .filter(MavenUtils::isMavenSource)
                .map(MavenUtils::getMavenModel)
                .collect(Collectors.toMap(
                        mavenModel -> new GroupArtifactVersion(
                                mavenModel.getPom().getGroupId(),
                                mavenModel.getPom().getArtifactId(),
                                mavenModel.getPom().getVersion()),
                        Function.identity()));
        return ListUtils.map(before, s -> {
            if (isMavenSource(s)) {
                MavenResolutionResult mavenModel = getMavenModel(s);

                //Find the highest scope of a transitive dependency on the JAX-WS API (if it exists at all)
                Scope apiScope = getTransitiveDependencyScope(mavenModel, JAKARTA_JAXWS_API_GROUP, JAKARTA_JAXWS_API_ARTIFACT, gavToModel);
                //Find the highest scope of a transitive dependency on the JAX-WS runtime (if it exists at all)
                Scope runtimeScope = getTransitiveDependencyScope(mavenModel, SUN_JAXWS_RUNTIME_GROUP, SUN_JAXWS_RUNTIME_ARTIFACT, gavToModel);

                if (apiScope != null && (runtimeScope == null || !apiScope.isInClasspathOf(runtimeScope))) {
                    //If the API is present and there is not a matching runtime in the transitive scope of the api, add the runtime.
                    String resolvedScope = apiScope == Scope.Test ? "test" : "provided";
                    return (SourceFile) new AddDependencyVisitor(
                            SUN_JAXWS_RUNTIME_GROUP, SUN_JAXWS_RUNTIME_ARTIFACT, "2.3.2",
                            null, resolvedScope, null, null, null,
                            null, null).visit(s, ctx);
                }
            }
            return s;
        });
    }

    /**
     * Finds the highest scope for a given group/artifact.
     *
     * @param mavenModel The maven model to search for a dependency.
     * @param groupId The group ID of the dependency
     * @param artifactId The artifact ID of the dependency
     * @param gavToModels A map of gav coordinates to "poms" that exist in the source set, these may have been manipulated by other visitors
     * @return The highest scope of the given dependency or null if the dependency does not exist.
     */
    @Nullable
    private static Scope getTransitiveDependencyScope(MavenResolutionResult mavenModel, String groupId, String artifactId, Map<GroupArtifactVersion, MavenResolutionResult> gavToModels) {
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
        return null;
    }

}
