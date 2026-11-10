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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.RemoveMethodInvocationsVisitor;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

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
            "directly or through a variable that only ever holds its result. Since Java 24 there is no system " +
            "`SecurityManager`, so these checks are dead code. Checks on any other `SecurityManager` instance are kept: " +
            "such an instance still enforces its own policy, and removing the check would silently allow every action. " +
            "Empty `null` checks on a local variable that only held `System.getSecurityManager()` are removed, " +
            "and so is the variable once it is not used otherwise.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                Preconditions.and(new UsesMethod<>(CHECK), new UsesMethod<>(GET_SECURITY_MANAGER)),
                new RemoveMethodInvocationsVisitor(singletonMap(CHECK, args -> true)) {
                    private Set<JavaType.Variable> systemVariables = emptySet();
                    private Set<JavaType.Variable> checkedLocals = new HashSet<>();

                    @Override
                    public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
                        if (tree instanceof JavaSourceFile) {
                            systemVariables = findSystemSecurityManagerVariables((JavaSourceFile) tree);
                            checkedLocals = new HashSet<>();
                            doAfterVisit(removeUnusedLocals(checkedLocals));
                        }
                        return super.visit(tree, ctx);
                    }

                    @Override
                    public @Nullable J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                        if (!CHECK.matches(method)) {
                            return super.visitMethodInvocation(method, ctx);
                        }
                        Expression select = method.getSelect() == null ? null : method.getSelect().unwrap();
                        boolean onSystem = GET_SECURITY_MANAGER.matches(select);
                        JavaType.Variable variable = select == null ? null : variableOf(select);
                        if (!onSystem && !systemVariables.contains(variable)) {
                            return method.withArguments(ListUtils.map(method.getArguments(), arg -> (Expression) visit(arg, ctx)));
                        }
                        if (variable != null && variable.getOwner() instanceof JavaType.Method) {
                            checkedLocals.add(variable);
                        }
                        if (onSystem) {
                            // Do not leave the select `System.getSecurityManager()` behind as a statement
                            return null;
                        }
                        return super.visitMethodInvocation(method, ctx);
                    }
                });
    }

    private static JavaIsoVisitor<ExecutionContext> removeUnusedLocals(Set<JavaType.Variable> locals) {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.Block visitBlock(J.Block block, ExecutionContext ctx) {
                J.Block b = super.visitBlock(block, ctx);
                if (locals.isEmpty()) {
                    return b;
                }
                J.Block withoutNullChecks = b.withStatements(ListUtils.filter(b.getStatements(), statement -> !isEmptyNullCheck(statement)));
                return withoutNullChecks.withStatements(ListUtils.map(withoutNullChecks.getStatements(), statement -> {
                    if (!(statement instanceof J.VariableDeclarations)) {
                        return statement;
                    }
                    J.VariableDeclarations declarations = (J.VariableDeclarations) statement;
                    List<J.VariableDeclarations.NamedVariable> used = ListUtils.filter(declarations.getVariables(), variable ->
                            !locals.contains(variable.getVariableType()) || isReferenced(withoutNullChecks, variable.getVariableType(), null));
                    return used == null || used.isEmpty() ? null : declarations.withVariables(used);
                }));
            }

            private boolean isEmptyNullCheck(Statement statement) {
                if (!(statement instanceof J.If)) {
                    return false;
                }
                J.If iff = (J.If) statement;
                if (iff.getElsePart() != null || !(iff.getThenPart() instanceof J.Block)) {
                    return false;
                }
                J.Block then = (J.Block) iff.getThenPart();
                if (!then.getStatements().isEmpty() || !then.getEnd().getComments().isEmpty()) {
                    return false;
                }
                Expression condition = iff.getIfCondition().getTree().unwrap();
                if (!(condition instanceof J.Binary) || ((J.Binary) condition).getOperator() != J.Binary.Type.NotEqual) {
                    return false;
                }
                J.Binary binary = (J.Binary) condition;
                Expression operand = J.Literal.isLiteralValue(binary.getRight(), null) ? binary.getLeft().unwrap() :
                        J.Literal.isLiteralValue(binary.getLeft(), null) ? binary.getRight().unwrap() : null;
                if (operand instanceof J.Assignment) {
                    // Dropping the assignment is only safe when nothing else in the file reads the variable
                    JavaType.Variable assigned = variableOf(((J.Assignment) operand).getVariable().unwrap());
                    return locals.contains(assigned) &&
                            !isReferenced(getCursor().firstEnclosingOrThrow(JavaSourceFile.class), assigned, statement);
                }
                return operand != null && locals.contains(variableOf(operand));
            }
        };
    }

    private static boolean isReferenced(J tree, JavaType.@Nullable Variable variable, @Nullable Statement excluded) {
        return new JavaIsoVisitor<AtomicBoolean>() {
            @Override
            public @Nullable J visit(@Nullable Tree tree, AtomicBoolean referenced) {
                return excluded != null && tree != null && excluded.getId().equals(tree.getId()) ? (J) tree : super.visit(tree, referenced);
            }

            @Override
            public J.Identifier visitIdentifier(J.Identifier identifier, AtomicBoolean referenced) {
                Object parent = getCursor().getParentTreeCursor().getValue();
                if (identifier.getFieldType() != null && identifier.getFieldType().equals(variable) &&
                        !(parent instanceof J.VariableDeclarations.NamedVariable && ((J.VariableDeclarations.NamedVariable) parent).getName() == identifier)) {
                    referenced.set(true);
                }
                return identifier;
            }
        }.reduce(tree, new AtomicBoolean()).get();
    }

    private static JavaType.@Nullable Variable variableOf(Expression expression) {
        if (expression instanceof J.Identifier) {
            return ((J.Identifier) expression).getFieldType();
        }
        if (expression instanceof J.FieldAccess) {
            return ((J.FieldAccess) expression).getName().getFieldType();
        }
        return null;
    }

    private static Set<JavaType.Variable> findSystemSecurityManagerVariables(JavaSourceFile sourceFile) {
        Map<JavaType.Variable, Boolean> onlyFromSystem = new JavaIsoVisitor<Map<JavaType.Variable, Boolean>>() {
            @Override
            public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, Map<JavaType.Variable, Boolean> acc) {
                JavaType.Variable type = variable.getVariableType();
                if (type == null || !TypeUtils.isAssignableTo(SECURITY_MANAGER, type.getType())) {
                    return super.visitVariable(variable, acc);
                }
                if (!(type.getOwner() instanceof JavaType.Method) &&
                        (!type.hasFlags(Flag.Private) && !type.hasFlags(Flag.Final) || isAnnotated())) {
                    // A field that other source files, Lombok-generated code or injection may assign
                    acc.put(type, false);
                } else if (variable.getInitializer() != null) {
                    acc.merge(type, GET_SECURITY_MANAGER.matches(variable.getInitializer().unwrap()), Boolean::logicalAnd);
                } else if (!(getCursor().getParentTreeCursor().getParentTreeCursor().getValue() instanceof J.Block)) {
                    // A parameter, catch or loop variable: its value comes from elsewhere
                    acc.put(type, false);
                }
                return super.visitVariable(variable, acc);
            }

            private boolean isAnnotated() {
                J.VariableDeclarations declarations = getCursor().getParentTreeCursor().getValue();
                J.ClassDeclaration classDeclaration = getCursor().firstEnclosing(J.ClassDeclaration.class);
                return !declarations.getLeadingAnnotations().isEmpty() ||
                        classDeclaration != null && classDeclaration.getLeadingAnnotations().stream().anyMatch(annotation -> {
                            JavaType.FullyQualified annotationType = TypeUtils.asFullyQualified(annotation.getType());
                            return annotationType != null && annotationType.getFullyQualifiedName().startsWith("lombok.");
                        });
            }

            @Override
            public J.Assignment visitAssignment(J.Assignment assignment, Map<JavaType.Variable, Boolean> acc) {
                JavaType.Variable type = variableOf(assignment.getVariable().unwrap());
                if (type != null && TypeUtils.isAssignableTo(SECURITY_MANAGER, type.getType())) {
                    acc.merge(type, GET_SECURITY_MANAGER.matches(assignment.getAssignment().unwrap()), Boolean::logicalAnd);
                }
                return super.visitAssignment(assignment, acc);
            }
        }.reduce(sourceFile, new HashMap<>());
        onlyFromSystem.values().removeIf(fromSystem -> !fromSystem);
        return onlyFromSystem.keySet();
    }
}
