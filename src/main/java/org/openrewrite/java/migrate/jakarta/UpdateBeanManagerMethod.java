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
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.JavaParser;

public class UpdateBeanManagerMethod extends Recipe {
    @Option(displayName = "Method Pattern",
            description = "A `BeanManager.fireEvent()` or `BeanManager.createInjectionTarget()` matching required",
            example = "jakarta.enterprise.inject.spi.BeanManager fireEvent()")
    @NonNull String methodPattern;

    @Override
    public @NotNull String getDisplayName() {
        return "Update `fireEvent()` and `createInjectionTarget()` calls";
    }

    @Override
    public @NotNull String getDescription() {
        return " Updates `BeanManager.fireEvent()` or `BeanManager.createInjectionTarget()`";
    }

    MethodMatcher methodInputPattern = null;

    @JsonCreator
    public UpdateBeanManagerMethod(@NonNull @JsonProperty("methodPattern") String methodPattern) {
        this.methodPattern = methodPattern;
        methodInputPattern = new MethodMatcher(methodPattern, false);
    }

    @Override
    public @NotNull TreeVisitor<?, ExecutionContext> getVisitor() {
        return new MethodInvocationVisitor(methodPattern);
    }

    private static class MethodInvocationVisitor extends JavaVisitor<ExecutionContext> {
        private final MethodMatcher methodPattern;

        private MethodInvocationVisitor(String methodPattern) {
            this.methodPattern = new MethodMatcher(methodPattern, false);
        }

        @Nullable
        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ec) {
            J.MethodInvocation mi = (J.MethodInvocation) super.visitMethodInvocation(method, ec);
            if (methodPattern.matches(method)) {
                String newMethodName = "";
                if (method.getSimpleName().equals("fireEvent")) {
                    newMethodName = "getEvent()." + "fire";

                    JavaType.Method type = method.getMethodType();
                    if (type != null) {
                        type = type.withName(newMethodName);
                    }
                    return method.withName(method.getName().withSimpleName(newMethodName)).withMethodType(type);
                } else if (method.getSimpleName().equals("createInjectionTarget")) {
                    maybeRemoveImport("jakarta.enterprise.inject.spi.BeanManager");
                    return JavaTemplate.builder("#{any(jakarta.enterprise.inject.spi.BeanManager)}.getInjectionTargetFactory(#{any(jakarta.enterprise.inject.spi.AnnotatedType)}).createInjectionTarget(null)")
                            .javaParser(JavaParser.fromJavaVersion().classpathFromResources(ec, "jakarta.enterprise.cdi-api-3.0.0-M4"))
                            .build()
                            .apply(updateCursor(mi), mi.getCoordinates().replace(), mi.getSelect(), mi.getArguments().get(0));

                }
            }
            return method;
        }
    }
}
