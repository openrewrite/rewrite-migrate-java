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
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.staticanalysis.kotlin.KotlinFileChecker;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.openrewrite.java.tree.J.Block.createEmptyBlock;

@Value
@EqualsAndHashCode(callSuper = false)
public class RefineSwitchCases extends Recipe {
    @Override
    public String getDisplayName() {
        return "Use switch cases refinement when possible";
    }

    @Override
    public String getDescription() {
        return "Use guarded switch case labels and guards if all the statements in the switch block do if/else if/else on the guarded label.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(Preconditions.not(new KotlinFileChecker<>()), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.Switch visitSwitch(J.Switch sw, ExecutionContext ctx) {
                J.Switch switch_ = super.visitSwitch(sw, ctx);
                J.Switch mappedSwitch = switch_.withCases(switch_.getCases()
                        .withStatements(ListUtils.flatMap(switch_.getCases().getStatements(), statement -> {
                            if (statement instanceof J.Case) {
                                J.Case case_ = (J.Case) statement;
                                if (!(case_.getBody() instanceof J.Block)) {
                                    return statement;
                                }
                                List<String> labelVars = case_.getCaseLabels().stream()
                                        .filter(J.VariableDeclarations.class::isInstance)
                                        .flatMap(it -> getVariablesCreated((J.VariableDeclarations) it).stream())
                                        .map(J.Identifier::getSimpleName)
                                        .collect(toList());
                                if (labelVars.isEmpty()) {
                                    return statement;
                                }
                                List<Statement> caseStatements = ((J.Block) case_.getBody()).getStatements();
                                if (caseStatements.size() == 1 && caseStatements.get(0) instanceof J.If) {
                                    J.If if_ = (J.If) caseStatements.get(0);
                                    if (getConditionVariables(if_.getIfCondition().getTree()).stream()
                                            .allMatch(conditionVariable -> labelVars.contains(conditionVariable.getSimpleName()))) {
                                        // Replace case with multiple cases
                                        return getCases(case_, if_);
                                    }
                                }
                            }
                            return statement;
                        })));
                return new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.Case visitCase(J.Case case_, ExecutionContext ctx) {
                        if (!(case_.getBody() instanceof J.Block) || !((J.Block) case_.getBody()).getStatements().isEmpty() ||((J.Block) case_.getBody()).getEnd().isEmpty()) {
                            return case_;
                        }
                        return case_.withBody(createEmptyBlock().withPrefix(Space.SINGLE_SPACE));
                    }
                }.visitSwitch(maybeAutoFormat(switch_, mappedSwitch, ctx), ctx);
            }

            private List<J.Identifier> getConditionVariables(@Nullable Expression expression) {
                if (expression instanceof J.Identifier) {
                    return singletonList((J.Identifier) expression);
                } else if (expression instanceof J.Binary) {
                    J.Binary binary = (J.Binary) expression;
                    List<J.Identifier> variables = new ArrayList<>();
                    variables.addAll(getConditionVariables(binary.getLeft()));
                    variables.addAll(getConditionVariables(binary.getRight()));
                    return variables;
                } else if (expression instanceof J.MethodInvocation) {
                    J.MethodInvocation methodInvocation = (J.MethodInvocation) expression;
                    List<J.Identifier> variables = new ArrayList<>();
                    variables.addAll(getConditionVariables(methodInvocation.getSelect()));
                    variables.addAll(getConditionVariables(methodInvocation.getArguments()));
                    return variables;
                }
                return emptyList();
            }

            private List<J.Identifier> getConditionVariables(List<Expression> expressions) {
                return expressions.stream()
                        .map(this::getConditionVariables)
                        .flatMap(List::stream)
                        .collect(toList());
            }

            private List<J.Identifier> getVariablesCreated(J.VariableDeclarations declarations) {
                List<J.Identifier> variables = new ArrayList<>();
                for (J.VariableDeclarations.NamedVariable variable : declarations.getVariables()) {
                    variables.add(variable.getName());
                }
                return variables;
            }

            private List<J.Case> getCases(J.Case case_, J.If if_) {
                if (case_.getBody() == null) {
                    return singletonList(case_);
                }
                List<J.Case> cases = new ArrayList<>();
                Statement caseBody = if_.getThenPart();
                if (caseBody instanceof J.Block && ((J.Block) caseBody).getStatements().size() == 1) {
                    caseBody = ((J.Block) caseBody).getStatements().get(0);
                }
                if (!(caseBody instanceof J.Block)) {
                    caseBody = caseBody.withPrefix(Space.SINGLE_SPACE);
                }
                cases.add(case_.withId(Tree.randomId()).withGuard(if_.getIfCondition().getTree().withPrefix(Space.SINGLE_SPACE)).withBody(caseBody));
                if (if_.getElsePart() == null) {
                    if (case_.getBody() instanceof J.Block) {
                        cases.add(case_.withBody(createEmptyBlock().withPrefix(Space.SINGLE_SPACE)));
                    }
                } else if (if_.getElsePart().getBody() instanceof J.If) {
                    cases.addAll(getCases(case_, (J.If) if_.getElsePart().getBody()));
                } else {
                    cases.add(case_.withBody(if_.getElsePart().getBody().withPrefix(Space.SINGLE_SPACE)));
                }

                return cases;
            }
        });
    }
}
