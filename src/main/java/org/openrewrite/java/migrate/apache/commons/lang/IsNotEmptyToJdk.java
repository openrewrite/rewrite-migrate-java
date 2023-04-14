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
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;

public class IsNotEmptyToJdk extends Recipe {
    @Override
    public String getDisplayName() {
        return "StringUtils.isNotEmpty to JDK methods";
    }

    @Override
    public String getDescription() {
        return "Converts Apache Commons Lang3's StringUtils.isNotEmpty to JDK methods.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        String className = "org.apache.commons.lang3.StringUtils";
        // TODO: Add StringUtils for Plexus and Maven shared as well
        return new JavaIsoVisitor<ExecutionContext>() {

            final MethodMatcher isEmptyMatcher = new MethodMatcher(className + " isEmpty(..)");
            final MethodMatcher isNotEmptyMatcher = new MethodMatcher(className + " isNotEmpty(..)");

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                J.MethodInvocation mi = super.visitMethodInvocation(method, executionContext);
                if (isEmptyMatcher.matches(mi)) {
                    // Replace with == null || .isEmpty()
                    return mi.withTemplate(
                            JavaTemplate.compile(this, "isEmpty", (String str) -> str == null || str.isEmpty()).build(),
                            mi.getCoordinates().replace());
                } else if (isNotEmptyMatcher.matches(mi)) {
                    // Replace with != null && !.isEmpty()
                    return mi.withTemplate(
                            JavaTemplate.compile(this, "isNotEmpty", (String str) -> str != null && !str.isEmpty()).build(),
                            mi.getCoordinates().replace());
                }
                return mi;
            }
        };
    }

}

