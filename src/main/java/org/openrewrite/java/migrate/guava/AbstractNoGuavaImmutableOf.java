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
package org.openrewrite.java.migrate.guava;

import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.*;

import java.time.Duration;

abstract class AbstractNoGuavaImmutableOf extends Recipe {

    private final String guavaType;
    private final String javaType;

    AbstractNoGuavaImmutableOf(String guavaType, String javaType) {
        this.guavaType = guavaType;
        this.javaType = javaType;
    }

    private String getShortType(String fullyQualifiedType) {
        return fullyQualifiedType.substring(javaType.lastIndexOf(".") + 1);
    }

    @Override
    public String getDisplayName() {
        return "Prefer `" + getShortType(javaType) + ".of(..)` in Java 9 or higher";
    }

    @Override
    public String getDescription() {
        return "Replaces `" + getShortType(guavaType) + ".of(..)` if the returned type is immediately down-cast.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(10);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        TreeVisitor<?, ExecutionContext> check = Preconditions.and(new UsesJavaVersion<>(9),
                new UsesType<>(guavaType, false));
        final MethodMatcher IMMUTABLE_MATCHER = new MethodMatcher(guavaType + " of(..)");
        return Preconditions.check(check, new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                if (IMMUTABLE_MATCHER.matches(method) && isParentTypeDownCast(method)) {
                    maybeRemoveImport(guavaType);
                    maybeAddImport(javaType);

                    String template;
                    Object[] args;
                    if (method.getArguments().isEmpty() || method.getArguments().get(0) instanceof J.Empty) {
                        template = getShortType(javaType) + ".of()";
                        args = new Object[]{};
                    } else if ("com.google.common.collect.ImmutableMap".equals(guavaType)) {
                        template = getShortType(javaType) + ".of(#{any()}, #{any()})";
                        args = new Object[]{method.getArguments().get(0), method.getArguments().get(1)};
                    } else {
                        template = getShortType(javaType) + ".of(#{any()})";
                        args = new Object[]{method.getArguments().get(0)};
                    }

                    J.MethodInvocation templated = JavaTemplate.builder(template)
                            .imports(javaType)
                            .build()
                            .apply(getCursor(),
                                    method.getCoordinates().replace(),
                                    args);
                    return templated.getPadding().withArguments(method.getPadding().getArguments());
                }
                return super.visitMethodInvocation(method, ctx);
            }

            private boolean isParentTypeDownCast(MethodCall immutableMethod) {
                J parent = getCursor().dropParentUntil(J.class::isInstance).getValue();
                boolean isParentTypeDownCast = false;
                if (parent instanceof J.VariableDeclarations.NamedVariable) {
                    isParentTypeDownCast = isParentTypeMatched(((J.VariableDeclarations.NamedVariable) parent).getType());
                } else if (parent instanceof J.Assignment) {
                    J.Assignment a = (J.Assignment) parent;
                    if (a.getVariable() instanceof J.Identifier && ((J.Identifier) a.getVariable()).getFieldType() != null) {
                        isParentTypeDownCast = isParentTypeMatched(((J.Identifier) a.getVariable()).getFieldType().getType());
                    } else if (a.getVariable() instanceof J.FieldAccess) {
                        isParentTypeDownCast = isParentTypeMatched(a.getVariable().getType());
                    }
                } else if (parent instanceof J.Return) {
                    // Does not currently support returns in lambda expressions.
                    J j = getCursor().dropParentUntil(is -> is instanceof J.MethodDeclaration || is instanceof J.CompilationUnit).getValue();
                    if (j instanceof J.MethodDeclaration) {
                        TypeTree returnType = ((J.MethodDeclaration) j).getReturnTypeExpression();
                        if (returnType != null) {
                            isParentTypeDownCast = isParentTypeMatched(returnType.getType());
                        }
                    }
                } else if (parent instanceof J.MethodInvocation) {
                    J.MethodInvocation m = (J.MethodInvocation) parent;
                    int index = m.getArguments().indexOf(immutableMethod);
                    if (m.getMethodType() != null && index != -1) {
                        isParentTypeDownCast = isParentTypeMatched(m.getMethodType().getParameterTypes().get(index));
                    }
                } else if (parent instanceof J.NewClass) {
                    J.NewClass c = (J.NewClass) parent;
                    int index = 0;
                    if (c.getConstructorType() != null) {
                        for (Expression argument : c.getArguments()) {
                            if (IMMUTABLE_MATCHER.matches(argument)) {
                                break;
                            }
                            index++;
                        }
                        if (c.getConstructorType() != null) {
                            isParentTypeDownCast = isParentTypeMatched(c.getConstructorType().getParameterTypes().get(index));
                        }
                    }
                } else if (parent instanceof J.NewArray) {
                    J.NewArray a = (J.NewArray) parent;
                    JavaType arrayType = a.getType();
                    while (arrayType instanceof JavaType.Array) {
                        arrayType = ((JavaType.Array) arrayType).getElemType();
                    }

                    isParentTypeDownCast = isParentTypeMatched(arrayType);
                }
                return isParentTypeDownCast;
            }

            private boolean isParentTypeMatched(@Nullable JavaType type) {
                JavaType.FullyQualified fq = TypeUtils.asFullyQualified(type);
                return TypeUtils.isOfClassType(fq, javaType) ||
                       TypeUtils.isOfClassType(fq, "java.lang.Object");
            }
        });
    }
}
