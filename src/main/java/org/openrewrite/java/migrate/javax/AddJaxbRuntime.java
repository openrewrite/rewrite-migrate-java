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
import org.openrewrite.*;
import org.openrewrite.gradle.ChangeDependency;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.search.FindGradleProject;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.maven.ChangeDependencyGroupIdAndArtifactId;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.Scope;
import org.openrewrite.xml.tree.Xml;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

@EqualsAndHashCode(callSuper = false)
@Value
public class AddJaxbRuntime extends ScanningRecipe<AddJaxbRuntime.Accumulator> {
    private static final String JACKSON_GROUP = "com.fasterxml.jackson.module";
    private static final String JACKSON_JAXB_ARTIFACT = "jackson-module-jaxb-annotations";

    private static final String JAKARTA_API_GROUP = "jakarta.xml.bind";
    private static final String JAKARTA_API_ARTIFACT = "jakarta.xml.bind-api";

    private static final String SUN_JAXB_RUNTIME_GROUP = "com.sun.xml.bind";
    private static final String SUN_JAXB_RUNTIME_ARTIFACT = "jaxb-impl";

    private static final String GLASSFISH_JAXB_RUNTIME_GROUP = "org.glassfish.jaxb";
    private static final String GLASSFISH_JAXB_RUNTIME_ARTIFACT = "jaxb-runtime";

    private static final String RUNTIME_VERSION = "2.3.x";

    @Option(displayName = "JAXB run-time",
            description = "Which implementation of the JAXB run-time that will be added to maven projects that have transitive dependencies on the JAXB API",
            valid = {"glassfish", "sun"},
            example = "glassfish")
    String runtime;

    String displayName = "Use latest JAXB API and runtime for Jakarta EE 8";

    String description = "Update build files to use the latest JAXB runtime from Jakarta EE 8 to maintain compatibility with " +
               "Java version 11 or greater. The recipe will add a JAXB run-time, in Gradle " +
               "`compileOnly`+`testImplementation` and Maven `provided` scope, to any project that has a transitive " +
               "dependency on the JAXB API. **The resulting dependencies still use the `javax` namespace, despite " +
               "the move to the Jakarta artifact**.";

    Duration estimatedEffortPerOccurrence = Duration.ofMinutes( 30 );

    Set<String> tags = new HashSet<>( Arrays.asList( "javax", "jakarta", "javaee", "jaxb", "glassfish", "java11" ) );

    public static class Accumulator {
        final AtomicBoolean usesJaxb = new AtomicBoolean(false);
        final ChangeDependency.Accumulator gradleChangeDependency;
        final org.openrewrite.gradle.UpgradeDependencyVersion.DependencyVersionState gradleUpgradeDependencyVersion;
        final ChangeDependencyGroupIdAndArtifactId.Accumulator mavenChangeDependency;
        final org.openrewrite.maven.UpgradeDependencyVersion.Accumulator mavenUpgradeDependencyVersion;

