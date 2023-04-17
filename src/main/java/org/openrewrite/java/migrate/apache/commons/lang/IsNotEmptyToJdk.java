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
import org.openrewrite.Applicability;
import org.openrewrite.ExecutionContext;
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

    static final String ORG_APACHE_COMMONS_LANG_3_STRING_UTILS = "org.apache.commons.lang3.StringUtils";
    static final String ORG_APACHE_MAVEN_SHARED_UTILS_STRING_UTILS = "org.apache.maven.shared.utils.StringUtils";
    static final String ORG_CODEHAUS_PLEXUS_UTIL_STRING_UTILS = "org.codehaus.plexus.util.StringUtils";

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
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return Applicability.or(
                new UsesType<>(ORG_APACHE_COMMONS_LANG_3_STRING_UTILS, false),
                new UsesType<>(ORG_APACHE_MAVEN_SHARED_UTILS_STRING_UTILS, false),
                new UsesType<>(ORG_CODEHAUS_PLEXUS_UTIL_STRING_UTILS, false));
    }

    @Override
    public Set<String> getTags() {
        return new HashSet<>(Arrays.asList("apache", "commons"));
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {
            private final MethodMatcher commonsIsEmptyMatcher = new MethodMatcher(ORG_APACHE_COMMONS_LANG_3_STRING_UTILS + " isEmpty(..)");
            private final MethodMatcher commonsIsNotEmptyMatcher = new MethodMatcher(ORG_APACHE_COMMONS_LANG_3_STRING_UTILS + " isNotEmpty(..)");
            private final MethodMatcher mavenSharedIsEmptyMatcher = new MethodMatcher(ORG_APACHE_MAVEN_SHARED_UTILS_STRING_UTILS + " isEmpty(..)");
            private final MethodMatcher mavenSharedIsNotEmptyMatcher = new MethodMatcher(ORG_APACHE_MAVEN_SHARED_UTILS_STRING_UTILS + " isNotEmpty(..)");
            private final MethodMatcher plexusIsEmptyMatcher = new MethodMatcher(ORG_CODEHAUS_PLEXUS_UTIL_STRING_UTILS + " isEmpty(..)");
            private final MethodMatcher plexusIsNotEmptyMatcher = new MethodMatcher(ORG_CODEHAUS_PLEXUS_UTIL_STRING_UTILS + " isNotEmpty(..)");

            private final JavaTemplate isEmptyReplacement = JavaTemplate.compile(this, "IsEmpty", (String s) -> (s == null || s.isEmpty())).build();
            private final JavaTemplate isNotEmptyReplacement = JavaTemplate.compile(this, "IsNotEmpty", (String s) -> (s != null && !s.isEmpty())).build();

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                J j = super.visitMethodInvocation(method, executionContext);
                if (!(j instanceof J.MethodInvocation)) {
                    return j;
                }
                J.MethodInvocation mi = (J.MethodInvocation) j;
                Expression arg = mi.getArguments().get(0);
                if (!(arg instanceof J.Identifier)) {
                    return j;
                }

                JavaTemplate replacementTemplate = getReplacementTemplate(mi);
                if (replacementTemplate != null) {
                    // Maybe remove imports
                    maybeRemoveImport(ORG_APACHE_COMMONS_LANG_3_STRING_UTILS);
                    maybeRemoveImport(ORG_APACHE_MAVEN_SHARED_UTILS_STRING_UTILS);
                    maybeRemoveImport(ORG_CODEHAUS_PLEXUS_UTIL_STRING_UTILS);

                    // Remove excess parentheses
                    doAfterVisit(new org.openrewrite.java.cleanup.UnnecessaryParentheses());

                    return mi.withTemplate(replacementTemplate, mi.getCoordinates().replace(), arg, arg);
                }
                return mi;
            }

            @Nullable
            private JavaTemplate getReplacementTemplate(J.MethodInvocation mi) {
                if (commonsIsEmptyMatcher.matches(mi) || mavenSharedIsEmptyMatcher.matches(mi) || plexusIsEmptyMatcher.matches(mi)) {
                    return isEmptyReplacement;
                } else if (commonsIsNotEmptyMatcher.matches(mi) || mavenSharedIsNotEmptyMatcher.matches(mi) || plexusIsNotEmptyMatcher.matches(mi)) {
                    return isNotEmptyReplacement;
                }
                return null;
            }
        };
    }
}
