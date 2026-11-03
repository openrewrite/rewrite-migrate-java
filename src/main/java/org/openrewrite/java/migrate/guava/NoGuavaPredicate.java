/*
 * Copyright 2025 the original author or authors.
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
import org.openrewrite.java.ChangeMethodName;
import org.openrewrite.java.ChangeType;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

public class NoGuavaPredicate extends Recipe {
    @Getter
    final String displayName = "Change Guava's `Predicate` into `java.util.function.Predicate` where possible";

    @Getter
    final String description = "Change the type only where no methods are used that explicitly require a Guava `Predicate`.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                new DoesNotRequireGuavaType("com.google.common.base.Predicate", "java.util.function.Predicate", null).getVisitor(),
                new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J preVisit(J java, ExecutionContext ctx) {
                        // Apply both sub-recipes once over the whole tree; without this preVisit
                        // re-runs them on every descendant node, making the recipe quadratic.
                        stopAfterPreVisit();
                        J j = (J) new ChangeMethodName(
                                "com.google.common.base.Predicate apply(..)",
                                "test",
                                true,
                                false
                        ).getVisitor().visitNonNull(java, ctx, getCursor().getParentOrThrow());
                        return (J) new ChangeType(
                                "com.google.common.base.Predicate",
                                "java.util.function.Predicate",
                                false
                        ).getVisitor().visitNonNull(j, ctx, getCursor().getParentOrThrow());
                    }
                }
        );
    }
}
