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
package org.openrewrite.java.migrate.guava;

import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;

abstract class AbstractNoGuavaImmutableOf extends Recipe {

    private final String guavaType;
    private final String javaType;

    @Option(displayName = "Whether to convert return type (the default value is false).",
            description = "converting the return type from Guava Type to Java Type " +
                          "The default value is false.",
            example = "true",
            required = false)
    @Nullable
    Boolean convertReturnType;

    AbstractNoGuavaImmutableOf(String guavaType, String javaType) {
        this.guavaType = guavaType;
        this.javaType = javaType;
    }

    AbstractNoGuavaImmutableOf(String guavaType, String javaType, @Nullable Boolean convertReturnType) {
        this.guavaType = guavaType;
        this.javaType = javaType;
        this.convertReturnType = convertReturnType;
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
                J.MethodInvocation mi = (J.MethodInvocation) super.visitMethodInvocation(method, ctx);
                if (!IMMUTABLE_MATCHER.matches(mi) || !isParentTypeDownCast(mi)) {
                    return mi;
                }
                maybeRemoveImport(guavaType);
                maybeAddImport(javaType);

                String template;
                Object[] templateArguments;
                List<Expression> methodArguments = mi.getArguments();
                if (methodArguments.isEmpty() || methodArguments.get(0) instanceof J.Empty) {
                    template = getShortType(javaType) + ".of()";
                    templateArguments = new Object[]{};
                } else if ("com.google.common.collect.ImmutableMap".equals(guavaType)) {
                    template = getShortType(javaType) + ".of(#{any()}, #{any()})";
                    templateArguments = new Object[]{methodArguments.get(0), methodArguments.get(1)};
                } else {
                    template = getShortType(javaType) + ".of(#{any()})";
                    templateArguments = new Object[]{methodArguments.get(0)};
                }

                J.MethodInvocation m = JavaTemplate.builder(template)
                        .imports(javaType)
                        .build()
                        .apply(getCursor(), mi.getCoordinates().replace(), templateArguments);
                m = m.getPadding().withArguments(mi.getPadding().getArguments());
                JavaType.Method newType = (JavaType.Method) visitType(mi.getMethodType(), ctx);
                m = m.withMethodType(newType).withName(m.getName().withType(newType));
                return super.visitMethodInvocation(m, ctx);
            }

            @Override
            public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                J.VariableDeclarations mv = (J.VariableDeclarations) super.visitVariableDeclarations(multiVariable, ctx);
                if (Boolean.TRUE.equals(convertReturnType) &&
                    multiVariable != mv &&
                    TypeUtils.isOfClassType(mv.getType(), guavaType)) {
                    JavaType newType = JavaType.buildType(javaType);
                    mv = mv.withTypeExpression(mv.getTypeExpression() == null ?
                            null : createNewTypeExpression(mv.getTypeExpression(), newType));

                    mv = mv.withVariables(ListUtils.map(mv.getVariables(), variable -> {
                        JavaType.FullyQualified varType = TypeUtils.asFullyQualified(variable.getType());
                        if (varType != null && !varType.equals(newType)) {
                            return variable.withType(newType).withName(variable.getName().withType(newType));
                        }
                        return variable;
                    }));
                }
                return mv;
            }

            private TypeTree createNewTypeExpression(TypeTree typeTree, JavaType newType) {
                if (typeTree instanceof J.ParameterizedType) {
                    J.ParameterizedType parameterizedType = (J.ParameterizedType) typeTree;
                    List<JRightPadded<Expression>> jRightPaddedList = new ArrayList<>();
                    parameterizedType.getTypeParameters().forEach(
                            expression -> {
                                if (expression instanceof J.ParameterizedType && TypeUtils.isOfClassType(expression.getType(), guavaType)) {
                                    jRightPaddedList.add(JRightPadded.build(((J.ParameterizedType) createNewTypeExpression((TypeTree) expression, newType))));
                                } else {
                                    jRightPaddedList.add(JRightPadded.build(expression));
                                }
                            });
                    NameTree clazz = new J.Identifier(
                            Tree.randomId(), Space.EMPTY, Markers.EMPTY, emptyList(), getShortType(javaType), null, null);
                    return parameterizedType.withClazz(clazz).withType(newType).getPadding().withTypeParameters(JContainer.build(jRightPaddedList));
                }
                return new J.Identifier(
                        typeTree.getId(),
                        typeTree.getPrefix(),
                        Markers.EMPTY,
                        emptyList(),
                        getShortType(javaType),
                        newType,
                        null
                );
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
                    if (m.getMethodType() != null) {
                        if (index != -1 && !m.getMethodType().getParameterTypes().isEmpty()) {
                            isParentTypeDownCast = isParentTypeMatched(m.getMethodType().getParameterTypes().get(index));
                        } else {
                            isParentTypeDownCast = !TypeUtils.isOfClassType(m.getMethodType().getReturnType(), guavaType);
                        }
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
                       (Boolean.TRUE.equals(convertReturnType) && TypeUtils.isOfClassType(fq, guavaType)) ||
                       TypeUtils.isOfClassType(fq, "java.lang.Object");
            }
        });
    }
}
