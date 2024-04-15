/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.java.migrate.jakarta;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;
import org.openrewrite.staticanalysis.RemoveUnusedLocalVariables;
import org.openrewrite.staticanalysis.SimplifyConstantIfBranchExecution;

import static org.openrewrite.Tree.randomId;

public class RemoveBeanIsNullable extends Recipe {
    @Override
    public String getDisplayName() {
        return "Remove `Bean.isNullable()`";
    }

    @Override
    public String getDescription() {
        return "`Bean.isNullable()` has been removed in CDI 4.0.0, and now always returns `false`.";
    }

    private static final MethodMatcher BEAN_ISNULLABLE = new MethodMatcher("jakarta.enterprise.inject.spi.Bean isNullable()", false);

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                if (BEAN_ISNULLABLE.matches(method)) {
                    // clean up leftover conditions and remove unused variables
                    doAfterVisit(new SimplifyConstantIfBranchExecution().getVisitor());
                    doAfterVisit(new RemoveUnusedLocalVariables(null).getVisitor());
                    return new J.Literal(randomId(), Space.SINGLE_SPACE, Markers.EMPTY, Boolean.FALSE, "false", null, JavaType.Primitive.Boolean);
                }
                return super.visitMethodInvocation(method, ctx);
            }
        };
    }
}