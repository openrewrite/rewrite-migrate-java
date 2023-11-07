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

import lombok.EqualsAndHashCode;
import lombok.Value;
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

@Value
@EqualsAndHashCode(callSuper = false)
public class UpdateBeanManagerMethod extends Recipe {
    @Option(displayName = "Method Pattern",
            description = "A `BeanManager.fireEvent()` or `BeanManager.createInjectionTarget()` matching required",
            example = "jakarta.enterprise.inject.spi.BeanManager fireEvent()")
    @NonNull String methodPattern;

    @Override
    public @NotNull String getDisplayName() {
        return "Update fireEvent() and createInjectionTarget() calls";
    }

    @Override
    public @NotNull String getDescription() {
        return " Updates `BeanManager.fireEvent()` or `BeanManager.createInjectionTarget()`";
    }

    @Override
    public @NotNull TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MethodInvocationVisitor(methodPattern);
    }

    private static class MethodInvocationVisitor extends JavaVisitor<ExecutionContext> {
        private final MethodMatcher METHOD_PATTERN;

        private MethodInvocationVisitor(String methodPattern) {
            METHOD_PATTERN = new MethodMatcher(methodPattern, false);
        }

        @Nullable
        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ec) {
            String newMethodName = "";
            if (METHOD_PATTERN.matches(method)) {
                if (method.getSimpleName().equals("fireEvent")){
                    newMethodName = "getEvent()." + "fire";
                }
                else if (method.getSimpleName().equals("createInjectionTarget")){
                    newMethodName = "getInjectionTargetFactory()." + "createInjectionTarget";
                }
               // newMethodName = newMethodName + method.getSimpleName();
                JavaType.Method type = method.getMethodType();
                if (type != null) {
                    type = type.withName(newMethodName);
                }
                method = method.withName(method.getName().withSimpleName(newMethodName)).withMethodType(type);
            }
            return method;
        }
    }
}
