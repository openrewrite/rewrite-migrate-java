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
package org.openrewrite.java.migrate;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;

public class NoGuavaDirectExecutor extends Recipe {
    private static final MethodMatcher DIRECT_EXECUTOR = new MethodMatcher("com.google.common.util.concurrent.MoreExecutors directExecutor()");

    @Override
    public String getDisplayName() {
        return "Use Java SDK instead of `MoreExecutors#directExecutor()`.";
    }

    @Override
    public String getDescription() {
        return "`Executor` is a SAM-compatible interface, so `Runnable::run` is just as succinct as `MoreExecutors.directExecutor()` but without the third-party library requirement.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new UsesMethod<>(DIRECT_EXECUTOR);
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {
            private final JavaTemplate template = template("Runnable::run")
                    .imports("java.lang.Runnable")
                    .build();

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                if(DIRECT_EXECUTOR.matches(method)) {
                    maybeRemoveImport("com.google.common.util.concurrent.MoreExecutors");
                    return method.withTemplate(template, method.getCoordinates().replace());
                }
                return super.visitMethodInvocation(method, executionContext);
            }
        };
    }
}
