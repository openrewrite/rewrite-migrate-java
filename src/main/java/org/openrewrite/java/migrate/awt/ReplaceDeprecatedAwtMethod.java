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
package org.openrewrite.java.migrate.awt;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class ReplaceDeprecatedAwtMethod extends Recipe {

    @Option(displayName = "Method pattern",
            description = "A method pattern matching the deprecated method; overrides of it are matched as well.",
            example = "java.awt.Component show()")
    String methodPattern;

    @Option(displayName = "New method name",
            description = "The name of the method that replaces the deprecated one.",
            example = "setVisible")
    String newMethodName;

    @Option(displayName = "Appended boolean argument",
            description = "A boolean literal to pass as an extra, last argument to the new method.",
            required = false,
            example = "true")
    @Nullable
    Boolean booleanArgument;

    String displayName = "Replace a deprecated AWT method";

    String description = "Replace calls to an AWT method deprecated since JDK 1.1 with calls to the method that " +
            "replaced it. Declarations and `super` calls are left alone: the replacements delegate back to the " +
            "deprecated methods, so a rewritten `super` call would reach overrides it used to bypass.";

    @Override
    public String getInstanceNameSuffix() {
        return String.format("`%s` to `%s`", methodPattern, newMethodName);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        MethodMatcher matcher = new MethodMatcher(methodPattern, true);
        return Preconditions.check(new UsesMethod<>(matcher), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                JavaType.Method type = mi.getMethodType();
                if (!matcher.matches(mi) || type == null || isSuperCall(mi)) {
                    return mi;
                }

                List<Expression> arguments = ListUtils.filter(mi.getArguments(), a -> !(a instanceof J.Empty));
                List<JavaType> parameterTypes = type.getParameterTypes();
                if (booleanArgument != null) {
                    arguments = ListUtils.concat(arguments, new J.Literal(Tree.randomId(),
                            arguments.isEmpty() ? Space.EMPTY : Space.SINGLE_SPACE, Markers.EMPTY,
                            booleanArgument, String.valueOf(booleanArgument), null, JavaType.Primitive.Boolean));
                    parameterTypes = ListUtils.concat(parameterTypes, JavaType.Primitive.Boolean);
                }

                JavaType.Method replacement = TypeUtils.findDeclaredMethod(
                        type.getDeclaringType(), newMethodName, parameterTypes).orElse(null);
                if (replacement == null) {
                    return mi;
                }
                return mi.withName(mi.getName().withSimpleName(newMethodName).withType(replacement))
                        .withMethodType(replacement)
                        .withArguments(arguments);
            }

            private boolean isSuperCall(J.MethodInvocation mi) {
                return mi.getSelect() instanceof J.Identifier &&
                        "super".equals(((J.Identifier) mi.getSelect()).getSimpleName());
            }
        });
    }
}
