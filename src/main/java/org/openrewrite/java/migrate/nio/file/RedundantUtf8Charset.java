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
package org.openrewrite.java.migrate.nio.file;

import lombok.Getter;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.ArrayList;
import java.util.List;

public class RedundantUtf8Charset extends Recipe {

    private static final List<MethodMatcher> MATCHERS = new ArrayList<>();

    static {
        for (String signature : new String[]{
                "java.nio.file.Files readString(java.nio.file.Path, java.nio.charset.Charset)",
                "java.nio.file.Files writeString(java.nio.file.Path, java.lang.CharSequence, java.nio.charset.Charset, ..)",
                "java.nio.file.Files readAllLines(java.nio.file.Path, java.nio.charset.Charset)",
                "java.nio.file.Files write(java.nio.file.Path, java.lang.Iterable, java.nio.charset.Charset, ..)",
                "java.nio.file.Files lines(java.nio.file.Path, java.nio.charset.Charset)",
                "java.nio.file.Files newBufferedReader(java.nio.file.Path, java.nio.charset.Charset)",
                "java.nio.file.Files newBufferedWriter(java.nio.file.Path, java.nio.charset.Charset, ..)"}) {
            MATCHERS.add(new MethodMatcher(signature));
        }
    }

    @Getter
    final String displayName = "Remove redundant `StandardCharsets.UTF_8` from `java.nio.file.Files` method calls";

    @Getter
    final String description = "The character based `java.nio.file.Files` methods always default to UTF-8, so passing " +
                               "`StandardCharsets.UTF_8` explicitly is redundant and can be removed.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>("java.nio.charset.StandardCharsets", false), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation mi = super.visitMethodInvocation(method, ctx);
                if (MATCHERS.stream().noneMatch(m -> m.matches(mi))) {
                    return mi;
                }
                List<Expression> arguments = mi.getArguments();
                for (int i = 0; i < arguments.size(); i++) {
                    if (isUtf8(arguments.get(i))) {
                        List<Expression> newArguments = new ArrayList<>(arguments);
                        newArguments.remove(i);
                        maybeRemoveImport("java.nio.charset.StandardCharsets");

                        JavaType.Method methodType = mi.getMethodType();
                        if (methodType != null && i < methodType.getParameterTypes().size()) {
                            List<JavaType> parameterTypes = new ArrayList<>(methodType.getParameterTypes());
                            List<String> parameterNames = new ArrayList<>(methodType.getParameterNames());
                            parameterTypes.remove(i);
                            parameterNames.remove(i);
                            methodType = methodType.withParameterTypes(parameterTypes).withParameterNames(parameterNames);
                        }
                        return mi.withArguments(newArguments)
                                .withMethodType(methodType)
                                .withName(mi.getName().withType(methodType));
                    }
                }
                return mi;
            }

            private boolean isUtf8(Expression argument) {
                if (!TypeUtils.isOfClassType(argument.getType(), "java.nio.charset.Charset")) {
                    return false;
                }
                if (argument instanceof J.FieldAccess) {
                    J.FieldAccess fieldAccess = (J.FieldAccess) argument;
                    return "UTF_8".equals(fieldAccess.getSimpleName()) &&
                           TypeUtils.isOfClassType(fieldAccess.getTarget().getType(), "java.nio.charset.StandardCharsets");
                }
                if (argument instanceof J.Identifier) {
                    J.Identifier identifier = (J.Identifier) argument;
                    JavaType.Variable fieldType = identifier.getFieldType();
                    return "UTF_8".equals(identifier.getSimpleName()) && fieldType != null &&
                           TypeUtils.isOfClassType(fieldType.getOwner(), "java.nio.charset.StandardCharsets");
                }
                return false;
            }
        });
    }
}
