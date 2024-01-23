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

import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.*;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;

import static java.util.Collections.nCopies;

public class MigrateCollectionsSingletonList extends Recipe {
    private static final MethodMatcher SINGLETON_LIST = new MethodMatcher("java.util.Collections singletonList(..)", true);

    @Override
    public String getDisplayName() {
        return "Prefer `List.of(..)`";
    }

    @Override
    public String getDescription() {
        return "Prefer `List.of(..)` instead of using `Collections.singletonList()` in Java 9 or higher.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        TreeVisitor<?, ExecutionContext> check = Preconditions.and(new UsesJavaVersion<>(9),
                new UsesMethod<>(SINGLETON_LIST), new NoMissingTypes());
        return Preconditions.check(check, new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                if (SINGLETON_LIST.matches(method)) {
                    maybeRemoveImport("java.util.Collections");
                    maybeAddImport("java.util.List");
                    String argsPlaceHolders = String.join(", ", nCopies(m.getArguments().size(), "#{any()}"));
                    J.MethodInvocation listOf = JavaTemplate.builder("List.of(" + argsPlaceHolders + ')')
                            .imports("java.util.List")
                            .build()
                            .apply(getCursor(), m.getCoordinates().replace(), m.getArguments().toArray());
                    return listOf; // TODO MethodInvocation type is missing or malformed
                }
                return m;
            }
        });
    }
}