        Accumulator(AddJaxbRuntime recipe, ExecutionContext ctx) {
            gradleChangeDependency = recipe.gradleChangeDependency().getInitialValue(ctx);
            gradleUpgradeDependencyVersion = recipe.gradleUpgradeDependencyVersion().getInitialValue(ctx);
            mavenChangeDependency = recipe.mavenChangeDependency().getInitialValue(ctx);
            mavenUpgradeDependencyVersion = recipe.mavenUpgradeDependencyVersion().getInitialValue(ctx);
        }
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator(this, ctx);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        // Delegates must scan too, or they cannot update versions held in properties of other files
        List<TreeVisitor<?, ExecutionContext>> delegateScanners = Arrays.asList(
                gradleChangeDependency().getScanner(acc.gradleChangeDependency),
                gradleUpgradeDependencyVersion().getScanner(acc.gradleUpgradeDependencyVersion),
                mavenUpgradeDependencyVersion().getScanner(acc.mavenUpgradeDependencyVersion),
                mavenChangeDependency().getScanner(acc.mavenChangeDependency));
        TreeVisitor<?, ExecutionContext> usesJaxb = new UsesType<>("javax.xml.bind..*", true);
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree preVisit(Tree tree, ExecutionContext ctx) {
                stopAfterPreVisit();
                if (!(tree instanceof SourceFile)) {
                    return tree;
                }
                SourceFile sourceFile = (SourceFile) tree;
                if (!acc.usesJaxb.get() && sourceFile instanceof J && usesJaxb.visit(tree, ctx) != tree) {
                    acc.usesJaxb.set(true);
                }
                for (TreeVisitor<?, ExecutionContext> scanner : delegateScanners) {
                    if (scanner.isAcceptable(sourceFile, ctx)) {
                        scanner.visit(tree, ctx);
                    }
                }
                return tree;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        TreeVisitor<?, ExecutionContext> gradleVisitor = Preconditions.check(new FindGradleProject(FindGradleProject.SearchCriteria.Marker).getVisitor(), new GroovyIsoVisitor<ExecutionContext>() {
            @Override
            public G.CompilationUnit visitCompilationUnit(G.CompilationUnit g, ExecutionContext ctx) {
                if (!acc.usesJaxb.get()) {
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

                if (rc.findResolvedDependency(runtimeGroupId(), runtimeArtifactId()) == null) {
                    g = (G.CompilationUnit) new org.openrewrite.gradle.AddDependencyVisitor(runtimeGroupId(), runtimeArtifactId(), RUNTIME_VERSION, null, "runtimeOnly", null, null, null, this::isTopLevel, null)
                            .visitNonNull(g, ctx);
                }
                return g;
            }

            private boolean isTopLevel(Cursor cursor) {
                if (cursor.getValue() instanceof J.Block) {
                    return cursor.getParentOrThrow().getValue() instanceof JavaSourceFile;
                }
                return cursor.getParentOrThrow().firstEnclosing(J.MethodInvocation.class) == null;
            }
        });

        TreeVisitor<?, ExecutionContext> mavenVisitor = new MavenIsoVisitor<ExecutionContext>() {
            @Override
            @SuppressWarnings("ConstantConditions")
            public Xml.Document visitDocument(Xml.Document d, ExecutionContext ctx) {
                if (!acc.usesJaxb.get()) {
                    return d;
                }
                MavenResolutionResult mavenModel = getResolutionResult();
                if (!mavenModel.findDependencies(JACKSON_GROUP, JACKSON_JAXB_ARTIFACT, Scope.Runtime).isEmpty() ||
                    mavenModel.findDependencies(JAKARTA_API_GROUP, JAKARTA_API_ARTIFACT, Scope.Runtime).isEmpty()) {
                    return d;
                }

                if (mavenModel.findDependencies(runtimeGroupId(), runtimeArtifactId(), Scope.Runtime).isEmpty()) {
                    d = (Xml.Document) new org.openrewrite.maven.AddDependencyVisitor(runtimeGroupId(), runtimeArtifactId(), RUNTIME_VERSION, null, Scope.Runtime.name().toLowerCase(), null, null, null, null, null)
                            .visitNonNull(d, ctx);
                } else {
                    d = (Xml.Document) mavenUpgradeDependencyVersion().getVisitor(acc.mavenUpgradeDependencyVersion)
                            .visitNonNull(d, ctx);
                }
                return d;
            }
        };

        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree preVisit(Tree tree, ExecutionContext ctx) {
                stopAfterPreVisit();
                if (!(tree instanceof SourceFile)) {
                    return tree;
                }
                // Normalize any existing runtime to the one selected, upgraded to the most current 2.3.x version.
                // Delegate visitors hold per-file state, so they are created for each file.
                Tree t = tree;
                for (TreeVisitor<?, ExecutionContext> delegate : Arrays.asList(
                        gradleChangeDependency().getVisitor(acc.gradleChangeDependency),
                        gradleUpgradeDependencyVersion().getVisitor(acc.gradleUpgradeDependencyVersion),
                        mavenUpgradeDependencyVersion().getVisitor(acc.mavenUpgradeDependencyVersion),
                        mavenChangeDependency().getVisitor(acc.mavenChangeDependency))) {
                    if (delegate.isAcceptable((SourceFile) t, ctx)) {
                        t = delegate.visitNonNull(t, ctx);
                    }
                }
                for (TreeVisitor<?, ExecutionContext> add : Arrays.asList(gradleVisitor, mavenVisitor)) {
                    if (add.isAcceptable((SourceFile) t, ctx)) {
                        t = add.visitNonNull(t, ctx);
                    }
                }
                return t;
            }
        };
    }

    private String runtimeGroupId() {
        return "sun".equals(runtime) ? SUN_JAXB_RUNTIME_GROUP : GLASSFISH_JAXB_RUNTIME_GROUP;
    }

    private String runtimeArtifactId() {
        return "sun".equals(runtime) ? SUN_JAXB_RUNTIME_ARTIFACT : GLASSFISH_JAXB_RUNTIME_ARTIFACT;
    }

    private String otherRuntimeGroupId() {
        return "sun".equals(runtime) ? GLASSFISH_JAXB_RUNTIME_GROUP : SUN_JAXB_RUNTIME_GROUP;
    }

    private String otherRuntimeArtifactId() {
        return "sun".equals(runtime) ? GLASSFISH_JAXB_RUNTIME_ARTIFACT : SUN_JAXB_RUNTIME_ARTIFACT;
    }

    private ChangeDependency gradleChangeDependency() {
        return new ChangeDependency(otherRuntimeGroupId(), otherRuntimeArtifactId(), runtimeGroupId(), runtimeArtifactId(), RUNTIME_VERSION, null, null, true);
    }

    private org.openrewrite.gradle.UpgradeDependencyVersion gradleUpgradeDependencyVersion() {
        return new org.openrewrite.gradle.UpgradeDependencyVersion(runtimeGroupId(), runtimeArtifactId(), RUNTIME_VERSION, null);
    }

    private ChangeDependencyGroupIdAndArtifactId mavenChangeDependency() {
        return new ChangeDependencyGroupIdAndArtifactId(otherRuntimeGroupId(), otherRuntimeArtifactId(), runtimeGroupId(), runtimeArtifactId(), RUNTIME_VERSION, null, false, true);
    }

    private org.openrewrite.maven.UpgradeDependencyVersion mavenUpgradeDependencyVersion() {
        return new org.openrewrite.maven.UpgradeDependencyVersion(runtimeGroupId(), runtimeArtifactId(), RUNTIME_VERSION, null, false, null);
    }
}
