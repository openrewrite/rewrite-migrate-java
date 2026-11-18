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
package org.openrewrite.java.migrate.lombok.log;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.EqualsAndHashCode;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.*;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.trait.Scope;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import static java.util.Comparator.comparing;

@EqualsAndHashCode(callSuper = false)
public class LogVisitor extends JavaIsoVisitor<ExecutionContext> {

    private static final String RENAMED_TO_LOG = LogVisitor.class.getName() + ".renamedToLog";

    private final String logType;
    private final String factoryType;
    private final MethodMatcher factoryMethodMatcher;
    private final String logAnnotation;
    @Nullable
    private final String fieldName;

    LogVisitor(String logType, String factoryMethodPattern, String logAnnotation, @Nullable String fieldName) {
        this.logType = logType;
        this.factoryType = factoryMethodPattern.substring(0, factoryMethodPattern.indexOf(' '));
        this.factoryMethodMatcher = new MethodMatcher(factoryMethodPattern);
        this.logAnnotation = logAnnotation;
        this.fieldName = fieldName;
    }

    TreeVisitor<?, ExecutionContext> withPrecondition() {
        return Preconditions.check(new UsesMethod<>(factoryMethodMatcher), this);
    }

    @Override
    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
        J.ClassDeclaration visitClassDeclaration = super.visitClassDeclaration(classDecl, ctx);
        if (visitClassDeclaration != classDecl) {
            maybeRemoveImport(logType);
            maybeRemoveImport(factoryType);
            maybeAddImport(logAnnotation);
            return JavaTemplate
                    .builder("@" + logAnnotation.substring(logAnnotation.lastIndexOf('.') + 1) + "\n")
                    .javaParser(JavaParser.fromJavaVersion().classpathFromResources(ctx, "lombok"))
                    .imports(logAnnotation)
                    .build()
                    .apply(
                            updateCursor(visitClassDeclaration),
                            visitClassDeclaration.getCoordinates().addAnnotation(comparing(J.Annotation::getSimpleName)));
        }
        return classDecl;
    }

    @Override
    public J.@Nullable VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
        if (!multiVariable.hasModifier(J.Modifier.Type.Private) ||
                !multiVariable.hasModifier(J.Modifier.Type.Static) ||
                !multiVariable.hasModifier(J.Modifier.Type.Final) ||
                multiVariable.getVariables().size() != 1 ||
                !TypeUtils.isAssignableTo(logType, multiVariable.getType())) {
            return multiVariable;
        }

        // name needs to match the name of the field that lombok creates
        J.VariableDeclarations.NamedVariable var = multiVariable.getVariables().get(0);
        if (fieldName != null && !fieldName.equals(var.getSimpleName())) {
            return multiVariable;
        }

        if (!factoryMethodMatcher.matches(var.getInitializer())) {
            return multiVariable;
        }

        J.ClassDeclaration classDeclaration = getCursor().firstEnclosing(J.ClassDeclaration.class);
        if (classDeclaration == null || classDeclaration.getType() == null) {
            return multiVariable;
        }

        J.MethodInvocation methodCall = (J.MethodInvocation) var.getInitializer();
        if (methodCall.getArguments().size() != 1 ||
                !isFactoryParameter(methodCall.getArguments().get(0), classDeclaration.getType())) {
            return multiVariable;
        }

        if (!"log".equals(var.getSimpleName())) {
            Scope.Binding binding = Scope.bindingOf(getCursor(), var).orElse(null);
            List<Scope.Binding> renamed = getCursor().dropParentUntil(JavaSourceFile.class::isInstance)
                    .computeMessageIfAbsent(RENAMED_TO_LOG, k -> new ArrayList<>());
            if (binding == null || !binding.canRenameTo("log") ||
                renamed.stream().anyMatch(other -> shadows(binding, other) || shadows(other, binding))) {
                return multiVariable;
            }
            renamed.add(binding);
            doAfterVisit(new ChangeFieldName<>(classDeclaration.getType().getFullyQualifiedName(), var.getSimpleName(), "log"));
        }

        return null;
    }

    /**
     * Whether the inner logger's {@code log} would hide the outer one from code in the inner logger's class that
     * reads the outer one. {@link Scope.Binding#canRenameTo(String)} cannot tell when both are renamed in one visit,
     * as it sees neither rename.
     */
    private static boolean shadows(Scope.Binding inner, Scope.Binding outer) {
        UUID outerClass = outer.getScope().getTree().getId();
        return inner.getScope().getCursor().getPathAsStream()
                       .anyMatch(tree -> tree instanceof J && ((J) tree).getId().equals(outerClass)) &&
               outer.references(inner.getScope().getCursor()).findAny().isPresent();
    }

    protected boolean isFactoryParameter(Expression argument, JavaType.FullyQualified classType) {
        return isClassLiteral(argument, classType);
    }

    static boolean isClassLiteral(@Nullable Expression expression, JavaType.FullyQualified classType) {
        if (!(expression instanceof J.FieldAccess)) {
            return false;
        }
        J.FieldAccess fieldAccess = (J.FieldAccess) expression;
        return "class".equals(fieldAccess.getSimpleName()) &&
                TypeUtils.isOfClassType(fieldAccess.getTarget().getType(), classType.getFullyQualifiedName());
    }
}
