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
package org.openrewrite.java.migrate.util;

import lombok.Getter;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class UseListOf extends Recipe {
    private static final MethodMatcher NEW_ARRAY_LIST = new MethodMatcher("java.util.ArrayList <constructor>()", true);
    private static final MethodMatcher LIST_ADD = new MethodMatcher("java.util.List add(..)", true);

    @Getter
    final String displayName = "Prefer `List.of(..)`";

    @Getter
    final String description = "Prefer `List.of(..)` instead of using `java.util.List#add(..)` in anonymous ArrayList initializers in Java 10 or higher. " +
            "This recipe will not modify code where the List is later mutated since `List.of` returns an immutable list.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                Preconditions.and(
                        new UsesJavaVersion<>(10),
                        new UsesMethod<>(NEW_ARRAY_LIST)),
                new JavaVisitor<ExecutionContext>() {
                    @Override
                    public J visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                        J.NewClass n = (J.NewClass) super.visitNewClass(newClass, ctx);
                        J.Block body = n.getBody();
                        if (NEW_ARRAY_LIST.matches(n) && body != null && body.getStatements().size() == 1) {
                            Statement statement = body.getStatements().get(0);
                            if (statement instanceof J.Block) {
                                List<Expression> args = new ArrayList<>();
                                StringJoiner listOf = new StringJoiner(", ", "List.of(", ")");
                                for (Statement stat : ((J.Block) statement).getStatements()) {
                                    if (!(stat instanceof J.MethodInvocation) || !LIST_ADD.matches((Expression) stat)) {
                                        return n;
                                    }
                                    J.MethodInvocation add = (J.MethodInvocation) stat;
                                    // List.add() takes only one argument
                                    if (add.getArguments().size() != 1) {
                                        return n;
                                    }
                                    args.add(add.getArguments().get(0));
                                    listOf.add("#{any()}");
                                }

                                maybeRemoveImport("java.util.ArrayList");
                                maybeAddImport("java.util.List");
                                return JavaTemplate.builder(listOf.toString())
                                        .contextSensitive()
                                        .imports("java.util.List")
                                        .build()
                                        .apply(updateCursor(n), n.getCoordinates().replace(), args.toArray());
                            }
                        }

                        return n;
                    }
                });
    }

}
