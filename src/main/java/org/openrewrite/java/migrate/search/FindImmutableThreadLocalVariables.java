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
import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.SearchResult;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.openrewrite.Preconditions.*;

@EqualsAndHashCode(callSuper = false)
public class FindImmutableThreadLocalVariables extends ScanningRecipe<FindImmutableThreadLocalVariables.ThreadLocalUsageAccumulator> {

    private static final String THREAD_LOCAL_FQN = "java.lang.ThreadLocal";
    private static final String INHERITED_THREAD_LOCAL_FQN = "java.lang.InheritableThreadLocal";
    private static final MethodMatcher THREAD_LOCAL_SET = new MethodMatcher(THREAD_LOCAL_FQN + " set(..)");
    private static final MethodMatcher THREAD_LOCAL_REMOVE = new MethodMatcher(THREAD_LOCAL_FQN + " remove()");
    private static final MethodMatcher INHERITABLE_THREAD_LOCAL_SET = new MethodMatcher(INHERITED_THREAD_LOCAL_FQN + " set(..)");
    private static final MethodMatcher INHERITABLE_THREAD_LOCAL_REMOVE = new MethodMatcher(INHERITED_THREAD_LOCAL_FQN + " remove()");

    @Override
    public String getDisplayName() {
        return "Find immutable ThreadLocal variables";
    }

    @Override
    public String getDescription() {
        return "Find `ThreadLocal` variables that are never mutated and could be candidates for migration to `ScopedValue` in Java 25+. " + "This recipe identifies `ThreadLocal` variables that are only initialized but never reassigned or modified through `set()` or `remove()` methods. " + "The recipe analyzes mutations across all source files in the project to provide accurate results.";
    }

    @Value
    static class ThreadLocalUsageAccumulator {
        // Map of ThreadLocal declarations: fullyQualifiedName -> declaration info
        Map<String, ThreadLocalDeclaration> declarations = new HashMap<>();
        // Map of ThreadLocal mutations: fullyQualifiedName -> list of mutation locations
        Map<String, List<MutationLocation>> mutations = new HashMap<>();
        // Map of ThreadLocal accesses: fullyQualifiedName -> list of access locations
        Map<String, List<AccessLocation>> accesses = new HashMap<>();

        void addDeclaration(String fullyQualifiedName, ThreadLocalDeclaration declaration) {
            declarations.put(fullyQualifiedName, declaration);
        }

        void addMutation(String fullyQualifiedName, MutationLocation mutation) {
            mutations.computeIfAbsent(fullyQualifiedName, k -> new ArrayList<>()).add(mutation);
        }

        void addAccess(String fullyQualifiedName, AccessLocation access) {
            accesses.computeIfAbsent(fullyQualifiedName, k -> new ArrayList<>()).add(access);
        }

        boolean hasExternalMutations(String fullyQualifiedName) {
            List<MutationLocation> mutationList = mutations.get(fullyQualifiedName);
            if (mutationList == null || mutationList.isEmpty()) {
                return false;
            }

            ThreadLocalDeclaration declaration = declarations.get(fullyQualifiedName);
            if (declaration == null) {
                return true; // Conservative: if we can't find the declaration, assume it can be mutated
            }

            // Check if any mutation is from a different file than the declaration
            for (MutationLocation mutation : mutationList) {
                if (!mutation.getSourcePath().equals(declaration.getSourcePath())) {
                    return true;
                }
            }
            return false;
        }

        boolean hasMutations(String fullyQualifiedName) {
            return mutations.containsKey(fullyQualifiedName) && !mutations.get(fullyQualifiedName).isEmpty();
        }
    }

    @Value
    static class ThreadLocalDeclaration {
        String fullyQualifiedName;  // e.g., "com.example.MyClass.MY_THREAD_LOCAL"
        String className;
        String fieldName;
        boolean isPrivate;
        boolean isStatic;
        boolean isFinal;
        Path sourcePath;
    }

    @Value
    static class MutationLocation {
        Path sourcePath;
        String className;
        String methodName;
        MutationType type;

        enum MutationType {
            REASSIGNMENT, SET_CALL, REMOVE_CALL, OTHER
        }
    }

    @Value
    static class AccessLocation {
        Path sourcePath;
        String className;
        String methodName;
        AccessType type;

        enum AccessType {
            GET_CALL, FIELD_READ, OTHER
        }
    }

