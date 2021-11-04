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
import org.openrewrite.maven.MavenVisitor;
import org.openrewrite.maven.RemoveDependency;
import org.openrewrite.maven.tree.Maven;
import org.openrewrite.maven.tree.Pom;
import org.openrewrite.maven.tree.Scope;
import org.openrewrite.xml.tree.Xml;

import java.util.List;
import java.util.Optional;
import java.util.Set;

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
    protected List<SourceFile> visit(List<SourceFile> before, ExecutionContext ctx) {
        // remove legacy jaxb-core, regardless of which runtime is being used.
        doNext(new RemoveDependency("com.sun.xml.bind", "jaxb-core", null));
        ReplaceRuntimeVisitor replaceRuntime = "sun".equals(runtime) ?
                new ReplaceRuntimeVisitor(GLASSFISH_JAXB_RUNTIME_GROUP, GLASSFISH_JAXB_RUNTIME_ARTIFACT, SUN_JAXB_RUNTIME_GROUP, SUN_JAXB_RUNTIME_ARTIFACT, "2.3.2") :
                new ReplaceRuntimeVisitor(SUN_JAXB_RUNTIME_GROUP, SUN_JAXB_RUNTIME_ARTIFACT, GLASSFISH_JAXB_RUNTIME_GROUP, GLASSFISH_JAXB_RUNTIME_ARTIFACT, "2.3.2");

        List<SourceFile> sources = ListUtils.map(before, s -> {
            if (s instanceof Maven) {
                return (Maven) replaceRuntime.visit(s, ctx);
            }
            return s;
        });

        sources = ListUtils.map(sources, s -> {
            if (s instanceof Maven) {
                Maven mavenSource = (Maven) s;
                Scope apiScope = getDependencyScope(mavenSource.getModel(), JAXB_API_GROUP, JAXB_API_ARTIFACT);
                Scope runtimeScope = "sun".equals(runtime) ?
                        getDependencyScope(mavenSource.getModel(), SUN_JAXB_RUNTIME_GROUP, SUN_JAXB_RUNTIME_ARTIFACT) :
                        getDependencyScope(mavenSource.getModel(), GLASSFISH_JAXB_RUNTIME_GROUP, GLASSFISH_JAXB_RUNTIME_ARTIFACT);
                if (apiScope != null && (runtimeScope == null || !apiScope.isInClasspathOf(runtimeScope))) {
                    String resolvedScope = apiScope == Scope.Test ? "test" : "provided";
                    AddDependencyVisitor addDependency = "sun".equals(runtime) ?
                            new AddDependencyVisitor(SUN_JAXB_RUNTIME_GROUP, SUN_JAXB_RUNTIME_ARTIFACT, "2.3.2", null, resolvedScope, null, null, null, null, null) :
                            new AddDependencyVisitor(GLASSFISH_JAXB_RUNTIME_GROUP, GLASSFISH_JAXB_RUNTIME_ARTIFACT, "2.3.2", null, resolvedScope, null, null, null, null, null);
                    return (SourceFile) addDependency.visit(mavenSource, ctx);
                }
                return mavenSource;
            }
            return s;
        });
        // TODO: auto-format after changes
        return sources;
    }

    /**
     * Finds the highest scope for a given group/artifact.
     *
     * @param pom The pom to search for a dependency.
     * @param groupId The group ID of the dependency
     * @param artifactId The artifact ID of the dependency
     * @return The highest scope of the given dependency or null if the dependency does not exist.
     */
    private @Nullable Scope getDependencyScope(Pom pom, String groupId, String artifactId) {
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

        return null;
    }

    @Value
    @EqualsAndHashCode(callSuper = true)
    private static class ReplaceRuntimeVisitor extends MavenVisitor {

        String oldGroupId;
        String oldArtifactId;
        String newGroupId;
        String newArtifactId;
        String newVersion;

        @Override
        public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
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
