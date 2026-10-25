/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.java.migrate.lombok;

import lombok.AccessLevel;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.marker.CompactConstructor;
import org.openrewrite.java.tree.*;

import java.util.ArrayList;
import java.util.List;

import static lombok.AccessLevel.*;
import static org.openrewrite.java.tree.J.Modifier.Type.*;

class LombokUtils {

    static boolean isGetter(Cursor cursor) {
        if (!(cursor.getValue() instanceof J.MethodDeclaration)) {
            return false;
        }
        J.MethodDeclaration method = cursor.getValue();
        if (method.getMethodType() == null) {
            return false;
        }
        if (method.getMarkers().findFirst(CompactConstructor.class).isPresent() || !(method.getParameters().get(0) instanceof J.Empty) || method.getReturnTypeExpression() == null) {
            return false;
        }
        if (method.getBody() == null ||
                method.getBody().getStatements().size() != 1 ||
                !(method.getBody().getStatements().get(0) instanceof J.Return)) {
            return false;
        }
        JavaType.FullyQualified declaringType = method.getMethodType().getDeclaringType();
        Expression returnExpression = ((J.Return) method.getBody().getStatements().get(0)).getExpression();
        if (returnExpression instanceof J.Identifier) {
            J.Identifier identifier = (J.Identifier) returnExpression;
            if (identifier.getFieldType() != null && declaringType == identifier.getFieldType().getOwner()) {
                // Don't replace instance methods accessing static fields
                if (identifier.getFieldType().hasFlags(Flag.Static)) {
                    return false;
                }
                return hasMatchingTypeAndGetterName(method, identifier.getType(), identifier.getSimpleName());
            }
        } else if (returnExpression instanceof J.FieldAccess) {
            J.FieldAccess fieldAccess = (J.FieldAccess) returnExpression;
            Expression target = fieldAccess.getTarget();
            // Only handle "this.field" pattern, not "someObject.field"
            if (target instanceof J.Identifier && "this".equals(((J.Identifier) target).getSimpleName())) {
                if (fieldAccess.getName().getFieldType() != null &&
                        declaringType == fieldAccess.getName().getFieldType().getOwner()) {
                    // Don't replace instance methods accessing static fields
                    if (fieldAccess.getName().getFieldType().hasFlags(Flag.Static)) {
                        return false;
                    }
                    return hasMatchingTypeAndGetterName(method, fieldAccess.getType(), fieldAccess.getSimpleName());
                }
            }
        }
        return false;
    }

    private static boolean hasMatchingTypeAndGetterName(J.MethodDeclaration method, @Nullable JavaType type, String simpleName) {
        if (method.getType() == type) {
            String deriveGetterMethodName = deriveGetterMethodName(type, simpleName);
            return method.getSimpleName().equals(deriveGetterMethodName);
        }
        return false;
    }

    public static boolean isEffectivelyGetter(J.MethodDeclaration method) {
        if (!method.getParameters().isEmpty() && !(method.getParameters().get(0) instanceof J.Empty)) {
            return false;
        }
        if (method.getBody() == null ||
                method.getBody().getStatements().size() != 1 ||
                !(method.getBody().getStatements().get(0) instanceof J.Return)) {
            return false;
        }
        Expression returnExpression = ((J.Return) method.getBody().getStatements().get(0)).getExpression();
        if (!(returnExpression instanceof J.Identifier) && !(returnExpression instanceof J.FieldAccess)) {
            return false;
        }
        // compiler already guarantees that the returned variable is a subtype of the method type, but we want an exact match
        return method.getType() == returnExpression.getType();
    }

    public static String deriveGetterMethodName(@Nullable JavaType type, String fieldName) {
        if (type == JavaType.Primitive.Boolean) {
            boolean alreadyStartsWithIs = fieldName.length() >= 3 &&
                    fieldName.substring(0, 3).matches("is[A-Z]");
            if (alreadyStartsWithIs) {
                return fieldName;
            }
            return "is" + StringUtils.capitalize(fieldName);
        }
        return "get" + StringUtils.capitalize(fieldName);
    }

