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
package org.openrewrite.java.migrate;

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.marker.JavaProject;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.newSetFromMap;

public class ArrayStoreExceptionToTypeNotPresentException extends ScanningRecipe<ArrayStoreExceptionToTypeNotPresentException.Accumulator> {

    private static final String ARRAY_STORE_EXCEPTION = "java.lang.ArrayStoreException";
    private static final String TYPE_NOT_PRESENT_EXCEPTION = "java.lang.TypeNotPresentException";
    private static final String TYPE_NOT_PRESENT_EXCEPTION_SIMPLE_NAME = "TypeNotPresentException";
    private static final MethodMatcher CLASS_GET_ANNOTATION = new MethodMatcher("java.lang.Class getAnnotation(java.lang.Class)");

    /**
     * A catch of any of these types already handles {@code TypeNotPresentException}.
     */
    private static final Set<String> HANDLES_TYPE_NOT_PRESENT_EXCEPTION = new HashSet<>(asList(
            "java.lang.RuntimeException", "java.lang.Exception", "java.lang.Throwable"));

    /**
     * The supertypes of {@code RuntimeException}, a closed set since {@code java.lang} cannot be extended. A
     * position declared with one of these accepts every {@code RuntimeException}, so it survives the widening.
     * Unresolved types are absent and so block it.
     */
    private static final Set<String> SUPERTYPES_OF_RUNTIME_EXCEPTION = new HashSet<>(asList(
            "java.lang.RuntimeException", "java.lang.Exception", "java.lang.Throwable",
            "java.lang.Object", "java.io.Serializable"));

    /**
     * Types accepting any {@code Class} whatever its type argument. Anything unlisted, such as
     * {@code java.lang.constant.Constable} (Java 12+), blocks the widening, as do unresolved types.
     */
    private static final Set<String> ACCEPTS_ANY_CLASS = new HashSet<>(asList(
            "java.lang.Class", "java.lang.Object", "java.io.Serializable",
            "java.lang.reflect.Type", "java.lang.reflect.AnnotatedElement", "java.lang.reflect.GenericDeclaration"));

    @Getter
    final String displayName = "Catch `TypeNotPresentException` thrown by `Class.getAnnotation()`";

    @Getter
    final String description = "Also catch `TypeNotPresentException` where `ArrayStoreException` is caught around `Class.getAnnotation()` to ensure compatibility with Java 11+. " +
            "The `ArrayStoreException` is retained as the protected code can still throw it for reasons unrelated to annotations.";

    /**
     * Where the sources declare their own {@code TypeNotPresentException}, the spliced simple name would resolve
     * to it instead, either failing to compile or silently catching the wrong type, so the scanner records those
     * declarations and the visitor qualifies the name there. Scoped per {@link JavaProject} marker, since only a
     * same-module declaration can shadow; unmarked sources share one scope.
     */
    public static class Accumulator {
        /**
         * Per project, the packages declaring a top-level {@code TypeNotPresentException}, {@code ""} for the
         * default package.
         */
        private final Map<@Nullable JavaProject, Set<String>> packagesByProject = new HashMap<>();

        /**
         * Per project, the classes declaring a nested {@code TypeNotPresentException}, which shadows through
         * inheritance and on-demand imports.
         */
        private final Map<@Nullable JavaProject, Set<String>> classesByProject = new HashMap<>();

        void recordPackage(@Nullable JavaProject project, String packageName) {
            packagesByProject.computeIfAbsent(project, key -> new HashSet<>()).add(packageName);
        }

        void recordClass(@Nullable JavaProject project, String className) {
            classesByProject.computeIfAbsent(project, key -> new HashSet<>()).add(className);
        }

        Set<String> packagesDeclaringTypeNotPresentException(@Nullable JavaProject project) {
            return packagesByProject.getOrDefault(project, emptySet());
        }

