package org.openrewrite.java.migrate.jakarta;

import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.search.FindAnnotations;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.SearchResult;

import java.util.concurrent.atomic.AtomicBoolean;

public class HasNoJakartaNullAnnotations extends ScanningRecipe<AtomicBoolean> {
    @Override
    public String getDisplayName() {
        return "Project has no Jakarta null annotations";
    }

    @Override
    public String getDescription() {
        return "Search for @Nonnull and @Nullable annotations, mark all source as found if no annotations are found.";
    }

    @Override
    public AtomicBoolean getInitialValue(ExecutionContext ctx) {
        return new AtomicBoolean();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(AtomicBoolean acc) {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                J.CompilationUnit c = super.visitCompilationUnit(cu, ctx);
                if (!acc.get()) {
                    if ((!FindAnnotations.find(c, "@jakarta.annotation.Nonnull", true).isEmpty()) ||
                            (!FindAnnotations.find(c, "@jakarta.annotation.Nullable", true).isEmpty())) {
                        acc.set(true);
                    }
                }
                return cu;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(AtomicBoolean acc) {
        return new TreeVisitor<Tree, ExecutionContext>() {
            @Override
            public Tree visit(@Nullable Tree tree, ExecutionContext ctx) {
                assert tree != null;
                if (!acc.get()) {
                    return SearchResult.found(tree, "Project has no Jakarta null annotations");
                }
                return tree;
            }
        };
    }
}
