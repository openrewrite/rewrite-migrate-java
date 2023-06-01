/*
 * Copyright 2022 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.migrate.util;

import org.openrewrite.Preconditions;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.J.Unary.Type;
import org.openrewrite.java.tree.Statement;

import java.time.Duration;

public class OptionalNotEmptyToIsPresent extends Recipe {

    private static final String JAVA_UTIL_OPTIONAL_IS_EMPTY = "java.util.Optional isEmpty()";

    @Override
    public String getDisplayName() {
        return "Prefer `Optional.isPresent()`";
    }

    @Override
    public String getDescription() {
        return "Prefer `Optional.isPresent()` instead of using `!Optional.isEmpty()` in Java 11 or higher.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(1);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        TreeVisitor<?, ExecutionContext> check = Preconditions.and(
                new UsesJavaVersion<>(11),
                new UsesMethod<>(JAVA_UTIL_OPTIONAL_IS_EMPTY));
        MethodMatcher optionalIsPresentMatcher = new MethodMatcher(JAVA_UTIL_OPTIONAL_IS_EMPTY);
        return Preconditions.check(check, new JavaVisitor<ExecutionContext>() {
            @Override
            public Statement visitStatement(Statement s, ExecutionContext ctx) {
                Statement statement = (Statement) super.visitStatement(s, ctx);
                if (statement instanceof J.Unary) {
                    J.Unary unary = (J.Unary) statement;
                    if (unary.getOperator() == Type.Not) {
                        Expression expression = unary.getExpression();
                        if (expression instanceof J.MethodInvocation) {
                            J.MethodInvocation m = (J.MethodInvocation) expression;
                            if (optionalIsPresentMatcher.matches(m)) {
                                return JavaTemplate.builder("#{any()}.isPresent()")
                                        .contextSensitive()
                                        .build()
                                        .apply(getCursor(),
                                                statement.getCoordinates().replace(),
                                                m.getSelect());
                            }
                        }
                    }
                }
                return statement;
            }
        });
    }
}
