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
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.Set;

import static java.util.Collections.singleton;

public class NoGuavaStringsIsNullOrEmpty extends Recipe {
    private static final MethodMatcher IS_NULL_OR_EMPTY = new MethodMatcher("com.google.common.base.Strings isNullOrEmpty(java.lang.String)");

    @Getter
    final String displayName = "Prefer `s == null || s.isEmpty()`";

    @Getter
    final String description = "Replace Guava's `Strings.isNullOrEmpty(s)` with `s == null || s.isEmpty()`, and its negation with " +
            "`s != null && !s.isEmpty()`. The argument is read twice, so only a variable, a field or a chain of field accesses " +
            "is converted, and a `volatile` field is left alone.";

    @Getter
    final Set<String> tags = singleton("guava");

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesMethod<>(IS_NULL_OR_EMPTY), new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitUnary(J.Unary unary, ExecutionContext ctx) {
                Expression checked = unary.getOperator() == J.Unary.Type.Not ? convertibleArgument(unary.getExpression()) : null;
                if (checked == null) {
                    return super.visitUnary(unary, ctx);
                }
                maybeRemoveImport("com.google.common.base.Strings");
                return JavaTemplate.apply("#{s:any(java.lang.String)} != null && !#{s}.isEmpty()",
                        getCursor(), unary.getCoordinates().replace(), checked);
            }

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                Expression checked = convertibleArgument(method);
                if (checked == null) {
                    return super.visitMethodInvocation(method, ctx);
                }
                maybeRemoveImport("com.google.common.base.Strings");
                return JavaTemplate.apply("#{s:any(java.lang.String)} == null || #{s}.isEmpty()",
                        getCursor(), method.getCoordinates().replace(), checked);
            }
        });
    }

    private static @Nullable Expression convertibleArgument(Expression expression) {
        if (!IS_NULL_OR_EMPTY.matches(expression)) {
            return null;
        }
        Expression argument = ((J.MethodInvocation) expression).getArguments().get(0);
        return readsSameValueTwice(argument) ? argument : null;
    }

    private static boolean readsSameValueTwice(Expression expression) {
        if (expression instanceof J.Identifier) {
            return isNonVolatileVariable(((J.Identifier) expression).getFieldType());
        }
        if (expression instanceof J.FieldAccess) {
            J.FieldAccess fieldAccess = (J.FieldAccess) expression;
            return isNonVolatileVariable(fieldAccess.getName().getFieldType()) && readsSameValueTwice(fieldAccess.getTarget());
        }
        return false;
    }

    private static boolean isNonVolatileVariable(JavaType.@Nullable Variable variable) {
        return variable != null && !variable.hasFlags(Flag.Volatile);
    }
}
