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

import lombok.NonNull;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavadocVisitor;
import org.openrewrite.java.migrate.joda.templates.AllTemplates;
import org.openrewrite.java.migrate.joda.templates.MethodTemplate;
import org.openrewrite.java.migrate.joda.templates.TimeClassMap;
import org.openrewrite.java.migrate.joda.templates.VarTemplates;
import org.openrewrite.java.tree.*;
import org.openrewrite.java.tree.J.VariableDeclarations.NamedVariable;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static org.openrewrite.java.migrate.joda.templates.TimeClassNames.*;

class JodaTimeVisitor extends ScopeAwareVisitor {

    private final boolean safeMigration;
    private final JodaTimeRecipe.Accumulator acc;

    public JodaTimeVisitor(JodaTimeRecipe.Accumulator acc, boolean safeMigration, LinkedList<VariablesInScope> scopes) {
        super(scopes);
        this.acc = acc;
        this.safeMigration = safeMigration;
    }

    @Override
    protected JavadocVisitor<ExecutionContext> getJavadocVisitor() {
        return new JavadocVisitor<ExecutionContext>(this) {
            /**
             * Do not visit the method referenced from the Javadoc, may cause recipe to fail.
             */
            @Override
            public Javadoc visitReference(Javadoc.Reference reference, ExecutionContext ctx) {
                return reference;
            }
        };
    }

    @Override
    public @NonNull J visitCompilationUnit(@NonNull J.CompilationUnit cu, @NonNull ExecutionContext ctx) {
        maybeRemoveImport(JODA_DATE_TIME);
        maybeRemoveImport(JODA_DATE_TIME_ZONE);
        maybeRemoveImport(JODA_TIME_FORMAT);
        maybeRemoveImport(JODA_DURATION);
        maybeRemoveImport(JODA_ABSTRACT_INSTANT);
        maybeRemoveImport(JODA_INSTANT);
        maybeRemoveImport("java.util.Locale");

        maybeAddImport(JAVA_DATE_TIME);
        maybeAddImport(JAVA_ZONE_OFFSET);
        maybeAddImport(JAVA_ZONE_ID);
        maybeAddImport(JAVA_INSTANT);
        maybeAddImport(JAVA_TIME_FORMATTER);
        maybeAddImport(JAVA_TIME_FORMAT_STYLE);
        maybeAddImport(JAVA_DURATION);
        maybeAddImport(JAVA_LOCAL_DATE);
        maybeAddImport(JAVA_LOCAL_TIME);
        maybeAddImport(JAVA_TEMPORAL_ISO_FIELDS);
        maybeAddImport(JAVA_CHRONO_FIELD);
        maybeAddImport(JAVA_UTIL_DATE);
        return super.visitCompilationUnit(cu, ctx);
    }

    @Override
    public @NonNull J visitVariableDeclarations(@NonNull J.VariableDeclarations multiVariable, @NonNull ExecutionContext ctx) {
        if (multiVariable.getTypeExpression() == null || !multiVariable.getType().isAssignableFrom(JODA_CLASS_PATTERN)) {
            return super.visitVariableDeclarations(multiVariable, ctx);
        }
        if (multiVariable.getVariables().stream().anyMatch(acc.getUnsafeVars()::contains)) {
            return multiVariable;
        }
        J.VariableDeclarations m = (J.VariableDeclarations) super.visitVariableDeclarations(multiVariable, ctx);
        return VarTemplates.getTemplate(multiVariable).<J>map(t -> t.apply(
                updateCursor(m),
                m.getCoordinates().replace(),
                VarTemplates.getTemplateArgs(m))).orElse(multiVariable);
    }

    @Override
    public @NonNull J visitVariable(@NonNull J.VariableDeclarations.NamedVariable variable, @NonNull ExecutionContext ctx) {
        if (!variable.getType().isAssignableFrom(JODA_CLASS_PATTERN)) {
            return super.visitVariable(variable, ctx);
        }
        if (acc.getUnsafeVars().contains(variable) || !(variable.getType() instanceof JavaType.Class)) {
            return variable;
        }
        JavaType.Class jodaType = (JavaType.Class) variable.getType();
        return variable
                .withType(TimeClassMap.getJavaTimeType(jodaType.getFullyQualifiedName()))
                .withInitializer((Expression) visit(variable.getInitializer(), ctx));
    }

    @Override
    public @NonNull J visitAssignment(@NonNull J.Assignment assignment, @NonNull ExecutionContext ctx) {
        J.Assignment a = (J.Assignment) super.visitAssignment(assignment, ctx);
        if (!a.getType().isAssignableFrom(JODA_CLASS_PATTERN)) {
            return a;
        }
        if (!(a.getVariable() instanceof J.Identifier)) {
            return assignment;
        }
        J.Identifier varName = (J.Identifier) a.getVariable();
        Optional<NamedVariable> mayBeVar = findVarInScope(varName.getSimpleName());
        if (!mayBeVar.isPresent() || acc.getUnsafeVars().contains(mayBeVar.get())) {
            return assignment;
        }
        return VarTemplates.getTemplate(assignment).<J>map(t -> t.apply(
                updateCursor(a),
                a.getCoordinates().replace(),
                varName,
                a.getAssignment())).orElse(assignment);
    }

