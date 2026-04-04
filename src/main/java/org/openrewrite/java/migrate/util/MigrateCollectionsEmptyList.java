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
package org.openrewrite.java.migrate.util;

import lombok.Getter;
import org.openrewrite.*;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.JavaType.ShallowClass;
import org.openrewrite.java.tree.Space;

import static java.util.Collections.emptyList;

public class MigrateCollectionsEmptyList extends Recipe {
    private static final MethodMatcher EMPTY_LIST = new MethodMatcher("java.util.Collections emptyList()");

    @Getter
    final String displayName = "Prefer `List.of()`";

    @Getter
    final String description = "Prefer `List.of()` instead of using `Collections.emptyList()` in Java 9 or higher.";

    @Override
    public JavaIsoVisitor<ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);

                if (EMPTY_LIST.matches(m)) {
                    maybeRemoveImport("java.util.Collections");
                    maybeAddImport("java.util.List");

                    JavaType.Class classType = ShallowClass.build("java.util.List");
                    JavaType.Method methodType = m.getMethodType().withName("of").withDeclaringType(classType);
                    m = m.withName(m.getName().withSimpleName("of").withType(methodType));
                    if (m.getSelect() instanceof J.Identifier) {
                        return m.withSelect(((J.Identifier) m.getSelect()).withSimpleName("List").withType(classType));
                    }
                    return m.withSelect(new J.Identifier(
                                    Tree.randomId(), m.getPrefix(), m.getMarkers(), emptyList(), "List", classType, null))
                            .withPrefix(Space.EMPTY);
                }

                return m;
            }
        };
    }
}
