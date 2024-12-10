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
package org.openrewrite.java.migrate.lombok;

import com.google.common.collect.ImmutableMap;
import lombok.AccessLevel;
import org.jspecify.annotations.Nullable;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static lombok.AccessLevel.*;
import static org.openrewrite.java.tree.J.Modifier.Type.*;

class LombokUtils {

    static boolean isEffectivelyGetter(J.MethodDeclaration method) {
        // Check signature: no parameters
        if (!(method.getParameters().get(0) instanceof J.Empty) || method.getReturnTypeExpression() == null) {
            return false;
        }
        // Check body: just a return statement
        if (method.getBody() == null ||
                method.getBody().getStatements().size() != 1 ||
                !(method.getBody().getStatements().get(0) instanceof J.Return)) {
            return false;
        }
        // Check return: type and matching field name
        Expression returnExpression = ((J.Return) method.getBody().getStatements().get(0)).getExpression();
        if (returnExpression instanceof J.Identifier) {
            J.Identifier identifier = (J.Identifier) returnExpression;
            return hasMatchingTypeAndName(method, identifier.getType(), identifier.getSimpleName());
        } else if (returnExpression instanceof J.FieldAccess) {
            J.FieldAccess fieldAccess = (J.FieldAccess) returnExpression;
            return hasMatchingTypeAndName(method, fieldAccess.getType(), fieldAccess.getSimpleName());
        }
        return false;
    }

    public static boolean isEffectivelySetter(J.MethodDeclaration method) {
        boolean isVoid = "void".equals(method.getType().toString());
        List<Statement> actualParameters = method.getParameters().stream()
                .filter(s -> !(s instanceof J.Empty))
                .collect(Collectors.toList());
        boolean oneParam = actualParameters.size() == 1;
        if (!isVoid || !oneParam)
            return false;

        J.VariableDeclarations variableDeclarations = (J.VariableDeclarations) actualParameters.get(0);
        J.VariableDeclarations.NamedVariable param = variableDeclarations.getVariables().get(0);
        String paramName = param.getName().toString();

        boolean singularStatement = method.getBody() != null //abstract methods can be null
                && method.getBody().getStatements().size() == 1
                && method.getBody().getStatements().get(0) instanceof J.Assignment;

        if (!singularStatement) {
            return false;
        }
        J.Assignment assignment = (J.Assignment) method.getBody().getStatements().get(0);

        J.FieldAccess fieldAccess = (J.FieldAccess) assignment.getVariable();

        return
                // assigned value is exactly the parameter
                assignment.getAssignment().toString().equals(paramName)

                        // type of parameter and field have to match
                        && param.getType().equals(fieldAccess.getType());

    }

    private static boolean hasMatchingTypeAndName(J.MethodDeclaration method, @Nullable JavaType type, String simpleName) {
        if (method.getType().equals(type)) {
            String deriveGetterMethodName = deriveGetterMethodName(type, simpleName);
            return method.getSimpleName().equals(deriveGetterMethodName);
        }
        return false;
    }

    private static String deriveGetterMethodName(@Nullable JavaType type, String fieldName) {
        if (type == JavaType.Primitive.Boolean) {
            boolean alreadyStartsWithIs = fieldName.length() >= 3 &&
                    fieldName.substring(0, 3).matches("is[A-Z]");
            if (alreadyStartsWithIs) {
                return fieldName;
            } else {
                return "is" + StringUtils.capitalize(fieldName);
            }
        }
        return "get" + StringUtils.capitalize(fieldName);
    }

    public static String deriveSetterMethodName(JavaType.Variable fieldType) {
        return "set" + StringUtils.capitalize(fieldType.getName());
    }

    static AccessLevel getAccessLevel(Collection<J.Modifier> modifiers) {
        Map<J.Modifier.Type, AccessLevel> map = ImmutableMap.<J.Modifier.Type, AccessLevel>builder()
                .put(Public, PUBLIC)
                .put(Protected, PROTECTED)
                .put(Private, PRIVATE)
                .build();

        return modifiers.stream()
                .map(modifier -> map.getOrDefault(modifier.getType(), AccessLevel.NONE))
                .filter(a -> a != AccessLevel.NONE)
                .findAny().orElse(AccessLevel.PACKAGE);
    }

}
