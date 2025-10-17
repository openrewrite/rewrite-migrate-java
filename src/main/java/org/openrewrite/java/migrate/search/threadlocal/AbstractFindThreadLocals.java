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
package org.openrewrite.java.migrate.search.threadlocal;

import lombok.Getter;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.migrate.search.ThreadLocalTable;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.SearchResult;

import java.nio.file.Path;
import java.util.*;

import static org.openrewrite.Preconditions.check;
import static org.openrewrite.Preconditions.or;


public abstract class AbstractFindThreadLocals extends ScanningRecipe<AbstractFindThreadLocals.ThreadLocalAccumulator> {

    protected static final String THREAD_LOCAL_FQN = "java.lang.ThreadLocal";
    protected static final String INHERITED_THREAD_LOCAL_FQN = "java.lang.InheritableThreadLocal";

    private static final MethodMatcher THREAD_LOCAL_SET = new MethodMatcher(THREAD_LOCAL_FQN + " set(..)");
    private static final MethodMatcher THREAD_LOCAL_REMOVE = new MethodMatcher(THREAD_LOCAL_FQN + " remove()");
    private static final MethodMatcher INHERITABLE_THREAD_LOCAL_SET = new MethodMatcher(INHERITED_THREAD_LOCAL_FQN + " set(..)");
    private static final MethodMatcher INHERITABLE_THREAD_LOCAL_REMOVE = new MethodMatcher(INHERITED_THREAD_LOCAL_FQN + " remove()");

    transient ThreadLocalTable dataTable = new ThreadLocalTable(this);

    @Value
    public static class ThreadLocalAccumulator {
        Map<String, ThreadLocalInfo> threadLocals = new HashMap<>();

        public void recordDeclaration(String fqn, Path sourcePath, boolean isPrivate, boolean isStatic, boolean isFinal) {
            threadLocals.computeIfAbsent(fqn, k -> new ThreadLocalInfo())
                    .setDeclaration(sourcePath, isPrivate, isStatic, isFinal);
        }

        public void recordMutation(String fqn, Path sourcePath, boolean isInitContext) {
            ThreadLocalInfo info = threadLocals.computeIfAbsent(fqn, k -> new ThreadLocalInfo());
            if (isInitContext) {
                info.addInitMutation(sourcePath);
            } else {
                info.addRegularMutation(sourcePath);
            }
        }

        public @Nullable ThreadLocalInfo getInfo(String fqn) {
            return threadLocals.get(fqn);
        }

        public boolean hasDeclarations() {
            return threadLocals.values().stream().anyMatch(ThreadLocalInfo::isDeclared);
        }
    }

    public static class ThreadLocalInfo {
        private @Nullable Path declarationPath;
        @Getter
        private boolean isPrivate;
        @Getter
        private boolean isStatic;
        @Getter
        private boolean isFinal;
        @Getter
        private boolean declared;
        private final Set<Path> initMutationPaths = new HashSet<>();
        private final Set<Path> regularMutationPaths = new HashSet<>();

        void setDeclaration(Path path, boolean priv, boolean stat, boolean fin) {
            this.declarationPath = path;
            this.isPrivate = priv;
            this.isStatic = stat;
            this.isFinal = fin;
            this.declared = true;
        }

        /**
         * Records a mutation from an initialization context (constructor/static initializer).
         */
        void addInitMutation(Path path) {
            initMutationPaths.add(path);
        }

        /**
         * Records a regular (non-initialization) mutation.
         */
        void addRegularMutation(Path path) {
            regularMutationPaths.add(path);
        }

        /**
         * Checks if there are no mutations (both init and regular).
         */
        public boolean hasNoMutation() {
            return initMutationPaths.isEmpty() && regularMutationPaths.isEmpty();
        }

        /**
         * Checks if there are only mutations from initialization contexts (constructors/static initializers).
         */
        public boolean hasOnlyInitMutations() {
            return !initMutationPaths.isEmpty() && regularMutationPaths.isEmpty();
        }

        /**
         * Checks if there are any mutations (both init and regular) from files other than the declaration file.
         */
        public boolean hasExternalMutations() {
            if (!declared || declarationPath == null) {
                return true; // Conservative
            }

            // Check if any mutation is from a different file
            return initMutationPaths.stream().anyMatch(p -> !p.equals(declarationPath)) ||
                   regularMutationPaths.stream().anyMatch(p -> !p.equals(declarationPath));
        }

        /**
         * Checks if all mutations (both init and regular) are from the same file as the declaration.
         */
        public boolean isOnlyLocallyMutated() {
            if (!declared || declarationPath == null) {
                return false;
            }

            // All mutations must be from the same file as declaration
            return initMutationPaths.stream().allMatch(p -> p.equals(declarationPath)) &&
                   regularMutationPaths.stream().allMatch(p -> p.equals(declarationPath));
        }
    }

