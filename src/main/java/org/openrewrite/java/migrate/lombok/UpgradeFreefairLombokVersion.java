/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.java.migrate.lombok;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.gradle.search.FindPlugins;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.maven.tree.Version;

import java.util.Set;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;

@EqualsAndHashCode(callSuper = false)
@Value
public class UpgradeFreefairLombokVersion extends Recipe {

    // Gradle build scripts lack type attribution for plugin extensions, so match by name (matchUnknownTypes=true at call sites).
    private static final MethodMatcher LOMBOK_EXTENSION = new MethodMatcher("* lombok(..)");
    private static final MethodMatcher PROPERTY_SET = new MethodMatcher("* set(..)");

    @Option(displayName = "Minimum Lombok version",
            description = "The Lombok version to upgrade to, if the configured version is lower.",
            example = "1.18.48")
    String version;

    String displayName = "Upgrade the Lombok version of the `io.freefair.lombok` Gradle plugin";

    String description = "The `io.freefair.lombok` Gradle plugin adds the Lombok dependency itself, using the version " +
            "configured in its `lombok { version = ... }` extension, so dependency upgrades don't see it. " +
            "Raise that version to the given minimum. Covers `version = ...` and `version.set(...)` inside `lombok { }`, " +
            "and `lombok.version = ...`, in Groovy and Kotlin DSL build scripts. Will not downgrade.";

    Set<String> tags = singleton("lombok");

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new FindPlugins("io.freefair.lombok", null), new JavaIsoVisitor<ExecutionContext>() {
            private boolean insideLombokExtension;

            @Override
            public J.Assignment visitAssignment(J.Assignment assignment, ExecutionContext ctx) {
                if (isLombokVersion(assignment.getVariable()) && assignment.getAssignment() instanceof J.Literal) {
                    return assignment.withAssignment(upgrade((J.Literal) assignment.getAssignment(), version));
                }
                return assignment;
            }

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                if (!insideLombokExtension && LOMBOK_EXTENSION.matches(method, true)) {
                    insideLombokExtension = true;
                    try {
                        return super.visitMethodInvocation(method, ctx);
                    } finally {
                        insideLombokExtension = false;
                    }
                }
                if (PROPERTY_SET.matches(method, true) && method.getSelect() != null &&
                        isLombokVersion(method.getSelect()) &&
                        method.getArguments().size() == 1 && method.getArguments().get(0) instanceof J.Literal) {
                    J.Literal literal = (J.Literal) method.getArguments().get(0);
                    J.Literal upgraded = upgrade(literal, version);
                    return upgraded == literal ? method : method.withArguments(singletonList(upgraded));
                }
                // Only closures can hold a `lombok { }` block or further `version` settings
                if (method.getArguments().stream().noneMatch(J.Lambda.class::isInstance)) {
                    return method;
                }
                return super.visitMethodInvocation(method, ctx);
            }

            private boolean isLombokVersion(Expression expression) {
                if (expression instanceof J.Identifier) {
                    return insideLombokExtension && "version".equals(((J.Identifier) expression).getSimpleName());
                }
                if (expression instanceof J.FieldAccess) {
                    J.FieldAccess fieldAccess = (J.FieldAccess) expression;
                    return "version".equals(fieldAccess.getSimpleName()) &&
                            fieldAccess.getTarget() instanceof J.Identifier &&
                            "lombok".equals(((J.Identifier) fieldAccess.getTarget()).getSimpleName());
                }
                return false;
            }
        });
    }

    private static J.Literal upgrade(J.Literal literal, String minimum) {
        if (!(literal.getValue() instanceof String) || literal.getValueSource() == null) {
            return literal;
        }
        String current = (String) literal.getValue();
        if (new Version(current).compareTo(new Version(minimum)) >= 0) {
            return literal;
        }
        // Preserve the original quote style (Groovy allows both ' and ").
        char quote = literal.getValueSource().charAt(0);
        return literal
                .withValue(minimum)
                .withValueSource(quote + minimum + quote);
    }
}