        Set<String> classesDeclaringTypeNotPresentException(@Nullable JavaProject project) {
            return classesByProject.getOrDefault(project, emptySet());
        }
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                if (TYPE_NOT_PRESENT_EXCEPTION_SIMPLE_NAME.equals(classDecl.getSimpleName())) {
                    JavaSourceFile sourceFile = getCursor().firstEnclosing(JavaSourceFile.class);
                    JavaProject project = javaProject(sourceFile);
                    JavaType.FullyQualified owner = classDecl.getType() == null ? null : classDecl.getType().getOwningClass();
                    if (owner != null) {
                        acc.recordClass(project, owner.getFullyQualifiedName());
                    } else if (sourceFile != null) {
                        acc.recordPackage(project, packageName(sourceFile));
                    }
                }
                return super.visitClassDeclaration(classDecl, ctx);
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return Preconditions.check(new UsesMethod<>(CLASS_GET_ANNOTATION), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.Try visitTry(J.Try tryStatement, ExecutionContext ctx) {
                J.Try try_ = super.visitTry(tryStatement, ctx);
                JavaSourceFile sourceFile = getCursor().firstEnclosing(JavaSourceFile.class);
                if (!(sourceFile instanceof J.CompilationUnit)) {
                    // A multi-catch is Java-only syntax, so other JVM languages are left alone
                    return try_;
                }
                if (anyCatchConcernsTypeNotPresentException(try_) || !protectedRegionCallsGetAnnotation(try_) ||
                        anyEnclosingCatchConcernsTypeNotPresentException(getCursor())) {
                    return try_;
                }
                Cursor tryCursor = getCursor();
                boolean qualify = typeNotPresentExceptionSimpleNameIsShadowed((J.CompilationUnit) sourceFile, tryCursor,
                        acc, javaProject(sourceFile));
                return try_.withCatches(ListUtils.map(try_.getCatches(), catch_ -> {
                    if (TypeUtils.isOfClassType(catch_.getParameter().getType(), ARRAY_STORE_EXCEPTION) &&
                            allParameterReferencesSurviveWidening(catch_, tryCursor)) {
                        return alsoCatchTypeNotPresentException(catch_, qualify);
                    }
                    return catch_;
                }));
            }
        });
    }

    /**
     * Only a try's resources and body are protected by its catches. A catch or finally block runs outside that
     * region, as do the method bodies of a lambda or class created inside it. Such a class's instance
     * initializers do run inside, but are left out too, which only costs a migration that is not applied.
     */
    private static boolean protectedRegionCallsGetAnnotation(J.Try try_) {
        AtomicBoolean found = new AtomicBoolean(false);
        JavaIsoVisitor<AtomicBoolean> scanner = new JavaIsoVisitor<AtomicBoolean>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, AtomicBoolean found) {
                if (CLASS_GET_ANNOTATION.matches(method)) {
                    found.set(true);
                    return method;
                }
                return super.visitMethodInvocation(method, found);
            }

            @Override
            public J.Lambda visitLambda(J.Lambda lambda, AtomicBoolean found) {
                return lambda;
            }

            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, AtomicBoolean found) {
                return classDecl;
            }

            @Override
            public J.NewClass visitNewClass(J.NewClass newClass, AtomicBoolean found) {
                if (newClass.getBody() != null) {
                    // Constructor arguments and instance initializers are evaluated here, only method bodies are
                    // deferred; the whole body is left out anyway, costing only a migration that is not applied
                    for (Expression argument : newClass.getArguments()) {
                        visit(argument, found);
                    }
                    return newClass;
                }
                return super.visitNewClass(newClass, found);
            }
        };
        if (try_.getResources() != null) {
            for (J.Try.Resource resource : try_.getResources()) {
                scanner.visit(resource, found);
            }
        }
        scanner.visit(try_.getBody(), found);
        return found.get();
    }

    private static boolean anyCatchConcernsTypeNotPresentException(J.Try try_) {
        for (J.Try.Catch catch_ : try_.getCatches()) {
            TypeTree typeExpression = catch_.getParameter().getTree().getTypeExpression();
            if (typeExpression instanceof J.MultiCatch) {
                for (NameTree alternative : ((J.MultiCatch) typeExpression).getAlternatives()) {
                    if (concernsTypeNotPresentException(alternative.getType())) {
                        return true;
                    }
                }
            } else if (typeExpression != null && concernsTypeNotPresentException(typeExpression.getType())) {
                return true;
            }
        }
        return false;
    }

    /**
     * An enclosing try whose protected region contains this one sees every {@code TypeNotPresentException} this
     * try does not catch, so widening here would silently reroute them and any such enclosing try blocks it.
     * Only enclosing tries containing this one in their body or resources count. The walk does not stop at
     * lambda or class boundaries, whose bodies may run inside the enclosing region, erring towards not widening.
     */
    private static boolean anyEnclosingCatchConcernsTypeNotPresentException(Cursor tryCursor) {
        J child = tryCursor.getValue();
        for (Cursor cursor = tryCursor.getParent(); cursor != null; cursor = cursor.getParent()) {
            Object value = cursor.getValue();
            if (value instanceof J.Try) {
                J.Try enclosing = (J.Try) value;
                boolean inProtectedRegion = child == enclosing.getBody() ||
                        enclosing.getResources() != null && enclosing.getResources().contains(child);
                if (inProtectedRegion && anyCatchConcernsTypeNotPresentException(enclosing)) {
                    return true;
                }
            }
            if (value instanceof J) {
                child = (J) value;
            }
        }
        return false;
    }

    /**
     * A supertype catch already handles it, and a catch of it or a subclass would become unreachable.
     */
    private static boolean concernsTypeNotPresentException(@Nullable JavaType type) {
        JavaType.FullyQualified fullyQualified = TypeUtils.asFullyQualified(type);
        return fullyQualified != null &&
                (HANDLES_TYPE_NOT_PRESENT_EXCEPTION.contains(fullyQualified.getFullyQualifiedName()) ||
                        TypeUtils.isAssignableTo(TYPE_NOT_PRESENT_EXCEPTION, fullyQualified));
    }

    /**
     * Per JLS 14.20 a multi-catch parameter is implicitly final and typed as the least upper bound of the
     * alternatives, here {@code RuntimeException}. Rather than enumerate the ways a handler can depend on the
     * narrower type, every reference must sit in a context that provably tolerates the wider one; anything
     * unrecognized leaves the catch untouched.
     */
    private static boolean allParameterReferencesSurviveWidening(J.Try.Catch catch_, Cursor tryCursor) {
        List<J.VariableDeclarations.NamedVariable> variables = catch_.getParameter().getTree().getVariables();
        if (variables.size() != 1) {
            return false;
        }
        String parameterName = variables.get(0).getSimpleName();
        JavaType.Variable parameterType = variables.get(0).getVariableType();
        AtomicBoolean unsafe = new AtomicBoolean(false);
        new JavaIsoVisitor<AtomicBoolean>() {
            @Override
            public J.Identifier visitIdentifier(J.Identifier identifier, AtomicBoolean unsafe) {
                if (referencesParameter(identifier, getCursor(), parameterName, parameterType) &&
                        !widenedReferenceIsSafe(getCursor())) {
                    unsafe.set(true);
                }
                return identifier;
            }
        }.visit(catch_, unsafe, tryCursor);
        return !unsafe.get();
    }

    /**
     * Whether this identifier uses the catch parameter. Provably-something-else identifiers, such as method or
     * member names, declarations and labels, are skipped; an unattributed one counts as a use.
     */
    private static boolean referencesParameter(J.Identifier identifier, Cursor cursor, String parameterName,
                                               JavaType.@Nullable Variable parameterType) {
        if (!parameterName.equals(identifier.getSimpleName())) {
            return false;
        }
        J parent = cursor.getParentTreeCursor().getValue();
        if (parent instanceof J.MethodInvocation && ((J.MethodInvocation) parent).getName() == identifier ||
                parent instanceof J.FieldAccess && ((J.FieldAccess) parent).getName() == identifier ||
                parent instanceof J.MemberReference && ((J.MemberReference) parent).getReference() == identifier ||
                parent instanceof J.VariableDeclarations.NamedVariable && ((J.VariableDeclarations.NamedVariable) parent).getName() == identifier ||
                parent instanceof J.Label || parent instanceof J.Break || parent instanceof J.Continue) {
            return false;
        }
        JavaType.Variable fieldType = identifier.getFieldType();
        if (fieldType != null && parameterType != null) {
            return fieldType == parameterType ||
                    fieldType.getName().equals(parameterType.getName()) &&
                            TypeUtils.isOfType(fieldType.getType(), parameterType.getType());
        }
        return true;
    }

    /**
     * Whether an expression the widening retypes keeps compiling, and keeps its meaning, in its context. An
     * allow list: an expression-bodied lambda, a switch, or anything unforeseen blocks the widening.
     */
    private static boolean widenedReferenceIsSafe(Cursor cursor) {
        J expression = cursor.getValue();
        Cursor parentCursor = cursor.getParentTreeCursor();
        J parent = parentCursor.getValue();
        if (parent instanceof J.Parentheses) {
            // The parenthesized expression widens with its content
            return widenedReferenceIsSafe(parentCursor);
        }
        if (parent instanceof J.Ternary) {
            // The reference can only be a result branch, and the conditional's own type widens with it
            J.Ternary ternary = (J.Ternary) parent;
            return (expression == ternary.getTruePart() || expression == ternary.getFalsePart()) &&
                    widenedReferenceIsSafe(parentCursor);
        }
        if (parent instanceof J.Binary || parent instanceof J.InstanceOf || parent instanceof J.Throw ||
                parent instanceof J.Assert) {
            // Concatenation, `==`/`!=`, a type test, throwing (`RuntimeException` is unchecked) and an assert
            // message all stay valid and unchanged for the values the original handler could receive
            return true;
        }
        if (parent instanceof J.AssignmentOperation) {
            // Only `String +=` compiles with an exception operand, and concatenation tolerates any
            // `RuntimeException`; the parameter as the assigned variable fails safe
            return expression == ((J.AssignmentOperation) parent).getAssignment();
        }
        if (parent instanceof J.ControlParentheses) {
            // Of the statements parenthesizing a bare expression only a synchronized monitor keeps its meaning,
            // any object being a valid monitor; a pattern switch selector is deliberately excluded
            return parentCursor.getParentTreeCursor().getValue() instanceof J.Synchronized;
        }
        if (parent instanceof J.TypeCast) {
            // The cast's own type is unchanged, but one narrower than `RuntimeException` would now throw
            return expression == ((J.TypeCast) parent).getExpression() &&
                    acceptsAnyRuntimeException(((J.TypeCast) parent).getType());
        }
        if (parent instanceof J.MethodInvocation) {
            J.MethodInvocation invocation = (J.MethodInvocation) parent;
            if (expression == invocation.getSelect()) {
                return invokedMethodRemainsAvailable(invocation.getMethodType()) &&
                        (!resultTypeDependsOnReceiverType(invocation.getMethodType()) ||
                                widenedResultIsSafe(parentCursor));
            }
            int argumentIndex = invocation.getArguments().indexOf(expression);
            return argumentIndex >= 0 && argumentRemainsCompatible(invocation.getMethodType(), argumentIndex, parentCursor);
        }
        if (parent instanceof J.NewClass) {
            int argumentIndex = ((J.NewClass) parent).getArguments().indexOf(expression);
            return argumentIndex >= 0 && argumentRemainsCompatible(((J.NewClass) parent).getMethodType(), argumentIndex, parentCursor);
        }
        if (parent instanceof J.MemberReference) {
            // Checking the result against the functional interface's method is not reliable here, so a
            // receiver-dependent result fails safe
            J.MemberReference reference = (J.MemberReference) parent;
            return expression == reference.getContaining() &&
                    invokedMethodRemainsAvailable(reference.getMethodType()) &&
                    !resultTypeDependsOnReceiverType(reference.getMethodType());
        }
        if (parent instanceof J.VariableDeclarations.NamedVariable) {
            // Covers an explicit declared type; `var` infers the narrower type and is rejected here
            J.VariableDeclarations.NamedVariable variable = (J.VariableDeclarations.NamedVariable) parent;
            return expression == variable.getInitializer() && acceptsAnyRuntimeException(variable.getType());
        }
        if (parent instanceof J.Assignment) {
            J.Assignment assignment = (J.Assignment) parent;
            if (expression == assignment.getVariable()) {
                // A multi-catch parameter is implicitly final
                return false;
            }
            return acceptsAnyRuntimeException(assignment.getVariable().getType());
        }
        if (parent instanceof J.Return) {
            JavaType returnType = enclosingMethodReturnType(parentCursor);
            return returnType != null && acceptsAnyRuntimeException(returnType);
        }
        if (parent instanceof J.NewArray) {
            J.NewArray newArray = (J.NewArray) parent;
            JavaType type = newArray.getType();
            return newArray.getInitializer() != null && newArray.getInitializer().contains(expression) &&
                    type instanceof JavaType.Array && acceptsAnyRuntimeException(((JavaType.Array) type).getElemType());
        }
        return isStatementPosition(parent);
    }

    /**
     * A parent holding the expression as a statement discards its value, so it compiles however its type
     * widens. Unbraced forms discard it exactly as a block does. A switch's arrow case is deliberately absent,
     * since there the expression may be the switch's own value.
     */
    private static boolean isStatementPosition(J parent) {
        return parent instanceof J.Block || parent instanceof J.If || parent instanceof J.If.Else ||
                parent instanceof J.Label || parent instanceof J.WhileLoop || parent instanceof J.DoWhileLoop ||
                parent instanceof J.ForLoop || parent instanceof J.ForEachLoop;
    }

    /**
     * A method stays available when its declaring type is a supertype of {@code RuntimeException}, which holds
     * for every resolvable call since {@code ArrayStoreException} declares none of its own.
     */
    private static boolean invokedMethodRemainsAvailable(JavaType.@Nullable Method methodType) {
        return methodType != null && acceptsAnyRuntimeException(methodType.getDeclaringType());
    }

    /**
     * Whether the invocation's result type widens with its receiver. Per JLS 4.3.2 {@code e.getClass()} is
     * {@code Class<? extends |E|>} over the receiver's *static* type, so widening it silently changes the
     * result. {@code getClass()} is the only receiver-polymorphic member in {@code java.lang} and is
     * recognized by name and arity; a signature mentioning {@code ArrayStoreException} or a type variable
     * counts too, covering any future member whose attribution exposes the same dependence.
     */
    private static boolean resultTypeDependsOnReceiverType(JavaType.Method methodType) {
        return "getClass".equals(methodType.getName()) && methodType.getParameterTypes().isEmpty() ||
                involvesReceiverTypeArgument(methodType.getReturnType(), newIdentitySet());
    }

    /**
     * Whether the context tolerates a result widened to {@code Class<? extends RuntimeException>}. Mirrors
     * {@link #widenedReferenceIsSafe} with the acceptance test adjusted to the class type.
     */
    private static boolean widenedResultIsSafe(Cursor cursor) {
        J expression = cursor.getValue();
        Cursor parentCursor = cursor.getParentTreeCursor();
        J parent = parentCursor.getValue();
        if (parent instanceof J.Parentheses) {
            return widenedResultIsSafe(parentCursor);
        }
        if (parent instanceof J.Ternary) {
            J.Ternary ternary = (J.Ternary) parent;
            return (expression == ternary.getTruePart() || expression == ternary.getFalsePart()) &&
                    widenedResultIsSafe(parentCursor);
        }
        if (parent instanceof J.Binary) {
            // Widening the wildcard's bound breaks neither concatenation nor the cast-compatibility `==` and
            // `!=` require, every such type staying castable
            J.Binary.Type operator = ((J.Binary) parent).getOperator();
            return operator == J.Binary.Type.Addition || operator == J.Binary.Type.Equal ||
                    operator == J.Binary.Type.NotEqual;
        }
        if (parent instanceof J.MethodInvocation) {
            J.MethodInvocation invocation = (J.MethodInvocation) parent;
            if (expression == invocation.getSelect()) {
                // A chained call is a member of `Class` and so stays available, valid exactly when its resolved
                // signature avoids the receiver's type argument; `cast()` and the like fail safe
                JavaType.Method methodType = invocation.getMethodType();
                if (methodType == null || involvesReceiverTypeArgument(methodType.getReturnType(), newIdentitySet())) {
                    return false;
                }
                for (JavaType parameterType : methodType.getParameterTypes()) {
                    if (involvesReceiverTypeArgument(parameterType, newIdentitySet())) {
                        return false;
                    }
                }
                return true;
            }
            int argumentIndex = invocation.getArguments().indexOf(expression);
            if (argumentIndex < 0 || invocation.getMethodType() == null) {
                return false;
            }
            JavaType parameterType = parameterType(invocation.getMethodType(), argumentIndex);
            return parameterType != null && acceptsWidenedClassResult(parameterType);
        }
        if (parent instanceof J.VariableDeclarations.NamedVariable) {
            J.VariableDeclarations.NamedVariable variable = (J.VariableDeclarations.NamedVariable) parent;
            return expression == variable.getInitializer() && acceptsWidenedClassResult(variable.getType());
        }
        if (parent instanceof J.Assignment) {
            J.Assignment assignment = (J.Assignment) parent;
            return expression != assignment.getVariable() && acceptsWidenedClassResult(assignment.getVariable().getType());
        }
        if (parent instanceof J.Return) {
            JavaType returnType = enclosingMethodReturnType(parentCursor);
            return returnType != null && acceptsWidenedClassResult(returnType);
        }
        return isStatementPosition(parent);
    }

    /**
     * Whether the type mentions {@code ArrayStoreException}, a type variable, a wildcard or an unresolved type
     * anywhere, in which case it cannot be relied on to survive the widening.
     */
    private static boolean involvesReceiverTypeArgument(@Nullable JavaType type, Set<JavaType> visited) {
        if (type == null || type instanceof JavaType.Unknown) {
            return true;
        }
        if (!visited.add(type)) {
            return false;
        }
        if (type instanceof JavaType.GenericTypeVariable) {
            return true;
        }
        JavaType.FullyQualified fullyQualified = TypeUtils.asFullyQualified(type);
        if (fullyQualified != null && ARRAY_STORE_EXCEPTION.equals(fullyQualified.getFullyQualifiedName())) {
            return true;
        }
        if (type instanceof JavaType.Parameterized) {
            for (JavaType typeParameter : ((JavaType.Parameterized) type).getTypeParameters()) {
                if (involvesReceiverTypeArgument(typeParameter, visited)) {
                    return true;
                }
            }
        } else if (type instanceof JavaType.Array) {
            return involvesReceiverTypeArgument(((JavaType.Array) type).getElemType(), visited);
        } else if (type instanceof JavaType.Intersection) {
            for (JavaType bound : ((JavaType.Intersection) type).getBounds()) {
                if (involvesReceiverTypeArgument(bound, visited)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Whether a position of this type accepts a {@code Class<? extends RuntimeException>}: raw {@code Class} or
     * a supertype, {@code Class<?>}, or a covariant wildcard whose bounds all accept any {@code RuntimeException}.
     */
    private static boolean acceptsWidenedClassResult(@Nullable JavaType type) {
        if (type instanceof JavaType.Parameterized) {
            JavaType.Parameterized parameterized = (JavaType.Parameterized) type;
            if (!"java.lang.Class".equals(parameterized.getType().getFullyQualifiedName()) ||
                    parameterized.getTypeParameters().size() != 1) {
                return false;
            }
            JavaType argument = parameterized.getTypeParameters().get(0);
            if (!(argument instanceof JavaType.GenericTypeVariable) ||
                    !"?".equals(((JavaType.GenericTypeVariable) argument).getName())) {
                return false;
            }
            JavaType.GenericTypeVariable wildcard = (JavaType.GenericTypeVariable) argument;
            if (wildcard.getBounds().isEmpty()) {
                return true;
            }
            if (wildcard.getVariance() != JavaType.GenericTypeVariable.Variance.COVARIANT) {
                return false;
            }
            for (JavaType bound : wildcard.getBounds()) {
                if (!acceptsAnyRuntimeException(bound)) {
                    return false;
                }
            }
            return true;
        }
        JavaType.FullyQualified fullyQualified = TypeUtils.asFullyQualified(type);
        return fullyQualified != null && ACCEPTS_ANY_CLASS.contains(fullyQualified.getFullyQualifiedName());
    }

    /**
     * The enclosing method declaration's return type, or null from a lambda, whose functional interface's
     * return type is not reliably recoverable here.
     */
    private static @Nullable JavaType enclosingMethodReturnType(Cursor returnCursor) {
        for (Cursor cursor = returnCursor.getParent(); cursor != null; cursor = cursor.getParent()) {
            Object enclosing = cursor.getValue();
            if (enclosing instanceof J.Lambda) {
                return null;
            }
            if (enclosing instanceof J.MethodDeclaration) {
                JavaType.Method methodType = ((J.MethodDeclaration) enclosing).getMethodType();
                return methodType == null ? null : methodType.getReturnType();
            }
        }
        return null;
    }

    private static boolean argumentRemainsCompatible(JavaType.@Nullable Method methodType, int argumentIndex,
                                                     Cursor invocationCursor) {
        if (methodType == null) {
            // Unresolved: the parameter's requirements are unknowable, so leave the catch alone
            return false;
        }
        JavaType parameterType = parameterType(methodType, argumentIndex);
        if (parameterType == null) {
            return false;
        }
        if (acceptsAnyRuntimeException(parameterType)) {
            return true;
        }
        return inferredTypeParameterAcceptsWidening(methodType, argumentIndex, invocationCursor);
    }

    private static @Nullable JavaType parameterType(JavaType.Method methodType, int argumentIndex) {
        List<JavaType> parameterTypes = methodType.getParameterTypes();
        if (parameterTypes.isEmpty()) {
            return null;
        }
        int parameterIndex = Math.min(argumentIndex, parameterTypes.size() - 1);
        JavaType parameterType = parameterTypes.get(parameterIndex);
        if (methodType.hasFlags(Flag.Varargs) && parameterIndex == parameterTypes.size() - 1 &&
                parameterType instanceof JavaType.Array) {
            // The reference is never an array, so in the variable arity position it is passed as an element
            return ((JavaType.Array) parameterType).getElemType();
        }
        return parameterType;
    }

    /**
     * The resolved method type reports the *inferred* argument type, so {@code Objects.requireNonNull(e)} looks
     * like it takes an {@code ArrayStoreException} although {@code <T> T requireNonNull(T)} would simply
     * re-infer. Consult the declaration instead: safe when the parameter is a type variable of the method
     * itself (a class variable is fixed by the receiver), its bounds all accept any {@code RuntimeException},
     * no other parameter constrains it, and a result mentioning it is itself used safely.
     */
    private static boolean inferredTypeParameterAcceptsWidening(JavaType.Method methodType, int argumentIndex,
                                                                Cursor invocationCursor) {
        J call = invocationCursor.getValue();
        if (!(call instanceof J.MethodInvocation) || ((J.MethodInvocation) call).getTypeParameters() != null) {
            // Explicit type arguments do not re-infer, and constructor inference is driven by the class type
            return false;
        }
        JavaType.Method declared = declaredMethod(methodType);
        if (declared == null) {
            return false;
        }
        JavaType declaredParameter = parameterType(declared, argumentIndex);
        if (!(declaredParameter instanceof JavaType.GenericTypeVariable)) {
            return false;
        }
        JavaType.GenericTypeVariable typeVariable = (JavaType.GenericTypeVariable) declaredParameter;
        if (declaredByClass(typeVariable.getName(), declared.getDeclaringType())) {
            return false;
        }
        for (JavaType bound : typeVariable.getBounds()) {
            if (!acceptsAnyRuntimeException(bound)) {
                return false;
            }
        }
        List<JavaType> declaredParameterTypes = declared.getParameterTypes();
        int parameterIndex = Math.min(argumentIndex, declaredParameterTypes.size() - 1);
        for (int i = 0; i < declaredParameterTypes.size(); i++) {
            if (i != parameterIndex && mentionsTypeVariable(declaredParameterTypes.get(i), typeVariable.getName(), newIdentitySet())) {
                return false;
            }
        }
        if (mentionsTypeVariable(declared.getReturnType(), typeVariable.getName(), newIdentitySet())) {
            // The call's own type widens with the parameter, so its context must be safe as well
            return widenedReferenceIsSafe(invocationCursor);
        }
        return true;
    }

    /**
     * The single declaration matching the resolved method by name and arity, or null when ambiguous.
     */
    private static JavaType.@Nullable Method declaredMethod(JavaType.Method methodType) {
        JavaType.Method declared = null;
        for (JavaType.Method candidate : methodType.getDeclaringType().getMethods()) {
            if (candidate.getName().equals(methodType.getName()) &&
                    candidate.getParameterTypes().size() == methodType.getParameterTypes().size()) {
                if (declared != null) {
                    return null;
                }
                declared = candidate;
            }
        }
        return declared;
    }

    /**
     * Whether the declaring class or an owner declares a type variable of this name. A method reusing the name
     * declares its own, so this errs towards the class and only blocks a widening that may have been safe.
     */
    private static boolean declaredByClass(String typeVariableName, JavaType.@Nullable FullyQualified declaringType) {
        for (JavaType.FullyQualified type = declaringType; type != null; type = type.getOwningClass()) {
            JavaType.FullyQualified unwrapped = type instanceof JavaType.Parameterized ? ((JavaType.Parameterized) type).getType() : type;
            for (JavaType typeParameter : unwrapped.getTypeParameters()) {
                if (typeParameter instanceof JavaType.GenericTypeVariable &&
                        typeVariableName.equals(((JavaType.GenericTypeVariable) typeParameter).getName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean mentionsTypeVariable(@Nullable JavaType type, String typeVariableName, Set<JavaType> visited) {
        if (type == null || !visited.add(type)) {
            return false;
        }
        if (type instanceof JavaType.GenericTypeVariable) {
            if (typeVariableName.equals(((JavaType.GenericTypeVariable) type).getName())) {
                return true;
            }
            for (JavaType bound : ((JavaType.GenericTypeVariable) type).getBounds()) {
                if (mentionsTypeVariable(bound, typeVariableName, visited)) {
                    return true;
                }
            }
        } else if (type instanceof JavaType.Array) {
            return mentionsTypeVariable(((JavaType.Array) type).getElemType(), typeVariableName, visited);
        } else if (type instanceof JavaType.Parameterized) {
            for (JavaType typeParameter : ((JavaType.Parameterized) type).getTypeParameters()) {
                if (mentionsTypeVariable(typeParameter, typeVariableName, visited)) {
                    return true;
                }
            }
        } else if (type instanceof JavaType.Intersection) {
            for (JavaType bound : ((JavaType.Intersection) type).getBounds()) {
                if (mentionsTypeVariable(bound, typeVariableName, visited)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Set<JavaType> newIdentitySet() {
        return newSetFromMap(new IdentityHashMap<>());
    }

    private static boolean acceptsAnyRuntimeException(@Nullable JavaType type) {
        JavaType.FullyQualified fullyQualified = TypeUtils.asFullyQualified(type);
        return fullyQualified != null && SUPERTYPES_OF_RUNTIME_EXCEPTION.contains(fullyQualified.getFullyQualifiedName());
    }

    /**
     * Whether the simple name would resolve to anything but {@code java.lang.TypeNotPresentException}: a class
     * or type parameter of that name in this file, a single-type import, a top-level class in this package or
     * reachable through an on-demand import, a nested class inherited from an enclosing class's supertype, or
     * any such type already referenced here. A shadowing class visible only as a compiled dependency, or
     * declared in another {@link JavaProject}, cannot be seen from here, so the simple name is emitted for it.
     */
    private static boolean typeNotPresentExceptionSimpleNameIsShadowed(J.CompilationUnit cu, Cursor tryCursor,
                                                                       Accumulator acc, @Nullable JavaProject project) {
        Set<String> declaringPackages = acc.packagesDeclaringTypeNotPresentException(project);
        Set<String> declaringClasses = acc.classesDeclaringTypeNotPresentException(project);
        if (declaringPackages.contains(packageName(cu))) {
            return true;
        }
        for (J.Import import_ : cu.getImports()) {
            String simpleName = import_.getQualid().getSimpleName();
            if ("*".equals(simpleName)) {
                String imported = qualifierName(import_.getQualid().getTarget());
                if (imported != null &&
                        (declaringPackages.contains(imported) || declaringClasses.contains(imported))) {
                    return true;
                }
            } else if (TYPE_NOT_PRESENT_EXCEPTION_SIMPLE_NAME.equals(simpleName)) {
                JavaType.FullyQualified imported = TypeUtils.asFullyQualified(import_.getQualid().getType());
                if (imported == null || !TYPE_NOT_PRESENT_EXCEPTION.equals(imported.getFullyQualifiedName())) {
                    return true;
                }
            }
        }
        for (JavaType type : cu.getTypesInUse().getTypesInUse()) {
            JavaType.FullyQualified used = TypeUtils.asFullyQualified(type);
            if (used != null && isForeignTypeNotPresentException(used.getFullyQualifiedName())) {
                return true;
            }
        }
        for (Cursor cursor = tryCursor; cursor != null; cursor = cursor.getParent()) {
            Object enclosing = cursor.getValue();
            JavaType.FullyQualified enclosingType = null;
            if (enclosing instanceof J.ClassDeclaration) {
                enclosingType = ((J.ClassDeclaration) enclosing).getType();
            } else if (enclosing instanceof J.NewClass && ((J.NewClass) enclosing).getBody() != null) {
                TypeTree clazz = ((J.NewClass) enclosing).getClazz();
                enclosingType = clazz == null ? null : TypeUtils.asFullyQualified(clazz.getType());
            }
            if (anySupertypeDeclaresTypeNotPresentException(enclosingType, declaringClasses, new HashSet<>())) {
                return true;
            }
        }
        return declaresTypeNotPresentException(cu);
    }

    /**
     * The {@link JavaProject} marker of this source file, or null where the build attached none, as in a
     * single-module parse; unmarked sources share the null scope.
     */
    private static @Nullable JavaProject javaProject(@Nullable JavaSourceFile sourceFile) {
        return sourceFile == null ? null : sourceFile.getMarkers().findFirst(JavaProject.class).orElse(null);
    }

    private static String packageName(JavaSourceFile sourceFile) {
        return sourceFile.getPackageDeclaration() == null ? "" : sourceFile.getPackageDeclaration().getPackageName();
    }

    private static @Nullable String qualifierName(Expression expression) {
        if (expression instanceof J.Identifier) {
            return ((J.Identifier) expression).getSimpleName();
        }
        if (expression instanceof J.FieldAccess) {
            String target = qualifierName(((J.FieldAccess) expression).getTarget());
            return target == null ? null : target + "." + ((J.FieldAccess) expression).getSimpleName();
        }
        return null;
    }

    private static boolean isForeignTypeNotPresentException(String fullyQualifiedName) {
        return !TYPE_NOT_PRESENT_EXCEPTION.equals(fullyQualifiedName) &&
                (TYPE_NOT_PRESENT_EXCEPTION_SIMPLE_NAME.equals(fullyQualifiedName) ||
                        fullyQualifiedName.endsWith("." + TYPE_NOT_PRESENT_EXCEPTION_SIMPLE_NAME) ||
                        fullyQualifiedName.endsWith("$" + TYPE_NOT_PRESENT_EXCEPTION_SIMPLE_NAME));
    }

    private static boolean anySupertypeDeclaresTypeNotPresentException(JavaType.@Nullable FullyQualified type,
                                                                       Set<String> declaringClasses, Set<String> visited) {
        for (JavaType.FullyQualified enclosing = type; enclosing != null; enclosing = enclosing.getSupertype()) {
            if (!visited.add(enclosing.getFullyQualifiedName())) {
                return false;
            }
            if (declaringClasses.contains(enclosing.getFullyQualifiedName())) {
                return true;
            }
            for (JavaType.FullyQualified interface_ : enclosing.getInterfaces()) {
                if (anySupertypeDeclaresTypeNotPresentException(interface_, declaringClasses, visited)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean declaresTypeNotPresentException(J.CompilationUnit cu) {
        AtomicBoolean found = new AtomicBoolean(false);
        new JavaIsoVisitor<AtomicBoolean>() {
            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, AtomicBoolean found) {
                if (TYPE_NOT_PRESENT_EXCEPTION_SIMPLE_NAME.equals(classDecl.getSimpleName())) {
                    found.set(true);
                    return classDecl;
                }
                return super.visitClassDeclaration(classDecl, found);
            }

            @Override
            public J.TypeParameter visitTypeParameter(J.TypeParameter typeParameter, AtomicBoolean found) {
                if (typeParameter.getName() instanceof J.Identifier &&
                        TYPE_NOT_PRESENT_EXCEPTION_SIMPLE_NAME.equals(((J.Identifier) typeParameter.getName()).getSimpleName())) {
                    found.set(true);
                    return typeParameter;
                }
                return super.visitTypeParameter(typeParameter, found);
            }
        }.visit(cu, found);
        return found.get();
    }

    /**
     * Assembled directly rather than through {@code JavaTemplate}: a catch parameter is not an insertion point,
     * and regenerating the catch would discard the type expression as written along with the parameter's
     * modifiers and annotations. Keeping it as the first alternative preserves all of that, as
     * {@code CombineSemanticallyEqualCatchBlocks} does upstream.
     */
    private static J.Try.Catch alsoCatchTypeNotPresentException(J.Try.Catch catch_, boolean qualify) {
        J.VariableDeclarations parameter = catch_.getParameter().getTree();
        TypeTree typeExpression = parameter.getTypeExpression();
        if (typeExpression == null) {
            return catch_;
        }
        TypeTree typeNotPresentException;
        if (qualify) {
            TypeTree qualified = TypeTree.build(TYPE_NOT_PRESENT_EXCEPTION);
            qualified = qualified.withType(JavaType.ShallowClass.build(TYPE_NOT_PRESENT_EXCEPTION));
            typeNotPresentException = qualified.withPrefix(Space.SINGLE_SPACE);
        } else {
            typeNotPresentException = new J.Identifier(Tree.randomId(), Space.SINGLE_SPACE, Markers.EMPTY,
                    emptyList(), TYPE_NOT_PRESENT_EXCEPTION_SIMPLE_NAME, JavaType.ShallowClass.build(TYPE_NOT_PRESENT_EXCEPTION), null);
        }
        J.MultiCatch multiCatch = new J.MultiCatch(Tree.randomId(), typeExpression.getPrefix(), Markers.EMPTY, asList(
                JRightPadded.<NameTree>build(typeExpression.withPrefix(Space.EMPTY)).withAfter(Space.SINGLE_SPACE),
                JRightPadded.build(typeNotPresentException)));
        return catch_.withParameter(catch_.getParameter().withTree(parameter.withTypeExpression(multiCatch)));
    }
}