    @Override
    public ThreadLocalUsageAccumulator getInitialValue(ExecutionContext ctx) {
        return new ThreadLocalUsageAccumulator();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(ThreadLocalUsageAccumulator acc) {
        return check(
                and(new UsesJavaVersion<>(25),
                        or(new UsesType<>(THREAD_LOCAL_FQN, true), new UsesType<>(INHERITED_THREAD_LOCAL_FQN, true))),
                new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                        // Store the current compilation unit's path for tracking locations
                        getCursor().putMessage("currentSourcePath", cu.getSourcePath());
                        return super.visitCompilationUnit(cu, ctx);
                    }

                    @Override
                    public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, ExecutionContext ctx) {
                        variable = super.visitVariable(variable, ctx);

                        if (isThreadLocalType(variable.getType())) {
                            // Check if this is a field (not a local variable)
                            // A field is declared directly in a class, not inside a method
                            J.MethodDeclaration enclosingMethod = getCursor().firstEnclosing(J.MethodDeclaration.class);
                            J.ClassDeclaration classDecl = getCursor().firstEnclosing(J.ClassDeclaration.class);

                            // It's a field if there's no enclosing method, or if the variable is declared
                            // directly in the class body (not inside a method body)
                            boolean isField = classDecl != null && (enclosingMethod == null || getCursor().getPathAsStream().filter(J.Block.class::isInstance).map(J.Block.class::cast).noneMatch(block -> block == enclosingMethod.getBody()));

                            if (isField) {
                                J.VariableDeclarations variableDecls = getCursor().firstEnclosing(J.VariableDeclarations.class);

                                if (classDecl != null && variableDecls != null) {
                                    String className = classDecl.getType() != null ? classDecl.getType().getFullyQualifiedName() : "UnknownClass";
                                    String fieldName = variable.getName().getSimpleName();
                                    String fullyQualifiedName = className + "." + fieldName;

                                    boolean isPrivate = variableDecls.getModifiers().stream().anyMatch(mod -> mod.getType() == J.Modifier.Type.Private);
                                    boolean isStatic = variableDecls.getModifiers().stream().anyMatch(mod -> mod.getType() == J.Modifier.Type.Static);
                                    boolean isFinal = variableDecls.getModifiers().stream().anyMatch(mod -> mod.getType() == J.Modifier.Type.Final);

                                    Path sourcePath = getCursor().firstEnclosingOrThrow(SourceFile.class).getSourcePath();

                                    ThreadLocalDeclaration declaration = new ThreadLocalDeclaration(fullyQualifiedName, className, fieldName, isPrivate, isStatic, isFinal, sourcePath);

                                    acc.addDeclaration(fullyQualifiedName, declaration);
                                }
                            }
                        }
                        return variable;
                    }

                    @Override
                    public J.Assignment visitAssignment(J.Assignment assignment, ExecutionContext ctx) {
                        assignment = super.visitAssignment(assignment, ctx);

                        // Check if we're reassigning a ThreadLocal field
                        if (isThreadLocalFieldAccess(assignment.getVariable())) {
                            String fullyQualifiedName = getFieldFullyQualifiedName(assignment.getVariable());
                            if (fullyQualifiedName != null) {
                                Path sourcePath = getCursor().firstEnclosingOrThrow(SourceFile.class).getSourcePath();
                                J.ClassDeclaration classDecl = getCursor().firstEnclosing(J.ClassDeclaration.class);
                                J.MethodDeclaration methodDecl = getCursor().firstEnclosing(J.MethodDeclaration.class);

                                String className = classDecl != null && classDecl.getType() != null ? classDecl.getType().getFullyQualifiedName() : "UnknownClass";
                                String methodName = methodDecl != null ? methodDecl.getSimpleName() : "UnknownMethod";

                                MutationLocation mutation = new MutationLocation(sourcePath, className, methodName, MutationLocation.MutationType.REASSIGNMENT);
                                acc.addMutation(fullyQualifiedName, mutation);
                            }
                        }
                        return assignment;
                    }

                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                        method = super.visitMethodInvocation(method, ctx);

                        // Check for ThreadLocal.set() or ThreadLocal.remove() calls
                        if (THREAD_LOCAL_SET.matches(method) || THREAD_LOCAL_REMOVE.matches(method) || INHERITABLE_THREAD_LOCAL_SET.matches(method) || INHERITABLE_THREAD_LOCAL_REMOVE.matches(method)) {

                            String fullyQualifiedName = getFieldFullyQualifiedName(method.getSelect());
                            if (fullyQualifiedName != null) {
                                Path sourcePath = getCursor().firstEnclosingOrThrow(SourceFile.class).getSourcePath();
                                J.ClassDeclaration classDecl = getCursor().firstEnclosing(J.ClassDeclaration.class);
                                J.MethodDeclaration methodDecl = getCursor().firstEnclosing(J.MethodDeclaration.class);

                                String className = classDecl != null && classDecl.getType() != null ? classDecl.getType().getFullyQualifiedName() : "UnknownClass";
                                String methodName = methodDecl != null ? methodDecl.getSimpleName() : "UnknownMethod";

                                MutationLocation.MutationType mutationType = (THREAD_LOCAL_SET.matches(method) || INHERITABLE_THREAD_LOCAL_SET.matches(method)) ? MutationLocation.MutationType.SET_CALL : MutationLocation.MutationType.REMOVE_CALL;

                                MutationLocation mutation = new MutationLocation(sourcePath, className, methodName, mutationType);
                                acc.addMutation(fullyQualifiedName, mutation);
                            }
                        } else if (method.getSimpleName().equals("get") && isThreadLocalType(method.getType())) {
                            // Track get() calls for completeness
                            String fullyQualifiedName = getFieldFullyQualifiedName(method.getSelect());
                            if (fullyQualifiedName != null) {
                                Path sourcePath = getCursor().firstEnclosingOrThrow(SourceFile.class).getSourcePath();
                                J.ClassDeclaration classDecl = getCursor().firstEnclosing(J.ClassDeclaration.class);
                                J.MethodDeclaration methodDecl = getCursor().firstEnclosing(J.MethodDeclaration.class);

                                String className = classDecl != null && classDecl.getType() != null ? classDecl.getType().getFullyQualifiedName() : "UnknownClass";
                                String methodName = methodDecl != null ? methodDecl.getSimpleName() : "UnknownMethod";

                                AccessLocation access = new AccessLocation(sourcePath, className, methodName, AccessLocation.AccessType.GET_CALL);
                                acc.addAccess(fullyQualifiedName, access);
                            }
                        }
                        return method;
                    }