    @Override
    public ThreadLocalAccumulator getInitialValue(ExecutionContext ctx) {
        return new ThreadLocalAccumulator();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(ThreadLocalAccumulator acc) {
        return check(
            or(new UsesType<>(THREAD_LOCAL_FQN, true),
               new UsesType<>(INHERITED_THREAD_LOCAL_FQN, true)),
            new JavaIsoVisitor<ExecutionContext>() {
                @Override
                public J.VariableDeclarations.NamedVariable visitVariable(
                        J.VariableDeclarations.NamedVariable variable, ExecutionContext ctx) {
                    variable = super.visitVariable(variable, ctx);

                    // Early return for non-ThreadLocal types
                    if (!isThreadLocalType(variable.getType())) {
                        return variable;
                    }

                    // Early return for local variables (not fields)
                    J.MethodDeclaration enclosingMethod = getCursor().firstEnclosing(J.MethodDeclaration.class);
                    if (enclosingMethod != null) {
                        return variable;
                    }

                    // Early return if not in a class
                    J.ClassDeclaration classDecl = getCursor().firstEnclosing(J.ClassDeclaration.class);
                    if (classDecl == null) {
                        return variable;
                    }

                    // Early return if we can't find the variable declarations
                    J.VariableDeclarations variableDecls = getCursor().firstEnclosing(J.VariableDeclarations.class);
                    if (variableDecls == null) {
                        return variable;
                    }

                    // Process ThreadLocal field declaration
                    JavaType.@Nullable FullyQualified classType = classDecl.getType();
                    String className = classType != null ? classType.getFullyQualifiedName() : "UnknownClass";
                    String fqn = className + "." + variable.getName().getSimpleName();

                    boolean isPrivate = variableDecls.hasModifier(J.Modifier.Type.Private);
                    boolean isStatic = variableDecls.hasModifier(J.Modifier.Type.Static);
                    boolean isFinal = variableDecls.hasModifier(J.Modifier.Type.Final);
                    Path sourcePath = getCursor().firstEnclosingOrThrow(SourceFile.class).getSourcePath();

                    acc.recordDeclaration(fqn, sourcePath, isPrivate, isStatic, isFinal);
                    return variable;
                }

                @Override
                public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                    method = super.visitMethodInvocation(method, ctx);

                    // Early return if not a ThreadLocal mutation method
                    if (!THREAD_LOCAL_SET.matches(method) && !THREAD_LOCAL_REMOVE.matches(method) &&
                        !INHERITABLE_THREAD_LOCAL_SET.matches(method) && !INHERITABLE_THREAD_LOCAL_REMOVE.matches(method)) {
                        return method;
                    }

                    String fqn = getFieldFullyQualifiedName(method.getSelect());
                    if (fqn == null) {
                        return method;
                    }

                    Path sourcePath = getCursor().firstEnclosingOrThrow(SourceFile.class).getSourcePath();
                    boolean isInitContext = isInInitializationContext();
                    acc.recordMutation(fqn, sourcePath, isInitContext);
                    return method;
                }

                @Override
                public J.Assignment visitAssignment(J.Assignment assignment, ExecutionContext ctx) {
                    assignment = super.visitAssignment(assignment, ctx);

                    // Early return if not a ThreadLocal field access
                    if (!isThreadLocalFieldAccess(assignment.getVariable())) {
                        return assignment;
                    }

                    String fqn = getFieldFullyQualifiedName(assignment.getVariable());
                    if (fqn == null) {
                        return assignment;
                    }

                    Path sourcePath = getCursor().firstEnclosingOrThrow(SourceFile.class).getSourcePath();
                    boolean isInitContext = isInInitializationContext();
                    acc.recordMutation(fqn, sourcePath, isInitContext);
                    return assignment;
                }

                private boolean isInInitializationContext() {
                    J.MethodDeclaration methodDecl = getCursor().firstEnclosing(J.MethodDeclaration.class);

                    if (methodDecl == null) {
                        // Check if we're in a static initializer block
                        return getCursor().getPathAsStream()
                            .filter(J.Block.class::isInstance)
                            .map(J.Block.class::cast)
                            .anyMatch(J.Block::isStatic);
                    }

                    // Check if it's a constructor
                    return methodDecl.isConstructor();
                }

                private boolean isThreadLocalFieldAccess(Expression expression) {
                    if (expression instanceof J.Identifier) {
                        return isThreadLocalType(expression.getType());
                    }
                    if (expression instanceof J.FieldAccess) {
                        return isThreadLocalType(expression.getType());
                    }
                    return false;
                }

                private @Nullable String getFieldFullyQualifiedName(@Nullable Expression expression) {
                    if (expression == null) {
                        return null;
                    }

                    JavaType.@Nullable Variable varType = null;
                    if (expression instanceof J.Identifier) {
                        varType = ((J.Identifier) expression).getFieldType();
                    } else if (expression instanceof J.FieldAccess) {
                        varType = ((J.FieldAccess) expression).getName().getFieldType();
                    }

                    if (varType == null) {
                        return null;
                    }

                    JavaType owner = varType.getOwner();
                    if (!(owner instanceof JavaType.FullyQualified)) {
                        return null;
                    }

                    return ((JavaType.FullyQualified) owner).getFullyQualifiedName() + "." + varType.getName();
                }

            });
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(ThreadLocalAccumulator acc) {
        return check(acc.hasDeclarations(),
            new JavaIsoVisitor<ExecutionContext>() {


                @Override
                public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                    multiVariable = super.visitVariableDeclarations(multiVariable, ctx);

                    J.ClassDeclaration classDecl = getCursor().firstEnclosing(J.ClassDeclaration.class);
                    if(classDecl == null) {
                       return multiVariable;
                    }

                    for (J.VariableDeclarations.NamedVariable variable : multiVariable.getVariables()) {
                        if (isThreadLocalType(variable.getType())) {
                            String className = classDecl.getType() != null ?
                                classDecl.getType().getFullyQualifiedName() : "UnknownClass";
                            String fieldName = variable.getName().getSimpleName();
                            String fqn = className + "." + fieldName;

                            ThreadLocalInfo info = acc.getInfo(fqn);
                            if (info != null && shouldMarkThreadLocal(info)) {
                                String message = getMessage(info);
                                String mutationType = getMutationType(info);

                                dataTable.insertRow(ctx, new ThreadLocalTable.Row(
                                    getCursor().firstEnclosingOrThrow(SourceFile.class).getSourcePath().toString(),
                                    className,
                                    fieldName,
                                    getAccessModifier(multiVariable),
                                    getFieldModifiers(multiVariable),
                                    mutationType,
                                    message
                                 ));

                                return SearchResult.found(multiVariable, message);
                            }
                        }
                    }

                    return multiVariable;
                }

                private String getAccessModifier(J.VariableDeclarations variableDecls) {
                    if (variableDecls.hasModifier(J.Modifier.Type.Private)) {
                        return "private";
                    }
                    if (variableDecls.hasModifier(J.Modifier.Type.Protected)) {
                        return "protected";
                    }
                    if (variableDecls.hasModifier(J.Modifier.Type.Public)) {
                        return "public";
                    }
                    return "package-private";
                }

                private String getFieldModifiers(J.VariableDeclarations variableDecls) {
                    List<String> mods = new ArrayList<>();
                    if (variableDecls.hasModifier(J.Modifier.Type.Static)) {
                        mods.add("static");
                    }
                    if (variableDecls.hasModifier(J.Modifier.Type.Final)) {
                        mods.add("final");
                    }
                    return String.join(" ", mods);
                }

            });
    }

