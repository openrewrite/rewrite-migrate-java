/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.java.migrate.util;

import org.openrewrite.Applicability;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.*;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;

import java.time.Duration;

public class MigrateCollectionsSingletonList extends Recipe {
    private static final MethodMatcher SINGLETON_LIST = new MethodMatcher("java.util.Collections singletonList(..)", true);
    @Nullable
    private static J.MethodInvocation listOfTemplate;

    @Override
    public String getDisplayName() {
        return "Prefer `List.of(..)`";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(5);
    }

    @Override
    public String getDescription() {
        return "Prefer `List.of(..)` instead of using `Collections.singletonList()` in Java 9 or higher.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return Applicability.and(new UsesJavaVersion<>(9),
                new UsesMethod<>(SINGLETON_LIST), new NoMissingTypes());
    }

    @Override
    protected JavaVisitor<ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
                if (SINGLETON_LIST.matches(method)) {
                    maybeRemoveImport("java.util.Collections");
                    maybeAddImport("java.util.List");
                    return getListOfTemplate().withArguments(m.getArguments()).withPrefix(m.getPrefix());
                }

                return m;
            }
        };
    }

    private static J.MethodInvocation getListOfTemplate() {
        if (listOfTemplate == null) {
            listOfTemplate = PartProvider.buildPart("import java.util.List;" +
                                                    "class A {\n" +
                                                    "    Object a=List.of(\"X\");" +
                                                    "\n}", J.MethodInvocation.class);
        }

        return listOfTemplate;
    }
}
