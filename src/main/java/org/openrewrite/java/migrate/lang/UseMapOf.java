/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.java.migrate.lang;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class UseMapOf extends Recipe {
    private static final MethodMatcher NEW_HASH_MAP = new MethodMatcher("java.util.HashMap <constructor>()", true);
    private static final MethodMatcher MAP_PUT = new MethodMatcher("java.util.Map put(..)", true);

    @Override
    public String getDisplayName() {
        return "Use `Map.of(..)` where possible";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(5);
    }

    @Override
    public String getDescription() {
        return "This succinct syntax was introduced in Java 10.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new UsesMethod<>(NEW_HASH_MAP);
    }

    @Override
    protected JavaVisitor<ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                J.NewClass n = (J.NewClass) super.visitNewClass(newClass, ctx);
                J.Block body = n.getBody();
                if (NEW_HASH_MAP.matches(n) && body != null) {
                    if (body.getStatements().size() == 1) {
                        Statement statement = body.getStatements().get(0);
                        if (statement instanceof J.Block) {
                            List<Expression> args = new ArrayList<>();
                            StringJoiner mapOf = new StringJoiner(", ", "Map.of(", ")");
                            for (Statement stat : ((J.Block) statement).getStatements()) {
                                if (stat instanceof J.MethodInvocation && MAP_PUT.matches((Expression) stat)) {
                                    J.MethodInvocation put = (J.MethodInvocation) stat;
                                    args.addAll(put.getArguments());
                                    mapOf.add("#{}");
                                    mapOf.add("#{}");
                                } else {
                                    return n;
                                }
                            }

                            maybeRemoveImport("java.util.HashMap");
                            maybeAddImport("java.util.Map");
                            return autoFormat(n.withTemplate(
                                    JavaTemplate
                                            .builder(this::getCursor, mapOf.toString())
                                            .imports("java.util.Map")
                                            .build(),
                                    n.getCoordinates().replace(),
                                    args.toArray()
                            ), ctx);
                        }
                    }
                }
                return n;
            }
        };
    }
}
