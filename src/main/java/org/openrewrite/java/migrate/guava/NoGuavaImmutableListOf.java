/*
 * Copyright 2021 the original author or authors.
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

import org.openrewrite.Applicability;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.*;

import java.time.Duration;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class NoGuavaImmutableListOf extends Recipe {
    private static final MethodMatcher IMMUTABLE_LIST_MATCHER = new MethodMatcher("com.google.common.collect.ImmutableList of(..)");

    @Override
    public String getDisplayName() {
        return "Use `List.of(..)` in Java 9 or higher";
    }

    @Override
    public String getDescription() {
        return "Replaces `ImmutableList.of(..)` if the returned type is immediately down-cast.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("RSPEC-4738");
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(10);
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return Applicability.and(new UsesJavaVersion<>(9),
                new UsesType<>("com.google.common.collect.ImmutableList"));
    }

    // Code is shared between `NoGuavaImmutableMapOf`, `NoGuavaImmutableListOf`, and `NoGuavaImmutableSetOf`.
    // Updates to either may apply to each of the recipes.
    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                if (IMMUTABLE_LIST_MATCHER.matches(method) && isParentTypeDownCast()) {
                    maybeRemoveImport("com.google.common.collect.ImmutableList");
                    maybeAddImport("java.util.List");

                    String template = method.getArguments().stream()
                            .map(arg -> {
                                if (arg.getType() instanceof JavaType.Primitive) {
                                    String type = "";
                                    if (JavaType.Primitive.Boolean == arg.getType()) {
                                        type = "Boolean";
                                    } else if (JavaType.Primitive.Byte == arg.getType()) {
                                        type = "Byte";
                                    } else if (JavaType.Primitive.Char == arg.getType()) {
                                        type = "Character";
                                    } else if (JavaType.Primitive.Double == arg.getType()) {
                                        type = "Double";
                                    } else if (JavaType.Primitive.Float == arg.getType()) {
                                        type = "Float";
                                    } else if (JavaType.Primitive.Int == arg.getType()) {
                                        type = "Integer";
                                    } else if (JavaType.Primitive.Long == arg.getType()) {
                                        type = "Long";
                                    } else if (JavaType.Primitive.Short == arg.getType()) {
                                        type = "Short";
                                    } else if (JavaType.Primitive.String == arg.getType()) {
                                        type = "String";
                                    }
                                    return TypeUtils.asFullyQualified(JavaType.buildType("java.lang." + type));
                                } else {
                                    return TypeUtils.asFullyQualified(arg.getType());
                                }
                            })
                            .filter(Objects::nonNull)
                            .map(type -> "#{any(" + type.getFullyQualifiedName() + ")}")
                            .collect(Collectors.joining(",",  "List.of(", ")"));

                    return method.withTemplate(
                            JavaTemplate.builder(this::getCursor, template)
                                    .imports("java.util.List")
                                    .build(),
                            method.getCoordinates().replace(),
                            method.getArguments().get(0) instanceof J.Empty ? new Object[]{} : method.getArguments().toArray());
                }
                return super.visitMethodInvocation(method, executionContext);
            }

            private boolean isParentTypeDownCast() {
                J parent = getCursor().dropParentUntil(is -> is instanceof J).getValue();
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
                    int index = 0;
                    for (Expression argument : m.getArguments()) {
                        if (IMMUTABLE_LIST_MATCHER.matches(argument)) {
                            break;
                        }
                        index++;
                    }
                    if (m.getMethodType() != null) {
                        isParentTypeDownCast = isParentTypeMatched(m.getMethodType().getParameterTypes().get(index));
                    }
                } else if (parent instanceof J.NewClass) {
                    J.NewClass c = (J.NewClass) parent;
                    int index = 0;
                    if (c.getConstructorType() != null && c.getArguments() != null) {
                        for (Expression argument : c.getArguments()) {
                            if (IMMUTABLE_LIST_MATCHER.matches(argument)) {
                                break;
                            }
                            index++;
                        }
                        if (c.getConstructorType() != null) {
                            isParentTypeDownCast = isParentTypeMatched(c.getConstructorType().getParameterTypes().get(index));
                        }
                    }
                }
                return isParentTypeDownCast;
            }

            private boolean isParentTypeMatched(@Nullable JavaType type) {
                JavaType.FullyQualified fq = TypeUtils.asFullyQualified(type);
                return TypeUtils.isOfClassType(fq, "java.util.List");
            }
        };
    }
}

