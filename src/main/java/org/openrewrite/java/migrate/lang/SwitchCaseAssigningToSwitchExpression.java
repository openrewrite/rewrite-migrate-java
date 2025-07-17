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
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.marker.Markers;
import org.openrewrite.staticanalysis.groovy.GroovyFileChecker;
import org.openrewrite.staticanalysis.kotlin.KotlinFileChecker;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Collections.singletonList;
import static org.openrewrite.Tree.randomId;


@Value
@EqualsAndHashCode(callSuper = false)
public class SwitchCaseAssigningToSwitchExpression extends Recipe {
    @Override
    public String getDisplayName() {
        return "Convert assigning Switch statements to Switch expressions";
    }

    @Override
    public String getDescription() {
        return "Switch statements for which each case is assigning a value to the same variable can be converted to a switch expression that returns the value of the variable. " +
               "This is only applicable for Java 21 and later, as switch expressions were introduced in Java 12, but this recipe requires the `yield` keyword.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        TreeVisitor<?, ExecutionContext> preconditions = Preconditions.and(
                new UsesJavaVersion<>(21),
                Preconditions.not(new KotlinFileChecker<>()), // necessary ?
                Preconditions.not(new GroovyFileChecker<>())  // necessary ?
        );

        return Preconditions.check(preconditions, new JavaVisitor<ExecutionContext>() {
                    @Override
                    public J visitBlock(J.Block block, ExecutionContext ctx) {
                        AtomicReference<J.Switch> originalSwitch = new AtomicReference<>();

                        J.Block b = block.withStatements(ListUtils.map(block.getStatements(), (index, statement) -> {
                            if (statement == originalSwitch.getAndSet(null)) {
                                // We've already converted the switch/assignments to an assignment with a switch expression.
                                return null;
                            }

                            if (index < block.getStatements().size() - 1 &&
                                statement instanceof J.VariableDeclarations &&
                                ((J.VariableDeclarations) statement).getVariables().size() == 1 &&
                                block.getStatements().get(index + 1) instanceof J.Switch
                            ) {
                                J.VariableDeclarations vd = (J.VariableDeclarations) statement;
                                J.Switch nextStatementSwitch = (J.Switch) block.getStatements().get(index + 1);
                                Optional<J.SwitchExpression> newSwitchExpression = buildNewSwitchExpression(nextStatementSwitch, vd.getVariables().get(0), ctx);
                                if (newSwitchExpression.isPresent()) {
                                    originalSwitch.set(nextStatementSwitch);
                                    return vd.withVariables(singletonList(vd.getVariables().get(0).withInitializer(newSwitchExpression.get())));
                                }
                            }
                            return statement;
                        }));
                        return super.visitBlock(b, ctx);
                    }

                    private Optional<J.SwitchExpression> buildNewSwitchExpression(J.Switch originalSwitch, J.VariableDeclarations.NamedVariable originalVariable, ExecutionContext ctx) {
                        final String variableName = originalVariable.getSimpleName();
                        AtomicBoolean isUnqualified = new AtomicBoolean();
                        AtomicBoolean isDefaultCaseAbsent = new AtomicBoolean(true);
                        AtomicBoolean isUsingArrows = new AtomicBoolean(true);

                        List<Statement> updatedCases = ListUtils.map(originalSwitch.getCases().getStatements(), s -> {
                            if (isUnqualified.get()) {
                                return null;
                            }

                            J.Case caseItem = (J.Case) s;

                            if (caseItem != null && caseItem.getCaseLabels().get(0) instanceof J.Identifier && ((J.Identifier) caseItem.getCaseLabels().get(0)).getSimpleName().equals("default")) {
                                isDefaultCaseAbsent.set(false);
                            }

                            if (caseItem == null) {
                                return null;
                            } else if (caseItem.getBody() != null) { // arrow cases
                                if (caseItem.getBody() instanceof J.Block) {
                                    J.Block block = (J.Block) caseItem.getBody();
                                    if (block.getStatements().size() == 1 && block.getStatements().get(0) instanceof J.Assignment) {
                                        J.Assignment assignment = (J.Assignment) block.getStatements().get(0);
                                        if (assignment.getVariable() instanceof J.Identifier) {
                                            J.Identifier variable = (J.Identifier) assignment.getVariable();
                                            if (variable.getSimpleName().equals(variableName)) {
                                                return caseItem.withBody(assignment.getAssignment());
                                            }
                                        }
                                    }

                                } else if (caseItem.getBody() instanceof J.Assignment) {
                                    J.Assignment assignment = (J.Assignment) caseItem.getBody();
                                    if (assignment.getVariable() instanceof J.Identifier) {
                                        J.Identifier variable = (J.Identifier) assignment.getVariable();
                                        if (variable.getSimpleName().equals(variableName)) {
                                            return caseItem.withBody(assignment.getAssignment());
                                        }
                                    }
                                }
                            } else {  // colon cases
                                isUsingArrows.set(false);
                                List<Statement> caseStatements = caseItem.getStatements();
                                if (caseStatements.isEmpty()) {
                                    return caseItem;
                                }

                                if (caseStatements.size() == 1 && caseStatements.get(0) instanceof J.Block) {
                                    caseStatements = ((J.Block) caseStatements.get(0)).getStatements();
                                }

                                if (caseStatements.size() == 2 &&
                                    caseStatements.get(0) instanceof J.Assignment &&
                                    caseStatements.get(1) instanceof J.Break) {
                                    J.Assignment assignment = (J.Assignment) caseStatements.get(0);
                                    if (assignment.getVariable() instanceof J.Identifier) {
                                        J.Identifier variable = (J.Identifier) assignment.getVariable();
                                        if (variable.getSimpleName().equals(variableName)) {
                                            J.Yield yieldStatement = new J.Yield(
                                                    randomId(),
                                                    assignment.getPrefix().withWhitespace(" "), // TODO: must be a better way to adjust the formatting when taken from a J.Block, see test convertColonCasesSimpleAssignationInBlockToSingleYield()
                                                    Markers.EMPTY,
                                                    false,
                                                    assignment.getAssignment()
                                            );
                                            return caseItem.withStatements(singletonList(yieldStatement));
                                        }
                                    }
                                }
                            }

                            isUnqualified.set(true);
                            return null;
                        });
                        if (isUnqualified.get()) {
                            return Optional.empty();
                        }

                        List<J> statements = new ArrayList<>();
                        statements.add(originalSwitch.getSelector().getTree());
                        statements.add(originalVariable.getInitializer().withPrefix(Space.SINGLE_SPACE));

                        StringBuilder template = new StringBuilder(
                                "Object o = switch (#{any()}) {\n" +
                                    "default" + (isUsingArrows.get() ? " ->" : ": yield") + " #{any()};\n" +
                                "}");
                        J.VariableDeclarations vd = JavaTemplate.apply(
                                template.toString(),
                                new Cursor(getCursor(), originalSwitch),
                                originalSwitch.getCoordinates().replace(), //right coordinates? We don't want to replace the switch, we modify it and move it as an assignement one line up.
                                statements.toArray()
                        );

                        J.SwitchExpression initializer = (J.SwitchExpression) Objects.requireNonNull(vd.getVariables().get(0).getInitializer());
                        if (isDefaultCaseAbsent.get()) {
                            updatedCases.add(initializer.getCases().getStatements().get(0));
                        }
                        return Optional.of(initializer.withCases(initializer.getCases().withStatements(updatedCases)));
                    }
                }
        );
    }
}
