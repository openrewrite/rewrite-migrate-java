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

import org.jetbrains.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

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
                new UsesType<>("org.apache.commons.lang3.StringUtils", false),
                new UsesType<>("org.apache.maven.shared.utils.StringUtils", false),
                new UsesType<>("org.codehaus.plexus.util.StringUtils", false));

        return Preconditions.check(precondition, new JavaVisitor<ExecutionContext>() {
            private final MethodMatcher isEmptyMatcher = new MethodMatcher("*..StringUtils isEmpty(..)");
            private final MethodMatcher isNotEmptyMatcher = new MethodMatcher("*..StringUtils isNotEmpty(..)");

            @SuppressWarnings("ConstantValue")
            private final JavaTemplate isEmptyReplacement = JavaTemplate.compile(this, "IsEmpty", (String s) -> (s == null || s.isEmpty())).build();
            @SuppressWarnings("ConstantValue")
            private final JavaTemplate isNotEmptyReplacement = JavaTemplate.compile(this, "IsNotEmpty", (String s) -> (s != null && !s.isEmpty())).build();

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J j = super.visitMethodInvocation(method, ctx);
                if (!(j instanceof J.MethodInvocation)) {
                    return j;
                }
                J.MethodInvocation mi = (J.MethodInvocation) j;
                Expression arg = mi.getArguments().get(0);
                if (!(arg instanceof J.Identifier) && !(arg instanceof J.FieldAccess)) {
                    return j;
                }

                JavaTemplate replacementTemplate = getReplacementTemplate(mi);
                if (replacementTemplate != null) {
                    // Maybe remove imports
                    maybeRemoveImport("org.apache.commons.lang3.StringUtils");
                    maybeRemoveImport("org.apache.maven.shared.utils.StringUtils");
                    maybeRemoveImport("org.codehaus.plexus.util.StringUtils");

                    // Remove excess parentheses
                    doAfterVisit(new org.openrewrite.java.cleanup.UnnecessaryParentheses());

                    return mi.withTemplate(replacementTemplate, getCursor(), mi.getCoordinates().replace(), arg, arg);
                }
                return mi;
            }

            @Nullable
            private JavaTemplate getReplacementTemplate(J.MethodInvocation mi) {
                if (isEmptyMatcher.matches(mi)) {
                    return isEmptyReplacement;
                } else if (isNotEmptyMatcher.matches(mi)) {
                    return isNotEmptyReplacement;
                }
                return null;
            }
        });
    }
}
