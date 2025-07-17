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
package org.openrewrite.java.migrate.javax;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.search.FindGradleProject;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.Scope;
import org.openrewrite.xml.tree.Xml;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

@EqualsAndHashCode(callSuper = false)
@Value
public class AddJaxbRuntime extends ScanningRecipe<AtomicBoolean> {
    private static final String JACKSON_GROUP = "com.fasterxml.jackson.module";
    private static final String JACKSON_JAXB_ARTIFACT = "jackson-module-jaxb-annotations";

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
        return "Update build files to use the latest JAXB runtime from Jakarta EE 8 to maintain compatibility with " +
               "Java version 11 or greater. The recipe will add a JAXB run-time, in Gradle " +
               "`compileOnly`+`testImplementation` and Maven `provided` scope, to any project that has a transitive " +
               "dependency on the JAXB API. **The resulting dependencies still use the `javax` namespace, despite " +
               "the move to the Jakarta artifact**.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(30);
    }

    @Override
    public Set<String> getTags() {
        return new HashSet<>(Arrays.asList("javax", "jakarta", "javaee", "jaxb", "glassfish", "java11"));
    }

    @Override
    public AtomicBoolean getInitialValue(ExecutionContext ctx) {
        return new AtomicBoolean(false);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(AtomicBoolean acc) {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (acc.get()) {
                    return (J) tree;
                }
                Tree t = new UsesType<ExecutionContext>("javax.xml.bind..*", true).visit(tree, ctx);
                if (t != tree) {
                    acc.set(true);
                }
                return (J) tree;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(AtomicBoolean acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                Tree t = gradleVisitor.visit(tree, ctx);
                return mavenVisitor.visit(t, ctx);
            }

            final TreeVisitor<?, ExecutionContext> gradleVisitor = Preconditions.check(new FindGradleProject(FindGradleProject.SearchCriteria.Marker).getVisitor(), new GroovyIsoVisitor<ExecutionContext>() {
                @Override
                public G.CompilationUnit visitCompilationUnit(G.CompilationUnit cu, ExecutionContext ctx) {
                    G.CompilationUnit g = cu;
                    if ("sun".equals(runtime)) {
                        if (getAfterVisit().isEmpty()) {
                            // Upgrade any previous runtimes to the most current 2.3.x version
                            doAfterVisit(new org.openrewrite.gradle.UpgradeDependencyVersion(SUN_JAXB_RUNTIME_GROUP, SUN_JAXB_RUNTIME_ARTIFACT, "2.3.x", null).getVisitor());
                        }
                        g = (G.CompilationUnit) new org.openrewrite.gradle.ChangeDependency(GLASSFISH_JAXB_RUNTIME_GROUP, GLASSFISH_JAXB_RUNTIME_ARTIFACT,
                                SUN_JAXB_RUNTIME_GROUP, SUN_JAXB_RUNTIME_ARTIFACT, "2.3.x", null, null
                        ).getVisitor().visitNonNull(g, ctx);
                    } else {
                        if (getAfterVisit().isEmpty()) {
                            // Upgrade any previous runtimes to the most current 2.3.x version
                            doAfterVisit(new org.openrewrite.gradle.UpgradeDependencyVersion(GLASSFISH_JAXB_RUNTIME_GROUP, GLASSFISH_JAXB_RUNTIME_ARTIFACT, "2.3.x", null).getVisitor());
                        }
                        g = (G.CompilationUnit) new org.openrewrite.gradle.ChangeDependency(SUN_JAXB_RUNTIME_GROUP, SUN_JAXB_RUNTIME_ARTIFACT,
                                GLASSFISH_JAXB_RUNTIME_GROUP, GLASSFISH_JAXB_RUNTIME_ARTIFACT, "2.3.x", null, null
                        ).getVisitor().visitNonNull(g, ctx);
                    }
                    if(!acc.get()) {
                        return g;
                    }

                    Optional<GradleProject> maybeGp = g.getMarkers().findFirst(GradleProject.class);
                    if (!maybeGp.isPresent()) {
                        return g;
                    }

                    GradleProject gp = maybeGp.get();
                    GradleDependencyConfiguration rc = gp.getConfiguration("runtimeClasspath");
                    if (rc == null || rc.findResolvedDependency(JAKARTA_API_GROUP, JAKARTA_API_ARTIFACT) == null ||
                        rc.findResolvedDependency(JACKSON_GROUP, JACKSON_JAXB_ARTIFACT) != null) {
                        return g;
                    }

                    String groupId = GLASSFISH_JAXB_RUNTIME_GROUP;
                    String artifactId = GLASSFISH_JAXB_RUNTIME_ARTIFACT;
                    String version = "2.3.x";
                    if ("sun".equals(runtime)) {
                        groupId = SUN_JAXB_RUNTIME_GROUP;
                        artifactId = SUN_JAXB_RUNTIME_ARTIFACT;
                    }
                    if (rc.findResolvedDependency(groupId, artifactId) == null) {
                        g = (G.CompilationUnit) new org.openrewrite.gradle.AddDependencyVisitor(groupId, artifactId, version, null, "runtimeOnly", null, null, null, null, null)
                                .visitNonNull(g, ctx);
                    }
                    return g;
                }
            });

            final TreeVisitor<?, ExecutionContext> mavenVisitor = new MavenIsoVisitor<ExecutionContext>() {
                @Override
                @SuppressWarnings("ConstantConditions")
                public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                    Xml.Document d = super.visitDocument(document, ctx);

                    //Normalize any existing runtimes to the one selected in this recipe.
                    if ("sun".equals(runtime)) {
                        d = jaxbDependencySwap(ctx, d, SUN_JAXB_RUNTIME_GROUP, SUN_JAXB_RUNTIME_ARTIFACT, GLASSFISH_JAXB_RUNTIME_GROUP, GLASSFISH_JAXB_RUNTIME_ARTIFACT);
                    } else {
                        //Upgrade any previous runtimes to the most current 2.3.x version
                        d = jaxbDependencySwap(ctx, d, GLASSFISH_JAXB_RUNTIME_GROUP, GLASSFISH_JAXB_RUNTIME_ARTIFACT, SUN_JAXB_RUNTIME_GROUP, SUN_JAXB_RUNTIME_ARTIFACT);
                    }
                    return maybeAddRuntimeDependency(d, ctx);
                }

                @SuppressWarnings("ConstantConditions")
                private Xml.Document maybeAddRuntimeDependency(Xml.Document d, ExecutionContext ctx) {
                    if(!acc.get()) {
                        return d;
                    }
                    MavenResolutionResult mavenModel = getResolutionResult();
                    if (!mavenModel.findDependencies(JACKSON_GROUP, JACKSON_JAXB_ARTIFACT, Scope.Runtime).isEmpty() ||
                        mavenModel.findDependencies(JAKARTA_API_GROUP, JAKARTA_API_ARTIFACT, Scope.Runtime).isEmpty()) {
                        return d;
                    }

                    String groupId = GLASSFISH_JAXB_RUNTIME_GROUP;
                    String artifactId = GLASSFISH_JAXB_RUNTIME_ARTIFACT;
                    String version = "2.3.x";
                    if ("sun".equals(runtime)) {
                        groupId = SUN_JAXB_RUNTIME_GROUP;
                        artifactId = SUN_JAXB_RUNTIME_ARTIFACT;
                    }
                    if (getResolutionResult().findDependencies(groupId, artifactId, Scope.Runtime).isEmpty()) {
                        d = (Xml.Document) new org.openrewrite.maven.AddDependencyVisitor(groupId, artifactId, version, null, Scope.Runtime.name().toLowerCase(), null, null, null, null, null)
                                .visitNonNull(d, ctx);
                    } else {
                        d = (Xml.Document) new org.openrewrite.maven.UpgradeDependencyVersion(groupId, artifactId, version, null, false, null).getVisitor()
                                .visitNonNull(d, ctx);
                    }
                    return d;
                }
            };
        };
    }

    private Xml.Document jaxbDependencySwap(ExecutionContext ctx, Xml.Document d, String sunJaxbRuntimeGroup, String sunJaxbRuntimeArtifact, String glassfishJaxbRuntimeGroup, String glassfishJaxbRuntimeArtifact) {
        d = (Xml.Document) new org.openrewrite.maven.UpgradeDependencyVersion(sunJaxbRuntimeGroup, sunJaxbRuntimeArtifact, "2.3.x", null, null, null)
                .getVisitor()
                .visitNonNull(d, ctx);
        d = (Xml.Document) new org.openrewrite.maven.ChangeDependencyGroupIdAndArtifactId(
                glassfishJaxbRuntimeGroup, glassfishJaxbRuntimeArtifact,
                sunJaxbRuntimeGroup, sunJaxbRuntimeArtifact, "2.3.x", null
        ).getVisitor().visitNonNull(d, ctx);
        d = (Xml.Document) new org.openrewrite.maven.ChangeManagedDependencyGroupIdAndArtifactId(
                glassfishJaxbRuntimeGroup, glassfishJaxbRuntimeArtifact,
                sunJaxbRuntimeGroup, sunJaxbRuntimeArtifact, "2.3.x"
        ).getVisitor().visitNonNull(d, ctx);
        return d;
    }
}
