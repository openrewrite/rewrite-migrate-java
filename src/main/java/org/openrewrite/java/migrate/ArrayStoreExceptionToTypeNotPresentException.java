package org.openrewrite.java.migrate;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.ChangeType;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ArrayStoreExceptionToTypeNotPresentException extends Recipe {
    @Override
    public String getDisplayName() {
        return "Handle TypeNotPresentException instead of ArrayStoreException";
    }

    @Override
    public String getDescription() {
        return "This recipe replaces catch blocks for ArrayStoreException around getAnnotation() with TypeNotPresentException or both exceptions, to ensure compatibility with Java 11+.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        final MethodMatcher classGetAnnotationMethod = new MethodMatcher("java.lang.Class getAnnotation(java.lang.Class)");
        return new JavaVisitor<ExecutionContext>() {

            @Override
            public J visitTry(J.Try tryStmt, ExecutionContext ctx) {
                boolean flag = false;
                if (containsGetAnnotation(tryStmt)) {
                    List<J.Try.Catch> updatedCatches = new ArrayList<>();
                    for (J.Try.Catch catchClause : tryStmt.getCatches()) {
                        if (catchClause.getParameter().getType() != null && catchClause.getParameter().getType().isAssignableFrom(Pattern.compile("java.lang.ArrayStoreException"))) {
                            J.Try.Catch updatedCatch = (J.Try.Catch) new ChangeType("java.lang.ArrayStoreException", "java.lang.TypeNotPresentException", true).getVisitor().visit(catchClause, ctx);
                            updatedCatches.add(updatedCatch);
                            flag = true;
                        } else {
                            updatedCatches.add(catchClause);
                        }
                    }
                    if (flag)
                        tryStmt = tryStmt.withCatches(updatedCatches);
                }
                return super.visitTry(tryStmt, ctx);
            }

            private boolean containsGetAnnotation(J.Try tryStmt) {
                return tryStmt.getBody().getStatements().stream()
                        .filter(Expression.class::isInstance)
                        .map(Expression.class::cast)
                        .anyMatch(classGetAnnotationMethod::matches);
            }
        };
    }
}

