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
package org.openrewrite.java.migrate.lang;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.StringJoiner;

public class MigrateCollectionsSingletonSet extends Recipe {
    private static final MethodMatcher SINGLETON_SET = new MethodMatcher("java.util.Collections singleton(..)", true);

    @Override
    public String getDisplayName() {
        return "Use `Set.of(..)` in Java 9 or higher";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(5);
    }

    @Override
    public String getDescription() {
        return "Replaces `Collections.singleton(<args>)))` with `Set.Of(<args>)`.";
    }

    @Override
    protected JavaVisitor<ExecutionContext> getSingleSourceApplicableTest() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext executionContext) {
                doAfterVisit(new UsesJavaVersion<>(9));
                doAfterVisit(new UsesMethod<>(SINGLETON_SET));
                return cu;
            }
        };
    }

    @Override
    protected JavaVisitor<ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                J.MethodInvocation m = (J.MethodInvocation) super.visitMethodInvocation(method, executionContext);
                if (SINGLETON_SET.matches(method)) {
                                maybeRemoveImport("java.util.Collections");

                                return autoFormat(m.withTemplate(
                                        JavaTemplate
                                                .builder(this::getCursor, "Set.of(#{any()})")
                                                .imports("java.util.Set")
                                                .build(),
                                        m.getCoordinates().replace(),
                                        m.getArguments().get(0)
                                ), executionContext);
                            }

                return m;
            }
        };
    }
}
