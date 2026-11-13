/*
 * Copyright 2026 the original author or authors.
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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.RemoveMethodInvocationsVisitor;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.*;
import org.openrewrite.staticanalysis.EmptyBlock;
import org.openrewrite.staticanalysis.RemoveUnusedLocalVariables;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonMap;

@Value
@EqualsAndHashCode(callSuper = false)
public class RemoveSystemSecurityManagerChecks extends Recipe {
    private static final String SECURITY_MANAGER = "java.lang.SecurityManager";
    private static final MethodMatcher CHECK = new MethodMatcher(SECURITY_MANAGER + " check*(..)");
    private static final MethodMatcher GET_SECURITY_MANAGER = new MethodMatcher("java.lang.System getSecurityManager()");

    String displayName = "Remove permission checks on the system `SecurityManager`";
    String description = "Remove `SecurityManager#check*(..)` calls whose receiver is `System.getSecurityManager()`, " +
            "directly or through a local variable that only ever holds its result. Since Java 24 there is no system " +
            "`SecurityManager`, so these checks are dead code. Checks on any other `SecurityManager`, such as a parameter, " +
            "a field or a custom subclass, are kept: such an instance still enforces its own policy, and removing the check " +
            "would silently allow every action. Blocks left empty and `SecurityManager` locals left unused are removed afterwards.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                Preconditions.and(new UsesMethod<>(CHECK), new UsesMethod<>(GET_SECURITY_MANAGER)),
                new RemoveMethodInvocationsVisitor(singletonMap(CHECK, args -> true)) {
                    private Set<JavaType.Variable> systemLocals = emptySet();

                    @Override
                    public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
                        if (!(tree instanceof JavaSourceFile)) {
                            return super.visit(tree, ctx);
                        }
                        systemLocals = findSystemSecurityManagerLocals((JavaSourceFile) tree);
                        J after = super.visit(tree, ctx);
                        if (after == tree || after == null) {
                            return after;
                        }
                        after = (J) new EmptyBlock().getVisitor().visitNonNull(after, ctx);
                        return (J) new RemoveUnusedLocalVariables(null, SECURITY_MANAGER, true).getVisitor().visitNonNull(after, ctx);
                    }

                    @Override
                    public @Nullable J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                        if (!CHECK.matches(method)) {
                            return super.visitMethodInvocation(method, ctx);
                        }
                        Expression select = method.getSelect() == null ? null : method.getSelect().unwrap();
                        boolean onSystem = GET_SECURITY_MANAGER.matches(select);
                        if (!onSystem && !(select instanceof J.Identifier && systemLocals.contains(((J.Identifier) select).getFieldType()))) {
                            return method;
                        }
                        // Do not leave the select `System.getSecurityManager()` behind as a statement
                        return onSystem ? null : super.visitMethodInvocation(method, ctx);
                    }
                });
    }

    private static Set<JavaType.Variable> findSystemSecurityManagerLocals(JavaSourceFile sourceFile) {
        Map<JavaType.Variable, Boolean> onlyFromSystem = new JavaIsoVisitor<Map<JavaType.Variable, Boolean>>() {
            @Override
            public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, Map<JavaType.Variable, Boolean> acc) {
                JavaType.Variable type = variable.getVariableType();
                if (type != null && TypeUtils.isOfClassType(type.getType(), SECURITY_MANAGER)) {
                    Expression initializer = variable.getInitializer();
                    acc.merge(type, isLocal() && (initializer == null || GET_SECURITY_MANAGER.matches(initializer.unwrap())), Boolean::logicalAnd);
                }
                return super.visitVariable(variable, acc);
            }

            private boolean isLocal() {
                Cursor block = getCursor().getParentTreeCursor().getParentTreeCursor();
                Object owner = block.getParentTreeCursor().getValue();
                return block.getValue() instanceof J.Block && !(owner instanceof J.ClassDeclaration || owner instanceof J.NewClass);
            }

            @Override
            public J.Assignment visitAssignment(J.Assignment assignment, Map<JavaType.Variable, Boolean> acc) {
                Expression variable = assignment.getVariable().unwrap();
                if (variable instanceof J.Identifier && ((J.Identifier) variable).getFieldType() != null) {
                    boolean fromSystem = GET_SECURITY_MANAGER.matches(assignment.getAssignment().unwrap());
                    acc.computeIfPresent(((J.Identifier) variable).getFieldType(), (local, onlyFromSystem) -> onlyFromSystem && fromSystem);
                }
                return super.visitAssignment(assignment, acc);
            }
        }.reduce(sourceFile, new HashMap<>());
        onlyFromSystem.values().removeIf(fromSystem -> !fromSystem);
        return onlyFromSystem.keySet();
    }
}
