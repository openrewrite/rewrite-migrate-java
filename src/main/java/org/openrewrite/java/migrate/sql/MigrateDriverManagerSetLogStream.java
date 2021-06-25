/*
 * Copyright 2020 the original author or authors.
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
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;

public class MigrateDriverManagerSetLogStream extends Recipe {
    private static final MethodMatcher METHOD_MATCHER = new MethodMatcher("java.sql.DriverManager setLogStream(java.io.PrintStream)");

    @Override
    public String getDisplayName() {
        return "Use `DriverManager#setLogWriter(java.io.PrintWriter)`";
    }

    @Override
    public String getDescription() {
        return "`DriverManager#setLogStream(java.io.PrintStream)` was deprecated in Java 1.2.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            final JavaTemplate template = JavaTemplate.builder(this::getCursor, "new java.io.PrintWriter(#{any(java.io.PrintStream)})")
                    .build();

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);

                if (METHOD_MATCHER.matches(m)) {
                    m = method.withName(m.getName().withName("setLogWriter"))
                            .withTemplate(template,
                                    m.getCoordinates().replaceArguments(),
                                    m.getArguments().get(0));
                }
                return m;
            }
        };
    }
}
