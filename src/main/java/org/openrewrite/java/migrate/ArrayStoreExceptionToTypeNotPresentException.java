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
     * The supertypes of {@code RuntimeException}, a closed set because {@code java.lang} can not be extended.
     * A position declared with one of these types accepts every {@code RuntimeException}, so it keeps compiling
     * and keeps accepting the same values when the catch parameter's type widens from
     * {@code ArrayStoreException} to {@code RuntimeException}. Unresolved types are not in the set, so they
     * conservatively block the widening.
     */
    private static final Set<String> SUPERTYPES_OF_RUNTIME_EXCEPTION = new HashSet<>(asList(
            "java.lang.RuntimeException", "java.lang.Exception", "java.lang.Throwable",
            "java.lang.Object", "java.io.Serializable"));

    /**
     * Types that accept any {@code Class} value regardless of its type argument: raw {@code Class} itself, and
     * the supertypes of {@code Class} a value is realistically declared with. Any supertype not listed here,
     * such as {@code java.lang.constant.Constable} (Java 12+), conservatively blocks the widening, as do
     * unresolved types.
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
     * Where the sources declare their own class named {@code TypeNotPresentException}, the spliced simple name
     * would resolve to it instead of to {@code java.lang.TypeNotPresentException}, either failing to compile or,
     * worse, silently catching the wrong type. The scanner records where such classes are declared so the
     * visitor can emit the fully qualified name at the affected sites.
     * <p>
     * The declarations are scoped per {@link JavaProject} marker: only a declaration in the same module can
     * shadow the simple name at compile time, so one module's {@code TypeNotPresentException} does not qualify
     * the name in the other modules of a multi-module repository. Sources without the marker share one scope.
     */
    public static class Accumulator {
        /**
         * Per project, the packages declaring a top-level class named {@code TypeNotPresentException},
         * {@code ""} for the default package.
         */
        private final Map<@Nullable JavaProject, Set<String>> packagesByProject = new HashMap<>();

        /**
         * Per project, the classes declaring a nested class named {@code TypeNotPresentException}, which
         * shadows through inheritance and through on-demand imports.
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
     * Only the resources and the body of a try are protected by its catches. A call in a catch or in the finally
     * block runs outside that region, and so do the method bodies of a lambda, anonymous class or local class
     * created inside the try. The instance initializers of such a class do run inside the protected region, but
     * are left out as well; that only costs a migration that is not applied.
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
                    // Constructor arguments are evaluated here, and so are the anonymous class's instance
                    // initializers; only its method bodies are deferred. The whole body is left out anyway,
                    // which only costs a migration that is not applied
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
     * A catch of an enclosing try whose protected region contains this try is reached by every
     * {@code TypeNotPresentException} this try does not catch. Widening a catch here would intercept those
     * exceptions before the enclosing handler sees them, silently rerouting them, so any enclosing try that
     * concerns itself with {@code TypeNotPresentException} blocks the widening. Only enclosing tries whose
     * body or resources contain this try count: from a catch or finally block the enclosing catches are no
     * longer reachable. The walk deliberately does not stop at lambda or class boundaries, whose bodies may
     * run inside the enclosing protected region; that errs towards not widening.
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
     * A catch of a supertype of {@code TypeNotPresentException} already handles it, and a catch of
     * {@code TypeNotPresentException} itself or of a subclass would become unreachable if it were added elsewhere.
     */
    private static boolean concernsTypeNotPresentException(@Nullable JavaType type) {
        JavaType.FullyQualified fullyQualified = TypeUtils.asFullyQualified(type);
        return fullyQualified != null &&
                (HANDLES_TYPE_NOT_PRESENT_EXCEPTION.contains(fullyQualified.getFullyQualifiedName()) ||
                        TypeUtils.isAssignableTo(TYPE_NOT_PRESENT_EXCEPTION, fullyQualified));
    }

    /**
     * Per JLS 14.20 a multi-catch parameter is implicitly final and its type is the least upper bound of the
     * alternatives, here {@code RuntimeException}. Widening therefore breaks any handler that assigns to the
     * parameter or uses it where the narrower {@code ArrayStoreException} type is required. Rather than
     * enumerating the ways a handler can depend on the narrower type, every reference to the parameter must
     * occur in a context that provably tolerates the wider type; any reference in an unrecognized context
     * means the catch is left untouched.
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
     * Whether this identifier is a use of the catch parameter. Identifiers that are provably something else, a
     * method or member name, a declaration or a label, are skipped. When variable attribution is missing the
     * identifier can not be told apart from the parameter, so it is conservatively treated as a use.
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
     * Whether an expression whose static type the widening changes from {@code ArrayStoreException} to
     * {@code RuntimeException} keeps compiling, and keeps the same meaning, in its enclosing context. This is
     * an allow-list: only contexts that provably tolerate the wider type are accepted, everything else fails
     * safe. In particular an expression-bodied lambda, a switch, or any unforeseen context blocks the widening.
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
            // The reference operations valid on an ArrayStoreException, string concatenation, == and !=,
            // a type test, throwing (RuntimeException is unchecked) and an assert message, all remain valid
            // and unchanged in behavior for the values the original handler could receive
            return true;
        }
        if (parent instanceof J.AssignmentOperation) {
            // Of the compound assignments only String's += compiles with an exception operand, and
            // concatenation tolerates any RuntimeException; the parameter as the assigned variable fails safe
            return expression == ((J.AssignmentOperation) parent).getAssignment();
        }
        if (parent instanceof J.ControlParentheses) {
            // Of the statements that parenthesize a bare expression, only a synchronized monitor keeps its
            // meaning with a widened operand, any object being a valid monitor; a pattern switch selector
            // is deliberately excluded
            return parentCursor.getParentTreeCursor().getValue() instanceof J.Synchronized;
        }
        if (parent instanceof J.TypeCast) {
            // The cast's own type does not change, but a cast to a type narrower than RuntimeException would
            // throw ClassCastException for the TypeNotPresentException values the widened handler receives
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
            // The reference's result type would have to be checked against the functional interface's method,
            // which is not reliably recoverable here, so a receiver-dependent result fails safe
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
     * A parent that holds the expression as a statement discards its value, so the expression keeps compiling
     * no matter how its type widens. Only reachable by recursion, since a bare identifier is not a statement.
     * The unbraced forms, {@code if (flag) Objects.requireNonNull(e);}, discard the value exactly like a
     * block does. A switch's arrow case is deliberately absent: there the expression may be the switch's own
     * value.
     */
    private static boolean isStatementPosition(J parent) {
        return parent instanceof J.Block || parent instanceof J.If || parent instanceof J.If.Else ||
                parent instanceof J.Label || parent instanceof J.WhileLoop || parent instanceof J.DoWhileLoop ||
                parent instanceof J.ForLoop || parent instanceof J.ForEachLoop;
    }

    /**
     * A method resolved against the parameter's receiver remains available when its declaring type is a
     * supertype of {@code RuntimeException}; {@code ArrayStoreException} declares no methods of its own, so
     * this holds for every resolvable call, and an unresolved one blocks the widening.
     */
    private static boolean invokedMethodRemainsAvailable(JavaType.@Nullable Method methodType) {
        return methodType != null && acceptsAnyRuntimeException(methodType.getDeclaringType());
    }

    /**
     * Whether the invocation's own result type widens along with its receiver. Per JLS 4.3.2 the type of
     * {@code e.getClass()} is {@code Class<? extends |E|>} where |E| is the erasure of the receiver's STATIC
     * type, so widening the receiver silently changes the result from
     * {@code Class<? extends ArrayStoreException>} to {@code Class<? extends RuntimeException>}.
     * {@code getClass()} is the only receiver-polymorphic member in {@code java.lang} and the parser
     * attributes it with its declared signature, so it is recognized by name and arity; a resolved signature
     * that mentions {@code ArrayStoreException} or a type variable is treated the same way, which also covers
     * a future member whose site-specific attribution exposes the dependence.
     */
    private static boolean resultTypeDependsOnReceiverType(JavaType.Method methodType) {
        return "getClass".equals(methodType.getName()) && methodType.getParameterTypes().isEmpty() ||
                involvesReceiverTypeArgument(methodType.getReturnType(), newIdentitySet());
    }

    /**
     * Whether the context of an invocation whose result type widens from
     * {@code Class<? extends ArrayStoreException>} to {@code Class<? extends RuntimeException>} tolerates the
     * wider result. Mirrors {@link #widenedReferenceIsSafe} with the acceptance test adjusted to the class
     * type; everything unrecognized fails safe.
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
            // Widening the wildcard's bound never breaks string concatenation, and it never removes the
            // cast-compatibility that == and != require: every type castable to Class<? extends
            // ArrayStoreException> is also castable to Class<? extends RuntimeException>
            J.Binary.Type operator = ((J.Binary) parent).getOperator();
            return operator == J.Binary.Type.Addition || operator == J.Binary.Type.Equal ||
                    operator == J.Binary.Type.NotEqual;
        }
        if (parent instanceof J.MethodInvocation) {
            J.MethodInvocation invocation = (J.MethodInvocation) parent;
            if (expression == invocation.getSelect()) {
                // A chained call is a member of Class, so it stays available; it remains valid exactly when
                // nothing in its resolved signature involves the receiver's type argument, as with getName().
                // A signature that does, as with cast() or getDeclaredConstructor(), fails safe
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
     * Whether the type mentions {@code ArrayStoreException}, a type variable, a wildcard or an unresolved
     * type anywhere in its structure, in which case it can not be relied on to survive the widening.
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
     * Whether a position declared with this type accepts a {@code Class<? extends RuntimeException>}: raw
     * {@code Class} or a supertype of it, {@code Class<?>}, or {@code Class} of a covariant wildcard whose
     * every bound accepts any {@code RuntimeException}.
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
     * The return type of the method declaration enclosing this return statement, or null from a lambda, whose
     * functional interface's return type is not reliably recoverable here.
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
     * The resolved method type reports the inferred argument type: {@code Objects.requireNonNull(e)} reports
     * its parameter as {@code ArrayStoreException} although the declaration is {@code <T> T requireNonNull(T)}
     * and would simply re-infer {@code T = RuntimeException} after the widening. Consult the declaration:
     * widening is safe when the parameter is a type variable of the method itself (a class type variable is
     * fixed by the receiver and can not re-infer), every bound accepts any {@code RuntimeException}, no other
     * parameter constrains the same variable, and a result whose type mentions the variable is itself only
     * used where the widened type is acceptable.
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
     * The single declaration matching the resolved method by name and arity, or null when it can not be
     * identified unambiguously.
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
     * Whether the declaring class or one of its owning classes declares a type variable of this name. A method
     * reusing such a name declares its own variable, so this errs towards attributing the variable to the
     * class, which only blocks a widening that may have been safe.
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
     * Whether the simple name {@code TypeNotPresentException} at this try would resolve to anything other
     * than {@code java.lang.TypeNotPresentException}: a class or type parameter of that name declared in this
     * file, a single-type import of another such class, a top-level class of that name in this file's package
     * or reachable through an on-demand import, a nested class of that name inherited from a supertype of an
     * enclosing class, or any other such type already referenced in this file. A shadowing class that exists
     * only as a compiled dependency, never as a source in this run and never referenced in this file, is not
     * visible here; the simple name is emitted for it. A class declared in a different {@link JavaProject} is
     * treated the same way: it can only shadow here by being on this module's compile classpath, which the
     * markers do not reveal, so it is handled like any other compiled dependency.
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
     * The {@link JavaProject} marker of this source file, or null where the build did not attach one, as in a
     * single-module parse; every unmarked source then shares the null scope.
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
     * The multi-catch is assembled directly rather than through {@code JavaTemplate}: a catch parameter is not
     * a template insertion point ({@code J.Try.Catch} and {@code J.MultiCatch} have no coordinates), and
     * regenerating the whole catch from a template would discard the original type expression as written along
     * with the parameter's modifiers and annotations. Keeping the existing type expression as the first
     * alternative and splicing in the one new name preserves all of that, as
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
