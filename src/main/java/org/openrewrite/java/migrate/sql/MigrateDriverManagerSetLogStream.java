/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.java.migrate.sql;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;

import java.util.Collections;
import java.util.Set;

public class MigrateDriverManagerSetLogStream extends Recipe {
    private static final MethodMatcher METHOD_MATCHER = new MethodMatcher("java.sql.DriverManager setLogStream(java.io.PrintStream)");

    @Override
    public String getDisplayName() {
        return "Use `DriverManager#setLogWriter(java.io.PrintWriter)`";
    }

    @Override
    public String getDescription() {
        return "Use `DriverManager#setLogWriter(java.io.PrintWriter)` instead of the deprecated `DriverManager#setLogStream(java.io.PrintStream)` in Java 1.2 or higher.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("deprecated");
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesMethod<>(METHOD_MATCHER), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                if (METHOD_MATCHER.matches(m)) {
                    m = m.withName(m.getName().withSimpleName("setLogWriter"));
                    m = JavaTemplate.builder("new java.io.PrintWriter(#{any(java.io.PrintStream)})")
                            .build()
                            .apply(updateCursor(m), m.getCoordinates().replaceArguments(), m.getArguments().get(0));
                }
                return m;
            }
        });
    }
}
