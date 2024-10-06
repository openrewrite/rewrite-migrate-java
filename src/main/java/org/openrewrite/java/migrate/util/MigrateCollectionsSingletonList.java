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

import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.NoMissingTypes;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.JavaType.ShallowClass;
import org.openrewrite.java.tree.Space;

import java.util.Collections;

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
                if (SINGLETON_LIST.matches(m) && isNotLiteralNull(m)) {
                    maybeRemoveImport("java.util.Collections");
                    maybeAddImport("java.util.List");

                    JavaType.Class classType = ShallowClass.build("java.util.List");
                    JavaType.Method methodType = m.getMethodType().withName("of").withDeclaringType(classType);
                    m = m.withName(m.getName().withSimpleName("of").withType(methodType));
                    if (m.getSelect() instanceof J.Identifier) {
                        return m.withSelect(((J.Identifier) m.getSelect()).withSimpleName("List").withType(classType));
                    }
                    return m.withSelect(new J.Identifier(
                                    Tree.randomId(), m.getPrefix(), m.getMarkers(), Collections.emptyList(), "List", classType, null))
                            .withPrefix(Space.EMPTY);
                }
                return m;
            }

            private boolean isNotLiteralNull(J.MethodInvocation m) {
                return !(m.getArguments().get(0) instanceof J.Literal &&
                         ((J.Literal) m.getArguments().get(0)).getValue() == null);
            }
        });
    }
}
