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
package org.openrewrite.java.migrate.lang;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.staticanalysis.kotlin.KotlinFileChecker;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;

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
    public @Nullable Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(3);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(Preconditions.not(new KotlinFileChecker<>()), new JavaIsoVisitor<ExecutionContext>() {

            @Override
            public J.Switch visitSwitch(J.Switch switch_, ExecutionContext ctx) {
                J.Switch aSwitch = super.visitSwitch(switch_, ctx);
                J.Block cases = aSwitch.getCases();
                List<Statement> statements = cases.getStatements();
                boolean changed = false;

                if (statements.isEmpty()) {
                    return aSwitch;
                }
                // Check if we will need to make a change
                List<Statement> newStatements = new ArrayList<>(statements.size());
                for (Statement statement : statements) {
                    if (statement instanceof J.Case) {
                        J.Case aCase = (J.Case) statement;
                        if (!(aCase.getBody() instanceof J.Block) || ((J.Block) aCase.getBody()).getStatements().isEmpty()) {
                            newStatements.add(statement);
                            continue;
                        }
                        List<String> labelVars = aCase.getCaseLabels().stream()
                                .filter(label -> label instanceof J.VariableDeclarations)
                                .map(label -> (J.VariableDeclarations) label)
                                .map(this::getVariablesCreated)
                                .flatMap(List::stream)
                                .map(J.Identifier::getSimpleName)
                                .collect(Collectors.toList());
                        if (labelVars.isEmpty()) {
                            newStatements.add(statement);
                            continue;
                        }
                        J.Block block = (J.Block) aCase.getBody();
                        List<Statement> caseStatements = block.getStatements();
                        if (caseStatements.size() == 1 && caseStatements.get(0) instanceof J.If) {
                            J.If ifStatement = (J.If) caseStatements.get(0);
                            J.ControlParentheses<Expression> condition = ifStatement.getIfCondition();
                            Expression conditionTree = condition.getTree();
                            List<J.Identifier> conditionVariables = getConditionVariables(conditionTree);
                            if (conditionVariables.stream().anyMatch(conditionVariable -> !labelVars.contains(conditionVariable.getSimpleName()))) {
                                newStatements.add(statement);
                                continue;
                            }
                            newStatements.addAll(getCases(aCase, ifStatement));
                            changed = true;
                        } else {
                            newStatements.add(statement);
                        }
                    }
                }

                if (!changed) {
                    return aSwitch;
                }

                return autoFormat(aSwitch.withCases(cases.withStatements(newStatements)), ctx);
            }

            private List<J.Identifier> getConditionVariables(@Nullable Expression expression) {
                List<J.Identifier> variables = new ArrayList<>();
                if (expression instanceof J.Identifier) {
                    variables.add((J.Identifier) expression);
                } else if (expression instanceof J.Binary) {
                    J.Binary binary = (J.Binary) expression;
                    variables.addAll(getConditionVariables(binary.getLeft()));
                    variables.addAll(getConditionVariables(binary.getRight()));
                } else if (expression instanceof J.MethodInvocation) {
                    J.MethodInvocation methodInvocation = (J.MethodInvocation) expression;
                    variables.addAll(getConditionVariables(methodInvocation.getSelect()));
                    variables.addAll(getConditionVariables(methodInvocation.getArguments()));
                }
                return variables;
            }

            private List<J.Identifier> getConditionVariables(List<Expression> expressions) {
                return expressions.stream()
                        .map(this::getConditionVariables)
                        .flatMap(List::stream)
                        .collect(Collectors.toList());
            }

            private List<J.Identifier> getVariablesCreated(J.VariableDeclarations declarations) {
                List<J.Identifier> variables = new ArrayList<>();
                for (J.VariableDeclarations.NamedVariable variable : declarations.getVariables()) {
                    variables.add(variable.getName());
                }
                return variables;
            }

            private List<J.Case> getCases(J.Case aCase, J.If ifStatement) {
                if (aCase.getBody() == null) {
                    return Collections.singletonList(aCase);
                }
                List<J.Case> cases = new ArrayList<>();
                Statement caseBody = ifStatement.getThenPart();
                if (caseBody instanceof J.Block && ((J.Block) caseBody).getStatements().size() == 1) {
                    caseBody = ((J.Block) caseBody).getStatements().get(0);
                }
                if (!(caseBody instanceof J.Block)) {
                    caseBody = caseBody.withPrefix(Space.SINGLE_SPACE);
                }
                cases.add(aCase.withId(Tree.randomId()).withGuard(ifStatement.getIfCondition().getTree().withPrefix(Space.SINGLE_SPACE)).withBody(caseBody));
                if (ifStatement.getElsePart() == null) {
                    if (aCase.getBody() instanceof J.Block) {
                        cases.add(aCase.withBody(((J.Block) aCase.getBody()).withStatements(emptyList()).withEnd(Space.EMPTY).withPrefix(Space.SINGLE_SPACE)));
                    }
                } else if (ifStatement.getElsePart().getBody() instanceof J.If) {
                    cases.addAll(getCases(aCase, (J.If) ifStatement.getElsePart().getBody()));
                } else {
                    cases.add(aCase.withBody(ifStatement.getElsePart().getBody().withPrefix(Space.SINGLE_SPACE)));
                }

                return cases;
            }
        });
    }
}
