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

import org.openrewrite.Applicability;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(5);
    }

    @Override
    public Set<String> getTags() {
        return new HashSet<>(Arrays.asList("RSPEC-4738", "guava"));
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getApplicableTest() {
        return Applicability.and(
                new UsesJavaVersion<>(9),
                new UsesType<>("com.google.common.base.Optional", true));
    }

    @Override
    protected UsesMethod<ExecutionContext> getSingleSourceApplicableTest() {
        return new UsesMethod<>(METHOD_MATCHER);
    }

    @Override
    protected JavaIsoVisitor<ExecutionContext> getVisitor() {
        return new PreferJavaUtilOptionalOrSupplierVisitor();
    }

    private static class PreferJavaUtilOptionalOrSupplierVisitor extends JavaIsoVisitor<ExecutionContext> {
        @Override
        public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext executionContext) {
            J.CompilationUnit c = super.visitCompilationUnit(cu, executionContext);
            maybeAddImport("java.util.Optional");
            maybeRemoveImport("com.google.common.base.Optional");
            return c;
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
            J.MethodInvocation j = super.visitMethodInvocation(method, executionContext);
            if (METHOD_MATCHER.matches(method)) {
                j = j.withTemplate(
                        JavaTemplate.builder(this::getCursor, "#{any(java.util.Optional)}.or(() -> #{any(java.util.Optional)})")
                                .imports("java.util.Optional")
                                .build(),
                        method.getCoordinates().replace(),
                        j.getSelect(),
                        j.getArguments().get(0)
                );
            }
            return j;
        }
    }
}
