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
package org.openrewrite.java.migrate.twilio;

import lombok.Getter;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JContainer;
import org.openrewrite.java.tree.JavaType;

public class TwilioJwtSecretToByteArray extends Recipe {
    private static final MethodMatcher ACCESS_TOKEN_BUILDER =
            new MethodMatcher("com.twilio.jwt.accesstoken.AccessToken$Builder <constructor>(String, String, String)");
    private static final MethodMatcher CLIENT_CAPABILITY_BUILDER =
            new MethodMatcher("com.twilio.jwt.client.ClientCapability$Builder <constructor>(String, String)");
    private static final MethodMatcher TASK_ROUTER_CAPABILITY_BUILDER =
            new MethodMatcher("com.twilio.jwt.taskrouter.TaskRouterCapability$Builder <constructor>(String, String, String, String)");

    private static final JavaType BYTE_ARRAY = new JavaType.Array(null, JavaType.Primitive.Byte, null);

    @Getter
    final String displayName = "Pass Twilio JWT secrets as `byte[]`";

    @Getter
    final String description = "Twilio Java SDK 10.8.0 changed the secret of `AccessToken.Builder`, " +
            "`ClientCapability.Builder` and `TaskRouterCapability.Builder` from `String` to `byte[]`. " +
            "The SDK used to call `String#getBytes()` itself, so this calls it at the construction site, " +
            "with `StandardCharsets.UTF_8` rather than the platform default. " +
            "Twilio secrets are ASCII, so the bytes and the signatures made with them stay the same.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                Preconditions.or(
                        new UsesMethod<>(ACCESS_TOKEN_BUILDER),
                        new UsesMethod<>(CLIENT_CAPABILITY_BUILDER),
                        new UsesMethod<>(TASK_ROUTER_CAPABILITY_BUILDER)),
                new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public boolean isAcceptable(SourceFile sourceFile, ExecutionContext ctx) {
                        // The template is Java; Kotlin has no `String#getBytes(Charset)`
                        return sourceFile instanceof J.CompilationUnit;
                    }

                    @Override
                    public J.NewClass visitNewClass(J.NewClass newClass, ExecutionContext ctx) {
                        J.NewClass n = super.visitNewClass(newClass, ctx);
                        int index = secretIndex(n);
                        if (index < 0) {
                            return n;
                        }
                        Expression secret = n.getArguments().get(index);
                        if (J.Literal.isLiteralValue(secret, null) || secret.getType() instanceof JavaType.Array) {
                            return n;
                        }

                        String receiver = isPrimary(secret) ? "#{any(String)}" : "(#{any(String)})";
                        Expression bytes = JavaTemplate.builder(receiver + ".getBytes(StandardCharsets.UTF_8)")
                                .imports("java.nio.charset.StandardCharsets")
                                .build()
                                .apply(new Cursor(getCursor(), secret), secret.getCoordinates().replace(), secret);
                        maybeAddImport("java.nio.charset.StandardCharsets");

                        // Through the padding, as the whitespace before a closing parenthesis on its own line is kept there
                        JContainer<Expression> arguments = n.getPadding().getArguments();
                        n = n.getPadding().withArguments(arguments.getPadding().withElements(ListUtils.map(
                                arguments.getPadding().getElements(), (i, arg) -> i == index ? arg.withElement(bytes) : arg)));
                        JavaType.Method constructor = n.getConstructorType();
                        if (constructor != null) {
                            n = n.withConstructorType(constructor.withParameterTypes(ListUtils.map(
                                    constructor.getParameterTypes(), (i, type) -> i == index ? BYTE_ARRAY : type)));
                        }
                        return n;
                    }

                    private int secretIndex(J.NewClass newClass) {
                        if (ACCESS_TOKEN_BUILDER.matches(newClass)) {
                            return 2;
                        }
                        if (CLIENT_CAPABILITY_BUILDER.matches(newClass) || TASK_ROUTER_CAPABILITY_BUILDER.matches(newClass)) {
                            return 1;
                        }
                        return -1;
                    }

                    private boolean isPrimary(Expression expression) {
                        return expression instanceof J.Identifier ||
                                expression instanceof J.FieldAccess ||
                                expression instanceof J.Literal ||
                                expression instanceof J.MethodInvocation ||
                                expression instanceof J.ArrayAccess ||
                                expression instanceof J.NewClass ||
                                expression instanceof J.Parentheses;
                    }
                });
    }
}
