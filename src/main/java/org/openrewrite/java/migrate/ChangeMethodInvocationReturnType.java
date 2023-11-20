/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.java.migrate;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.Markers;

import static java.util.Collections.emptyList;

@Value
@EqualsAndHashCode(callSuper = true)
public class ChangeMethodInvocationReturnType extends Recipe {

    @Option(displayName = "Method pattern",
            description = "A method pattern that is used to find matching method declarations/invocations.",
            example = "org.mockito.Matchers anyVararg()")
    String methodPattern;

    @Option(displayName = "New method invocation return type",
            description = "The return return type of method invocation.",
            example = "long")
    String newReturnType;

    @Override
    public String getDisplayName() {
        return "Change method invocation return type";
    }

    @Override
    public String getDescription() {
        return "Changes the return type of a method invocation.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            private final MethodMatcher methodMatcher = new MethodMatcher(methodPattern, false);

            private boolean methodUpdated;

            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                JavaType.Method type = m.getMethodType();
                if (methodMatcher.matches(method) && type != null && !newReturnType.equals(type.getReturnType().toString())) {
                    type = type.withReturnType(JavaType.buildType(newReturnType));
                    m = m.withMethodType(type);
                    methodUpdated = true;
                }
                return m;
            }

            @Override
            public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                methodUpdated = false;
                JavaType.FullyQualified originalType = multiVariable.getTypeAsFullyQualified();
                J.VariableDeclarations mv = super.visitVariableDeclarations(multiVariable, ctx);

                if (methodUpdated) {
                    JavaType newType = JavaType.buildType(newReturnType);
                    JavaType.FullyQualified newFieldType = TypeUtils.asFullyQualified(newType);

                    maybeAddImport(newFieldType);
                    maybeRemoveImport(originalType);

                    mv = mv.withTypeExpression(mv.getTypeExpression() == null ?
                            null :
                            new J.Identifier(mv.getTypeExpression().getId(),
                                    mv.getTypeExpression().getPrefix(),
                                    Markers.EMPTY,
                                    emptyList(),
                                    newReturnType,
                                    newType,
                                    null
                            )
                    );

                    mv = mv.withVariables(ListUtils.map(mv.getVariables(), var -> {
                        JavaType.FullyQualified varType = TypeUtils.asFullyQualified(var.getType());
                        if (varType != null && !varType.equals(newType)) {
                            return var.withType(newType).withName(var.getName().withType(newType));
                        }
                        return var;
                    }));
                }

                return mv;
            }
        };
    }
}
