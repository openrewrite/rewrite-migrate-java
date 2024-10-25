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
package org.openrewrite.java.migrate.joda;

import fj.data.Option;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.analysis.dataflow.Dataflow;
import org.openrewrite.analysis.dataflow.analysis.SinkFlowSummary;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.J.VariableDeclarations.NamedVariable;
import org.openrewrite.java.tree.JavaType;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.openrewrite.java.migrate.joda.templates.TimeClassNames.JODA_CLASS_PATTERN;

public class JodaTimeScanner extends ScopeAwareVisitor {

    @Getter
    private final Set<NamedVariable> unsafeVars;

    private final Map<NamedVariable, Set<NamedVariable>> varDependencies = new HashMap<>();

    public JodaTimeScanner(Set<NamedVariable> unsafeVars, LinkedList<VariablesInScope> scopes) {
        super(scopes);
        this.unsafeVars = unsafeVars;
    }

    public JodaTimeScanner(Set<NamedVariable> unsafeVars) {
        this(unsafeVars, new LinkedList<>());
    }

    @Override
    public J visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
        super.visitCompilationUnit(cu, ctx);
        Set<NamedVariable> allReachable = new HashSet<>();
        for (NamedVariable var : unsafeVars) {
            dfs(var, allReachable);
        }
        unsafeVars.addAll(allReachable);
        return cu;
    }

    @Override
    public NamedVariable visitVariable(NamedVariable variable, ExecutionContext ctx) {
        if (!variable.getType().isAssignableFrom(JODA_CLASS_PATTERN)) {
            return variable;
        }
        // TODO: handle class variables && method parameters
        if (!isLocalVar(variable)) {
            unsafeVars.add(variable);
            return variable;
        }
        variable = (NamedVariable) super.visitVariable(variable, ctx);

        if (!variable.getType().isAssignableFrom(JODA_CLASS_PATTERN) || variable.getInitializer() == null) {
            return variable;
        }
        List<Expression> sinks = findSinks(variable.getInitializer());

        Cursor currentScope = getCurrentScope();
        J.Block block = currentScope.getValue();
        new AddSafeCheckMarker(sinks).visit(block, ctx, currentScope.getParent());
        processMarkersOnExpression(sinks, variable);
        return variable;
    }

    @Override
    public J.Assignment visitAssignment(J.Assignment assignment, ExecutionContext ctx) {
        Expression var = assignment.getVariable();
        // not joda expr or not local variable
        if (!isJodaExpr(var) || !(var instanceof J.Identifier)) {
            return assignment;
        }
        J.Identifier ident = (J.Identifier) var;
        Optional<NamedVariable> mayBeVar = findVarInScope(ident.getSimpleName());
        if (!mayBeVar.isPresent()) {
            return assignment;
        }
        NamedVariable variable = mayBeVar.get();
        Cursor varScope = findScope(variable);
        List<Expression> sinks = findSinks(assignment.getAssignment());
        new AddSafeCheckMarker(sinks).visit(varScope.getValue(), ctx, varScope.getParent());
        processMarkersOnExpression(sinks, variable);
        return assignment;
    }

    private void processMarkersOnExpression(List<Expression> expressions, NamedVariable var) {
        for (Expression expr : expressions) {
            Optional<SafeCheckMarker> mayBeMarker = expr.getMarkers().findFirst(SafeCheckMarker.class);
            if (!mayBeMarker.isPresent()) {
                continue;
            }
            SafeCheckMarker marker = mayBeMarker.get();
            if (!marker.isSafe()) {
                unsafeVars.add(var);
            }
            if (!marker.getReferences().isEmpty()) {
                varDependencies.compute(var, (k, v) -> v == null ? new HashSet<>() : v).addAll(marker.getReferences());
                for (NamedVariable ref : marker.getReferences()) {
                    varDependencies.compute(ref, (k, v) -> v == null ? new HashSet<>() : v).add(var);
                }
            }
        }
    }

    private boolean isJodaExpr(Expression expression) {
        return expression.getType() != null && expression.getType().isAssignableFrom(JODA_CLASS_PATTERN);
    }

    private List<Expression> findSinks(Expression expr) {
        Cursor cursor = new Cursor(getCursor(), expr);
        Option<SinkFlowSummary> mayBeSinks = Dataflow.startingAt(cursor).findSinks(new JodaTimeFlowSpec());
        if (mayBeSinks.isNone()) {
            return Collections.emptyList();
        }
        return mayBeSinks.some().getExpressionSinks();
    }

    private boolean isLocalVar(NamedVariable variable) {
        if (!(variable.getVariableType().getOwner() instanceof JavaType.Method)) {
            return false;
        }
        J j = getCursor().dropParentUntil(t -> t instanceof J.Block || t instanceof J.MethodDeclaration).getValue();
        return j instanceof J.Block;
    }

    private void dfs(NamedVariable root, Set<NamedVariable> visited) {
        if (visited.contains(root)) {
            return;
        }
        visited.add(root);
        for (NamedVariable dep : varDependencies.getOrDefault(root, Collections.emptySet())) {
            dfs(dep, visited);
        }
    }

    @RequiredArgsConstructor
    private class AddSafeCheckMarker extends JavaIsoVisitor<ExecutionContext> {

        @NonNull
        private List<Expression> expressions;

        @Override
        public Expression visitExpression(Expression expression, ExecutionContext ctx) {
            int index = expressions.indexOf(expression);
            if (index == -1) {
                return super.visitExpression(expression, ctx);
            }
            Expression withMarker = expression.withMarkers(expression.getMarkers().addIfAbsent(getMarker(expression, ctx)));
            expressions.set(index, withMarker);
            return withMarker;
        }

        private SafeCheckMarker getMarker(Expression expr, ExecutionContext ctx) {
            Optional<SafeCheckMarker> mayBeMarker = expr.getMarkers().findFirst(SafeCheckMarker.class);
            if (mayBeMarker.isPresent()) {
                return mayBeMarker.get();
            }

            Cursor boundary = findBoundaryCursorForJodaExpr();
            boolean isSafe = true;
            // TODO: handle return statement
            if (boundary.getParentTreeCursor().getValue() instanceof J.Return) {
                isSafe = false;
            }
            Expression boundaryExpr = boundary.getValue();
            J j = new JodaTimeVisitor(new HashSet<>(), scopes).visit(boundaryExpr, ctx, boundary.getParentTreeCursor());
            Set<NamedVariable> referencedVars = new HashSet<>();
            new FindVarReferences().visit(expr, referencedVars, getCursor().getParentTreeCursor());
            AtomicBoolean hasJodaType = new AtomicBoolean();
            new HasJodaType().visit(j, hasJodaType);
            isSafe = isSafe && !hasJodaType.get() && !referencedVars.contains(null);
            referencedVars.remove(null);
            return new SafeCheckMarker(UUID.randomUUID(), isSafe, referencedVars);
        }

        /**
         * Traverses the cursor to find the first non-Joda expression in the path.
         * If no non-Joda expression is found, it returns the cursor pointing
         * to the last Joda expression whose parent is not an Expression.
         */
        private Cursor findBoundaryCursorForJodaExpr() {
            Cursor cursor = getCursor();
            while (cursor.getValue() instanceof Expression && isJodaExpr(cursor.getValue())) {
                Cursor parent = cursor.getParentTreeCursor();
                if (parent.getValue() instanceof J && !(parent.getValue() instanceof Expression)) {
                    return cursor;
                }
                cursor = parent;
            }
            return cursor;
        }
    }

    private class FindVarReferences extends JavaIsoVisitor<Set<NamedVariable>> {

        @Override
        public J.Identifier visitIdentifier(J.Identifier ident, Set<NamedVariable> vars) {
            if (!isJodaExpr(ident) || ident.getFieldType() == null) {
                return ident;
            }
            if (ident.getFieldType().getOwner() instanceof JavaType.Class) {
                vars.add(null); // class variable not supported yet.
            }

            // find variable in the closest scope
            findVarInScope(ident.getSimpleName()).ifPresent(vars::add);
            return ident;
        }
    }

    private static class HasJodaType extends JavaIsoVisitor<AtomicBoolean> {
        @Override
        public Expression visitExpression(Expression expression, AtomicBoolean hasJodaType) {
            if (hasJodaType.get()) {
                return expression;
            }
            if (expression.getType() != null && expression.getType().isAssignableFrom(JODA_CLASS_PATTERN)) {
                hasJodaType.set(true);
            }
            return super.visitExpression(expression, hasJodaType);
        }
    }
}