    /**
     * Determines whether a ThreadLocal should be marked based on its usage info.
     * Implementations should define the criteria for marking.
     * It is used to decide if a ThreadLocal variable should be highlighted in the results.
     * If an expected ThreadLocal instance is missing from the results, consider adjusting this method.
     *
     * @param info The ThreadLocalInfo containing usage details.
     * @return true if the ThreadLocal should be marked, false otherwise.
     */
    protected abstract boolean shouldMarkThreadLocal(ThreadLocalInfo info);
    /**
     * Generates a descriptive message about the ThreadLocal's usage pattern.
     * Implementations should provide context-specific messages.
     * It is used to receive the Markers message and the Data Tables detailed message.
     *
     * @param info The ThreadLocalInfo containing usage details.
     * @return A string message describing the ThreadLocal's usage.
     */
    protected abstract String getMessage(ThreadLocalInfo info);
    /**
     * Determines the mutation type of the ThreadLocal based on its usage info.
     * Implementations should define the mutation categories.
     * It is used to populate the Data Tables human-readable mutation type column.
     *
     * @param info The ThreadLocalInfo containing usage details.
     * @return A string representing the mutation type.
     */
    protected abstract String getMutationType(ThreadLocalInfo info);

    protected static boolean isThreadLocalType(@Nullable JavaType type) {
        return TypeUtils.isOfClassType(type, THREAD_LOCAL_FQN) ||
               TypeUtils.isOfClassType(type, INHERITED_THREAD_LOCAL_FQN);
    }
}