    static boolean isSetter(Cursor cursor) {
        if (!(cursor.getValue() instanceof J.MethodDeclaration)) {
            return false;
        }
        J.MethodDeclaration method = cursor.getValue();

        if (method.getType() != JavaType.Primitive.Void) {
            return false;
        }
        if (method.getParameters().size() != 1 || method.getParameters().get(0) instanceof J.Empty) {
            return false;
        }
        if (method.getBody() == null || //abstract methods can be null
                method.getBody().getStatements().size() != 1 ||
                !(method.getBody().getStatements().get(0) instanceof J.Assignment)) {
            return false;
        }

        // Check there's no up/down cast between parameter and field
        J.VariableDeclarations.NamedVariable param = ((J.VariableDeclarations) method.getParameters().get(0)).getVariables().get(0);
        J.Assignment assignment = (J.Assignment) method.getBody().getStatements().get(0);
        Expression variable = assignment.getVariable();
        if (param.getType() != variable.getType() || !isReferenceTo(assignment.getAssignment(), param)) {
            return false;
        }

        JavaType.FullyQualified declaringType = method.getMethodType().getDeclaringType();
        if (variable instanceof J.Identifier) {
            J.Identifier assignedVar = (J.Identifier) variable;
            if (hasMatchingSetterMethodName(method, assignedVar.getSimpleName())) {
                if (assignedVar.getFieldType() != null && declaringType == assignedVar.getFieldType().getOwner()) {
                    // Don't replace instance methods accessing static fields
                    return !assignedVar.getFieldType().hasFlags(Flag.Static);
                }
            }
        } else if (variable instanceof J.FieldAccess) {
            J.FieldAccess assignedField = (J.FieldAccess) variable;
            if (hasMatchingSetterMethodName(method, assignedField.getSimpleName())) {
                Expression target = assignedField.getTarget();
                // Only handle "this.field" pattern, not "someObject.field"
                if (target instanceof J.Identifier && "this".equals(((J.Identifier) target).getSimpleName())) {
                    if (assignedField.getName().getFieldType() != null &&
                            declaringType == assignedField.getName().getFieldType().getOwner()) {
                        // Don't replace instance methods accessing static fields
                        return !assignedField.getName().getFieldType().hasFlags(Flag.Static);
                    }
                }
            }
        }

        return false;
    }

    private static boolean hasMatchingSetterMethodName(J.MethodDeclaration method, String simpleName) {
        return method.getSimpleName().equals("set" + StringUtils.capitalize(simpleName));
    }

    public static boolean isEffectivelySetter(J.MethodDeclaration method) {
        if (method.getType() != JavaType.Primitive.Void) {
            return false;
        }
        if (method.getParameters().size() != 1 || method.getParameters().get(0) instanceof J.Empty) {
            return false;
        }

        J.VariableDeclarations variableDeclarations = (J.VariableDeclarations) method.getParameters().get(0);
        J.VariableDeclarations.NamedVariable param = variableDeclarations.getVariables().get(0);
        String paramName = param.getName().toString();

        if (method.getBody() == null ||
                method.getBody().getStatements().size() != 1 ||
                !(method.getBody().getStatements().get(0) instanceof J.Assignment)) {
            return false;
        }
        J.Assignment assignment = (J.Assignment) method.getBody().getStatements().get(0);

        if (!(assignment.getVariable() instanceof J.FieldAccess) && !(assignment.getVariable() instanceof J.Identifier)) {
            return false;
        }

        JavaType fieldType = assignment.getVariable().getType();
        return assignment.getAssignment().toString().equals(paramName) &&
                        param.getType() != null &&
                        param.getType().equals(fieldType);
    }

    public static String deriveSetterMethodName(JavaType.Variable fieldType) {
        return "set" + StringUtils.capitalize(fieldType.getName());
    }

    static AccessLevel getAccessLevel(J.MethodDeclaration methodDeclaration) {
        if (methodDeclaration.hasModifier(Public)) {
            return PUBLIC;
        }
        if (methodDeclaration.hasModifier(Protected)) {
            return PROTECTED;
        }
        if (methodDeclaration.hasModifier(Private)) {
            return PRIVATE;
        }
        return PACKAGE;
    }

