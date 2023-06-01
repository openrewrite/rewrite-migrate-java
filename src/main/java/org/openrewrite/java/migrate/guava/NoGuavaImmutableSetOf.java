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

import org.openrewrite.Preconditions;
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
import java.util.*;
import java.util.stream.Collectors;

public class NoGuavaImmutableSetOf extends Recipe {
    private static final MethodMatcher IMMUTABLE_SET_MATCHER = new MethodMatcher("com.google.common.collect.ImmutableSet of(..)");

    @Override
    public String getDisplayName() {
        //language=markdown
        return "Prefer `Set.of(..)` in Java 9 or higher";
    }

    @Override
    public String getDescription() {
        //language=markdown
        return "Replaces `ImmutableSet.of(..)` if the returned type is immediately down-cast.\n" +
               "  Java 9 introduced `List#of(..)`, `Map#of(..)`, `Set#of(..)` which is similar to `ImmutableList#of(..)`, " +
               "`ImmutableMap#of(..)`, `ImmutableSet#of(..)`, but has a subtle difference.\n" +
               "  As per the Java 9 documentation, [`Set.of` provides an unspecified iteration order on the set of " +
               "elements and is subject to change](https://docs.oracle.com/javase/9/docs/api/java/util/Set.html), whereas " +
               "[Guava `ImmutableSet` preserves the order from construction time](https://github.com/google/guava/wiki/ImmutableCollectionsExplained#how).\n" +
               "  This is worth pointing out in case your usage calls for iteration order being important.";
    }

    @Override
    public Set<String> getTags() {
        return new HashSet<>(Arrays.asList("RSPEC-4738", "guava"));
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(10);
    }

    // Code is shared between `NoGuavaImmutableMapOf`, `NoGuavaImmutableListOf`, and `NoGuavaImmutableSetOf`.
    // Updates to either may apply to each of the recipes.
    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(Preconditions.and(new UsesJavaVersion<>(9),
                new UsesType<>("com.google.common.collect.ImmutableSet", false)), new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                if (IMMUTABLE_SET_MATCHER.matches(method) && isParentTypeDownCast()) {
                    maybeRemoveImport("com.google.common.collect.ImmutableSet");
                    maybeAddImport("java.util.Set");

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
                            .collect(Collectors.joining(",", "Set.of(", ")"));

                    return JavaTemplate.builder(template)
                            .contextSensitive()
                            .imports("java.util.Set")
                            .build()
                            .apply(getCursor(),
                                    method.getCoordinates().replace(),
                                    method.getArguments().get(0) instanceof J.Empty ? new Object[]{} : method.getArguments().toArray());
                }
                return super.visitMethodInvocation(method, ctx);
            }

            private boolean isParentTypeDownCast() {
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
                    int index = 0;
                    for (Expression argument : m.getArguments()) {
                        if (IMMUTABLE_SET_MATCHER.matches(argument)) {
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
                            if (IMMUTABLE_SET_MATCHER.matches(argument)) {
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
                return TypeUtils.isOfClassType(fq, "java.util.Set") || TypeUtils.isOfType(fq, JavaType.ShallowClass.build("java.lang.Object"));
            }
        });
    }
}

