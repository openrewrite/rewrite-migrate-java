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

package org.openrewrite.java.migrate.jakarta;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.NonNull;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.MethodCall;

public class GetRealPathUpdate extends Recipe {
    final private String SERVLET_CONTEXT = "jakarta.servlet.ServletContext";
    final private String UPDATE_METHOD = "getContext().";
    @Option(displayName = "Method Pattern",
            description = "A jakarta.servlet.ServletRequest or jakarta.servlet.ServletRequestWrapper getRealPath(String) matching required",
            example = "jakarta.servlet.ServletRequest getRealPath(String)")
    @NonNull String methodPattern;

    // All recipes must be serializable. This is verified by RewriteTest.rewriteRun() in your tests.
    @JsonCreator
    public GetRealPathUpdate(@NonNull @JsonProperty("methodPattern") String methodPattern) {
        this.methodPattern = methodPattern;
    }

    @Override
    public @NotNull String getDisplayName() {
        return "Remove methods calls";
    }

    @Override
    public @NotNull String getDescription() {
        return "Updates `getRealPath()` for `jakarta.servlet.ServletRequest` and `jakarta.servlet.ServletRequestWrapper`";
    }

    @Override
    public @NotNull TreeVisitor<?, ExecutionContext> getVisitor() {
        return new GetRealPathUpdate.MethodInvocationVisitor();
    }

    private class MethodInvocationVisitor extends JavaVisitor<ExecutionContext> {
        private final MethodMatcher METHOD_PATTERN = new MethodMatcher(methodPattern, false);

        @Nullable
        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ec) {

            if (METHOD_PATTERN.matches(method)) {
                maybeAddImport(SERVLET_CONTEXT);
                String newMethodName = UPDATE_METHOD + method.getSimpleName();
                JavaType.Method type = method.getMethodType();
                if (type != null) {
                    type = type.withName(newMethodName);
                }
                method = method.withName(method.getName().withSimpleName(newMethodName)).withMethodType(type);
            }
            return method;
        }

        @Nullable
        private <M extends MethodCall> M visitMethodCall(M methodCall) {

            if (METHOD_PATTERN.matches(methodCall)) {
                return methodCall;
            }
            J.Block parentBlock = getCursor().firstEnclosing(J.Block.class);
            if (parentBlock != null && !parentBlock.getStatements().contains(methodCall)) {
                return methodCall;
            }
            return null;
        }
    }

}
