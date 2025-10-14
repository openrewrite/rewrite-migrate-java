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
package org.openrewrite.java.migrate.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.SearchResult;

import java.util.HashSet;
import java.util.Set;

@EqualsAndHashCode(callSuper = false)
public class FindImmutableThreadLocalVariables extends ScanningRecipe<FindImmutableThreadLocalVariables.ThreadLocalAccumulator> {

    private static final MethodMatcher THREAD_LOCAL_SET = new MethodMatcher("java.lang.ThreadLocal set(..)");
    private static final MethodMatcher THREAD_LOCAL_REMOVE = new MethodMatcher("java.lang.ThreadLocal remove()");
    private static final MethodMatcher INHERITABLE_THREAD_LOCAL_SET = new MethodMatcher("java.lang.InheritableThreadLocal set(..)");
    private static final MethodMatcher INHERITABLE_THREAD_LOCAL_REMOVE = new MethodMatcher("java.lang.InheritableThreadLocal remove()");

    @Override
    public String getDisplayName() {
        return "Find immutable ThreadLocal variables";
    }

    @Override
    public String getDescription() {
        return "Find `ThreadLocal` variables that are never mutated and could be candidates for migration to `ScopedValue` in Java 25+. " +
               "This recipe identifies `ThreadLocal` variables that are only initialized but never reassigned or modified through `set()` or `remove()` methods. " +
               "Note: This recipe only analyzes mutations within the same source file. ThreadLocal fields accessible from other classes " +
               "(public, protected, or package-private) may be mutated elsewhere in the codebase.";
    }

    @Value
    static class ThreadLocalAccumulator {
        Set<ThreadLocalVariable> allThreadLocals = new HashSet<>();
        Set<ThreadLocalVariable> mutatedThreadLocals = new HashSet<>();

        public boolean isImmutable(ThreadLocalVariable var) {
            return allThreadLocals.contains(var) && !mutatedThreadLocals.contains(var);
        }
    }

    @Value
    static class ThreadLocalVariable {
        String name;
        String className;

        static ThreadLocalVariable fromVariable(J.VariableDeclarations.NamedVariable variable, J.ClassDeclaration classDecl) {
            String className = classDecl != null && classDecl.getType() != null ?
                    classDecl.getType().getFullyQualifiedName() : "";
            return new ThreadLocalVariable(variable.getName().getSimpleName(), className);
        }
    }

