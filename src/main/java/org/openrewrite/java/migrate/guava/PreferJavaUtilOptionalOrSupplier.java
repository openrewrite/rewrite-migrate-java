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
package org.openrewrite.java.migrate.guava;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Value
@EqualsAndHashCode(callSuper = true)
public class PreferJavaUtilOptionalOrSupplier extends Recipe {

    static final MethodMatcher METHOD_MATCHER = new MethodMatcher("com.google.common.base.Optional or(com.google.common.base.Optional)");

    @Override
    public String getDisplayName() {
        return "Prefer `java.util.Optional#or(Supplier<T extends java.util.Optional<T>>)`";
    }

    @Override
    public String getDescription() {
        return "Prefer `java.util.Optional#or(Supplier<T extends java.util.Optional<T>>)` over `com.google.common.base.Optional#or(com.google.common.base.Optional).";
    }

    @Override
    public Set<String> getTags() {
        return new HashSet<>(Arrays.asList("RSPEC-4738", "guava"));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                Preconditions.and(new UsesJavaVersion<>(9),
                        new UsesMethod<>(METHOD_MATCHER)),
                new PreferJavaUtilOptionalOrSupplierVisitor());
    }

    private static class PreferJavaUtilOptionalOrSupplierVisitor extends JavaIsoVisitor<ExecutionContext> {

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation j = super.visitMethodInvocation(method, ctx);
            if (METHOD_MATCHER.matches(method)) {
                j = JavaTemplate.builder("#{any(java.util.Optional)}.or(() -> #{any(java.util.Optional)})")
                        .contextSensitive()
                        .imports("java.util.Optional")
                        .build()
                        .apply(
                                getCursor(),
                                method.getCoordinates().replace(),
                                j.getSelect(),
                                j.getArguments().get(0));
                maybeAddImport("java.util.Optional");
                maybeRemoveImport("com.google.common.base.Optional");
            }
            return j;
        }
    }
}