    @Override
    public @NonNull J visitNewClass(@NonNull J.NewClass newClass, @NonNull ExecutionContext ctx) {
        MethodCall updated = (MethodCall) super.visitNewClass(newClass, ctx);
        if (hasJodaType(updated.getArguments())) {
            return newClass;
        }
        return migrateMethodCall(newClass, updated);
    }


    @Override
    public @NonNull J visitMethodInvocation(@NonNull J.MethodInvocation method, @NonNull ExecutionContext ctx) {
        J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
        if (hasJodaType(m.getArguments()) || isJodaVarRef(m.getSelect())) {
            return method;
        }
        return migrateMethodCall(method, m);
    }

    @Override
    public @NonNull J visitFieldAccess(@NonNull J.FieldAccess fieldAccess, @NonNull ExecutionContext ctx) {
        J.FieldAccess f = (J.FieldAccess) super.visitFieldAccess(fieldAccess, ctx);
        if (TypeUtils.isOfClassType(f.getType(), JODA_DATE_TIME_ZONE) && f.getSimpleName().equals("UTC")) {
            return JavaTemplate.builder("ZoneOffset.UTC")
                    .imports(JAVA_ZONE_OFFSET)
                    .build()
                    .apply(updateCursor(f), f.getCoordinates().replace());
        }
        return f;
    }

    @Override
    public @NonNull J visitIdentifier(@NonNull J.Identifier ident, @NonNull ExecutionContext ctx) {
        if (!isJodaVarRef(ident)) {
            return super.visitIdentifier(ident, ctx);
        }
        Optional<NamedVariable> mayBeVar = findVarInScope(ident.getSimpleName());
        if (!mayBeVar.isPresent() || acc.getUnsafeVars().contains(mayBeVar.get())) {
            return ident;
        }

        JavaType.FullyQualified jodaType = ((JavaType.Class) ident.getType());
        JavaType.FullyQualified fqType = TimeClassMap.getJavaTimeType(jodaType.getFullyQualifiedName());

        return ident.withType(fqType)
                .withFieldType(ident.getFieldType().withType(fqType));
    }

    private J migrateMethodCall(MethodCall original, MethodCall updated) {
        if (!original.getMethodType().getDeclaringType().isAssignableFrom(JODA_CLASS_PATTERN)) {
            return updated; // not a joda type, no need to migrate
        }
        MethodTemplate template = AllTemplates.getTemplate(original);
        if (template == null) {
            return original; // unhandled case
        }
        Optional<J> maybeUpdated = applyTemplate(original, updated, template);
        if (!maybeUpdated.isPresent()) {
            return original; // unhandled case
        }
        Expression updatedExpr = (Expression) maybeUpdated.get();
        if (!safeMigration || !isArgument(original)) {
            return updatedExpr;
        }
        // this expression is an argument to a method call
        MethodCall parentMethod = getCursor().getParentTreeCursor().getValue();
        if (parentMethod.getMethodType().getDeclaringType().isAssignableFrom(JODA_CLASS_PATTERN)) {
            return updatedExpr;
        }
        int argPos = parentMethod.getArguments().indexOf(original);
        JavaType paramType = parentMethod.getMethodType().getParameterTypes().get(argPos);
        if (TypeUtils.isAssignableTo(paramType, updatedExpr.getType())) {
            return updatedExpr;
        }
        String paramName = parentMethod.getMethodType().getParameterNames().get(argPos);
        NamedVariable var = acc.getVarTable().getVarByName(parentMethod.getMethodType(), paramName);
        if (var != null && !acc.getUnsafeVars().contains(var)) {
            return updatedExpr;
        }
        return original;
    }

    private boolean hasJodaType(List<Expression> exprs) {
        for (Expression expr : exprs) {
            JavaType exprType = expr.getType();
            if (exprType != null && exprType.isAssignableFrom(JODA_CLASS_PATTERN)) {
                return true;
            }
        }
        return false;
    }

    private Optional<J> applyTemplate(MethodCall original, MethodCall updated, MethodTemplate template) {
        if (template.getMatcher().matches(original)) {
            Expression[] args = template.getTemplateArgsFunc().apply(updated);
            if (args.length == 0) {
                return Optional.of(template.getTemplate().apply(updateCursor(updated), updated.getCoordinates().replace()));
            }
            return Optional.of(template.getTemplate().apply(updateCursor(updated), updated.getCoordinates().replace(), (Object[]) args));
        }
        return Optional.empty(); // unhandled case
    }

    private boolean isJodaVarRef(@Nullable Expression expr) {
        if (expr == null || expr.getType() == null || !expr.getType().isAssignableFrom(JODA_CLASS_PATTERN)) {
            return false;
        }
        if (expr instanceof J.FieldAccess) {
            return ((J.FieldAccess) expr).getName().getFieldType() != null;
        }
        if (expr instanceof J.Identifier) {
            return ((J.Identifier) expr).getFieldType() != null;
        }
        return false;
    }

    private boolean isArgument(J expr) {
        if (!(getCursor().getParentTreeCursor().getValue() instanceof MethodCall)) {
            return false;
        }
        MethodCall methodCall = getCursor().getParentTreeCursor().getValue();
        return methodCall.getArguments().contains(expr);
    }
}
