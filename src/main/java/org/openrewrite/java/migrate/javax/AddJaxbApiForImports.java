/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.java.migrate.javax;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.java.tree.J;
import org.openrewrite.maven.AddDependencyVisitor;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.Scope;
import org.openrewrite.xml.tree.Xml;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Value
@EqualsAndHashCode(callSuper = false)
public class AddJaxbApiForImports extends ScanningRecipe<AddJaxbApiForImports.Accumulator> {
    String displayName = "Add JAXB API dependencies for explicit imports";
    String description = "Add the JAXB API to the nearest Maven module importing javax.xml.bind types, " +
            "including imports whose types could not be resolved by the parser.";

    static class Accumulator {
        final Set<Path> poms = new HashSet<>();
        final Set<Path> sources = new HashSet<>();
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree preVisit(Tree tree, ExecutionContext ctx) {
                stopAfterPreVisit();
                if (tree instanceof Xml.Document && tree.getMarkers().findFirst(MavenResolutionResult.class).isPresent()) {
                    acc.poms.add(((Xml.Document) tree).getSourcePath());
                } else if (tree instanceof J.CompilationUnit) {
                    J.CompilationUnit cu = (J.CompilationUnit) tree;
                    for (J.Import anImport : cu.getImports()) {
                        if (anImport.getTypeName().startsWith("javax.xml.bind.")) {
                            acc.sources.add(cu.getSourcePath());
                            break;
                        }
                    }
                }
                return tree;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        Map<Path, String> scopes = new HashMap<>();
        for (Path source : acc.sources) {
            Path nearest = null;
            for (Path pom : acc.poms) {
                Path root = pom.getParent();
                if ((root == null || source.startsWith(root)) &&
                    (nearest == null || pom.getNameCount() > nearest.getNameCount())) {
                    nearest = pom;
                }
            }
            if (nearest != null) {
                Path relative = nearest.getParent() == null ? source : nearest.getParent().relativize(source);
                String scope = relative.startsWith("src/test") ? "test" : "compile";
                scopes.merge(nearest, scope, (a, b) -> "compile".equals(a) || "compile".equals(b) ? "compile" : "test");
            }
        }
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                String scope = scopes.get(document.getSourcePath());
                if (scope == null || !getResolutionResult().findDependencies("jakarta.xml.bind", "jakarta.xml.bind-api",
                        "test".equals(scope) ? Scope.Test : Scope.Compile).isEmpty()) {
                    return document;
                }
                return (Xml.Document) new AddDependencyVisitor("jakarta.xml.bind", "jakarta.xml.bind-api",
                        "2.3.x", null, scope, null, null, null, null, null).visitNonNull(document, ctx);
            }
        };
    }
}
