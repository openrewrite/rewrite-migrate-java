/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.java.migrate.guava;

import org.openrewrite.ExecutionContext;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.ArrayList;
import java.util.List;

import static org.openrewrite.java.migrate.guava.PreferJavaStringJoin.JOIN_METHOD_MATCHER;
import static org.openrewrite.java.tree.TypeUtils.isAssignableTo;
import static org.openrewrite.java.tree.TypeUtils.isString;

class PreferJavaStringJoinVisitor extends JavaIsoVisitor<ExecutionContext> {
    private static final MethodMatcher ON_METHOD_MATCHER =
            new MethodMatcher("com.google.common.base.Joiner on(String)");

    @Override
    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
        J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);

        if (!JOIN_METHOD_MATCHER.matches(mi) || !(mi.getSelect() instanceof J.MethodInvocation) || !ON_METHOD_MATCHER.matches(mi.getSelect())) {
            return mi;
        }

        boolean rewriteToJavaString = false;

        List<Expression> arguments = mi.getArguments();
        if (arguments.size() == 1) {
            JavaType javaType = arguments.get(0).getType();

            rewriteToJavaString = isCompatibleArray(javaType) || isCompatibleIterable(javaType);
        } else if (arguments.size() >= 2) {
            rewriteToJavaString = isCompatibleArguments(arguments);
        }

        if (rewriteToJavaString) {
            J.MethodInvocation select = (J.MethodInvocation) mi.getSelect();
            assert select != null;
            List<Expression> newArgs = appendArguments(select.getArguments(), mi.getArguments());

            maybeRemoveImport("com.google.common.base.Joiner");

            return JavaTemplate.<J.MethodInvocation>apply(
                    "String.join(#{any(java.lang.CharSequence)}",
                    getCursor(),
                    mi.getCoordinates().replace(),
                    select.getArguments().get(0)
            ).withArguments(newArgs);
        }
        return mi;
    }

    private boolean isCompatibleArguments(List<Expression> arguments) {
        return arguments.stream().map(Expression::getType).allMatch(PreferJavaStringJoinVisitor::isCharSequence);
    }

    private boolean isCompatibleArray(@Nullable JavaType javaType) {
        if (javaType instanceof JavaType.Array) {
            return isCharSequence(((JavaType.Array) javaType).getElemType());
        }
        return false;
    }

    private boolean isCompatibleIterable(@Nullable JavaType javaType) {
        if (isAssignableTo(Iterable.class.getName(), javaType) && javaType instanceof JavaType.Parameterized) {
            List<JavaType> typeParameters = ((JavaType.Parameterized) javaType).getTypeParameters();
            return typeParameters.size() == 1 && isCharSequence(typeParameters.get(0));
        }
        return false;
    }

    private static boolean isCharSequence(@Nullable JavaType javaType) {
        return isString(javaType) || isAssignableTo(CharSequence.class.getName(), javaType);
    }

    private List<Expression> appendArguments(List<Expression> firstArgs, List<Expression> secondArgs) {
        ArrayList<Expression> args = new ArrayList<>(firstArgs);
        if (!secondArgs.isEmpty()) {
            Expression e = secondArgs.remove(0);
            args.add(e.withPrefix(e.getPrefix().withWhitespace(" ")));
            args.addAll(secondArgs);
        }
        return args;
    }
}
