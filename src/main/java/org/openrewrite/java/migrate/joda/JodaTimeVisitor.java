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

import org.openrewrite.ExecutionContext;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.migrate.joda.templates.*;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.migrate.joda.templates.DateTimeFormatTemplates;
import org.openrewrite.java.migrate.joda.templates.DateTimeTemplates;
import org.openrewrite.java.migrate.joda.templates.DurationTemplates;
import org.openrewrite.java.migrate.joda.templates.MethodTemplate;
import org.openrewrite.java.migrate.joda.templates.TimeZoneTemplates;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.MethodCall;
import org.openrewrite.java.tree.TypeUtils;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.openrewrite.java.migrate.joda.templates.TimeClassNames.*;


public class JodaTimeVisitor extends JavaVisitor<ExecutionContext> {

  private final MethodMatcher anyNewDateTime = new MethodMatcher(JODA_DATE_TIME + "<constructor>(..)");
  private final MethodMatcher anyDateTime = new MethodMatcher(JODA_DATE_TIME + " *(..)");
  private final MethodMatcher zoneFor = new MethodMatcher(JODA_DATE_TIME_ZONE + " for*(..)");
  private final MethodMatcher anyTimeFormatter = new MethodMatcher(JODA_TIME_FORMAT + " *(..)");
  private final MethodMatcher anyNewDuration = new MethodMatcher(JODA_DURATION + "<constructor>(..)");
  private final MethodMatcher anyDuration = new MethodMatcher(JODA_DURATION + " *(..)");

  @Override
  public J visitCompilationUnit(@NonNull J.CompilationUnit cu, @NonNull ExecutionContext ctx) {
    maybeRemoveImport(JODA_DATE_TIME);
    maybeRemoveImport(JODA_DATE_TIME_ZONE);
    maybeRemoveImport(JODA_TIME_FORMAT);
    maybeRemoveImport(JODA_DURATION);
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
    return super.visitCompilationUnit(cu, ctx);
  }

  @Override
  public J visitVariable(@NonNull J.VariableDeclarations.NamedVariable variable, @NonNull ExecutionContext ctx) {
    // TODO implement logic for safe variable migration
    if (variable.getType().isAssignableFrom(JODA_CLASS_PATTERN)) {
      return variable;
    }
    return super.visitVariable(variable, ctx);
  }

  @Override
  public J visitNewClass(@NonNull J.NewClass newClass, @NonNull ExecutionContext ctx) {
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
  public J visitMethodInvocation(@NonNull J.MethodInvocation method, @NonNull ExecutionContext ctx) {
    J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
    if (hasJodaType(m.getArguments()) || isVarRef(m.getSelect())) {
      return method;
    }
    if (zoneFor.matches(method)) {
      return applyTemplate(method, m, TimeZoneTemplates.getTemplates()).orElse(method);
    }
    if (anyDateTime.matches(method)) {
      return applyTemplate(method, m, DateTimeTemplates.getTemplates()).orElse(method);
    }
    if (anyTimeFormatter.matches(method)) {
      return applyTemplate(method, m, DateTimeFormatTemplates.getTemplates()).orElse(method);
    }
    if (anyDuration.matches(method)) {
      return applyTemplate(method, m, DurationTemplates.getTemplates()).orElse(method);
    }
    if (areArgumentsAssignable(m)) {
      return m;
    }
    return method;
  }

  @Override
  public J visitFieldAccess(@NonNull J.FieldAccess fieldAccess, @NonNull ExecutionContext ctx) {
    J.FieldAccess f = (J.FieldAccess) super.visitFieldAccess(fieldAccess, ctx);
    if (TypeUtils.isOfClassType(f.getType(), JODA_DATE_TIME_ZONE) && f.getSimpleName().equals("UTC")) {
      return JavaTemplate.builder("ZoneOffset.UTC")
          .imports(JAVA_ZONE_OFFSET)
          .build()
          .apply(updateCursor(f), f.getCoordinates().replace());
    }
    return f;
  }

  private boolean hasJodaType(List<Expression> exprs) {
    for (Expression expr : exprs) {
      JavaType exprType = expr.getType();
      if (exprType != null && exprType.isAssignableFrom(Pattern.compile("org.joda.time.*"))) {
        return true;
      }
    }
    return false;
  }

  private Optional<MethodCall> applyTemplate(MethodCall original, MethodCall updated, List<MethodTemplate> templates) {
    for (MethodTemplate template : templates) {
      if (template.getMatcher().matches(original)) {
        Expression[] args = template.getTemplateArgsFunc().apply(updated);
        if (args.length == 0) {
          return Optional.of(template.getTemplate().apply(updateCursor(updated), updated.getCoordinates().replace()));
        }
        return Optional.of(template.getTemplate().apply(updateCursor(updated), updated.getCoordinates().replace(), args));
      }
    }
    return Optional.empty(); // unhandled case
  }

  private boolean areArgumentsAssignable(MethodCall m) {
    if (m.getArguments().size() != m.getMethodType().getParameterTypes().size()) {
      return false;
    }
    for (int i = 0; i < m.getArguments().size(); i++) {
      if (!TypeUtils.isAssignableTo(m.getMethodType().getParameterTypes().get(i), m.getArguments().get(i).getType())) {
        return false;
      }
    }
    return true;
  }

  private boolean isVarRef(Expression expr) {
    if (expr instanceof J.FieldAccess) {
      return ((J.FieldAccess) expr).getName().getFieldType() != null;
    }
    if (expr instanceof J.Identifier) {
      return ((J.Identifier) expr).getFieldType() != null;
    }
    return false;
  }
}
