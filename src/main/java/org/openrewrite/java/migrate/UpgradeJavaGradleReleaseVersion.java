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
package org.openrewrite.java.migrate;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import static java.util.Collections.singletonList;

@Value
@EqualsAndHashCode(callSuper = false)
public class UpgradeJavaGradleReleaseVersion extends Recipe {
    private static final MethodMatcher GET_RELEASE = new MethodMatcher("org.gradle.api.tasks.compile.CompileOptions getRelease()", true);
    private static final MethodMatcher SET = new MethodMatcher("org.gradle.api.provider.Property set(..)", true);

    @Option(displayName = "Java version",
            description = "The minimum Java release version to use in Java-authored Gradle compiler configuration.",
            example = "25")
    Integer version;

    String displayName = "Upgrade Java release in Java-authored Gradle configuration";

    String description = "Upgrade integer literals passed to `CompileOptions.getRelease().set(...)` in Java sources, " +
            "such as Gradle convention plugins. Preserves higher releases and dynamic values.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                if (!SET.matches(m) || m.getSelect() == null || m.getArguments().size() != 1 ||
                        !(m.getSelect().unwrap() instanceof J.MethodInvocation) ||
                        !GET_RELEASE.matches((J.MethodInvocation) m.getSelect().unwrap())) {
                    return m;
                }
                Expression argument = m.getArguments().get(0);
                if (argument instanceof J.Literal) {
                    J.Literal literal = (J.Literal) argument;
                    if (literal.getValue() instanceof Integer && (Integer) literal.getValue() < version) {
                        return m.withArguments(singletonList(literal.withValue(version).withValueSource(version.toString())));
                    }
                }
                return m;
            }
        };
    }
}
