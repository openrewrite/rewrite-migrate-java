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
package org.openrewrite.java.migrate.guava;

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TypeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Collections.singleton;

public class NoGuavaPreconditionsCheckArgumentAndState extends Recipe {
    private static final MethodMatcher CHECK_ARGUMENT = new MethodMatcher("com.google.common.base.Preconditions checkArgument(boolean, ..)");
    private static final MethodMatcher CHECK_STATE = new MethodMatcher("com.google.common.base.Preconditions checkState(boolean, ..)");

    @Getter
    final String displayName = "Prefer `if`/`throw` over Guava's `checkArgument` and `checkState`";

    @Getter
    final String description = "Replace Guava's `Preconditions.checkArgument(..)` and `Preconditions.checkState(..)` statements " +
            "with an `if` that throws `IllegalArgumentException` or `IllegalStateException`. A message template converts " +
            "to `String.format(..)` only when it is a string literal whose placeholders are all `%s` and match the arguments.";

    @Getter
    final Set<String> tags = singleton("guava");

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                Preconditions.or(new UsesMethod<>(CHECK_ARGUMENT), new UsesMethod<>(CHECK_STATE)),
                new JavaVisitor<ExecutionContext>() {
                    @Override
                    public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                        J.MethodInvocation mi = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
                        String exception = CHECK_ARGUMENT.matches(mi) ? "IllegalArgumentException" :
                                CHECK_STATE.matches(mi) ? "IllegalStateException" : null;
                        // As the sole statement of an unbraced `if`, an inserted `if` would capture that `if`'s `else`
                        if (exception == null || !(getCursor().getParentTreeCursor().getValue() instanceof J.Block)) {
                            return mi;
                        }
                        List<Expression> message = mi.getArguments().subList(1, mi.getArguments().size());
                        String messageCode = messageCode(message);
                        if (messageCode == null) {
                            return mi;
                        }
                        List<Object> parameters = new ArrayList<>();
                        String conditionCode = negatedConditionCode(mi.getArguments().get(0), parameters);
                        parameters.addAll(message);

                        maybeRemoveImport("com.google.common.base.Preconditions");
                        maybeRemoveImport("com.google.common.base.Preconditions.checkArgument");
                        maybeRemoveImport("com.google.common.base.Preconditions.checkState");
                        return JavaTemplate.builder("if (" + conditionCode + ") {\n" +
                                                    "    throw new " + exception + "(" + messageCode + ");\n" +
                                                    "}")
                                .build()
                                .apply(getCursor(), mi.getCoordinates().replace(), parameters.toArray());
                    }
                }
        );
    }

    private static @Nullable String messageCode(List<Expression> message) {
        if (message.isEmpty()) {
            return "";
        }
        for (Expression part : message) {
            // `String.valueOf(null)` resolves to `valueOf(char[])` and throws.
            // An array's length decides how many arguments `String.format` receives.
            if (J.Literal.isLiteralValue(part, null) || part.getType() instanceof JavaType.Array || hasSideEffect(part)) {
                return null;
            }
        }
        if (message.size() == 1) {
            return TypeUtils.isString(message.get(0).getType()) ? "#{any(java.lang.String)}" : "String.valueOf(#{any()})";
        }
        if (!(message.get(0) instanceof J.Literal) || !(((J.Literal) message.get(0)).getValue() instanceof String)) {
            return null;
        }
        // Guava substitutes only `%s` and appends unmatched arguments, where `String.format` interprets every `%`
        String template = (String) ((J.Literal) message.get(0)).getValue();
        int placeholders = template.split("%s", -1).length - 1;
        if (template.replace("%s", "").indexOf('%') >= 0 || placeholders != message.size() - 1) {
            return null;
        }
        StringBuilder code = new StringBuilder("String.format(#{any(java.lang.String)}");
        for (int i = 1; i < message.size(); i++) {
            code.append(", #{any()}");
        }
        return code.append(")").toString();
    }

    /**
     * Guava evaluates its message arguments before the call, but the `if` evaluates them only when the check fails.
     * So an argument that assigns or increments keeps the Guava call. Method calls are assumed to have no side effects.
     */
    private static boolean hasSideEffect(Expression expression) {
        AtomicBoolean found = new AtomicBoolean();
        new JavaIsoVisitor<AtomicBoolean>() {
            @Override
            public J.Assignment visitAssignment(J.Assignment assignment, AtomicBoolean f) {
                f.set(true);
                return assignment;
            }

            @Override
            public J.AssignmentOperation visitAssignmentOperation(J.AssignmentOperation assignOp, AtomicBoolean f) {
                f.set(true);
                return assignOp;
            }

            @Override
            public J.Unary visitUnary(J.Unary unary, AtomicBoolean f) {
                if (unary.getOperator().isModifying()) {
                    f.set(true);
                }
                return super.visitUnary(unary, f);
            }
        }.visit(expression, found);
        return found.get();
    }

    private static String negatedConditionCode(Expression condition, List<Object> parameters) {
        if (condition instanceof J.Unary && ((J.Unary) condition).getOperator() == J.Unary.Type.Not) {
            Expression negated = ((J.Unary) condition).getExpression();
            parameters.add(negated instanceof J.Parentheses ?
                    ((Expression) ((J.Parentheses<?>) negated).getTree()).withPrefix(Space.EMPTY) : negated);
            return "#{any(boolean)}";
        }
        if (condition instanceof J.Binary) {
            J.Binary binary = (J.Binary) condition;
            // `<`, `<=`, `>` and `>=` stay under `!`, since a NaN operand makes both a comparison and its inverse false
            if (binary.getOperator() == J.Binary.Type.Equal || binary.getOperator() == J.Binary.Type.NotEqual) {
                parameters.add(binary.withOperator(binary.getOperator() == J.Binary.Type.Equal ?
                        J.Binary.Type.NotEqual : J.Binary.Type.Equal));
                return "#{any(boolean)}";
            }
        }
        parameters.add(condition);
        if (condition instanceof J.Identifier || condition instanceof J.FieldAccess || condition instanceof J.MethodInvocation ||
            condition instanceof J.Parentheses || condition instanceof J.Literal) {
            return "!#{any(boolean)}";
        }
        return "!(#{any(boolean)})";
    }
}
