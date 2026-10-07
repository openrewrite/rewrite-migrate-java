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
package org.openrewrite.java.migrate.jakarta;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.AddDependencyVisitor;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.Scope;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;
import org.openrewrite.xml.tree.Xml;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Value
@EqualsAndHashCode(callSuper = false)
public class RetainJaxbApiForArquillianRecorder extends ScanningRecipe<Set<String>> {
    private static final VersionComparator JAKARTA_API = Objects.requireNonNull(Semver.validate("[3,)", null).getValue());
    String displayName = "Retain the JAXB 2 API for Arquillian Recorder 1.x";
    String description = "Retain the legacy JAXB API in Maven modules using Arquillian Recorder 1.x binaries, " +
                         "which still reference javax.xml.bind classes after the application's Jakarta migration.";

    @Override
    public Set<String> getInitialValue(ExecutionContext ctx) {
        return new HashSet<>();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Set<String> projects) {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                projects.add(projectKey(getResolutionResult()));
                return document;
            }
        };
    }

    static String projectKey(MavenResolutionResult model) {
        return model.getPom().getGroupId() + ":" + model.getPom().getArtifactId() + ":" + model.getPom().getVersion();
    }

    static boolean hasJakartaApi(MavenResolutionResult model) {
        return model.getDependencies().values().stream().flatMap(Collection::stream)
                .anyMatch(d -> "jakarta.xml.bind".equals(d.getGroupId()) &&
                        "jakarta.xml.bind-api".equals(d.getArtifactId()) && JAKARTA_API.isValid(null, d.getVersion()));
    }

    static boolean externalRecorder(ResolvedDependency dependency, Set<String> projects) {
        return "org.arquillian.extension".equals(dependency.getGroupId()) &&
               dependency.getArtifactId().startsWith("arquillian-recorder-") &&
               dependency.getVersion().startsWith("1.") &&
               !projects.contains(dependency.getGroupId() + ":" + dependency.getArtifactId() + ":" + dependency.getVersion());
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Set<String> projects) {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                if (!hasJakartaApi(getResolutionResult())) {
                    return document;
                }
                for (Scope scope : new Scope[]{Scope.Compile, Scope.Runtime, Scope.Provided, Scope.Test}) {
                    for (ResolvedDependency dependency : getResolutionResult().getDependencies()
                            .getOrDefault(scope, Collections.emptyList())) {
                        if (externalRecorder(dependency, projects)) {
                            return (Xml.Document) new AddDependencyVisitor("javax.xml.bind", "jaxb-api", "2.3.x", null,
                                    scope == Scope.Test ? "test" : "runtime", null, null, null, null, null)
                                    .visitNonNull(document, ctx);
                        }
                    }
                }
                return document;
            }
        };
    }
}
