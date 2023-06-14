package org.openrewrite.java.migrate.lang.var;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.Cursor;
import org.openrewrite.java.tree.*;

import static java.util.Objects.*;

public final class DeclarationCheck {

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
    public static boolean isVarApplicable(@NotNull Cursor cursor, @NotNull J.VariableDeclarations vd) {
        boolean isInsideMethod = DeclarationCheck.isInsideMethod(cursor);
        boolean isMethodParameter = DeclarationCheck.isMethodParameter(vd, cursor);
        boolean isInsideInitializer = DeclarationCheck.isInsideInitializer(cursor, 0);
        boolean isSingleDefinition = DeclarationCheck.isInitializedSingleDefinition(vd);

        return (isInsideMethod || isInsideInitializer) && !isMethodParameter && isSingleDefinition;
    }

    /**
     * Determine if a variable definition defines a single variable that is directly initialized with value different from null, which not make use of var.
     *
     * @param vd variable definition at hand
     * @return true if single variable definition with initialization and without var
     */
    private static boolean isInitializedSingleDefinition(@NotNull J.VariableDeclarations vd) {
        TypeTree typeExpression = vd.getTypeExpression();

        boolean definesSigleVariable = vd.getVariables().size() == 1;
        boolean isPureAssigment = JavaType.Primitive.Null.equals(vd.getType());
        if (!definesSigleVariable || isPureAssigment) return false;

        Expression initializer = vd.getVariables().get(0).getInitializer();
        boolean isDeclarationOnly = isNull(initializer);
        if (isDeclarationOnly) return false;

        initializer = initializer.unwrap();
        boolean isNullAssigment = initializer instanceof J.Literal && isNull(((J.Literal) initializer).getValue());
        boolean alreadyUseVar = typeExpression instanceof J.Identifier && "var".equals(((J.Identifier) typeExpression).getSimpleName());
        if (isNullAssigment || alreadyUseVar) return false;

        return true;
    }

    /**
     * Determine whether the variable declaration at hand defines a primitive variable
     *
     * @param vd variable declaration at hand
     * @return true iff declares primitive type
     */
    public static boolean isPrimitive(@NotNull J.VariableDeclarations vd) {
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
    public static boolean declarationHasType(@NotNull J.VariableDeclarations vd, @NotNull JavaType type) {
        TypeTree typeExpression = vd.getTypeExpression();
        return typeExpression != null && type.equals(typeExpression.getType());
    }

    /**
     * Determine whether the definition or the initializer uses generics types
     *
     * @param vd variable definition at hand
     * @return true if definition or initializer uses generic types
     */
    public static boolean useGenerics(@NotNull J.VariableDeclarations vd) {
        TypeTree typeExpression = vd.getTypeExpression();
        Expression initializer = vd.getVariables().get(0).getInitializer().unwrap();

        boolean isGenericDefinition = typeExpression instanceof J.ParameterizedType;
        boolean isGenericInitializer = initializer instanceof J.NewClass
                                       && ((J.NewClass) initializer).getClazz() instanceof J.ParameterizedType;
        return isGenericDefinition || isGenericInitializer;
    }

    /**
     * Determin if the initilizer uses the ternary operator <code>Expression ? if-then : else</code>
     *
     * @param vd variable declaration at hand
     * @return true iff the ternary operator is used in the initialization
     */
    public static boolean initializedByTernary(@NotNull J.VariableDeclarations vd) {
        Expression initializer = vd.getVariables().get(0).getInitializer().unwrap();
        return initializer instanceof J.Ternary;
    }

    /**
     * Determines if a cursor is contained inside a Method declaration without an intermediate Class declaration
     *
     * @param cursor value to determine
     */
    private static boolean isInsideMethod(@NotNull Cursor cursor) {
        Object current = cursor.getValue();

        boolean atRoot = Cursor.ROOT_VALUE.equals(current);
        boolean atClassDeclaration = current instanceof J.ClassDeclaration;
        boolean atMethodDeclaration = current instanceof J.MethodDeclaration;

        if (atRoot || atClassDeclaration) return false;
        if (atMethodDeclaration) return true;
        return isInsideMethod(requireNonNull(cursor.getParent()));
    }

    /**
     * Determine if the variable declaration at hand is part of a method declaration
     *
     * @param vd     variable declaration to check
     * @param cursor current location
     * @return true iff vd is part of a method declaration
     */
    private static boolean isMethodParameter(@NotNull J.VariableDeclarations vd, @NotNull Cursor cursor) {
        J.MethodDeclaration methodDeclaration = cursor.firstEnclosing(J.MethodDeclaration.class);
        return nonNull(methodDeclaration) && methodDeclaration.getParameters().contains(vd);
    }

    /**
     * Determine if the visitors location is inside an instance or static initializer block
     *
     * @param cursor           visitors location
     * @param nestedBlockLevel number of blocks, default for start 0
     * @return true iff the courser is inside an instance or static initializer block
     */
    private static boolean isInsideInitializer(@NotNull Cursor cursor, int nestedBlockLevel) {
        if (Cursor.ROOT_VALUE.equals(cursor.getValue())) {
            return false;
        }
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
