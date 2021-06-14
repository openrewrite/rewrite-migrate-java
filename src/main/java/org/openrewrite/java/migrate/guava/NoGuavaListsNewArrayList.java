/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.java.migrate.guava;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;

public class NoGuavaListsNewArrayList extends Recipe {
    private static final MethodMatcher NEW_ARRAY_LIST = new MethodMatcher("com.google.common.collect.Lists newArrayList()");

    @Override
    public String getDisplayName() {
        return "Use `new ArrayList<>()` instead of Guava";
    }

    @Override
    public String getDescription() {
        return "Prefer the Java standard library over third-party usage of Guava in simple cases like this.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getApplicableTest() {
        return new UsesMethod<>(NEW_ARRAY_LIST);
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {
            private final JavaTemplate newArrayList = template("new ArrayList<>()")
                    .imports("java.util.ArrayList")
                    .build();

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                if (NEW_ARRAY_LIST.matches(method)) {
                    maybeRemoveImport("com.google.common.collect.Lists");
                    maybeAddImport("java.util.ArrayList");
                    return method.withTemplate(newArrayList, method.getCoordinates().replace());
                }
                return super.visitMethodInvocation(method, executionContext);
            }
        };
    }
}
