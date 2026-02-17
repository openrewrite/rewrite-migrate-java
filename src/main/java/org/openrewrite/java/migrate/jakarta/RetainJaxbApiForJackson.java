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
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.gradle.search.FindGradleProject;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.Scope;
import org.openrewrite.xml.tree.Xml;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * When migrating from {@code javax.xml.bind} to {@code jakarta.xml.bind} (version 3.0+),
 * the {@code javax.xml.bind:jaxb-api} dependency is replaced. However, if
 * {@code jackson-module-jaxb-annotations} is on the classpath, it still requires
 * the {@code javax.xml.bind} namespace at runtime. This recipe re-adds
 * {@code javax.xml.bind:jaxb-api} as a runtime dependency to prevent
 * {@code NoClassDefFoundError} at runtime.
 */
@EqualsAndHashCode(callSuper = false)
@Value
public class RetainJaxbApiForJackson extends Recipe {
    private static final String JACKSON_GROUP = "com.fasterxml.jackson.module";
    private static final String JACKSON_JAXB_ARTIFACT = "jackson-module-jaxb-annotations";
    private static final String JAXB_API_GROUP = "javax.xml.bind";
    private static final String JAXB_API_ARTIFACT = "jaxb-api";
    private static final String JAXB_API_VERSION = "2.3.x";

    @Override
    public String getDisplayName() {
        return "Retain `javax.xml.bind:jaxb-api` when `jackson-module-jaxb-annotations` is present";
    }

    @Override
    public String getDescription() {
        return "When migrating from `javax.xml.bind` to `jakarta.xml.bind` 3.0+, the `javax.xml.bind:jaxb-api` " +
               "dependency is normally replaced. However, if `jackson-module-jaxb-annotations` is on the classpath " +
               "(and still uses the `javax.xml.bind` namespace), this recipe ensures `javax.xml.bind:jaxb-api` " +
               "remains available as a runtime dependency to prevent `NoClassDefFoundError`.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                tree = gradleVisitor.visit(tree, ctx);
                tree = mavenVisitor.visit(tree, ctx);
                return tree;
            }

            final TreeVisitor<?, ExecutionContext> gradleVisitor = Preconditions.check(
                    new FindGradleProject(FindGradleProject.SearchCriteria.Marker).getVisitor(),
                    new GroovyIsoVisitor<ExecutionContext>() {
                        @Override
                        public G.CompilationUnit visitCompilationUnit(G.CompilationUnit cu, ExecutionContext ctx) {
                            Optional<GradleProject> maybeGp = cu.getMarkers().findFirst(GradleProject.class);
                            if (!maybeGp.isPresent()) {
                                return cu;
                            }
                            GradleProject gp = maybeGp.get();
                            GradleDependencyConfiguration rc = gp.getConfiguration("runtimeClasspath");
                            if (rc == null) {
                                return cu;
                            }
                            // Only retain if jackson-module-jaxb-annotations is on the classpath
                            if (rc.findResolvedDependency(JACKSON_GROUP, JACKSON_JAXB_ARTIFACT) == null) {
                                return cu;
                            }
                            return (G.CompilationUnit) new org.openrewrite.gradle.AddDependencyVisitor(
                                    JAXB_API_GROUP, JAXB_API_ARTIFACT, JAXB_API_VERSION,
                                    null, "runtimeOnly", null, null, null, null, null)
                                    .visitNonNull(cu, ctx);
                        }
                    }
            );

            final TreeVisitor<?, ExecutionContext> mavenVisitor = new MavenIsoVisitor<ExecutionContext>() {
                @Override
                public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                    MavenResolutionResult mavenModel = getResolutionResult();
                    // Check if jackson-module-jaxb-annotations is on the classpath
                    if (!hasJacksonJaxbAnnotations(mavenModel)) {
                        return document;
                    }
                    return (Xml.Document) new org.openrewrite.maven.AddDependencyVisitor(
                            JAXB_API_GROUP, JAXB_API_ARTIFACT, JAXB_API_VERSION,
                            null, Scope.Runtime.name().toLowerCase(), null, null, null, null, null)
                            .visitNonNull(document, ctx);
                }

                private boolean hasJacksonJaxbAnnotations(MavenResolutionResult mavenModel) {
                    for (Map.Entry<Scope, List<ResolvedDependency>> entry : mavenModel.getDependencies().entrySet()) {
                        for (ResolvedDependency dependency : entry.getValue()) {
                            if (JACKSON_GROUP.equals(dependency.getGroupId()) &&
                                JACKSON_JAXB_ARTIFACT.equals(dependency.getArtifactId())) {
                                return true;
                            }
                        }
                    }
                    return false;
                }
            };
        };
    }
}
