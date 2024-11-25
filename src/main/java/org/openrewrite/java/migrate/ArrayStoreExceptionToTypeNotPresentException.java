/*
 * Copyright 2024 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
        return "ArrayStoreException to TypeNotPresentException around getAnnotation() method ";
    }

    @Override
    public String getDescription() {
        return "Replace catch blocks for `ArrayStoreException` around `getAnnotation()` with `TypeNotPresentException` to ensure compatibility with Java 11+.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        final MethodMatcher classGetAnnotationMethod = new MethodMatcher("java.lang.Class getAnnotation(java.lang.Class)");
        return new JavaVisitor<ExecutionContext>() {

            @Override
            public J visitTry(J.Try tryStmt, ExecutionContext ctx) {
                if (!containsGetAnnotation(tryStmt)) {
                    return super.visitTry(tryStmt, ctx);
                }
                boolean flag = false;
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
                    if (flag) {
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
