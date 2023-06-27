/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.java.migrate.lang.var;

import org.openrewrite.Cursor;
import org.openrewrite.java.tree.*;

import static java.util.Objects.requireNonNull;

final class DeclarationCheck {

    private DeclarationCheck() {

    }

    /**
     * Determine if var is applicable with regard to location and decleation type.
     * <p>
     * Var is applicable inside methods and initializer blocks for single variable definition.
     * Var is *not* applicable to method definitions.
     *
     * @param cursor location of the visitor
     * @param vd     variable definition at question
     * @return true if var is applicable in general
     */
    public static boolean isVarApplicable(Cursor cursor, J.VariableDeclarations vd) {
        if (isField(vd, cursor) || isMethodParameter(vd, cursor) || !isSingleVariableDefinition(vd) || initializedByTernary(vd)) {
            return false;
        }

        return isInsideMethod(cursor) || isInsideInitializer(cursor, 0);
    }

    /**
     * Determine if a variable definition defines a single variable that is directly initialized with value different from null, which not make use of var.
     *
     * @param vd variable definition at hand
     * @return true if single variable definition with initialization and without var
     */
    private static boolean isSingleVariableDefinition(J.VariableDeclarations vd) {
        TypeTree typeExpression = vd.getTypeExpression();

        boolean definesSingleVariable = vd.getVariables().size() == 1;
        boolean isPureAssigment = JavaType.Primitive.Null.equals(vd.getType());
        if (!definesSingleVariable || isPureAssigment) return false;

        Expression initializer = vd.getVariables().get(0).getInitializer();
        boolean isDeclarationOnly = initializer == null;
        if (isDeclarationOnly) return false;

        initializer = initializer.unwrap();
        boolean isNullAssigment = initializer instanceof J.Literal && ((J.Literal) initializer).getValue() == null;
        boolean alreadyUseVar = typeExpression instanceof J.Identifier && "var".equals(((J.Identifier) typeExpression).getSimpleName());
        return !isNullAssigment && !alreadyUseVar;
    }

    /**
     * Determine whether the variable declaration at hand defines a primitive variable
     *
     * @param vd variable declaration at hand
     * @return true iff declares primitive type
     */
    public static boolean isPrimitive(J.VariableDeclarations vd) {
        TypeTree typeExpression = vd.getTypeExpression();
        return typeExpression instanceof J.Primitive;
    }

    /**
     * Checks whether the variable declaration at hand has the type
     *
     * @param vd   variable declaration at hand
     * @param type type in question
     * @return true iff the declaration has a matching type definition
     */
    public static boolean declarationHasType(J.VariableDeclarations vd, JavaType type) {
        TypeTree typeExpression = vd.getTypeExpression();
        return typeExpression != null && type.equals(typeExpression.getType());
    }

    /**
     * Determine whether the definition or the initializer uses generics types
     *
     * @param vd variable definition at hand
     * @return true if definition or initializer uses generic types
     */
    public static boolean useGenerics(J.VariableDeclarations vd) {
        TypeTree typeExpression = vd.getTypeExpression();
        boolean isGenericDefinition = typeExpression instanceof J.ParameterizedType;
        if (isGenericDefinition) return true;

        Expression initializer = vd.getVariables().get(0).getInitializer();
        if (initializer == null) return false;
        initializer = initializer.unwrap();

        return initializer instanceof J.NewClass
               && ((J.NewClass) initializer).getClazz() instanceof J.ParameterizedType;
    }

    /**
     * Determin if the initilizer uses the ternary operator <code>Expression ? if-then : else</code>
     *
     * @param vd variable declaration at hand
     * @return true iff the ternary operator is used in the initialization
     */
    public static boolean initializedByTernary(J.VariableDeclarations vd) {
        Expression initializer = vd.getVariables().get(0).getInitializer();
        return initializer != null && initializer.unwrap() instanceof J.Ternary;
    }

    /**
     * Determines if a cursor is contained inside a Method declaration without an intermediate Class declaration
     *
     * @param cursor value to determine
     */
    private static boolean isInsideMethod(Cursor cursor) {
        Object value = cursor
                .dropParentUntil(p -> p instanceof J.MethodDeclaration || p instanceof J.ClassDeclaration|| p.equals(Cursor.ROOT_VALUE))
                .getValue();

        boolean isNotRoot = !Cursor.ROOT_VALUE.equals(value);
        boolean isNotClassDeclaration = !(value instanceof J.ClassDeclaration);
        boolean isMethodDeclaration = value instanceof J.MethodDeclaration;

        return isNotRoot && isNotClassDeclaration && isMethodDeclaration;
    }

    private static boolean isField(J.VariableDeclarations vd, Cursor cursor) {
        Cursor parent = cursor.getParentTreeCursor();
        if (parent.getParent() == null) {
            return false;
        }
        Cursor grandparent = parent.getParentTreeCursor();
        return parent.getValue() instanceof J.Block && (grandparent.getValue() instanceof J.ClassDeclaration || grandparent.getValue() instanceof J.NewClass);
    }

    /**
     * Determine if the variable declaration at hand is part of a method declaration
     *
     * @param vd     variable declaration to check
     * @param cursor current location
     * @return true iff vd is part of a method declaration
     */
    private static boolean isMethodParameter(J.VariableDeclarations vd, Cursor cursor) {
        J.MethodDeclaration methodDeclaration = cursor.firstEnclosing(J.MethodDeclaration.class);
        return methodDeclaration != null && methodDeclaration.getParameters().contains(vd);
    }

    /**
     * Determine if the visitors location is inside an instance or static initializer block
     *
     * @param cursor           visitors location
     * @param nestedBlockLevel number of blocks, default for start 0
     * @return true iff the courser is inside an instance or static initializer block
     */
    private static boolean isInsideInitializer(Cursor cursor, int nestedBlockLevel) {
        if (Cursor.ROOT_VALUE.equals(cursor.getValue())) return false;

        Object currentStatement = cursor.getValue();

        // initializer blocks are blocks inside the class definition block, therefor a nesting of 2 is mandatory
        boolean isClassDeclaration = currentStatement instanceof J.ClassDeclaration;
        boolean followedByTwoBlock = nestedBlockLevel >= 2;
        if (isClassDeclaration && followedByTwoBlock) return true;

        // count direct block nesting (block containing a block), but ignore paddings
        boolean isBlock = currentStatement instanceof J.Block;
        boolean isNoPadding = !(currentStatement instanceof JRightPadded);
        if (isBlock) nestedBlockLevel += 1;
        else if (isNoPadding) nestedBlockLevel = 0;

        return isInsideInitializer(requireNonNull(cursor.getParent()), nestedBlockLevel);
    }
}
