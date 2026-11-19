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
package org.openrewrite.java.migrate.search;

import lombok.Getter;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.maven.tree.Dependency;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.ResolvedPom;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class HasStandaloneTomcat8 extends ScanningRecipe<Map<Path, Boolean>> {
    private static final String TOMCAT = "org.apache.tomcat";
    private static final String EMBEDDED = "org.apache.tomcat.embed";
    private static final String BOOT = "org.springframework.boot";
    private static final String POM_XML = "pom.xml";
    private static final Pattern TOMCAT_8_0 = Pattern.compile("8\\.0\\.[0-9]+");

    @Getter
    final String displayName = "Find standalone embedded Tomcat 8.0 Maven modules";

    @Getter
    final String description = "Find Maven modules that explicitly configure embedded Tomcat 8.0 outside Spring Boot management.";

    @Override
    public Map<Path, Boolean> getInitialValue(ExecutionContext ctx) {
        return new HashMap<>();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Map<Path, Boolean> modules) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree preVisit(Tree tree, ExecutionContext ctx) {
                stopAfterPreVisit();
                if (tree instanceof SourceFile) {
                    Path path = ((SourceFile) tree).getSourcePath();
                    if (POM_XML.equals(path.getFileName().toString())) {
                        modules.put(path, tree.getMarkers().findFirst(MavenResolutionResult.class)
                                .map(HasStandaloneTomcat8::isStandaloneTomcat8)
                                .orElse(false));
                    }
                }
                return tree;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Map<Path, Boolean> modules) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree preVisit(Tree tree, ExecutionContext ctx) {
                stopAfterPreVisit();
                if (!(tree instanceof SourceFile)) {
                    return tree;
                }
                for (Path dir = ((SourceFile) tree).getSourcePath().getParent(); dir != null; dir = dir.getParent()) {
                    Boolean matches = modules.get(dir.resolve(POM_XML));
                    if (matches != null) {
                        return matches ? SearchResult.found(tree) : tree;
                    }
                }
                return Boolean.TRUE.equals(modules.get(Paths.get(POM_XML))) ? SearchResult.found(tree) : tree;
            }
        };
    }

    private static boolean isStandaloneTomcat8(MavenResolutionResult mrr) {
        ResolvedPom pom = mrr.getPom();
        for (MavenResolutionResult parent = mrr; parent != null; parent = parent.getParent()) {
            if (parent.getPom().getRequested().getParent() != null &&
                    BOOT.equals(parent.getPom().getRequested().getParent().getGroupId())) {
                return false;
            }
        }
        if (pom.getRequestedDependencies().stream().anyMatch(dep -> BOOT.equals(pom.getValue(dep.getGroupId()))) ||
                pom.getDependencyManagement().stream().anyMatch(dep -> dep.getBomGav() != null &&
                        BOOT.equals(dep.getBomGav().getGroupId()))) {
            return false;
        }

        List<Dependency> directDependencies = pom.getRequested().getDependencies();
        List<Dependency> tomcat = directDependencies.stream()
                .filter(dep -> isContainerDependency(pom, dep))
                .collect(Collectors.toList());
        if (tomcat.stream().noneMatch(dep -> "tomcat-embed-core".equals(dep.getArtifactId()))) {
            return false;
        }
        for (Dependency dep : tomcat) {
            String requested = dep.getVersion();
            String resolved = pom.getValue(requested);
            if (resolved == null || !TOMCAT_8_0.matcher(resolved).matches()) {
                return false;
            }
            // A property used by another dependency would move it as a side effect.
            if (requested != null && requested.startsWith("${") && pom.getRequestedDependencies().stream()
                    .anyMatch(other -> requested.equals(other.getVersion()) && !isContainerDependency(pom, other))) {
                return false;
            }
        }
        return true;
    }

    private static boolean isContainerDependency(ResolvedPom pom, Dependency dependency) {
        String group = pom.getValue(dependency.getGroupId());
        String artifact = dependency.getArtifactId();
        return EMBEDDED.equals(group) && "tomcat-embed-core".equals(artifact) ||
                TOMCAT.equals(group) && ("tomcat-catalina".equals(artifact) || "tomcat-util".equals(artifact));
    }
}