    /**
     * Returns the "required" fields for a class in declaration order: non-static final fields
     * without initializers, plus non-static {@code @lombok.NonNull} fields without initializers.
     */
    static List<J.VariableDeclarations.NamedVariable> getRequiredFields(J.ClassDeclaration classDecl) {
        List<J.VariableDeclarations.NamedVariable> result = new ArrayList<>();
        for (Statement stmt : classDecl.getBody().getStatements()) {
            if (!(stmt instanceof J.VariableDeclarations)) {
                continue;
            }
            J.VariableDeclarations varDecls = (J.VariableDeclarations) stmt;
            if (varDecls.hasModifier(Static)) {
                continue;
            }
            boolean isFinal = varDecls.hasModifier(Final);
            boolean hasNonNull = varDecls.getLeadingAnnotations().stream()
                    .anyMatch(new AnnotationMatcher("@lombok.NonNull")::matches);
            if (!isFinal && !hasNonNull) {
                continue;
            }
            for (J.VariableDeclarations.NamedVariable var : varDecls.getVariables()) {
                if (var.getInitializer() == null) {
                    result.add(var);
                }
            }
        }
        return result;
    }

    static List<J.VariableDeclarations.NamedVariable> getAllNonStaticFields(J.ClassDeclaration classDecl) {
        List<J.VariableDeclarations.NamedVariable> result = new ArrayList<>();
        for (Statement stmt : classDecl.getBody().getStatements()) {
            if (!(stmt instanceof J.VariableDeclarations)) {
                continue;
            }
            J.VariableDeclarations varDecls = (J.VariableDeclarations) stmt;
            if (varDecls.hasModifier(Static)) {
                continue;
            }
            result.addAll(varDecls.getVariables());
        }
        return result;
    }

    /**
     * Checks that a constructor's body consists entirely of simple field assignments
     * matching the given fields in order, with matching parameter types.
     */
    static boolean isConstructorAssigningExactFields(J.MethodDeclaration constructor,
                                                     List<J.VariableDeclarations.NamedVariable> expectedFields) {
        if (constructor.getBody() == null) {
            return false;
        }
        List<Statement> statements = constructor.getBody().getStatements();
        if (statements.size() != expectedFields.size()) {
            return false;
        }

        List<Statement> params = constructor.getParameters();
        if (expectedFields.isEmpty()) {
            return false;
        }
        if (params.size() != expectedFields.size() || params.get(0) instanceof J.Empty) {
            return false;
        }

        JavaType.FullyQualified declaringType = constructor.getMethodType() != null ?
                constructor.getMethodType().getDeclaringType() : null;

        for (int i = 0; i < expectedFields.size(); i++) {
            J.VariableDeclarations.NamedVariable expectedField = expectedFields.get(i);

            if (!(params.get(i) instanceof J.VariableDeclarations)) {
                return false;
            }
            J.VariableDeclarations paramDecl = (J.VariableDeclarations) params.get(i);
            J.VariableDeclarations.NamedVariable param = paramDecl.getVariables().get(0);
            if (!TypeUtils.isOfType(param.getType(), expectedField.getType())) {
                return false;
            }

            if (!(statements.get(i) instanceof J.Assignment)) {
                return false;
            }
            J.Assignment assignment = (J.Assignment) statements.get(i);

            String assignedFieldName = getAssignedFieldName(assignment.getVariable(), declaringType);
            if (assignedFieldName == null || !assignedFieldName.equals(expectedField.getSimpleName())) {
                return false;
            }

            if (!isReferenceTo(assignment.getAssignment(), param)) {
                return false;
            }
        }
        return true;
    }

    private static @Nullable String getAssignedFieldName(Expression variable, JavaType.@Nullable FullyQualified declaringType) {
        if (variable instanceof J.Identifier) {
            J.Identifier id = (J.Identifier) variable;
            if (id.getFieldType() != null && (declaringType == null || declaringType == id.getFieldType().getOwner())) {
                return id.getSimpleName();
            }
        } else if (variable instanceof J.FieldAccess) {
            J.FieldAccess fa = (J.FieldAccess) variable;
            Expression target = fa.getTarget();
            if (target instanceof J.Identifier && "this".equals(((J.Identifier) target).getSimpleName())) {
                if (fa.getName().getFieldType() != null &&
                        (declaringType == null || declaringType == fa.getName().getFieldType().getOwner())) {
                    return fa.getSimpleName();
                }
            }
        }
        return null;
    }

    private static boolean isReferenceTo(Expression expr, J.VariableDeclarations.NamedVariable param) {
        if (expr instanceof J.Identifier) {
            J.Identifier id = (J.Identifier) expr;
            return id.getSimpleName().equals(param.getSimpleName()) &&
                    TypeUtils.isOfType(id.getType(), param.getType());
        }
        return false;
    }

}
