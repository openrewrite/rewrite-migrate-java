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
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;

@Value
@EqualsAndHashCode(callSuper = false)
public class UpdateBeanIsNullable extends Recipe {

    @Override
    public @NotNull String getDisplayName() {
        return "Remove methods calls";
    }

    @Override
    public @NotNull String getDescription() {
        return "Checks for a method patterns and removes the method call from the class";
    }

    @Override
    public @NotNull TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MethodInvocationVisitor("jakarta.enterprise.inject.spi.Bean isNullable()");
    }

    private static class MethodInvocationVisitor extends JavaVisitor<ExecutionContext> {
        private final MethodMatcher METHOD_PATTERN;

        private MethodInvocationVisitor(String methodPattern) {
            METHOD_PATTERN = new MethodMatcher(methodPattern, false);
        }

        @Nullable
        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ec) {
            if (!METHOD_PATTERN.matches(method)) {
                return method;
            }
            //case where we delete here create a build a template with just false being returned
            return JavaTemplate.builder("Boolean.valueOf(\"false\")").build().apply(getCursor(), method.getCoordinates().replace());
        }
    }

}
