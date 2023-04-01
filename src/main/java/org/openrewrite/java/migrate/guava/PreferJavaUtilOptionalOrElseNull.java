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

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class PreferJavaUtilOptionalOrElseNull extends Recipe {
    @Override
    public String getDisplayName() {
        return "Prefer `java.util.Optional#orElse(null)` over `com.google.common.base.Optional#orNull()`";
    }

    @Override
    public String getDescription() {
        return "Replaces `com.google.common.base.Optional#orNull()` with `java.util.Optional#orElse(null)`.";
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
    protected UsesType<ExecutionContext> getApplicableTest() {
        return new UsesType<>("com.google.common.base.Optional", false);
    }

    @Override
    protected UsesMethod<ExecutionContext> getSingleSourceApplicableTest() {
        return new UsesMethod<>("com.google.common.base.Optional orNull()");
    }

    @Override
    protected JavaIsoVisitor<ExecutionContext> getVisitor() {
        return new PreferJavaUtilOptionalOrElseNullVisitor();
    }

    private static class PreferJavaUtilOptionalOrElseNullVisitor extends JavaIsoVisitor<ExecutionContext> {
        private static final MethodMatcher OPTIONAL_OR_NULL_MATCHER = new MethodMatcher("com.google.common.base.Optional orNull()");

        @Override
        public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
            J.CompilationUnit c = super.visitCompilationUnit(cu, ctx);
            maybeAddImport("java.util.Optional");
            maybeRemoveImport("com.google.common.base.Optional");
            return c;
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
            if (OPTIONAL_OR_NULL_MATCHER.matches(mi)) {
                return mi.withName(mi.getName().withSimpleName("orElse"))
                        .withTemplate(JavaTemplate.builder(this::getCursor, "null").build(), mi.getCoordinates().replaceArguments());
            }
            return mi;
        }
    }
}