                    private boolean isThreadLocalFieldAccess(Expression expression) {
                        if (expression instanceof J.Identifier) {
                            J.Identifier id = (J.Identifier) expression;
                            return isThreadLocalType(id.getType());
                        } else if (expression instanceof J.FieldAccess) {
                            J.FieldAccess fieldAccess = (J.FieldAccess) expression;
                            return isThreadLocalType(fieldAccess.getType());
                        }
                        return false;
                    }

                    private String getFieldFullyQualifiedName(Expression expression) {
                        if (expression instanceof J.Identifier) {
                            J.Identifier id = (J.Identifier) expression;
                            JavaType.Variable varType = id.getFieldType();
                            if (varType != null && varType.getOwner() instanceof JavaType.FullyQualified) {
                                JavaType.FullyQualified owner = (JavaType.FullyQualified) varType.getOwner();
                                return owner.getFullyQualifiedName() + "." + varType.getName();
                            }
                        } else if (expression instanceof J.FieldAccess) {
                            J.FieldAccess fieldAccess = (J.FieldAccess) expression;
                            JavaType.Variable varType = fieldAccess.getName().getFieldType();
                            if (varType != null && varType.getOwner() instanceof JavaType.FullyQualified) {
                                JavaType.FullyQualified owner = (JavaType.FullyQualified) varType.getOwner();
                                return owner.getFullyQualifiedName() + "." + varType.getName();
                            }
                        }
                        return null;
                    }
                });
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(ThreadLocalUsageAccumulator acc) {
        return check(!acc.declarations.isEmpty(), check(
                and(new UsesJavaVersion<>(25),
                        or(new UsesType<>(THREAD_LOCAL_FQN, true), new UsesType<>(INHERITED_THREAD_LOCAL_FQN, true))),
                new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                        multiVariable = super.visitVariableDeclarations(multiVariable, ctx);

                        // Check if any of the variables in this declaration are immutable ThreadLocals
                        J.ClassDeclaration classDecl = getCursor().firstEnclosing(J.ClassDeclaration.class);

                        for (J.VariableDeclarations.NamedVariable variable : multiVariable.getVariables()) {
                            if (isThreadLocalType(variable.getType()) && classDecl != null) {
                                String className = classDecl.getType() != null ? classDecl.getType().getFullyQualifiedName() : "UnknownClass";
                                String fieldName = variable.getName().getSimpleName();
                                String fullyQualifiedName = className + "." + fieldName;

                                ThreadLocalDeclaration declaration = acc.declarations.get(fullyQualifiedName);
                                if (declaration != null) {
                                    boolean hasMutations = acc.hasMutations(fullyQualifiedName);
                                    boolean hasExternalMutations = acc.hasExternalMutations(fullyQualifiedName);

                                    if (!hasMutations) {
                                        // No mutations at all
                                        String message = declaration.isPrivate() ? "ThreadLocal candidate for ScopedValue migration - never mutated after initialization" : "ThreadLocal candidate for ScopedValue migration - never mutated in project (but accessible from outside due to non-private access)";
                                        return SearchResult.found(multiVariable, message);
                                    } else if (!hasExternalMutations && declaration.isPrivate()) {
                                        // Has mutations, but only in the declaring file and it's private
                                        // This is still safe since no external access is possible
                                        // However, we should check if mutations are only in the initializer
                                        List<MutationLocation> mutations = acc.mutations.get(fullyQualifiedName);
                                        boolean onlyLocalMutations = mutations.stream().allMatch(m -> m.getSourcePath().equals(declaration.getSourcePath()));

                                        if (onlyLocalMutations) {
                                            // Check if it's only mutated in the same class
                                            // For now, we'll be conservative and not mark it
                                            continue;
                                        }
                                    }
                                }
                            }
                        }

                        return multiVariable;
                    }
                }));
    }

    private static boolean isThreadLocalType(JavaType type) {
        return TypeUtils.isOfClassType(type, THREAD_LOCAL_FQN) || TypeUtils.isOfClassType(type, INHERITED_THREAD_LOCAL_FQN);
    }
}
