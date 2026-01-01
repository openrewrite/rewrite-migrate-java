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

public class HasNoJakartaAnnotations extends ScanningRecipe<HasNoJakartaAnnotations.Accumulator> {
    @Override
    public String getDisplayName() {
        return "Project has no Jakarta annotations";
    }

    @Override
    public String getDescription() {
        return "Mark all source as found per `JavaProject` where no Jakarta annotations are found. " +
                "This is useful mostly as a precondition for recipes that require Jakarta annotations to be present.";
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
    public TreeVisitor<?, ExecutionContext> getScanner(HasNoJakartaAnnotations.Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree preVisit(Tree tree, ExecutionContext ctx) {
                stopAfterPreVisit();
                if (tree instanceof J) {
                    tree.getMarkers().findFirst(JavaProject.class)
                            .filter(jp -> !acc.getProjectsWithDependency().contains(jp))
                            .filter(jp -> !FindAnnotations.find((J) tree, "@jakarta.annotation.*", true).isEmpty())
                            .ifPresent(jp -> acc.getProjectsWithDependency().add(jp));
                }
                return tree;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(HasNoJakartaAnnotations.Accumulator acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree preVisit(Tree tree, ExecutionContext ctx) {
                stopAfterPreVisit();
                return tree.getMarkers().findFirst(JavaProject.class)
                        .filter(it -> !acc.getProjectsWithDependency().contains(it))
                        .map(__ -> SearchResult.found(tree, "Project has no Jakarta annotations"))
                        .orElse(tree);
            }
        };
    }
}
