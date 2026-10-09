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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

public class ArrayStoreExceptionToTypeNotPresentException extends ScanningRecipe<ArrayStoreExceptionToTypeNotPresentException.Accumulator> {

    private static final String ARRAY_STORE_EXCEPTION = "java.lang.ArrayStoreException";
    private static final String TYPE_NOT_PRESENT_EXCEPTION = "java.lang.TypeNotPresentException";
    private static final String TYPE_NOT_PRESENT_EXCEPTION_SIMPLE_NAME = "TypeNotPresentException";
    private static final String PACKAGE_SEPARATOR = ".";
    private static final String NESTED_TYPE_SEPARATOR = "$";
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

    @Getter
    final String displayName = "Catch `TypeNotPresentException` thrown by `Class.getAnnotation()`";

    @Getter
    final String description = "Also catch `TypeNotPresentException` where `ArrayStoreException` is caught around `Class.getAnnotation()` to ensure compatibility with Java 11+. " +
            "The `ArrayStoreException` is retained as the protected code can still throw it for reasons unrelated to annotations.";

    /**
     * Where the sources declare their own {@code TypeNotPresentException}, the spliced simple name could
     * resolve to it instead, either failing to compile or silently catching the wrong type, so the scanner
     * records those declarations and the visitor leaves the affected sources unchanged. Scoped per
     * {@link JavaProject} marker, since only a same-module declaration can shadow; unmarked sources share
     * one scope.
     */
    public static class Accumulator {
        private final Set<@Nullable JavaProject> declaringProjects = new HashSet<>();

        void recordDeclaration(@Nullable JavaProject project) {
            declaringProjects.add(project);
        }

