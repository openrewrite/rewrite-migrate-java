/*
 * Copyright 2024 the original author or authors.
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
import org.openrewrite.java.tree.J;

public class DeprecatedCountStackFramesMethod extends Recipe {
    private static final MethodMatcher THREAD_COUNT_STACK_FRAMES = new MethodMatcher("java.lang.Thread countStackFrames()", false);
    private static final String THREAD_PACKAGE = "java.lang.Thread ";

    @Override
    public String getDisplayName() {
        return "Remove `Thread.countStackFrames()`";
    }

    @Override
    public String getDescription() {
        return "`Thread.countStackFrames()` has been removed in Java SE 14." +
                "It is now replaced by \"Integer.valueOf(\"0\")\" to return 0." +
                "The updated line is dead code and should be eventually removed.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                if (THREAD_COUNT_STACK_FRAMES.matches(method)) {
                    maybeRemoveImport(THREAD_PACKAGE);
                    return JavaTemplate.builder("Integer.valueOf(\"0\")").build()
                            .apply(getCursor(), method.getCoordinates().replace());
                }
                return super.visitMethodInvocation(method, ctx);
            }
        };
    }
}
