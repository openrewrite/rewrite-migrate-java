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
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.analysis.dataflow.Dataflow;
import org.openrewrite.analysis.dataflow.analysis.SinkFlowSummary;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.J.VariableDeclarations.NamedVariable;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.MethodCall;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.openrewrite.java.migrate.joda.templates.TimeClassNames.JODA_CLASS_PATTERN;

class JodaTimeScanner extends ScopeAwareVisitor {

    @Getter
    private final JodaTimeRecipe.Accumulator acc;

    private final Map<NamedVariable, Set<NamedVariable>> varDependencies = new HashMap<>();
    private final Map<JavaType, Set<String>> unsafeVarsByType = new HashMap<>();
    private final Map<JavaType.Method, Set<NamedVariable>> methodReferencedVars = new HashMap<>();
    private final Map<JavaType.Method, Set<UnresolvedVar>> methodUnresolvedReferencedVars = new HashMap<>();

    public JodaTimeScanner(JodaTimeRecipe.Accumulator acc) {
        super(new LinkedList<>());
        this.acc = acc;
    }

    @Override
    public J visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
        super.visitCompilationUnit(cu, ctx);
        Set<NamedVariable> allReachable = new HashSet<>();
        for (NamedVariable var : acc.getUnsafeVars()) {
            dfs(var, allReachable);
        }
        acc.getUnsafeVars().addAll(allReachable);