        boolean declaresTypeNotPresentException(@Nullable JavaProject project) {
            return declaringProjects.contains(project);
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
                    acc.recordDeclaration(javaProject(getCursor().firstEnclosing(JavaSourceFile.class)));
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
                        anyEnclosingCatchConcernsTypeNotPresentException(getCursor()) ||
                        typeNotPresentExceptionSimpleNameIsShadowed((J.CompilationUnit) sourceFile, acc, javaProject(sourceFile))) {
                    return try_;
                }
                Cursor tryCursor = getCursor();
                return try_.withCatches(ListUtils.map(try_.getCatches(), catch_ -> {
                    if (TypeUtils.isOfClassType(catch_.getParameter().getType(), ARRAY_STORE_EXCEPTION) &&
                            allParameterReferencesSurviveWidening(catch_, tryCursor)) {
                        return alsoCatchTypeNotPresentException(catch_);
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
     * alternatives, here {@code RuntimeException}. Rather than prove every way a handler can depend on the
     * narrower type safe, only a short allow list of common contexts is recognized; anything else leaves the
     * catch untouched, which only costs a migration that is not applied.
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
     * Whether an expression the widening retypes keeps compiling, and keeps its meaning, in its context. A
     * deliberately short allow list covering the common handler shapes; anything unrecognized blocks the
     * widening.
     */
    private static boolean widenedReferenceIsSafe(Cursor cursor) {
        J expression = cursor.getValue();
        Cursor parentCursor = cursor.getParentTreeCursor();
        J parent = parentCursor.getValue();
        if (parent instanceof J.Parentheses) {
            // The parenthesized expression widens with its content
            return widenedReferenceIsSafe(parentCursor);
        }
        if (parent instanceof J.Binary || parent instanceof J.InstanceOf || parent instanceof J.Throw) {
            // Concatenation, `==`/`!=`, a type test and throwing (`RuntimeException` is unchecked) all stay
            // valid and unchanged for the values the original handler could receive
            return true;
        }
        if (parent instanceof J.MethodInvocation) {
            J.MethodInvocation invocation = (J.MethodInvocation) parent;
            if (expression == invocation.getSelect()) {
                // Per JLS 4.3.2 `e.getClass()` is `Class<? extends |E|>` over the receiver's *static* type, so
                // its result widens with the receiver and blocks the widening
                return invokedMethodRemainsAvailable(invocation.getMethodType()) &&
                        !"getClass".equals(invocation.getSimpleName());
            }
            return argumentRemainsCompatible(invocation.getMethodType(), invocation.getArguments().indexOf(expression));
        }
        if (parent instanceof J.NewClass) {
            return argumentRemainsCompatible(((J.NewClass) parent).getMethodType(),
                    ((J.NewClass) parent).getArguments().indexOf(expression));
        }
        if (parent instanceof J.VariableDeclarations.NamedVariable) {
            // Covers an explicit declared type; `var` infers the narrower type and is rejected here
            J.VariableDeclarations.NamedVariable variable = (J.VariableDeclarations.NamedVariable) parent;
            return expression == variable.getInitializer() && acceptsAnyRuntimeException(variable.getType());
        }
        if (parent instanceof J.Assignment) {
            // The parameter as the assigned variable fails: a multi-catch parameter is implicitly final
            J.Assignment assignment = (J.Assignment) parent;
            return expression != assignment.getVariable() && acceptsAnyRuntimeException(assignment.getVariable().getType());
        }
        return false;
    }

    /**
     * A method stays available when its declaring type is a supertype of {@code RuntimeException}, which holds
     * for every resolvable call since {@code ArrayStoreException} declares none of its own.
     */
    private static boolean invokedMethodRemainsAvailable(JavaType.@Nullable Method methodType) {
        return methodType != null && acceptsAnyRuntimeException(methodType.getDeclaringType());
    }

    private static boolean argumentRemainsCompatible(JavaType.@Nullable Method methodType, int argumentIndex) {
        if (methodType == null || argumentIndex < 0) {
            // Unresolved: the parameter's requirements are unknowable, so leave the catch alone
            return false;
        }
        // The resolved type reports the *inferred* argument type, so a generic parameter such as
        // `<T> T requireNonNull(T)` resolves to `ArrayStoreException` and is rejected with it
        JavaType parameterType = parameterType(methodType, argumentIndex);
        return parameterType != null && acceptsAnyRuntimeException(parameterType);
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

    private static boolean acceptsAnyRuntimeException(@Nullable JavaType type) {
        JavaType.FullyQualified fullyQualified = TypeUtils.asFullyQualified(type);
        return fullyQualified != null && SUPERTYPES_OF_RUNTIME_EXCEPTION.contains(fullyQualified.getFullyQualifiedName());
    }

    /**
     * Whether the simple name could resolve to anything but {@code java.lang.TypeNotPresentException}: a class
     * of that name declared anywhere in this file's {@link JavaProject}, a type parameter of that name in this
     * file, a single-type import of another such class, or any such type already referenced here. Such sources
     * are left unchanged; emitting a qualified name instead was judged not worth the machinery. A shadowing
     * class visible only as a compiled dependency, or declared in another project, cannot be seen from here
     * and is not detected.
     */
    private static boolean typeNotPresentExceptionSimpleNameIsShadowed(J.CompilationUnit cu, Accumulator acc,
                                                                       @Nullable JavaProject project) {
        if (acc.declaresTypeNotPresentException(project)) {
            return true;
        }
        for (J.Import import_ : cu.getImports()) {
            if (TYPE_NOT_PRESENT_EXCEPTION_SIMPLE_NAME.equals(import_.getQualid().getSimpleName())) {
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
        return declaresTypeNotPresentException(cu);
    }

    /**
     * The {@link JavaProject} marker of this source file, or null where the build attached none, as in a
     * single-module parse; unmarked sources share the null scope.
     */
    private static @Nullable JavaProject javaProject(@Nullable JavaSourceFile sourceFile) {
        return sourceFile == null ? null : sourceFile.getMarkers().findFirst(JavaProject.class).orElse(null);
    }

    private static boolean isForeignTypeNotPresentException(String fullyQualifiedName) {
        return !TYPE_NOT_PRESENT_EXCEPTION.equals(fullyQualifiedName) &&
                (TYPE_NOT_PRESENT_EXCEPTION_SIMPLE_NAME.equals(fullyQualifiedName) ||
                        fullyQualifiedName.endsWith(PACKAGE_SEPARATOR + TYPE_NOT_PRESENT_EXCEPTION_SIMPLE_NAME) ||
                        fullyQualifiedName.endsWith(NESTED_TYPE_SEPARATOR + TYPE_NOT_PRESENT_EXCEPTION_SIMPLE_NAME));
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
    private static J.Try.Catch alsoCatchTypeNotPresentException(J.Try.Catch catch_) {
        J.VariableDeclarations parameter = catch_.getParameter().getTree();
        TypeTree typeExpression = parameter.getTypeExpression();
        if (typeExpression == null) {
            return catch_;
        }
        TypeTree typeNotPresentException = new J.Identifier(Tree.randomId(), Space.SINGLE_SPACE, Markers.EMPTY,
                emptyList(), TYPE_NOT_PRESENT_EXCEPTION_SIMPLE_NAME, JavaType.ShallowClass.build(TYPE_NOT_PRESENT_EXCEPTION), null);
        J.MultiCatch multiCatch = new J.MultiCatch(Tree.randomId(), typeExpression.getPrefix(), Markers.EMPTY, asList(
                JRightPadded.<NameTree>build(typeExpression.withPrefix(Space.EMPTY)).withAfter(Space.SINGLE_SPACE),
                JRightPadded.build(typeNotPresentException)));
        return catch_.withParameter(catch_.getParameter().withTree(parameter.withTypeExpression(multiCatch)));
    }
}
