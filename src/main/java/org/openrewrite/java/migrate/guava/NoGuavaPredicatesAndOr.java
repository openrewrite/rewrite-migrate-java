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
package org.openrewrite.java.migrate.guava;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.ShortenFullyQualifiedTypeReferences;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static java.util.Collections.singleton;

public class NoGuavaPredicatesAndOr extends Recipe {
    private static final MethodMatcher PREDICATES_AND = new MethodMatcher("com.google.common.base.Predicates and(..)");
    private static final MethodMatcher PREDICATES_OR = new MethodMatcher("com.google.common.base.Predicates or(..)");
    private static final List<MethodMatcher> PREDICATES_METHODS_HANDLED = Arrays.asList(new MethodMatcher("com.google.common.base.Predicates equalTo(..)"),
            PREDICATES_AND, PREDICATES_OR, new MethodMatcher("com.google.common.base.Predicates not(..)"));

    @Override
    public String getDisplayName() {
        return "Prefer `Predicate.and(Predicate)`";
    }

    @Override
    public String getDescription() {
        return "Prefer `Predicate.and(Predicate)` over `Predicates.and(Predicate, Predicate)`.";
    }

    @Override
    public Set<String> getTags() {
        return singleton("guava");
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        TreeVisitor<?, ExecutionContext> precondition = Preconditions.or(new UsesMethod<>(PREDICATES_AND), new UsesMethod<>(PREDICATES_OR));
        return Preconditions.check(precondition, new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                if (PREDICATES_AND.matches(method)) {
                    return handlePredicatesMethod(method, "and");
                }
                if (PREDICATES_OR.matches(method)) {
                    return handlePredicatesMethod(method, "or");
                }

                return super.visitMethodInvocation(method, ctx);
            }

            private J handlePredicatesMethod(J.MethodInvocation method, String operation) {
                List<Expression> arguments = method.getArguments();
                if (arguments.size() < 2) {
                    return method;
                }

                // Avoid generic type issues, for method of "com.google.common.base.Predicates", by not making any changes just yet
                if (atLeastOneArgumentIsMethodInvocationOfPredicates(arguments)) {
                    return method;
                }

                maybeRemoveImport("com.google.common.base.Predicates");

                // Build the chain: first.operation(second).operation(third)...
                Expression result = arguments.get(0);

               // If the first argument is a method reference or a lambda, wrap it with a cast
                if ((result instanceof J.MemberReference || result instanceof J.Lambda) && result.getType() != null) {
                    String typeString = result.getType().toString().replace("com.google.common.base.", "");
                    result = JavaTemplate.apply("((" + typeString + ") #{any()})", getCursor(), method.getCoordinates().replace(), result);
                }
                for (int i = 1; i < arguments.size(); i++) {
                    result = JavaTemplate.apply("#{any(java.util.function.Predicate)}." + operation + "(#{any(java.util.function.Predicate)})",
                            getCursor(), method.getCoordinates().replace(), result, arguments.get(i));
                }

                doAfterVisit(ShortenFullyQualifiedTypeReferences.modifyOnly(result));
                return result;
            }

        });
    }

    private boolean atLeastOneArgumentIsMethodInvocationOfPredicates(List<Expression> arguments) {
        for (Expression expression : arguments) {
            if (expression instanceof J.MethodInvocation) {
                if (!isMethodHandled((J.MethodInvocation) expression)) {
                    JavaType.Method methodType = ((J.MethodInvocation) expression).getMethodType();
                    if (methodType != null) {
                        if (TypeUtils.isOfClassType(methodType.getDeclaringType(), "com.google.common.base.Predicates")) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean isMethodHandled(J.MethodInvocation methodInvocation) {
        for (MethodMatcher methodMatcher : PREDICATES_METHODS_HANDLED) {
            if (methodMatcher.matches(methodInvocation)) {
                return true;
            }
        }
        return false;
    }
}
