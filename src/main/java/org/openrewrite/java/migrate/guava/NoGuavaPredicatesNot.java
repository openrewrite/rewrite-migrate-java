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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.ChangeMethodTargetToStatic;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

public class NoGuavaPredicatesNot extends Recipe {
    private static final String PREDICATES_NOT_PATTERN = "com.google.common.base.Predicates not(com.google.common.base.Predicate)";
    private static final MethodMatcher PREDICATES_NOT = new MethodMatcher(PREDICATES_NOT_PATTERN);

    @Getter
    final String displayName = "Prefer `Predicate.not(Predicate)` or `Predicate.negate()`";

    @Getter
    final String description = "Prefer `java.util.function.Predicate.not(Predicate)` over Guava's `Predicates.not(Predicate)` " +
                               "on Java 11 or higher, and `Predicate.negate()` before Java 11.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesMethod<>(PREDICATES_NOT), new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                if (new UsesJavaVersion<>(1, 10).visit(cu, ctx) == cu) {
                    doAfterVisit(new ChangeMethodTargetToStatic(PREDICATES_NOT_PATTERN, "java.util.function.Predicate", null, null).getVisitor());
                    return cu;
                }
                return super.visitCompilationUnit(cu, ctx);
            }

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation mi = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
                if (!PREDICATES_NOT.matches(mi)) {
                    return mi;
                }
                Expression predicate = mi.getArguments().get(0);
                // A lambda or method reference fits Guava's `not` whichever `Predicate` type the file ends up with
                if (predicate instanceof J.Lambda || predicate instanceof J.MemberReference) {
                    return mi;
                }
                maybeRemoveImport("com.google.common.base.Predicates");
                return JavaTemplate.apply("#{any(java.util.function.Predicate)}.negate()", getCursor(), mi.getCoordinates().replace(), predicate);
            }
        });
    }
}
