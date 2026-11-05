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
package org.openrewrite.java.migrate.guava;

import org.openrewrite.Cursor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

final class LambdaCapture {
    private LambdaCapture() {
    }

    /**
     * Whether a lambda written at the cursor may read every variable of the expression.
     */
    static boolean canCapture(Cursor cursor, Expression expression) {
        Set<String> variables = new HashSet<>();
        new JavaIsoVisitor<Set<String>>() {
            @Override
            public J.Identifier visitIdentifier(J.Identifier identifier, Set<String> names) {
                if (identifier.getFieldType() != null) {
                    names.add(identifier.getSimpleName());
                }
                return identifier;
            }
        }.visit(expression, variables);
        if (variables.isEmpty()) {
            return true;
        }
        J.MethodDeclaration method = cursor.firstEnclosing(J.MethodDeclaration.class);
        return method != null && !isAssigned(method, variables);
    }

    // XXX rewrite-java has no query for whether a local variable is effectively final,
    // so this takes any assignment or increment of a variable of that name in the method as a reassignment.
    private static boolean isAssigned(J.MethodDeclaration method, Set<String> names) {
        AtomicBoolean assigned = new AtomicBoolean();
        new JavaIsoVisitor<AtomicBoolean>() {
            @Override
            public J.Assignment visitAssignment(J.Assignment assignment, AtomicBoolean found) {
                found.compareAndSet(false, isNamed(assignment.getVariable()));
                return super.visitAssignment(assignment, found);
            }

            @Override
            public J.AssignmentOperation visitAssignmentOperation(J.AssignmentOperation assignOp, AtomicBoolean found) {
                found.compareAndSet(false, isNamed(assignOp.getVariable()));
                return super.visitAssignmentOperation(assignOp, found);
            }

            @Override
            public J.Unary visitUnary(J.Unary unary, AtomicBoolean found) {
                found.compareAndSet(false, unary.getOperator().isModifying() && isNamed(unary.getExpression()));
                return super.visitUnary(unary, found);
            }

            private boolean isNamed(Expression variable) {
                Expression unwrapped = variable;
                while (unwrapped instanceof J.Parentheses) {
                    unwrapped = (Expression) ((J.Parentheses<?>) unwrapped).getTree();
                }
                return unwrapped instanceof J.Identifier && names.contains(((J.Identifier) unwrapped).getSimpleName());
            }
        }.visit(method, assigned);
        return assigned.get();
    }
}
