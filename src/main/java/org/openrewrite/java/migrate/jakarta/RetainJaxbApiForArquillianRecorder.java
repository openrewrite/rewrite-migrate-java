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
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.maven.AddDependencyVisitor;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.Scope;
import org.openrewrite.xml.tree.Xml;

import java.util.Collections;

@Value
@EqualsAndHashCode(callSuper = false)
public class RetainJaxbApiForArquillianRecorder extends Recipe {
    String displayName = "Retain the JAXB 2 API for Arquillian Recorder 1.x";
    String description = "Retain the legacy JAXB API in Maven modules using Arquillian Recorder 1.x binaries, " +
                         "which still reference javax.xml.bind classes after the application's Jakarta migration.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                for (Scope scope : new Scope[]{Scope.Compile, Scope.Runtime, Scope.Provided, Scope.Test}) {
                    for (ResolvedDependency dependency : getResolutionResult().getDependencies()
                            .getOrDefault(scope, Collections.emptyList())) {
                        if ("org.arquillian.extension".equals(dependency.getGroupId()) &&
                            dependency.getArtifactId().startsWith("arquillian-recorder-") &&
                            dependency.getVersion().startsWith("1.")) {
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
