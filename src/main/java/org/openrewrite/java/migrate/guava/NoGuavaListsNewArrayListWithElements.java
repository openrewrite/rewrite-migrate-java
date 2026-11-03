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
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.J;

import java.util.Set;

import static java.util.Collections.nCopies;
import static java.util.Collections.singleton;

public class NoGuavaListsNewArrayListWithElements extends Recipe {
    private static final MethodMatcher NEW_ARRAY_LIST = new MethodMatcher("com.google.common.collect.Lists newArrayList(..)");

    @Getter
    final String displayName = "Prefer `new ArrayList<>(Arrays.asList(..))`";

    @Getter
    final String description = "Prefer `new ArrayList<>(Arrays.asList(..))` over Guava's `Lists.newArrayList(E...)`.";

    @Getter
    final Set<String> tags = singleton("guava");

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesMethod<>(NEW_ARRAY_LIST), new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
                if (!NEW_ARRAY_LIST.matches(m) || m.getMethodType() == null || !m.getMethodType().hasFlags(Flag.Varargs)) {
                    return m;
                }
                // Without its type arguments, the diamond infers the element type from the elements,
                // which can be narrower than the one the call named
                if (m.getTypeParameters() != null) {
                    return m;
                }
                maybeRemoveImport("com.google.common.collect.Lists");
                maybeAddImport("java.util.ArrayList");
                maybeAddImport("java.util.Arrays");
                return JavaTemplate.builder("new ArrayList<>(Arrays.asList(" +
                                            String.join(", ", nCopies(m.getArguments().size(), "#{any()}")) + "))")
                        .imports("java.util.ArrayList", "java.util.Arrays")
                        .build()
                        .apply(getCursor(), m.getCoordinates().replace(), m.getArguments().toArray());
            }
        });
    }
}