    @Override
    public ThreadLocalAccumulator getInitialValue(ExecutionContext ctx) {
        return new ThreadLocalAccumulator();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(ThreadLocalAccumulator acc) {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, ExecutionContext ctx) {
                variable = super.visitVariable(variable, ctx);

                if (isThreadLocalType(variable.getType())) {
                    J.ClassDeclaration classDecl = getCursor().firstEnclosing(J.ClassDeclaration.class);
                    ThreadLocalVariable tlVar = ThreadLocalVariable.fromVariable(variable, classDecl);
                    acc.allThreadLocals.add(tlVar);

                    // Check if this is a local variable - we don't mark local variables
                    J.Block enclosingBlock = getCursor().firstEnclosing(J.Block.class);
                    J.MethodDeclaration enclosingMethod = getCursor().firstEnclosing(J.MethodDeclaration.class);
                    if (enclosingMethod != null && enclosingBlock != null && enclosingBlock != enclosingMethod.getBody()) {
                        // This is a local variable inside a method
                        acc.mutatedThreadLocals.add(tlVar);
                    }
                }
                return variable;
            }

            @Override
            public J.Assignment visitAssignment(J.Assignment assignment, ExecutionContext ctx) {
                assignment = super.visitAssignment(assignment, ctx);

                // Check if we're reassigning a ThreadLocal variable
                if (assignment.getVariable() instanceof J.Identifier) {
                    J.Identifier id = (J.Identifier) assignment.getVariable();
                    if (isThreadLocalType(id.getType())) {
                        J.ClassDeclaration classDecl = getCursor().firstEnclosing(J.ClassDeclaration.class);
                        String className = classDecl != null && classDecl.getType() != null ?
                                classDecl.getType().getFullyQualifiedName() : "";
                        acc.mutatedThreadLocals.add(new ThreadLocalVariable(id.getSimpleName(), className));
                    }
                }
                return assignment;
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                method = super.visitMethodInvocation(method, ctx);

                // Check for ThreadLocal.set() or ThreadLocal.remove() calls
                if (THREAD_LOCAL_SET.matches(method) || THREAD_LOCAL_REMOVE.matches(method) ||
                    INHERITABLE_THREAD_LOCAL_SET.matches(method) || INHERITABLE_THREAD_LOCAL_REMOVE.matches(method)) {

                    J.ClassDeclaration classDecl = getCursor().firstEnclosing(J.ClassDeclaration.class);
                    String className = classDecl != null && classDecl.getType() != null ?
                            classDecl.getType().getFullyQualifiedName() : "";

                    // Get the ThreadLocal instance being mutated
                    String varName = null;
                    if (method.getSelect() instanceof J.Identifier) {
                        varName = ((J.Identifier) method.getSelect()).getSimpleName();
                    } else if (method.getSelect() instanceof J.FieldAccess) {
                        varName = ((J.FieldAccess) method.getSelect()).getSimpleName();
                    }

                    if (varName != null) {
                        acc.mutatedThreadLocals.add(new ThreadLocalVariable(varName, className));
                    }
                }
                return method;
            }

            @Override
            public J.Unary visitUnary(J.Unary unary, ExecutionContext ctx) {
                unary = super.visitUnary(unary, ctx);

                // Check for operations that would mutate the ThreadLocal reference (unlikely but thorough)
                if (unary.getExpression() instanceof J.Identifier) {
                    J.Identifier id = (J.Identifier) unary.getExpression();
                    if (isThreadLocalType(id.getType())) {
                        switch (unary.getOperator()) {
                            case PreIncrement:
                            case PreDecrement:
                            case PostIncrement:
                            case PostDecrement:
                                J.ClassDeclaration classDecl = getCursor().firstEnclosing(J.ClassDeclaration.class);
                                String className = classDecl != null && classDecl.getType() != null ?
                                        classDecl.getType().getFullyQualifiedName() : "";
                                acc.mutatedThreadLocals.add(new ThreadLocalVariable(id.getSimpleName(), className));
                                break;
                            default:
                                break;
                        }
                    }
                }
                return unary;
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(ThreadLocalAccumulator acc) {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                multiVariable = super.visitVariableDeclarations(multiVariable, ctx);

                // Check if any of the variables in this declaration are immutable ThreadLocals
                J.ClassDeclaration classDecl = getCursor().firstEnclosing(J.ClassDeclaration.class);
                for (J.VariableDeclarations.NamedVariable variable : multiVariable.getVariables()) {
                    if (isThreadLocalType(variable.getType())) {
                        ThreadLocalVariable tlVar = ThreadLocalVariable.fromVariable(variable, classDecl);
                        if (acc.isImmutable(tlVar)) {
                            // Only mark private fields as candidates, since public/protected/package-private
                            // fields could be mutated from other classes
                            boolean isPrivate = multiVariable.getModifiers().stream()
                                    .anyMatch(mod -> mod.getType() == J.Modifier.Type.Private);

                            String message = isPrivate
                                ? "ThreadLocal candidate for ScopedValue migration - never mutated after initialization"
                                : "ThreadLocal candidate for ScopedValue migration - never mutated in this file (but may be mutated elsewhere due to non-private access)";

                            return SearchResult.found(multiVariable, message);
                        }
                    }
                }

                return multiVariable;
            }
        };
    }

    private static boolean isThreadLocalType(JavaType type) {
        return TypeUtils.isOfClassType(type, "java.lang.ThreadLocal") ||
               TypeUtils.isOfClassType(type, "java.lang.InheritableThreadLocal");
    }
}