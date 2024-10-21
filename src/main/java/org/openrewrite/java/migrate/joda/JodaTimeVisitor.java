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
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.migrate.joda.templates.*;
import org.openrewrite.java.tree.*;

import java.util.List;
import java.util.Optional;

import static org.openrewrite.java.migrate.joda.templates.TimeClassNames.*;

public class JodaTimeVisitor extends JavaVisitor<ExecutionContext> {

    private final MethodMatcher anyNewDateTime = new MethodMatcher(JODA_DATE_TIME + "<constructor>(..)");
    private final MethodMatcher anyDateTime = new MethodMatcher(JODA_DATE_TIME + " *(..)");
    private final MethodMatcher anyBaseDateTime = new MethodMatcher(JODA_BASE_DATE_TIME + " *(..)");
    private final MethodMatcher zoneFor = new MethodMatcher(JODA_DATE_TIME_ZONE + " for*(..)");
    private final MethodMatcher anyTimeFormatter = new MethodMatcher(JODA_TIME_FORMAT + " *(..)");
    private final MethodMatcher anyNewDuration = new MethodMatcher(JODA_DURATION + "<constructor>(..)");
    private final MethodMatcher anyDuration = new MethodMatcher(JODA_DURATION + " *(..)");
    private final MethodMatcher anyAbstractInstant = new MethodMatcher(JODA_ABSTRACT_INSTANT + " *(..)");

    private boolean scanMode;

    public JodaTimeVisitor(boolean scanMode) {
        this.scanMode = scanMode;
    }

    public JodaTimeVisitor() {
        this(false);
    }


    @Override
    public @NonNull J visitCompilationUnit(@NonNull J.CompilationUnit cu, @NonNull ExecutionContext ctx) {
        maybeRemoveImport(JODA_DATE_TIME);
        maybeRemoveImport(JODA_DATE_TIME_ZONE);
        maybeRemoveImport(JODA_TIME_FORMAT);
        maybeRemoveImport(JODA_DURATION);
        maybeRemoveImport(JODA_ABSTRACT_INSTANT);
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
    public @NonNull J visitVariable(@NonNull J.VariableDeclarations.NamedVariable variable, @NonNull ExecutionContext ctx) {
        // TODO implement logic for safe variable migration
        if (variable.getType().isAssignableFrom(JODA_CLASS_PATTERN)) {
            return variable;
        }
        return super.visitVariable(variable, ctx);
    }

    @Override
    public @NonNull J visitAssignment(@NonNull J.Assignment assignment, @NonNull ExecutionContext ctx) {
        J.Assignment a = (J.Assignment) super.visitAssignment(assignment, ctx);
        return a.withType(a.getVariable().getType());
    }

    @Override
    public @NonNull J visitNewClass(@NonNull J.NewClass newClass, @NonNull ExecutionContext ctx) {
        MethodCall updated = (MethodCall) super.visitNewClass(newClass, ctx);
        if (hasJodaType(updated.getArguments())) {
            return newClass;
        }
        if (anyNewDateTime.matches(newClass)) {
            return applyTemplate(newClass, updated, DateTimeTemplates.getTemplates()).orElse(newClass);
        }
        if (anyNewDuration.matches(newClass)) {
            return applyTemplate(newClass, updated, DurationTemplates.getTemplates()).orElse(newClass);
        }
        if (areArgumentsAssignable(updated)) {
            return updated;
        }
        return newClass;
    }


    @Override
    public @NonNull J visitMethodInvocation(@NonNull J.MethodInvocation method, @NonNull ExecutionContext ctx) {
        J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
        if (hasJodaType(m.getArguments()) || isJodaVarRef(m.getSelect())) {
            return method;
        }
        if (zoneFor.matches(method)) {
            return applyTemplate(method, m, TimeZoneTemplates.getTemplates()).orElse(method);
        }
        if (anyDateTime.matches(method) || anyBaseDateTime.matches(method)) {
            return applyTemplate(method, m, DateTimeTemplates.getTemplates()).orElse(method);
        }
        if (anyAbstractInstant.matches(method)) {
            return applyTemplate(method, m, AbstractInstantTemplates.getTemplates()).orElse(method);
        }
        if (anyTimeFormatter.matches(method)) {
            return applyTemplate(method, m, DateTimeFormatTemplates.getTemplates()).orElse(method);
        }
        if (anyDuration.matches(method)) {
            return applyTemplate(method, m, DurationTemplates.getTemplates()).orElse(method);
        }
        if (method.getSelect() != null &&
                method.getSelect().getType() != null &&
                method.getSelect().getType().isAssignableFrom(JODA_CLASS_PATTERN)) {
            return method; // unhandled case
        }
        if (areArgumentsAssignable(m)) {
            return m;
        }
        return method;
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
        if (!(isJodaVarRef(ident) && scanMode)) {
            return super.visitIdentifier(ident, ctx);
        }

        // TODO: support migration for class variables
        if (!(ident.getType() instanceof JavaType.Class)) {
            return ident;
        }

        JavaType.FullyQualified jodaType = ((JavaType.Class) ident.getType());
        JavaType.FullyQualified fqType = TimeClassMap.getJavaTimeType(jodaType.getFullyQualifiedName());

        return ident.withType(fqType)
                .withFieldType(ident.getFieldType().withType(fqType));
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

    private Optional<J> applyTemplate(MethodCall original, MethodCall updated, List<MethodTemplate> templates) {
        for (MethodTemplate template : templates) {
            if (template.getMatcher().matches(original)) {
                Expression[] args = template.getTemplateArgsFunc().apply(updated);
                if (args.length == 0) {
                    return Optional.of(template.getTemplate().apply(updateCursor(updated), updated.getCoordinates().replace()));
                }
                return Optional.of(template.getTemplate().apply(updateCursor(updated), updated.getCoordinates().replace(), (Object[]) args));
            }
        }
        return Optional.empty(); // unhandled case
    }

    private boolean areArgumentsAssignable(MethodCall m) {
        if (m.getMethodType() == null || getArgumentsCount(m) != m.getMethodType().getParameterTypes().size()) {
            return false;
        }
        if (getArgumentsCount(m) == 0) {
            return true;
        }
        for (int i = 0; i < m.getArguments().size(); i++) {
            if (!TypeUtils.isAssignableTo(m.getMethodType().getParameterTypes().get(i), m.getArguments().get(i).getType())) {
                return false;
            }
        }
        return true;
    }

    private int getArgumentsCount(MethodCall m) {
        if (m.getArguments().size() == 1 && m.getArguments().get(0) instanceof J.Empty) {
            return 0;
        }
        return m.getArguments().size();
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
}
