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
package org.openrewrite.java.migrate.lang;

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.Set;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;

public class MigrateSecurityManagerMulticast extends Recipe {
    private static final MethodMatcher MULTICAST_METHOD = new MethodMatcher("java.lang.SecurityManager checkMulticast(java.net.InetAddress, byte)");

    @Getter
    final String displayName = "Use `SecurityManager#checkMulticast(InetAddress)`";

    @Getter
    final String description = "Use `SecurityManager#checkMulticast(InetAddress)` instead of the deprecated " +
            "`SecurityManager#checkMulticast(InetAddress, byte)` in Java 1.4 or higher. " +
            "The two overloads are separate methods that a `SecurityManager` subclass can override independently, " +
            "so a call is only migrated when the receiver is a plain `new SecurityManager()` instance, " +
            "where no override can be reached, and when evaluating the discarded time to live argument " +
            "can neither be observed nor throw.";

    @Getter
    final Set<String> tags = singleton( "deprecated" );

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesMethod<>(MULTICAST_METHOD), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);

                if (MULTICAST_METHOD.matches(m) && m.getArguments().size() == 2 &&
                        cannotDispatchToAnOverride(m.getSelect()) &&
                        isPureAndNonThrowing(m.getArguments().get(1))) {
                    JavaType.Method singleArgumentOverload = singleArgumentOverload(m.getMethodType());
                    if (singleArgumentOverload != null) {
                        J.MethodInvocation migrated = m.withArguments(singletonList(m.getArguments().get(0)))
                                .withMethodType(singleArgumentOverload);
                        if (migrated.getName().getType() != null) {
                            migrated = migrated.withName(migrated.getName().withType(singleArgumentOverload));
                        }
                        return migrated;
                    }
                }
                return m;
            }
        });
    }

    /**
     * {@code checkMulticast(InetAddress, byte)} and {@code checkMulticast(InetAddress)} are separate virtual
     * methods, so switching a call from one to the other changes which method runs whenever the receiver holds a
     * {@code SecurityManager} subclass that overrides either overload. The receiver's runtime class is only known
     * here when the receiver is a direct {@code new SecurityManager()} allocation without an anonymous class body.
     * For {@code java.lang.SecurityManager} itself both overloads are specified to call {@code checkPermission}
     * with the same {@code SocketPermission}, and the two argument form is documented not to use the time to
     * live, so only then is the switch unobservable. Every other receiver, including any expression whose static
     * type merely is {@code SecurityManager}, may hold a subclass at run time and is left unchanged.
     */
    private static boolean cannotDispatchToAnOverride(@Nullable Expression select) {
        J receiver = select;
        while (receiver instanceof J.Parentheses) {
            receiver = ((J.Parentheses<?>) receiver).getTree();
        }
        if (!(receiver instanceof J.NewClass)) {
            return false;
        }
        J.NewClass newClass = (J.NewClass) receiver;
        return newClass.getBody() == null && TypeUtils.isOfClassType(newClass.getType(), "java.lang.SecurityManager");
    }

    /**
     * The migrated call must reference the declared {@code checkMulticast(InetAddress)} overload rather than a
     * synthetic method type trimmed down from the two argument overload, which would carry over the latter's
     * deprecation metadata. When the overload cannot be resolved the call is left unchanged.
     */
    private static JavaType.@Nullable Method singleArgumentOverload(JavaType.@Nullable Method twoArgumentOverload) {
        if (twoArgumentOverload == null) {
            return null;
        }
        for (JavaType.Method candidate : twoArgumentOverload.getDeclaringType().getMethods()) {
            if ("checkMulticast".equals(candidate.getName()) && candidate.getParameterTypes().size() == 1) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * The one argument overload ignores the time to live, so dropping the second argument also stops evaluating it.
     * That is only safe when evaluating the argument can neither be observed nor throw, which is limited to
     * constants and reads of a local variable or parameter of primitive type. Anything else, such as a method
     * invocation, an increment, an array access, a field read that might be volatile, or an unboxing conversion,
     * is left unchanged.
     */
    private static boolean isPureAndNonThrowing(Expression argument) {
        if (argument instanceof J.Literal) {
            return true;
        }
        if (argument instanceof J.TypeCast) {
            // A primitive to primitive cast neither throws nor has side effects, unlike a reference or unboxing cast.
            J.TypeCast typeCast = (J.TypeCast) argument;
            return typeCast.getType() instanceof JavaType.Primitive && isPureAndNonThrowing(typeCast.getExpression());
        }
        if (argument instanceof J.Identifier) {
            JavaType.Variable variable = ((J.Identifier) argument).getFieldType();
            return variable != null && variable.getType() instanceof JavaType.Primitive &&
                    variable.getOwner() instanceof JavaType.Method;
        }
        return false;
    }
}
