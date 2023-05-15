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
package org.openrewrite.java.migrate.logging;

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

public class MigrateLoggerLogrbToUseResourceBundle extends Recipe {
    private static final MethodMatcher MATCHER = new MethodMatcher("java.util.logging.Logger logrb(java.util.logging.Level, String, String, String, String, ..)");

    @Override
    public String getDisplayName() {
        return "Use `Logger#logrb(.., ResourceBundle bundleName, ..)`";
    }

    @Override
    public String getDescription() {
        return "Use `Logger#logrb(.., ResourceBundle bundleName, ..)` instead of the deprecated `java.util.logging.Logger#logrb(.., String bundleName, ..)` in Java 8 or higher.";
    }

    public Set<String> getTags() {
        return Collections.singleton("deprecated");
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesMethod<>(MATCHER), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = method;
                if (MATCHER.matches(m)) {
                    m = m.withTemplate(
                            JavaTemplate.builder("#{any(java.util.logging.Level)}, #{any(String)}, #{any(String)}, ResourceBundle.getBundle(#{any(String)}), #{any(String)}" + (m.getArguments().size() == 6 ? ", #{any()}" : ""))
                                    .context(getCursor())
                                    .imports("java.util.ResourceBundle")
                                    .build(),
                            getCursor(),
                            m.getCoordinates().replaceArguments(),
                            m.getArguments().toArray()
                    );
                    maybeAddImport("java.util.ResourceBundle");
                }
                return super.visitMethodInvocation(m, ctx);
            }
        });
    }

}
