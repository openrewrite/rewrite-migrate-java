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
package org.openrewrite.java.migrate.jersey;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.gradle.marker.GradleDependencyConfiguration;
import org.openrewrite.gradle.marker.GradleProject;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.maven.tree.Dependency;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.Pom;
import org.openrewrite.maven.tree.ResolvedDependency;
import org.openrewrite.maven.tree.Scope;

import java.util.*;
import java.util.function.Predicate;

import static java.util.Collections.newSetFromMap;

/**
 * Jersey 1 and Jersey 2 share the {@code javax.ws.rs} packages, but not their implementations, so a module can only
 * move to Jersey 2 once nothing else it depends on still needs Jersey 1 at runtime.
 */
@Value
@EqualsAndHashCode(callSuper = false)
public class Jersey1RepositoryCanMigrate extends ScanningRecipe<Jersey1RepositoryCanMigrate.Accumulator> {

    String displayName = "Find repositories that can migrate from Jersey 1";

    String description = "Marks every file of a repository whose Maven or Gradle build declares a Jersey 1 " +
            "(`com.sun.jersey`) dependency itself. A repository is left alone as a whole when one of its modules gets " +
            "Jersey 1 through a library that is not Jersey, such as Dropwizard before 0.8, Eureka 1 or Hadoop, since " +
            "that library keeps using Jersey 1; when a module declares Jersey 1 as `provided` or `compileOnly`, since " +
            "the container or the code being instrumented supplies it; and when it uses the Jersey 1 Guice integration, " +
            "for which Jersey 2 has no counterpart; and when a module that declares Jersey 1 also uses Jersey 3 or " +
            "later, whose `org.glassfish.jersey` packages are those of Jersey 2. Nor does it migrate a repository with classes of its own in a " +
            "`com.sun.jersey` package, which extend or instrument Jersey 1, or whose Kotlin, Groovy or Scala code uses " +
            "Jersey 1, which these recipes do not change.";

    public static class Accumulator {
        boolean declaresJersey1;
        boolean blocked;

        /**
         * The libraries that are not Jersey, nor a module of this repository, through which a module gets Jersey 1.
         */
        final Set<String> librariesBringingJersey1 = new TreeSet<>();

        boolean canMigrate() {
            return declaresJersey1 && !blocked && librariesBringingJersey1.isEmpty();
        }
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree preVisit(Tree tree, ExecutionContext ctx) {
                stopAfterPreVisit();
                if (extendsJersey1(tree)) {
                    acc.blocked = true;
                }
                tree.getMarkers().findFirst(MavenResolutionResult.class).ifPresent(maven -> scanMaven(maven, acc));
                tree.getMarkers().findFirst(GradleProject.class).ifPresent(gradle -> scanGradle(gradle, acc));
                return tree;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public @Nullable Tree preVisit(Tree tree, ExecutionContext ctx) {
                stopAfterPreVisit();
                return acc.canMigrate() ? SearchResult.found(tree) : tree;
            }
        };
    }

    private static boolean extendsJersey1(Tree tree) {
        if (tree instanceof JavaSourceFile) {
            J.Package pkg = ((JavaSourceFile) tree).getPackageDeclaration();
            if (pkg != null && pkg.getPackageName().startsWith("com.sun.jersey")) {
                return true;
            }
        }
        if (tree instanceof SourceFile) {
            String path = ((SourceFile) tree).getSourcePath().toString();
            return (path.endsWith(".kt") || path.endsWith(".groovy") || path.endsWith(".scala")) &&
                    ((SourceFile) tree).printAll().contains("com.sun.jersey");
        }
        return false;
    }

