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
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.MethodCall;



public class ReturnFalse extends Recipe {

    @Option(displayName = "Method Pattern",
            description = "A method pattern for matching required method definition.",
            example = "*..* hello(..)")
    @NonNull
    String methodPattern;

    @Override
    public @NotNull String getDisplayName() {
        return "Remove methods calls";
    }

    @Override
    public @NotNull String getDescription() {
        return "Checks for a method patterns and removes the method call from the class";
    }
    // All recipes must be serializable. This is verified by RewriteTest.rewriteRun() in your tests.
    @JsonCreator
    public ReturnFalse(@NonNull @JsonProperty("methodPattern") String methodPattern) {
        this.methodPattern = methodPattern;
    }
    @Override
    public @NotNull TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MethodInvocationVisitor();
    }

    private class MethodInvocationVisitor extends JavaVisitor<ExecutionContext> {
        private final MethodMatcher METHOD_PATTERN = new MethodMatcher(methodPattern, false);

        @Nullable
        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ec) {
            return visitMethodCall(method);
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
            //case where we delete here create a build a template with just false being returned
            return JavaTemplate.builder("false").build().apply(updateCursor(methodCall), methodCall.getCoordinates().replace(), methodCall.getArguments().get(0));
            //return null;
        }
    }

}
