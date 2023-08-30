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
package org.openrewrite.java.migrate.apache.commons.lang;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class IsNotEmptyToJdk extends Recipe {

    @Override
    public String getDisplayName() {
        return "Replace any StringUtils#isEmpty(String) and #isNotEmpty(String)";
    }

    @Override
    public String getDescription() {
        return "Replace any `StringUtils#isEmpty(String)` and `#isNotEmpty(String)` with `s == null || s.isEmpty()` and `s != null && !s.isEmpty()`.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(1);
    }

    @Override
    public Set<String> getTags() {
        return new HashSet<>(Arrays.asList("apache", "commons"));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        TreeVisitor<?, ExecutionContext> precondition = Preconditions.or(
                new UsesMethod<>("org.apache.commons.lang3.StringUtils isEmpty(..)"),
                new UsesMethod<>("org.apache.commons.lang3.StringUtils isNotEmpty(..)"),
                new UsesMethod<>("org.apache.maven.shared.utils.StringUtils isEmpty(..)"),
                new UsesMethod<>("org.apache.maven.shared.utils.StringUtils isNotEmpty(..)"),
                new UsesMethod<>("org.codehaus.plexus.util.StringUtils isEmpty(..)"),
                new UsesMethod<>("org.codehaus.plexus.util.StringUtils isNotEmpty(..)"));

        return Preconditions.check(precondition, new JavaVisitor<ExecutionContext>() {
            private final MethodMatcher isEmptyMatcher = new MethodMatcher("*..StringUtils isEmpty(..)");
            private final MethodMatcher isNotEmptyMatcher = new MethodMatcher("*..StringUtils isNotEmpty(..)");
            private final MethodMatcher trimMatcher = new MethodMatcher("java.lang.String trim()");

            private final JavaTemplate isEmptyReplacement = JavaTemplate.compile(this, "IsEmpty", (String s) -> (s == null || s.isEmpty())).build();
            private final JavaTemplate isNotEmptyReplacement = JavaTemplate.compile(this, "IsNotEmpty", (String s) -> (s != null && !s.isEmpty())).build();
            private final JavaTemplate isEmptyTrimmed = JavaTemplate.compile(this, "IsEmptyTrimmed", (JavaTemplate.F1<?, ?>) (String s) -> s.trim().isEmpty()).build();
            private final JavaTemplate isNotEmptyTrimmed = JavaTemplate.compile(this, "IsNotEmptyTrimmed", (String s) -> !s.trim().isEmpty()).build();

            @Override
            public J visitMethodInvocation(J.MethodInvocation mi, ExecutionContext ctx) {
                boolean isEmptyCall = isEmptyMatcher.matches(mi);
                if (!isEmptyCall && !isNotEmptyMatcher.matches(mi)) {
                    return mi;
                }

                Expression arg = mi.getArguments().get(0);

                // Replace StringUtils.isEmpty(var) with var == null || var.isEmpty()
                if (isArgumentSafeToRepeat(arg)) {
                    JavaTemplate replacementTemplate = isEmptyCall ? isEmptyReplacement : isNotEmptyReplacement;
                    // Maybe remove imports
                    maybeRemoveImport("org.apache.commons.lang3.StringUtils");
                    maybeRemoveImport("org.apache.maven.shared.utils.StringUtils");
                    maybeRemoveImport("org.codehaus.plexus.util.StringUtils");
                    // Remove excess parentheses inserted in lambda that may be required depending on the context
                    doAfterVisit(new org.openrewrite.staticanalysis.UnnecessaryParentheses().getVisitor());
                    return replacementTemplate.apply(updateCursor(mi), mi.getCoordinates().replace(), arg, arg);
                }

                // Replace StringUtils.isEmpty(var.trim()) with var.trim().isEmpty()
                if (trimMatcher.matches(arg)
                        && (((J.MethodInvocation) arg).getSelect() instanceof J.Identifier || ((J.MethodInvocation) arg).getSelect() instanceof J.FieldAccess)) {
                    JavaTemplate replacementTemplate = isEmptyCall ? isEmptyTrimmed : isNotEmptyTrimmed;
                    // Maybe remove imports
                    maybeRemoveImport("org.apache.commons.lang3.StringUtils");
                    maybeRemoveImport("org.apache.maven.shared.utils.StringUtils");
                    maybeRemoveImport("org.codehaus.plexus.util.StringUtils");
                    return replacementTemplate.apply(updateCursor(mi), mi.getCoordinates().replace(), ((J.MethodInvocation) arg).getSelect());
                }

                return super.visitMethodInvocation(mi, ctx);
            }

            private boolean isArgumentSafeToRepeat(Expression arg) {
                // Allow simple getters that return a String
                if (arg instanceof J.MethodInvocation
                        && ((J.MethodInvocation) arg).getSelect() instanceof J.Identifier
                        && ((J.MethodInvocation) arg).getSimpleName().startsWith("get")
                        && (((J.MethodInvocation) arg).getArguments().isEmpty() || ((J.MethodInvocation) arg).getArguments().get(0) instanceof J.Empty)
                        && TypeUtils.isAssignableTo("java.lang.String", ((J.MethodInvocation) arg).getMethodType())) {
                    return true;
                }
                return arg instanceof J.Identifier || arg instanceof J.FieldAccess;
            }
        });
    }
}