    private static void scanMaven(MavenResolutionResult maven, Accumulator acc) {
        Set<String> projects = new HashSet<>();
        projects.add(maven.getPom().getGroupId() + ":" + maven.getPom().getArtifactId());
        for (Pom pom : maven.getProjectPoms().values()) {
            projects.add(pom.getGroupId() + ":" + pom.getArtifactId());
        }
        boolean declaresJersey1 = false;
        boolean usesJersey3 = false;
        for (Map.Entry<Scope, List<ResolvedDependency>> byScope : maven.getDependencies().entrySet()) {
            for (ResolvedDependency dependency : byScope.getValue()) {
                usesJersey3 |= isJersey3OrLater(dependency);
                if (dependency.getDepth() != 0) {
                    continue;
                }
                if (isJersey1(dependency)) {
                    declaresJersey1 = true;
                    String scope = maven.getPom().getValue(dependency.getRequested().getScope());
                    if ("provided".equals(scope) || "system".equals(scope) || isJersey1Guice(dependency)) {
                        acc.blocked = true;
                    } else {
                        acc.declaresJersey1 = true;
                    }
                } else if (!projects.contains(dependency.getGroupId() + ":" + dependency.getArtifactId()) &&
                        bringsJersey1(dependency, d -> projects.contains(d.getGroupId() + ":" + d.getArtifactId()))) {
                    acc.librariesBringingJersey1.add(dependency.getGroupId() + ":" + dependency.getArtifactId());
                }
            }
        }
        if (declaresJersey1 && usesJersey3) {
            acc.blocked = true;
        }
    }

    private static void scanGradle(GradleProject gradle, Accumulator acc) {
        // A dependency on another project of the same build shares its group and version.
        Predicate<ResolvedDependency> isProject = d -> d.getGroupId().equals(gradle.getGroup()) &&
                Objects.equals(d.getVersion(), gradle.getVersion());
        boolean declaresJersey1 = false;
        boolean usesJersey3 = false;
        for (GradleDependencyConfiguration configuration : gradle.getConfigurations()) {
            String name = configuration.getName();
            boolean compileOnly = name.endsWith("ompileOnly") || "providedCompile".equals(name) ||
                    "providedRuntime".equals(name);
            for (Dependency requested : configuration.getRequested()) {
                if (requested.getGroupId() != null && requested.getGroupId().startsWith("com.sun.jersey")) {
                    declaresJersey1 = true;
                    if (compileOnly || "jersey-guice".equals(requested.getArtifactId())) {
                        acc.blocked = true;
                    } else {
                        acc.declaresJersey1 = true;
                    }
                }
            }
            if (configuration.isCanBeResolved()) {
                for (ResolvedDependency dependency : configuration.getResolved()) {
                    usesJersey3 |= isJersey3OrLater(dependency);
                }
                for (ResolvedDependency dependency : configuration.getDirectResolved()) {
                    if (!isJersey1(dependency) && !isProject.test(dependency) && bringsJersey1(dependency, isProject)) {
                        acc.librariesBringingJersey1.add(dependency.getGroupId() + ":" + dependency.getArtifactId());
                    }
                }
            }
        }
        if (declaresJersey1 && usesJersey3) {
            acc.blocked = true;
        }
    }

    /**
     * Jersey 3 keeps the {@code org.glassfish.jersey} packages of Jersey 2, so the two cannot share a class path.
     */
    private static boolean isJersey3OrLater(ResolvedDependency dependency) {
        if (!dependency.getGroupId().startsWith("org.glassfish.jersey")) {
            return false;
        }
        String version = dependency.getVersion();
        int end = 0;
        while (end < version.length() && Character.isDigit(version.charAt(end))) {
            end++;
        }
        return end > 0 && end < 9 && Integer.parseInt(version.substring(0, end)) >= 3;
    }

    static boolean isJersey1(ResolvedDependency dependency) {
        return dependency.getGroupId().startsWith("com.sun.jersey");
    }

    private static boolean isJersey1Guice(ResolvedDependency dependency) {
        return "com.sun.jersey.contribs".equals(dependency.getGroupId()) &&
                "jersey-guice".equals(dependency.getArtifactId());
    }

    private static boolean bringsJersey1(ResolvedDependency root, Predicate<ResolvedDependency> isProject) {
        Set<ResolvedDependency> visited = newSetFromMap(new IdentityHashMap<>());
        Deque<ResolvedDependency> pending = new ArrayDeque<>(root.getDependencies());
        while (!pending.isEmpty()) {
            ResolvedDependency dependency = pending.removeFirst();
            if (!visited.add(dependency)) {
                continue;
            }
            if (isJersey1(dependency)) {
                return true;
            }
            if (isProject.test(dependency)) {
                // Modules of this repository are looked at as modules of their own.
                continue;
            }
            pending.addAll(dependency.getDependencies());
        }
        return false;
    }
}
