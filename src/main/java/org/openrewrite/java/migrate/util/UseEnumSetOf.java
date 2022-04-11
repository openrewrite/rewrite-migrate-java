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

import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.time.Duration;
import java.util.List;
import java.util.StringJoiner;

public class UseEnumSetOf extends Recipe {
    private static final MethodMatcher SET_OF = new MethodMatcher("java.util.Set of(..)", true);

    @Override
    public String getDisplayName() {
        return "Use `EnumSet of(..)`";
    }

    @Override
    public String getDescription() {
        return "Replaces `Set of(..)` with `EnumSet of(..)` if the arguments are enums.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(2);
    }

    @Override
    protected JavaVisitor<ExecutionContext> getSingleSourceApplicableTest() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext executionContext) {
                doAfterVisit(new UsesJavaVersion<>(9));
                doAfterVisit(new UsesMethod<>(SET_OF));
                return cu;
            }
        };
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {

        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                J.MethodInvocation m = super.visitMethodInvocation(method, executionContext);

                if (SET_OF.matches(method) && method.getType() instanceof JavaType.Parameterized
                        && !TypeUtils.isOfClassType(method.getType(), "java.util.EnumSet")) {
                    Cursor parent = getCursor().dropParentUntil(is -> is instanceof J.Assignment || is instanceof J.VariableDeclarations || is instanceof J.Block);
                    if (!(parent.getValue() instanceof J.Block)) {
                        JavaType type = parent.getValue() instanceof J.Assignment ?
                                ((J.Assignment) parent.getValue()).getType() : ((J.VariableDeclarations) parent.getValue()).getVariables().get(0).getType();
                        if (isAssignmentSetOfEnum(type)) {
                            maybeAddImport("java.util.EnumSet");

                            StringJoiner setOf = new StringJoiner(", ", "EnumSet.of(", ")");
                            List<Expression> args = m.getArguments();
                            args.forEach(o -> setOf.add("#{any()}"));
                            return autoFormat(
                                    m.withTemplate(
                                            JavaTemplate.builder(this::getCursor, "EnumSet.of(#{any()})")
                                                    .imports("java.util.EnumSet").build(),
                                            m.getCoordinates().replace(),
                                            args.toArray()),
                                    executionContext);
                        }
                    }
                }
                return m;
            }

            private boolean isAssignmentSetOfEnum(@Nullable JavaType type) {
                if (type instanceof JavaType.Parameterized) {
                    JavaType.Parameterized parameterized = (JavaType.Parameterized) type;
                    if (TypeUtils.isOfClassType(parameterized.getType(), "java.util.Set")) {
                        return ((JavaType.Parameterized) type).getTypeParameters().stream()
                                .filter(o -> o instanceof JavaType.Class)
                                .map(o -> (JavaType.Class) o)
                                .anyMatch(o -> o.getKind() == JavaType.FullyQualified.Kind.Enum);
                    }
                }
                return false;
            }
        };
    }
}
