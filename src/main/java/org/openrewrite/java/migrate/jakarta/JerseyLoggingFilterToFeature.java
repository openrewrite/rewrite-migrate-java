/*
 * Copyright 2024 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.java.migrate.jakarta;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;

@Value
@EqualsAndHashCode(callSuper = false)
public class JerseyLoggingFilterToFeature extends Recipe {
    String displayName = "Replace Jersey logging filter with logging feature";
    String description = "Replace the removed Jersey `LoggingFilter(Logger, boolean)` constructor when passed directly to JAX-RS `Configurable.register`, preserving the logger and entity logging setting. Other usages require manual migration.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            final MethodMatcher constructor = new MethodMatcher("org.glassfish.jersey.filter.LoggingFilter <constructor>(java.util.logging.Logger, boolean)");

            final MethodMatcher javaxRegister = new MethodMatcher("javax.ws.rs.core.Configurable register(..)", true);
            final MethodMatcher jakartaRegister = new MethodMatcher("jakarta.ws.rs.core.Configurable register(..)", true);

            @Override
            public J.NewClass visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                J.NewClass n = super.visitNewClass(newClass, ctx);
                if (!constructor.matches(n) || n.getBody() != null) {
                    return n;
                }
                Object parent = getCursor().getParentTreeCursor().getValue();
                if (!(parent instanceof J.MethodInvocation)) {
                    return n;
                }
                J.MethodInvocation registration = (J.MethodInvocation) parent;
                if ((!javaxRegister.matches(registration) && !jakartaRegister.matches(registration)) ||
                    registration.getArguments().get(0) != newClass) {
                    return n;
                }
                maybeRemoveImport("org.glassfish.jersey.filter.LoggingFilter");
                maybeAddImport("org.glassfish.jersey.logging.LoggingFeature");
                maybeAddImport("java.util.logging.Level");
                return JavaTemplate.builder("new LoggingFeature(#{any(java.util.logging.Logger)}, Level.INFO, #{any(boolean)} ? LoggingFeature.Verbosity.PAYLOAD_ANY : LoggingFeature.Verbosity.HEADERS_ONLY, 8192)")
                        .imports("org.glassfish.jersey.logging.LoggingFeature", "java.util.logging.Level")
                        .javaParser(JavaParser.fromJavaVersion().dependsOn(
                                "package org.glassfish.jersey.logging; public class LoggingFeature { " +
                                "public enum Verbosity { PAYLOAD_ANY, HEADERS_ONLY } " +
                                "public LoggingFeature(java.util.logging.Logger logger, java.util.logging.Level level, Verbosity verbosity, Integer maxEntitySize) {} }"))
                        .build()
                        .apply(getCursor(), n.getCoordinates().replace(), n.getArguments().get(0), n.getArguments().get(1));
            }
        };
    }
}
