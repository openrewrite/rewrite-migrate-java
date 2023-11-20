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
package org.openrewrite.java.migrate.lang;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;

import java.util.Collections;
import java.util.Set;

public class RemoveRuntimeTraceMethods extends Recipe {
    private static final MethodMatcher TRACE_INSTRUCTIONS = new MethodMatcher("java.lang.Runtime traceInstructions(boolean)");
    private static final MethodMatcher TRACE_METHOD_CALLS = new MethodMatcher("java.lang.Runtime traceMethodCalls(boolean)");

    @Override
    public String getDisplayName() {
        return "Remove deprecated statements from Runtime module";
    }

    @Override
    public String getDescription() {
        return "Remove deprecated invocations of `Runtime.traceInstructions()` and `Runtime.traceMethodCalls()` which have no alternatives needed.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("JDK-8225330");
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(Preconditions.or(
                        new UsesMethod<>(TRACE_INSTRUCTIONS),
                        new UsesMethod<>(TRACE_METHOD_CALLS)),
                new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                        J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                        if (TRACE_INSTRUCTIONS.matches(mi) || TRACE_METHOD_CALLS.matches(mi)) {
                            return null;
                        }
                        return mi;
                    }
                });
    }
}
