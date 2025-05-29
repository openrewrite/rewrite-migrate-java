/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.java.migrate.lang;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.staticanalysis.kotlin.KotlinFileChecker;

import java.time.Duration;

import static java.util.Collections.singletonList;

@Value
@EqualsAndHashCode(callSuper = false)
public class SwitchCaseEnumGuardToLabel extends Recipe {
    @Override
    public String getDisplayName() {
        return "Use switch cases labels for enums";
    }

    @Override
    public String getDescription() {
        return "Use switch case labels when a guard is checking equality with an enum.";
    }

    @Override
    public @Nullable Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(3);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(Preconditions.not(new KotlinFileChecker<>()), new JavaIsoVisitor<ExecutionContext>() {

            @Override
            public J.Case visitCase(J.Case _case, ExecutionContext ctx) {
                J.Case aCase = super.visitCase(_case, ctx);

                J.VariableDeclarations.NamedVariable label = getCreatedLabelVariable(aCase);
                if (label == null) {
                    return aCase;
                }

                JavaType type = label.getType();
                if (type instanceof JavaType.Class && ((JavaType.Class) type).getKind() == JavaType.FullyQualified.Kind.Enum) {
                    J.FieldAccess guardedEnum = getGuardedEnum(aCase, label);
                    if (guardedEnum != null) {
                        J modifiedBody = enumReferencesToEnumValue(label.getSimpleName(), guardedEnum)
                                .visit(aCase.getBody(), ctx);
                        return aCase.withGuard(null)
                                .withCaseLabels(singletonList(guardedEnum.withPrefix(aCase.getCaseLabels().get(0).getPrefix())))
                                .withBody(modifiedBody);
                    }
                }
                return aCase;
            }

            private J.VariableDeclarations.@Nullable NamedVariable getCreatedLabelVariable(J.Case aCase) {
                if (aCase.getCaseLabels().size() != 1 || !(aCase.getCaseLabels().get(0) instanceof J.VariableDeclarations)) {
                    return null;
                }
                J.VariableDeclarations decl = (J.VariableDeclarations) aCase.getCaseLabels().get(0);
                if (decl.getVariables().size() != 1) {
                    return null;
                }
                return decl.getVariables().get(0);
            }

            private J.@Nullable FieldAccess getGuardedEnum(J.Case aCase, J.VariableDeclarations.NamedVariable label) {
                Expression guard = aCase.getGuard();
                if (guard == null) {
                    return null;
                }
                Expression select = null;
                Expression equalTo = null;
                if (guard instanceof J.MethodInvocation) {
                    J.MethodInvocation methodGuard = (J.MethodInvocation) guard;
                    if ("equals".equals(methodGuard.getSimpleName()) && methodGuard.getArguments().size() == 1) {
                        select = methodGuard.getSelect();
                        equalTo = methodGuard.getArguments().get(0);
                    }
                } else if (guard instanceof J.Binary) {
                    J.Binary binaryGuard = (J.Binary) guard;
                    if (J.Binary.Type.Equal == binaryGuard.getOperator()) {
                        select = binaryGuard.getLeft();
                        equalTo = binaryGuard.getRight();
                    }
                }
                if (select instanceof J.FieldAccess && equalTo instanceof J.Identifier && label.getName().getSimpleName().equals(((J.Identifier) equalTo).getSimpleName())) {
                    return (J.FieldAccess) select;
                } else if (equalTo instanceof J.FieldAccess && select instanceof J.Identifier && label.getName().getSimpleName().equals(((J.Identifier) select).getSimpleName())) {
                    return (J.FieldAccess) equalTo;
                }
                return null;
            }

            private JavaVisitor<ExecutionContext> enumReferencesToEnumValue(String name, J.FieldAccess enumReference) {
                return new JavaVisitor<ExecutionContext>() {
                    @Override
                    public J visitIdentifier(J.Identifier ident, ExecutionContext ctx) {
                        J.Identifier identifier = (J.Identifier) super.visitIdentifier(ident, ctx);
                        if (identifier.getSimpleName().equals(name) && TypeUtils.isOfType(identifier.getType(), enumReference.getType())) {
                            return enumReference.withPrefix(identifier.getPrefix());
                        }
                        return identifier;
                    }
                };
            }
        });
    }
}
