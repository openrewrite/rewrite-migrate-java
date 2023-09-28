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

import org.jetbrains.annotations.NotNull;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.MethodCall;

public class RemoveNewInstance extends Recipe {
    //TDDO te matchmaker should find a way to check for an interface implementation
    private static final MethodMatcher METHOD_PATTERN = new MethodMatcher("jakarta.xml.soap.SOAPElementFactory newInstance()", false);

    @Override
    public String getDisplayName() {
        return "SOAPElementFactory.newInstance() is removed";
    }

    @Override
    public String getDescription() {
        return "The static method SOAPElementFactory.newInstance() is removed without replacement. " +
                "See the Jakarta EE 9.1 documentation for more information about the replacement for this removed class.";
    }

    @Override
    public @NotNull TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MethodInvocationVisitor();
    }

    private static class MethodInvocationVisitor extends JavaVisitor<ExecutionContext> {

        @SuppressWarnings("NullableProblems")
        @Nullable
        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ec) {
            return visitMethodCall(method);
        }

        @Nullable
        private <M extends MethodCall> M visitMethodCall(M methodCall) {
            //return method call if they dont match
            if (!METHOD_PATTERN.matches(methodCall)) {
                return methodCall;
            }
            J.Block parentBlock = getCursor().firstEnclosing(J.Block.class);
            //if the block doesn't contain method return it
            if (parentBlock != null && !parentBlock.getStatements().contains(methodCall)) {
                return methodCall;
            }
            return null;
        }
    }

}
