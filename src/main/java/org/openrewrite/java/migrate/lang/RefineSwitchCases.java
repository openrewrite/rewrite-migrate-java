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
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.staticanalysis.kotlin.KotlinFileChecker;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toSet;
import static org.openrewrite.java.tree.J.Block.createEmptyBlock;

@EqualsAndHashCode(callSuper = false)
@Value
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
                                if (!(case_.getBody() instanceof J.Block) || case_.getGuard() != null) {
                                    return statement;
                                }
                                List<Statement> caseStatements = ((J.Block) case_.getBody()).getStatements();
                                if (caseStatements.size() == 1 && caseStatements.get(0) instanceof J.If) {
                                    J.If if_ = (J.If) caseStatements.get(0);
                                    if (extractLabelVariables(case_)
                                            .containsAll(extractConditionVariables(if_.getIfCondition().getTree()))) {
                                        // Replace case with multiple cases
                                        return createGuardedCases(case_, if_);
                                    }
                                }
                            }
                            return statement;
                        })));
                if (mappedSwitch != switch_) {
                    return new JavaIsoVisitor<ExecutionContext>() {
                        @Override
                        public J.Case visitCase(J.Case case_, ExecutionContext ctx) {
                            // Remove any trailing new line in empty case body
                            if (case_.getBody() instanceof J.Block) {
                                J.Block body = (J.Block) case_.getBody();
                                if (body.getStatements().isEmpty() &&
                                        body.getEnd().getComments().isEmpty() &&
                                        !body.getEnd().isEmpty()) {
                                    return case_.withBody(body.withEnd(Space.EMPTY));
                                }
                            }
                            return case_;
                        }
                    }.visitSwitch(autoFormat(mappedSwitch, ctx), ctx);
                }
                return switch_;
            }

            private Set<String> extractLabelVariables(J.Case case_) {
                return case_.getCaseLabels().stream()
                        .filter(J.VariableDeclarations.class::isInstance)
                        .map(J.VariableDeclarations.class::cast)
                        .map(J.VariableDeclarations::getVariables)
                        .flatMap(List::stream)
                        .map(J.VariableDeclarations.NamedVariable::getName)
                        .map(J.Identifier::getSimpleName)
                        .collect(toSet());
            }

            private Set<String> extractConditionVariables(Expression expression) {
                return new JavaIsoVisitor<Set<String>>() {
                    @Override
                    public J.Identifier visitIdentifier(J.Identifier identifier, Set<String> identifiers) {
                        identifiers.add(identifier.getSimpleName());
                        return super.visitIdentifier(identifier, identifiers);
                    }

                    @Override
                    public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, Set<String> identifiers) {
                        // Do not pull out method name as identifier, just any select and arguments
                        super.visit(method.getSelect(), identifiers);
                        method.getArguments().forEach(arg -> super.visit(arg, identifiers));
                        return method;
                    }
                }.reduce(expression, new HashSet<>());
            }

            private List<J.Case> createGuardedCases(J.Case case_, J.If if_) {
                if (case_.getBody() == null) {
                    return singletonList(case_);
                }
                List<J.Case> cases = new ArrayList<>();
                Statement caseBody = if_.getThenPart();
                if (caseBody instanceof J.Block && ((J.Block) caseBody).getStatements().size() == 1) {
                    caseBody = ((J.Block) caseBody).getStatements().get(0);
                }
                cases.add(case_.withId(Tree.randomId()).withGuard(if_.getIfCondition().getTree().withPrefix(Space.SINGLE_SPACE)).withBody(caseBody));
                if (if_.getElsePart() == null) {
                    if (case_.getBody() instanceof J.Block) {
                        cases.add(case_.withBody(createEmptyBlock().withPrefix(Space.SINGLE_SPACE)));
                    }
                } else if (if_.getElsePart().getBody() instanceof J.If) {
                    cases.addAll(createGuardedCases(case_, (J.If) if_.getElsePart().getBody()));
                } else {
                    cases.add(case_.withBody(if_.getElsePart().getBody()));
                }

                return cases;
            }
        });
    }
}
