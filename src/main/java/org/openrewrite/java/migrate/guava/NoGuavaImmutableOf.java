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

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.*;

import java.util.Objects;
import java.util.stream.Collectors;

public class NoGuavaImmutableOf extends Recipe {
    private final MethodMatcher IMMUTABLE_LIST_MATCHER = new MethodMatcher("com.google.common.collect.ImmutableList of(..)");
    private final MethodMatcher IMMUTABLE_MAP_MATCHER = new MethodMatcher("com.google.common.collect.ImmutableMap of(..)");
    private final MethodMatcher IMMUTABLE_SET_MATCHER = new MethodMatcher("com.google.common.collect.ImmutableSet of(..)");

    @Override
    public String getDisplayName() {
        return "Use `List.of(..)`, `Map.of(..)`, and `Set.of(..)` in Java 9 or higher";
    }

    @Override
    public String getDescription() {
        return "Replaces `ImmutableList.of(..)`, `ImmutableMap.of(..)`, and `ImmutableSet.of(..)` if the returned type is immediately down-cast.";
    }

    @Override
    protected JavaVisitor<ExecutionContext> getSingleSourceApplicableTest() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext executionContext) {
                doAfterVisit(new UsesJavaVersion<>(9));
                doAfterVisit(new UsesType<>("com.google.common.collect.ImmutableMap"));
                doAfterVisit(new UsesType<>("com.google.common.collect.ImmutableList"));
                doAfterVisit(new UsesType<>("com.google.common.collect.ImmutableSet"));
                return cu;
            }
        };
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                String immutableFqn = "";
                String replacementFqn = "";
                MethodMatcher methodMatcher = null;
                if (IMMUTABLE_LIST_MATCHER.matches(method)) {
                    methodMatcher = IMMUTABLE_LIST_MATCHER;
                    immutableFqn = "com.google.common.collect.ImmutableList";
                    replacementFqn = "java.util.List";
                } else if (IMMUTABLE_MAP_MATCHER.matches(method)) {
                    methodMatcher = IMMUTABLE_MAP_MATCHER;
                    immutableFqn = "com.google.common.collect.ImmutableMap";
                    replacementFqn = "java.util.Map";
                } else if (IMMUTABLE_SET_MATCHER.matches(method)) {
                    methodMatcher = IMMUTABLE_SET_MATCHER;
                    immutableFqn = "com.google.common.collect.ImmutableSet";
                    replacementFqn = "java.util.Set";
                }

                if (methodMatcher != null && methodMatcher.matches(method) && isParentTypeDownCast(methodMatcher, replacementFqn)) {
                    maybeRemoveImport(immutableFqn);
                    maybeAddImport(replacementFqn);

                    JavaType.FullyQualified fq = TypeUtils.asFullyQualified(JavaType.buildType(replacementFqn));
                    if (fq != null) {
                        if (method.getArguments().get(0) instanceof J.Empty) {
                            return method.withTemplate(
                                    JavaTemplate.builder(this::getCursor, fq.getClassName() + ".of()")
                                            .imports(replacementFqn)
                                            .build(),
                                    method.getCoordinates().replace());
                        } else {
                            String template = method.getArguments().stream()
                                    .map(arg -> {
                                        if (arg.getType() instanceof JavaType.Primitive) {
                                            return TypeUtils.asFullyQualified(JavaType.buildType("java.lang." + arg.getType()));
                                        } else {
                                            return TypeUtils.asFullyQualified(arg.getType());
                                        }
                                    })
                                    .filter(Objects::nonNull)
                                    .map(type -> "#{any(" + type.getFullyQualifiedName() + ")}")
                                    .collect(Collectors.joining(",", fq.getClassName() + ".of(", ")"));

                            return method.withTemplate(
                                    JavaTemplate.builder(this::getCursor, template)
                                            .imports(replacementFqn)
                                            .build(),
                                    method.getCoordinates().replace(),
                                    method.getArguments().toArray());
                        }
                    }
                }
                return super.visitMethodInvocation(method, executionContext);
            }

            private boolean isParentTypeDownCast(MethodMatcher methodMatcher, String replacementFqn) {
                J parent = getCursor().dropParentUntil(is -> is instanceof J).getValue();
                boolean isParentTypeDownCast = false;
                if (parent instanceof J.VariableDeclarations.NamedVariable) {
                    isParentTypeDownCast = isParentTypeMatched(((J.VariableDeclarations.NamedVariable) parent).getType(), replacementFqn);
                } else if (parent instanceof J.Assignment) {
                    J.Assignment a = (J.Assignment) parent;
                    if (a.getVariable() instanceof J.Identifier && ((J.Identifier) a.getVariable()).getFieldType() != null) {
                        isParentTypeDownCast = isParentTypeMatched(((J.Identifier) a.getVariable()).getFieldType(), replacementFqn);
                    } else if (a.getVariable() instanceof J.FieldAccess) {
                        isParentTypeDownCast = isParentTypeMatched(a.getVariable().getType(), replacementFqn);
                    }
                } else if (parent instanceof J.Return) {
                    // Does not currently support returns in lambda expressions.
                    J j = getCursor().dropParentUntil(is -> is instanceof J.MethodDeclaration || is instanceof J.CompilationUnit).getValue();
                    if (j instanceof J.MethodDeclaration) {
                        TypeTree returnType = ((J.MethodDeclaration) j).getReturnTypeExpression();
                        if (returnType != null) {
                            isParentTypeDownCast = isParentTypeMatched(returnType.getType(), replacementFqn);
                        }
                    }
                } else if (parent instanceof J.MethodInvocation) {
                    J.MethodInvocation m = (J.MethodInvocation) parent;
                    int index = 0;
                    for (Expression argument : m.getArguments()) {
                        if (methodMatcher.matches(argument)) {
                            break;
                        }
                        index++;
                    }
                    if (m.getType() != null && m.getType().getResolvedSignature() != null) {
                        isParentTypeDownCast = isParentTypeMatched(m.getType().getResolvedSignature().getParamTypes().get(index), replacementFqn);
                    }
                } else if (parent instanceof J.NewClass) {
                    J.NewClass c = (J.NewClass) parent;
                    int index = 0;
                    if (c.getConstructorType() != null && c.getArguments() != null) {
                        for (Expression argument : c.getArguments()) {
                            if (methodMatcher.matches(argument)) {
                                break;
                            }
                            index++;
                        }
                        if (c.getConstructorType().getResolvedSignature() != null) {
                            isParentTypeDownCast = isParentTypeMatched(c.getConstructorType().getResolvedSignature().getParamTypes().get(index), replacementFqn);
                        }
                    }
                }
                return isParentTypeDownCast;
            }

            private boolean isParentTypeMatched(@Nullable JavaType type, String replacementFqn) {
                JavaType.FullyQualified fq = TypeUtils.asFullyQualified(type);
                return TypeUtils.isOfClassType(fq, replacementFqn);
            }
        };
    }
}

