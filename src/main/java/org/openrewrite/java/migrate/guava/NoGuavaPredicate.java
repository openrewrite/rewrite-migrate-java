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

import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.ChangeType;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaSourceFile;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.marker.SearchResult;

public class NoGuavaPredicate extends Recipe {
    @Override
    public String getDisplayName() {
        return "Change Guava's `Predicate` into `java.util.function.Predicate` where possible";
    }

    @Override
    public String getDescription() {
        return "Change the type only where no methods are used that explicitly require a Guava `Predicate`.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                Preconditions.not(new UsesPredicateMethod<>()),
                new ChangeType(
                        "com.google.common.base.Predicate",
                        "java.util.function.Predicate",
                        false)
                        .getVisitor()
        );
    }

    private static class UsesPredicateMethod<P> extends JavaIsoVisitor<P> {
        private static final MethodMatcher PREDICATE_METHOD_MATCHER = new MethodMatcher("*..* *(.., com.google.common.base.Predicate)");
        private static final MethodMatcher NOT_MATCHER = new MethodMatcher("*..* not(com.google.common.base.Predicate)");

        @Override
        public J preVisit(J tree, P p) {
            stopAfterPreVisit();
            if (tree instanceof JavaSourceFile) {
                JavaSourceFile cu = (JavaSourceFile) tree;
                for (JavaType.Method type : cu.getTypesInUse().getUsedMethods()) {
                    if (PREDICATE_METHOD_MATCHER.matches(type) &&
                            // Make an exception for `not` methods; those can be safely converted
                            !NOT_MATCHER.matches(type)) {
                        return SearchResult.found(cu);
                    }
                }
            }
            return tree;
        }
    }
}
