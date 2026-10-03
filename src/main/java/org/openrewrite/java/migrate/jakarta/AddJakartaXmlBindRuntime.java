/*
 * Copyright 2025 the original author or authors.
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
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.tree.J;
import org.openrewrite.maven.AddDependencyVisitor;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.Scope;
import org.openrewrite.xml.tree.Xml;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Value
@EqualsAndHashCode(callSuper = false)
public class AddJakartaXmlBindRuntime extends ScanningRecipe<AddJakartaXmlBindRuntime.Accumulator> {
    String displayName = "Add a Jakarta JAXB runtime for Maven applications";
    String description = "Add a Jakarta JAXB 3 runtime when a Maven module calls JAXBContext.newInstance and has no " +
            "explicit JAXB provider or provided Jakarta platform. Java 8's built-in javax provider cannot serve Jakarta calls. " +
            "Annotation-only modules are left alone and test-only calls receive a test dependency.";

    public static class Accumulator {
        final Set<Path> projects = new HashSet<>();
        final Map<Path, Boolean> calls = new HashMap<>();

        boolean belongsTo(Path source, Path project) {
            Path nearest = null;
            for (Path candidate : projects) {
                if ((candidate.toString().isEmpty() || source.startsWith(candidate)) &&
                    (nearest == null || candidate.toString().length() > nearest.toString().length())) {
                    nearest = candidate;
                }
            }
            return project.equals(nearest);
        }
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator();
    }

    private static Path directory(Path source) {
        return source.getParent() == null ? Paths.get("") : source.getParent();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree preVisit(Tree tree, ExecutionContext ctx) {
                stopAfterPreVisit();
                if (tree instanceof Xml.Document && tree.getMarkers().findFirst(MavenResolutionResult.class).isPresent()) {
                    acc.projects.add(directory(((Xml.Document) tree).getSourcePath()));
                } else if (tree instanceof J.CompilationUnit) {
                    J.CompilationUnit cu = (J.CompilationUnit) tree;
                    boolean imported = cu.getImports().stream().anyMatch(i -> {
                        String name = i.getQualid().printTrimmed();
                        return "javax.xml.bind.JAXBContext".equals(name) || "jakarta.xml.bind.JAXBContext".equals(name) ||
                               "javax.xml.bind.*".equals(name) || "jakarta.xml.bind.*".equals(name);
                    });
                    new JavaIsoVisitor<ExecutionContext>() {
                        @Override
                        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                            if ("newInstance".equals(method.getSimpleName())) {
                                String owner = method.getMethodType() == null ? "" :
                                        method.getMethodType().getDeclaringType().getFullyQualifiedName();
                                String select = method.getSelect() == null ? "" : method.getSelect().printTrimmed();
                                if ("javax.xml.bind.JAXBContext".equals(owner) || "jakarta.xml.bind.JAXBContext".equals(owner) ||
                                    "javax.xml.bind.JAXBContext".equals(select) || "jakarta.xml.bind.JAXBContext".equals(select) ||
                                    imported && "JAXBContext".equals(select)) {
                                    boolean test = cu.getMarkers().findFirst(JavaSourceSet.class)
                                            .map(s -> "test".equals(s.getName()))
                                            .orElse(cu.getSourcePath().toString().replace('\\', '/').contains("src/test/"));
                                    acc.calls.merge(cu.getSourcePath(), test, (a, b) -> a && b);
                                }
                            }
                            return super.visitMethodInvocation(method, ctx);
                        }
                    }.visit(cu, ctx);
                }
                return tree;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return new MavenIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                Path project = directory(document.getSourcePath());
                boolean used = false;
                boolean testOnly = true;
                for (Map.Entry<Path, Boolean> call : acc.calls.entrySet()) {
                    if (acc.belongsTo(call.getKey(), project)) {
                        used = true;
                        testOnly &= call.getValue();
                    }
                }
                if (!used) {
                    return document;
                }
                MavenResolutionResult model = getResolutionResult();
                final boolean tests = testOnly;
                boolean hasProvider = model.getDependencies().entrySet().stream()
                        .filter(e -> tests || e.getKey() != Scope.Test)
                        .flatMap(e -> e.getValue().stream())
                        .anyMatch(d ->
                                "org.glassfish.jaxb".equals(d.getGroupId()) && "jaxb-runtime".equals(d.getArtifactId()) ||
                                "com.sun.xml.bind".equals(d.getGroupId()) && "jaxb-impl".equals(d.getArtifactId()) ||
                                "org.eclipse.persistence".equals(d.getGroupId()) &&
                                        ("org.eclipse.persistence.moxy".equals(d.getArtifactId()) || "eclipselink".equals(d.getArtifactId())) ||
                                "jakarta.platform".equals(d.getGroupId()) &&
                                        ("jakarta.jakartaee-api".equals(d.getArtifactId()) || "jakarta.jakartaee-web-api".equals(d.getArtifactId())) ||
                                "javax".equals(d.getGroupId()) &&
                                        ("javaee-api".equals(d.getArtifactId()) || "javaee-web-api".equals(d.getArtifactId())));
                if (hasProvider || "war".equals(model.getPom().getPackaging()) || "ear".equals(model.getPom().getPackaging())) {
                    return document;
                }
                return (Xml.Document) new AddDependencyVisitor("org.glassfish.jaxb", "jaxb-runtime", "3.0.x", null,
                        testOnly ? "test" : "runtime", null, null, null, null, null).visitNonNull(document, ctx);
            }
        };
    }
}
