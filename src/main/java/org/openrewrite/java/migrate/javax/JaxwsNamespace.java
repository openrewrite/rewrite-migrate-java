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
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.Scope;
import java.util.Collections;
import org.openrewrite.semver.Semver;
import org.openrewrite.semver.VersionComparator;

import static java.util.Objects.requireNonNull;

/** Build-file precondition: Jakarta JAX-WS 3+ cannot supply javax.xml.ws or javax.jws types. */
@Value
@EqualsAndHashCode(callSuper = false)
public class JaxwsNamespace extends Recipe {
    private static final VersionComparator LEGACY = requireNonNull(Semver.validate("[0,3)", null).getValue());
    private static final VersionComparator MODERN = requireNonNull(Semver.validate("[3,)", null).getValue());

    @Option(displayName = "Modern namespace", description = "Whether to match graphs using Jakarta JAX-WS or JWS API 3 or later.", example = "true")
    boolean modern;

    @Option(displayName = "Missing legacy API", description = "When set, also require the corresponding javax API to be absent from the main compilation classpath.", example = "ws", required = false, valid = {"ws", "jws"})
    @Nullable String missingLegacyApi;

    public JaxwsNamespace(boolean modern) {
        this(modern, null);
    }

    @JsonCreator
    public JaxwsNamespace(@JsonProperty("modern") boolean modern,
                          @JsonProperty("missingLegacyApi") @Nullable String missingLegacyApi) {
        this.modern = modern;
        this.missingLegacyApi = missingLegacyApi;
    }

    String displayName = "Check the JAX-WS dependency namespace";
    String description = "Select build files by their resolved JAX-WS namespace, including transitive and managed APIs.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (!(tree instanceof SourceFile)) {
                    return tree;
                }
                boolean found = matches(tree, JaxwsNamespace::isModern, true);
                boolean legacyAvailable = missingLegacyApi != null && mainCompileHasLegacy(tree, missingLegacyApi);
                return found == modern && !legacyAvailable ? SearchResult.found(tree) : tree;
            }
        };
    }

    private interface ApiPredicate {
        boolean test(String group, String artifact, @Nullable String version);
    }

    private static boolean matches(Tree tree, ApiPredicate predicate, boolean includeManaged) {
        return tree.getMarkers().findFirst(MavenResolutionResult.class).map(m ->
                m.getDependencies().values().stream().anyMatch(dependencies ->
                        ResolvedDependencyGraph.anyMatch(dependencies, d ->
                                predicate.test(d.getGroupId(), d.getArtifactId(), d.getVersion()))) ||
                includeManaged && m.getPom().getDependencyManagement().stream().anyMatch(d ->
                        predicate.test(d.getGroupId(), d.getArtifactId(), d.getVersion()))).orElse(false) ||
                tree.getMarkers().findFirst(GradleProject.class).map(g ->
                        g.getConfigurations().stream().filter(c -> isApplicationConfiguration(c.getName())).anyMatch(c ->
                                ResolvedDependencyGraph.anyMatch(c.getDirectResolved(), d ->
                                        predicate.test(d.getGroupId(), d.getArtifactId(), d.getVersion())))).orElse(false);
    }

    static boolean isApplicationConfiguration(String name) {
        return !"classpath".equals(name) && !name.endsWith("AnnotationProcessor") &&
               !"annotationProcessor".equals(name);
    }

    private static boolean mainCompileHasLegacy(Tree tree, String api) {
        return tree.getMarkers().findFirst(MavenResolutionResult.class).map(m ->
                ResolvedDependencyGraph.anyMatch(m.getDependencies().getOrDefault(Scope.Compile, Collections.emptyList()),
                        d -> isLegacy(api, d.getGroupId(), d.getArtifactId(), d.getVersion())) ||
                m.getDependencies().getOrDefault(Scope.Provided, Collections.emptyList()).stream().anyMatch(d ->
                        declaredScope(m, d) == Scope.Provided &&
                        ResolvedDependencyGraph.anyMatch(Collections.singletonList(d), child ->
                                isLegacy(api, child.getGroupId(), child.getArtifactId(), child.getVersion())))).orElse(false) ||
               tree.getMarkers().findFirst(GradleProject.class).map(g ->
                       g.getConfigurations().stream().filter(c -> "compileClasspath".equals(c.getName()) ||
                               "compileOnly".equals(c.getName()) || "implementation".equals(c.getName()) || "api".equals(c.getName()))
                               .anyMatch(c -> ResolvedDependencyGraph.anyMatch(c.getDirectResolved(), d ->
                                       isLegacy(api, d.getGroupId(), d.getArtifactId(), d.getVersion())))).orElse(false);
    }

    private static Scope declaredScope(MavenResolutionResult model, org.openrewrite.maven.tree.ResolvedDependency dependency) {
        String scope = dependency.getRequested() == null ? null : model.getPom().getValue(dependency.getRequested().getScope());
        return scope == null ? model.getPom().getManagedScope(dependency.getGroupId(), dependency.getArtifactId(),
                dependency.getType(), dependency.getClassifier()) : Scope.fromName(scope);
    }

    static boolean isLegacy(String api, String group, String artifact, @Nullable String version) {
        if ("ws".equals(api)) {
            return "javax.xml.ws".equals(group) && "jaxws-api".equals(artifact) ||
                   "jakarta.xml.ws".equals(group) && "jakarta.xml.ws-api".equals(artifact) &&
                   version != null && LEGACY.isValid(null, version);
        }
        return "jws".equals(api) && ("javax.jws".equals(group) && "javax.jws-api".equals(artifact) ||
                "jakarta.jws".equals(group) && "jakarta.jws-api".equals(artifact) &&
                version != null && LEGACY.isValid(null, version));
    }

    static boolean isModern(String group, String artifact, @Nullable String version) {
        return (("jakarta.xml.ws".equals(group) && "jakarta.xml.ws-api".equals(artifact)) ||
                ("jakarta.jws".equals(group) && "jakarta.jws-api".equals(artifact))) &&
               version != null && MODERN.isValid(null, version);
    }
}