        Set<JavaType.Method> unsafeMethods = new HashSet<>();
        acc.getSafeMethodMap().forEach((method, isSafe) -> {
            if (!isSafe) {
                unsafeMethods.add(method);
                return;
            }
            Set<NamedVariable> intersection = new HashSet<>(methodReferencedVars.getOrDefault(method, Collections.emptySet()));
            intersection.retainAll(acc.getUnsafeVars());
            if (!intersection.isEmpty()) {
                unsafeMethods.add(method);
            }
        });
        for (JavaType.Method method : unsafeMethods) {
            acc.getSafeMethodMap().put(method, false);
            acc.getUnsafeVars().addAll(methodReferencedVars.getOrDefault(method, Collections.emptySet()));
        }
        return cu;
    }

    @Override
    public J visitVariable(NamedVariable variable, ExecutionContext ctx) {
        if (!variable.getType().isAssignableFrom(JODA_CLASS_PATTERN)) {
            return super.visitVariable(variable, ctx);
        }
        // TODO: handle class variables
        if (isClassVar(variable)) {
            acc.getUnsafeVars().add(variable);
            return variable;
        }
        variable = (NamedVariable) super.visitVariable(variable, ctx);

        if (!variable.getType().isAssignableFrom(JODA_CLASS_PATTERN)) {
            return variable;
        }
        boolean isMethodParam = getCursor().getParentTreeCursor() // VariableDeclaration
                .getParentTreeCursor() // MethodDeclaration
                .getValue() instanceof J.MethodDeclaration;
        Cursor cursor = null;
        if (isMethodParam) {
            cursor = getCursor();
        } else if (variable.getInitializer() != null) {
            cursor = new Cursor(getCursor(), variable.getInitializer());
        }
        if (cursor == null) {
            return variable;
        }
        List<Expression> sinks = findSinks(cursor);

        Cursor currentScope = getCurrentScope();
        new AddSafeCheckMarker(sinks).visit(currentScope.getValue(), ctx, currentScope.getParentOrThrow());
        processMarkersOnExpression(sinks, variable);
        return variable;
    }

    @Override
    public J visitAssignment(J.Assignment assignment, ExecutionContext ctx) {
        Expression var = assignment.getVariable();
        // not joda expr or not local variable
        if (!isJodaExpr(var) || !(var instanceof J.Identifier)) {
            return super.visitAssignment(assignment, ctx);
        }
        J.Identifier ident = (J.Identifier) var;
        Optional<NamedVariable> mayBeVar = findVarInScope(ident.getSimpleName());
        if (!mayBeVar.isPresent()) {
            return super.visitAssignment(assignment, ctx);
        }
        NamedVariable variable = mayBeVar.get();
        Cursor varScope = findScope(variable);
        List<Expression> sinks = findSinks(new Cursor(getCursor(), assignment.getAssignment()));
        new AddSafeCheckMarker(sinks).visit(varScope.getValue(), ctx, varScope.getParentOrThrow());
        processMarkersOnExpression(sinks, variable);
        return super.visitAssignment(assignment, ctx);
    }

    @Override
    public J visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
        acc.getVarTable().addVars(method);
        unsafeVarsByType.getOrDefault(method.getMethodType(), Collections.emptySet()).forEach(varName -> {
            NamedVariable var = acc.getVarTable().getVarByName(method.getMethodType(), varName);
            if (var != null) { // var can only be null if method is not correctly type attributed
                acc.getUnsafeVars().add(var);
            }
        });
        Set<UnresolvedVar> unresolvedVars = methodUnresolvedReferencedVars.remove(method.getMethodType());
        if (unresolvedVars != null) {
            unresolvedVars.forEach(var -> {
                NamedVariable namedVar = acc.getVarTable().getVarByName(var.getDeclaringType(), var.getVarName());
                if (namedVar != null) {
                    methodReferencedVars.computeIfAbsent(method.getMethodType(), k -> new HashSet<>()).add(namedVar);
                }
            });
        }
        return super.visitMethodDeclaration(method, ctx);
    }

    @Override
    public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
        if (!isJodaExpr(method) || method.getMethodType().getDeclaringType().isAssignableFrom(JODA_CLASS_PATTERN)) {
            return super.visitMethodInvocation(method, ctx);
        }
        Cursor boundary = findBoundaryCursorForJodaExpr(getCursor());
        J j = new JodaTimeVisitor(new JodaTimeRecipe.Accumulator(), false, scopes)
                .visit(boundary.getValue(), ctx, boundary.getParentTreeCursor());

        boolean isSafe = j != boundary.getValue();
        acc.getSafeMethodMap().compute(method.getMethodType(), (k, v) -> v == null ? isSafe : v && isSafe);
        J parent = boundary.getParentTreeCursor().getValue();
        if (parent instanceof NamedVariable) {
            methodReferencedVars.computeIfAbsent(method.getMethodType(), k -> new HashSet<>())
                    .add((NamedVariable) parent);
        }
        if (parent instanceof J.Assignment) {
            J.Assignment assignment = (J.Assignment) parent;
            if (assignment.getVariable() instanceof J.Identifier) {
                J.Identifier ident = (J.Identifier) assignment.getVariable();
                findVarInScope(ident.getSimpleName())
                        .map(var -> methodReferencedVars.computeIfAbsent(method.getMethodType(), k -> new HashSet<>()).add(var));
            }
        }
        if (parent instanceof MethodCall) {
            MethodCall parentMethod = (MethodCall) parent;
            int argPos = parentMethod.getArguments().indexOf(boundary.getValue());
            if (argPos == -1) {
                return method;
            }
            String paramName = parentMethod.getMethodType().getParameterNames().get(argPos);
            NamedVariable var = acc.getVarTable().getVarByName(parentMethod.getMethodType(), paramName);
            if (var != null) {
                methodReferencedVars.computeIfAbsent(method.getMethodType(), k -> new HashSet<>()).add(var);
            } else {
                methodUnresolvedReferencedVars.computeIfAbsent(method.getMethodType(), k -> new HashSet<>())
                        .add(new UnresolvedVar(parentMethod.getMethodType(), paramName));
            }
        }
        return method;
    }

    @Override
    public J.Return visitReturn(J.Return _return, ExecutionContext ctx) {
        if (_return.getExpression() == null) {
            return _return;
        }
        Expression expr = _return.getExpression();
        if (!isJodaExpr(expr)) {
            return _return;
        }
        J methodOrLambda = getCursor().dropParentUntil(j -> j instanceof J.MethodDeclaration || j instanceof J.Lambda).getValue();
        if (methodOrLambda instanceof J.Lambda) {
            return _return;
        }
        J.MethodDeclaration method = (J.MethodDeclaration) methodOrLambda;
        Expression updatedExpr = (Expression) new JodaTimeVisitor(acc, true, scopes)
                .visit(expr, ctx, getCursor().getParentTreeCursor());
        boolean isSafe = !isJodaExpr(updatedExpr);

        addReferencedVars(expr, method.getMethodType());
        acc.getSafeMethodMap().compute(method.getMethodType(), (k, v) -> v == null ? isSafe : v && isSafe);
        if (!isSafe) {
            acc.getUnsafeVars().addAll(methodReferencedVars.get(method.getMethodType()));
        }
        return _return;
    }

    private void processMarkersOnExpression(List<Expression> expressions, NamedVariable var) {
        for (Expression expr : expressions) {
            Optional<SafeCheckMarker> mayBeMarker = expr.getMarkers().findFirst(SafeCheckMarker.class);
            if (!mayBeMarker.isPresent()) {
                continue;
            }
            SafeCheckMarker marker = mayBeMarker.get();
            if (!marker.isSafe()) {
                acc.getUnsafeVars().add(var);
            }
            if (!marker.getReferences().isEmpty()) {
                varDependencies.compute(var, (k, v) -> v == null ? new HashSet<>() : v).addAll(marker.getReferences());
                for (NamedVariable ref : marker.getReferences()) {
                    varDependencies.compute(ref, (k, v) -> v == null ? new HashSet<>() : v).add(var);
                }
            }
        }
    }

    /**
     * Traverses the cursor to find the first non-Joda expression in the path.
     * If no non-Joda expression is found, it returns the cursor pointing
     * to the last Joda expression whose parent is not an Expression.
     */
    private static Cursor findBoundaryCursorForJodaExpr(Cursor cursor) {
        while (cursor.getValue() instanceof Expression && isJodaExpr(cursor.getValue())) {
            Cursor parent = cursor.getParentTreeCursor();
            if (parent.getValue() instanceof J && !(parent.getValue() instanceof Expression)) {
                return cursor;
            }
            cursor = parent;
        }
        return cursor;
    }

    private static boolean isJodaExpr(Expression expression) {
        return expression.getType() != null && expression.getType().isAssignableFrom(JODA_CLASS_PATTERN);
    }

    private List<Expression> findSinks(Cursor cursor) {
        Option<SinkFlowSummary> mayBeSinks = Dataflow.startingAt(cursor).findSinks(new JodaTimeFlowSpec());
        if (mayBeSinks.isNone()) {
            return Collections.emptyList();
        }
        return mayBeSinks.some().getExpressionSinks();
    }

    private boolean isClassVar(NamedVariable variable) {
        return variable.getVariableType().getOwner() instanceof JavaType.Class;
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

    private void addReferencedVars(Expression expr, JavaType.Method method) {
        Set<@Nullable NamedVariable> referencedVars = new HashSet<>();
        new FindVarReferences().visit(expr, referencedVars, getCursor().getParentTreeCursor());
        referencedVars.remove(null);
        methodReferencedVars.computeIfAbsent(method, k -> new HashSet<>()).addAll(referencedVars);
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
            SafeCheckMarker marker = getMarker(expression, ctx);
            if (!marker.isSafe()) {
                Optional<Cursor> mayBeArgCursor = findArgumentExprCursor();
                if (mayBeArgCursor.isPresent()) {
                    MethodCall parentMethod = mayBeArgCursor.get().getParentTreeCursor().getValue();
                    int argPos = parentMethod.getArguments().indexOf(mayBeArgCursor.get().getValue());
                    String paramName = parentMethod.getMethodType().getParameterNames().get(argPos);
                    unsafeVarsByType.computeIfAbsent(parentMethod.getMethodType(), k -> new HashSet<>()).add(paramName);
                }
            }
            Expression withMarker = expression.withMarkers(expression.getMarkers().addIfAbsent(marker));
            expressions.set(index, withMarker);
            return withMarker;
        }

        private SafeCheckMarker getMarker(Expression expr, ExecutionContext ctx) {
            Optional<SafeCheckMarker> mayBeMarker = expr.getMarkers().findFirst(SafeCheckMarker.class);
            if (mayBeMarker.isPresent()) {
                return mayBeMarker.get();
            }

            Cursor boundary = findBoundaryCursorForJodaExpr(getCursor());
            boolean isSafe = true;
            if (boundary.getParentTreeCursor().getValue() instanceof J.Return) {
                // TODO: handle return statement in lambda
                isSafe = boundary.dropParentUntil(j -> j instanceof J.MethodDeclaration || j instanceof J.Lambda)
                  .getValue() instanceof J.MethodDeclaration;
            }
            Expression boundaryExpr = boundary.getValue();
            J j = new JodaTimeVisitor(new JodaTimeRecipe.Accumulator(), false, scopes)
                    .visit(boundaryExpr, ctx, boundary.getParentTreeCursor());
            Set<@Nullable NamedVariable> referencedVars = new HashSet<>();
            new FindVarReferences().visit(expr, referencedVars, getCursor().getParentTreeCursor());
            AtomicBoolean hasJodaType = new AtomicBoolean();
            new HasJodaType().visit(j, hasJodaType);
            isSafe = isSafe && !hasJodaType.get() && !referencedVars.contains(null);
            referencedVars.remove(null);
            return new SafeCheckMarker(UUID.randomUUID(), isSafe, referencedVars);
        }

        private Optional<Cursor> findArgumentExprCursor() {
            Cursor cursor = getCursor();
            while (cursor.getValue() instanceof Expression && isJodaExpr(cursor.getValue())) {
                Cursor parentCursor = cursor.getParentTreeCursor();
                if (parentCursor.getValue() instanceof MethodCall &&
                    ((MethodCall) parentCursor.getValue()).getArguments().contains(cursor.getValue())) {
                    return Optional.of(cursor);
                }
                cursor = parentCursor;
            }
            return Optional.empty();
        }
    }

    private class FindVarReferences extends JavaIsoVisitor<Set<@Nullable NamedVariable>> {

        @Override
        public J.Identifier visitIdentifier(J.Identifier ident, Set<@Nullable NamedVariable> vars) {
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

    @Value
    private static class UnresolvedVar {
        JavaType declaringType;
        String varName;
    }
}
