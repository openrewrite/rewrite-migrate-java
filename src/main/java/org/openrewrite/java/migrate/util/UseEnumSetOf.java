/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.java.migrate.util;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TypeUtils;

import java.time.Duration;
import java.util.List;
import java.util.StringJoiner;

public class UseEnumSetOf extends Recipe {
    private static final MethodMatcher SET_OF = new MethodMatcher("java.util.Set of(..)", true);
    private static final String METHOD_TYPE = "java.util.EnumSet";
    @Override
    public String getDisplayName() {
        return "Prefer `EnumSet of(..)`";
    }

    @Override
    public String getDescription() {
        return "Prefer `EnumSet of(..)` instead of using `Set of(..)` when the arguments are enums in Java 5 or higher.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(2);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(Preconditions.and(new UsesJavaVersion<>(9),
                new UsesMethod<>(SET_OF)), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
                J.MethodInvocation methodInvocation = super.visitMethodInvocation(method, ctx);

                if (SET_OF.matches(method) && method.getType() instanceof JavaType.Parameterized
                    && !TypeUtils.isOfClassType(method.getType(), METHOD_TYPE)) {
                    Cursor parent = getCursor().dropParentUntil(is -> is instanceof J.Assignment || is instanceof J.VariableDeclarations || is instanceof J.Block);
                    if (!(parent.getValue() instanceof J.Block)) {
                        JavaType type = parent.getValue() instanceof J.Assignment ?
                                ((J.Assignment) parent.getValue()).getType() : ((J.VariableDeclarations) parent.getValue()).getVariables().get(0).getType();
                        if (isAssignmentSetOfEnum(type)) {
                            maybeAddImport(METHOD_TYPE);

                            List<Expression> args = methodInvocation.getArguments();
                            if (isArrayParameter(args)) {
                                return methodInvocation;
                            }
                            StringJoiner setOf = initStringJoiner(args);
                            args.forEach(o -> setOf.add("#{any()}"));

                            return createNewMethodInvocation(methodInvocation, (JavaType.Parameterized) type, setOf);
                        }
                    }
                }
                return methodInvocation;
            }

            private StringJoiner initStringJoiner(List<Expression> args) {
                if(args.get(0) instanceof J.Empty) {
                    return new StringJoiner(", ", "EnumSet.noneOf(", ")");
                }
                return new StringJoiner(", ", "EnumSet.of(", ")");
            }

          private J.MethodInvocation createNewMethodInvocation(J.MethodInvocation methodInvocation,
                                                         JavaType.Parameterized type,
                                                         StringJoiner setOf) {
            J.MethodInvocation visitMethodInvocation = JavaTemplate.builder(setOf.toString())
                .contextSensitive()
                .imports(METHOD_TYPE)
                .build()
                .apply(updateCursor(methodInvocation), methodInvocation.getCoordinates().replace(),
                    methodInvocation.getArguments().toArray());

            if (methodInvocation.getArguments().get(0) instanceof J.Empty) {
              JavaType.Method methodType = methodInvocation.getMethodType().withName("noneOf");
              JavaType.Class parameterType = JavaType.ShallowClass.build(
                  type.getTypeParameters().get(0).toString());
              return visitMethodInvocation.withMethodType(methodType)
                  .withArguments(singletonList(new J.Identifier(
                      Tree.randomId(), Space.EMPTY, methodInvocation.getMarkers(), emptyList(),
                      parameterType.getClassName() + ".class", parameterType, null)));
            }
            return visitMethodInvocation;
          }

            private boolean isAssignmentSetOfEnum(@Nullable JavaType type) {
                if (type instanceof JavaType.Parameterized) {
                    JavaType.Parameterized parameterized = (JavaType.Parameterized) type;
                    if (TypeUtils.isOfClassType(parameterized.getType(), "java.util.Set")) {
                        return ((JavaType.Parameterized) type).getTypeParameters().stream()
                                .filter(org.openrewrite.java.tree.JavaType.Class.class::isInstance)
                                .map(org.openrewrite.java.tree.JavaType.Class.class::cast)
                                .anyMatch(o -> o.getKind() == JavaType.FullyQualified.Kind.Enum);
                    }
                }
                return false;
            }

            private boolean isArrayParameter(final List<Expression> args) {
                if (args.size() != 1) {
                    return false;
                }
                JavaType type = args.get(0).getType();
                return TypeUtils.asArray(type) != null;
            }
        });
    }
}
