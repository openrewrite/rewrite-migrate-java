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

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.marker.JavaProject;
import org.openrewrite.java.search.FindAnnotations;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public class HasNoJakartaNullAnnotations extends ScanningRecipe<HasNoJakartaNullAnnotations.Accumulator> {
    @Override
    public String getDisplayName() {
        return "Project has no Jakarta null annotations";
    }

    @Override
    public String getDescription() {
        return "Search for `@Nonnull` and `@Nullable` annotations, mark all source as found if no annotations are found.";
    }

    @Value
    public static class Accumulator {
        Set<JavaProject> projectsWithDependency;
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator(new HashSet<>());
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(HasNoJakartaNullAnnotations.Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                assert tree != null;
                if (tree instanceof J) {
                    tree.getMarkers().findFirst(JavaProject.class)
                            .filter(jp -> !acc.getProjectsWithDependency().contains(jp))
                            .filter(__ ->!FindAnnotations.find((J) tree, "@jakarta.annotation.Nonnull", true).isEmpty() ||
                                    !FindAnnotations.find((J) tree, "@jakarta.annotation.Nullable", true).isEmpty())
                            .ifPresent(it -> acc.getProjectsWithDependency().add(it));
                }
                return tree;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(HasNoJakartaNullAnnotations.Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                assert tree != null;
                return tree.getMarkers().findFirst(JavaProject.class)
                        .filter(it -> !acc.getProjectsWithDependency().contains(it))
                        .map(__ -> SearchResult.found(tree, "Project has no Jakarta null annotations"))
                        .orElse(tree);
            }
        };
    }
}
