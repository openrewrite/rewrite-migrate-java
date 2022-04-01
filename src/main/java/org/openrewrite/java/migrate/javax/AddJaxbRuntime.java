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
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.maven.AddDependencyVisitor;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.maven.RemoveDependency;
import org.openrewrite.maven.tree.*;
import org.openrewrite.xml.tree.Xml;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.openrewrite.java.migrate.MavenUtils.getMavenModel;
import static org.openrewrite.java.migrate.MavenUtils.isMavenSource;

@Value
@EqualsAndHashCode(callSuper = true)
public class AddJaxbRuntime extends Recipe {

    private static final String JAXB_API_GROUP = "jakarta.xml.bind";
    private static final String JAXB_API_ARTIFACT = "jakarta.xml.bind-api";

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
        return "Add JAXB run-time dependency to a Maven project";
    }

    @Override
    public String getDescription() {
        return "This recipe will add a JAXB run-time dependency to any maven project that has a transitive dependency on JAXB.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(5);
    }

    @Override
    protected List<SourceFile> visit(List<SourceFile> before, ExecutionContext ctx) {
        //remove legacy jaxb-core, regardless of which runtime is being used.
        doNext(new RemoveDependency("com.sun.xml.bind", "jaxb-core", null));
        ReplaceRuntimeVisitor replaceRuntime = "sun".equals(runtime) ?
                new ReplaceRuntimeVisitor(GLASSFISH_JAXB_RUNTIME_GROUP, GLASSFISH_JAXB_RUNTIME_ARTIFACT, SUN_JAXB_RUNTIME_GROUP, SUN_JAXB_RUNTIME_ARTIFACT, "2.3.2") :
                new ReplaceRuntimeVisitor(SUN_JAXB_RUNTIME_GROUP, SUN_JAXB_RUNTIME_ARTIFACT, GLASSFISH_JAXB_RUNTIME_GROUP, GLASSFISH_JAXB_RUNTIME_ARTIFACT, "2.3.2");

        Map<GroupArtifactVersion, MavenResolutionResult> gavToModel = new HashMap<>();
        List<SourceFile> sources = ListUtils.map(before, s -> {
            if (isMavenSource(s)) {
                Xml.Document mavenSource = (Xml.Document) replaceRuntime.visitNonNull(s, ctx);
                MavenResolutionResult mavenModel = getMavenModel(mavenSource);
                gavToModel.put(new GroupArtifactVersion(mavenModel.getPom().getGroupId(),mavenModel.getPom().getArtifactId(), mavenModel.getPom().getVersion()), mavenModel);
                return mavenSource;
            }
            return s;
        });

        sources = ListUtils.map(sources, s -> {
            if (isMavenSource(s)) {
                MavenResolutionResult mavenModel = getMavenModel(s);
                Scope apiScope = getTransitiveDependencyScope(mavenModel, JAXB_API_GROUP, JAXB_API_ARTIFACT, gavToModel);
                Scope runtimeScope = "sun".equals(runtime) ?
                        getTransitiveDependencyScope(mavenModel, SUN_JAXB_RUNTIME_GROUP, SUN_JAXB_RUNTIME_ARTIFACT, gavToModel) :
                        getTransitiveDependencyScope(mavenModel, GLASSFISH_JAXB_RUNTIME_GROUP, GLASSFISH_JAXB_RUNTIME_ARTIFACT, gavToModel);
                if (apiScope != null && (runtimeScope == null || !apiScope.isInClasspathOf(runtimeScope))) {
                    String resolvedScope = apiScope == Scope.Test ? "test" : "provided";
                    AddDependencyVisitor addDependency = "sun".equals(runtime) ?
                            new AddDependencyVisitor(SUN_JAXB_RUNTIME_GROUP, SUN_JAXB_RUNTIME_ARTIFACT, "2.3.2", null, resolvedScope, null, null, null, null, null) :
                            new AddDependencyVisitor(GLASSFISH_JAXB_RUNTIME_GROUP, GLASSFISH_JAXB_RUNTIME_ARTIFACT, "2.3.2", null, resolvedScope, null, null, null, null, null);
                    return (SourceFile) addDependency.visit(s, ctx);
                }
            }
            return s;
        });

        return sources;
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
    private Scope getTransitiveDependencyScope(MavenResolutionResult mavenModel, String groupId, String artifactId, Map<GroupArtifactVersion, MavenResolutionResult> gavToModels) {

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

    @Value
    @EqualsAndHashCode(callSuper = true)
    private static class ReplaceRuntimeVisitor extends MavenIsoVisitor<ExecutionContext> {

        String oldGroupId;
        String oldArtifactId;
        String newGroupId;
        String newArtifactId;
        String newVersion;

        @Override
        public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
            if (isDependencyTag(oldGroupId, oldArtifactId)) {
                Optional<Xml.Tag> scopeTag = tag.getChild("scope");
                String scope = scopeTag.isPresent() && scopeTag.get().getValue().isPresent() ? scopeTag.get().getValue().get() : null;
                doAfterVisit(new RemoveDependency(oldGroupId, oldArtifactId, scope));
                doAfterVisit(new AddDependencyVisitor(newGroupId, newArtifactId, newVersion, null, scope, null, null, null, null, null));
            }
            return tag;
        }
    }
}
