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
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.ChangeMethodName;
import org.openrewrite.java.ChangeType;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.J;

import java.util.Arrays;
import java.util.List;

public class NoGuavaPredicate extends Recipe {
    private static final String GUAVA_PREDICATE = "com.google.common.base.Predicate";
    private static final String JAVA_PREDICATE = "java.util.function.Predicate";

    @Getter
    final String displayName = "Change Guava's `Predicate` into `java.util.function.Predicate` where possible";

    @Getter
    final String description = "Replace `Predicates.and`, `or`, `not` and `equalTo` and change the type, " +
            "in source files where no remaining method call requires a Guava `Predicate`.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J preVisit(J java, ExecutionContext ctx) {
                // The sub-recipes each visit the whole tree, so this visitor stops at the source file
                stopAfterPreVisit();
                Cursor parent = getCursor().getParentOrThrow();
                J j = java;
                for (Recipe conversion : conversions()) {
                    j = (J) conversion.getVisitor().visitNonNull(j, ctx, parent);
                }
                // The conversions produce `java.util.function.Predicate` values, which fit only where the type changes too.
                // `NoGuavaPredicatesAndOr` keeps some calls, so the check runs on the converted file.
                if (new DoesNotRequireGuavaType(GUAVA_PREDICATE, JAVA_PREDICATE, null).getVisitor().visitNonNull(j, ctx, parent) == j) {
                    return java;
                }
                j = (J) new ChangeMethodName(GUAVA_PREDICATE + " apply(..)", "test", true, false)
                        .getVisitor().visitNonNull(j, ctx, parent);
                return (J) new ChangeType(GUAVA_PREDICATE, JAVA_PREDICATE, false)
                        .getVisitor().visitNonNull(j, ctx, parent);
            }
        };
    }

    private static List<Recipe> conversions() {
        return Arrays.asList(
                new NoGuavaPredicatesAndOr(),
                new NoGuavaPredicatesEqualTo(),
                new NoGuavaPredicatesNot());
    }
}
