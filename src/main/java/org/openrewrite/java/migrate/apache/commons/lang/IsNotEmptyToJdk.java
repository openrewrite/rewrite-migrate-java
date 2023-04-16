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
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

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
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {

            private final MethodMatcher commonsIsEmptyMatcher = new MethodMatcher(org.apache.commons.lang3.StringUtils.class.getName() + " isEmpty(..)");
            private final MethodMatcher commonsIsNotEmptyMatcher = new MethodMatcher(org.apache.commons.lang3.StringUtils.class.getName() + " isNotEmpty(..)");
            private final MethodMatcher plexusIsEmptyMatcher = new MethodMatcher(org.codehaus.plexus.util.StringUtils.class.getName() + " isEmpty(..)");
            private final MethodMatcher plexusIsNotEmptyMatcher = new MethodMatcher(org.codehaus.plexus.util.StringUtils.class.getName() + " isNotEmpty(..)");
            private final MethodMatcher mavenSharedIsEmptyMatcher = new MethodMatcher(org.apache.maven.shared.utils.StringUtils.class.getName() + " isEmpty(..)");
            private final MethodMatcher mavenSharedIsNotEmptyMatcher = new MethodMatcher(org.apache.maven.shared.utils.StringUtils.class.getName() + " isNotEmpty(..)");

            private final JavaTemplate isEmptyReplacement = JavaTemplate.compile(this, "IsEmpty", (String s) -> (s == null || s.isEmpty())).build();
            private final JavaTemplate isNotEmptyReplacement = JavaTemplate.compile(this, "IsNotEmpty", (String s) -> (s != null && !s.isEmpty())).build();

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                J j = super.visitMethodInvocation(method, executionContext);
                if (j instanceof J.MethodInvocation) {
                    J.MethodInvocation mi = (J.MethodInvocation) j;
                    Expression arg = mi.getArguments().get(0);

                    // Maybe remove imports
                    maybeRemoveImport("org.apache.commons.lang3.StringUtils");
                    maybeRemoveImport("org.codehaus.plexus.util.StringUtils");
                    maybeRemoveImport("org.apache.maven.shared.utils.StringUtils");

                    // Remove excess parentheses
                    doAfterVisit(new org.openrewrite.java.cleanup.UnnecessaryParentheses());

                    // JavaTemplate.compile name can not be an expression; only a string literal; hence repetition here
                    if (commonsIsEmptyMatcher.matches(mi) || plexusIsEmptyMatcher.matches(mi) || mavenSharedIsEmptyMatcher.matches(mi)) {
                        return mi.withTemplate(isEmptyReplacement, mi.getCoordinates().replace(), arg, arg);
                    } else if (commonsIsNotEmptyMatcher.matches(mi) || plexusIsNotEmptyMatcher.matches(mi) || mavenSharedIsNotEmptyMatcher.matches(mi)) {
                        return mi.withTemplate(isNotEmptyReplacement, mi.getCoordinates().replace(), arg, arg);
                    }
                }
                return j;
            }
        };
    }
}
