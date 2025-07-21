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
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;
import org.openrewrite.staticanalysis.groovy.GroovyFileChecker;
import org.openrewrite.staticanalysis.kotlin.KotlinFileChecker;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
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
                "This is only applicable for Java 17 and later.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        TreeVisitor<?, ExecutionContext> preconditions = Preconditions.and(
                new UsesJavaVersion<>(17),
                Preconditions.not(new KotlinFileChecker<>()),
                Preconditions.not(new GroovyFileChecker<>())
        );
        return Preconditions.check(preconditions, new JavaVisitor<ExecutionContext>() {
                    @Override
                    public J visitBlock(J.Block block, ExecutionContext ctx) {
                        AtomicReference<J.@Nullable Switch> originalSwitch = new AtomicReference<>();
                        AtomicReference<J.@Nullable Return> inlinedReturn = new AtomicReference<>();

                        int lastIndex = block.getStatements().size() - 1;
                        J.Block b = block.withStatements(ListUtils.map(block.getStatements(), (index, statement) -> {
                            if (statement == originalSwitch.getAndSet(null)) {
                                // We've already converted the switch/assignments to an assignment with a switch expression.
                                return null;
                            }

                            if (index == lastIndex && inlinedReturn.get() != null) {
                                return inlinedReturn.get();
                            }

                            if (index < lastIndex &&
                                    statement instanceof J.VariableDeclarations &&
                                    ((J.VariableDeclarations) statement).getVariables().size() == 1 &&
                                    !canHaveSideEffects(((J.VariableDeclarations) statement).getVariables().get(0).getInitializer()) &&
                                    block.getStatements().get(index + 1) instanceof J.Switch
                            ) {
                                J.VariableDeclarations vd = (J.VariableDeclarations) statement;
                                J.Switch nextStatementSwitch = (J.Switch) block.getStatements().get(index + 1);

                                J.VariableDeclarations.NamedVariable originalVariable = vd.getVariables().get(0);
                                J.SwitchExpression newSwitchExpression = buildNewSwitchExpression(nextStatementSwitch, originalVariable);

                                if (newSwitchExpression != null) {
                                    originalSwitch.set(nextStatementSwitch);
                                    J.Return lastReturn = canSwitchBeReturnedInline(index, block.getStatements(), originalVariable.getSimpleName());
                                    if (lastReturn != null) {
                                        inlinedReturn.set(
                                                lastReturn
                                                        .withExpression(newSwitchExpression)
                                                        .withPrefix(lastReturn.getPrefix()
                                                                .withComments(ListUtils.concatAll(vd.getComments(), ListUtils.concatAll(nextStatementSwitch.getComments(), lastReturn.getComments())))
                                                                .withWhitespace(vd.getPrefix().getWhitespace())
                                                        )
                                        );
                                        return null; // We're inlining on return, remove the original variable declaration.
                                    } else {
                                        return vd
                                                .withVariables(singletonList(originalVariable.withInitializer(newSwitchExpression)))
                                                .withComments(ListUtils.concatAll(vd.getComments(), nextStatementSwitch.getComments()));
                                    }
                                }
                            }
                            return statement;
                        }));
                        return super.visitBlock(b, ctx);
                    }

                    private J.@Nullable Return canSwitchBeReturnedInline(int currentStatementIndex, List<Statement> blockStatements, String originalVariableName) {
                        if (currentStatementIndex + 3 == blockStatements.size()) {
                            Statement lastStatement = blockStatements.get(currentStatementIndex + 2);
                            if (lastStatement instanceof J.Return) {
                                J.Return lastReturn = (J.Return) lastStatement;
                                if (lastReturn.getExpression() instanceof J.Identifier) {
                                    J.Identifier identifier = (J.Identifier) lastReturn.getExpression();
                                    if (identifier.getSimpleName().equals(originalVariableName)) {
                                        return lastReturn;
                                    }
                                }
                            }
                        }
                        return null;
                    }

                    private J.@Nullable SwitchExpression buildNewSwitchExpression(J.Switch originalSwitch, J.VariableDeclarations.NamedVariable originalVariable) {
                        final String variableName = originalVariable.getSimpleName();
                        AtomicBoolean isQualified = new AtomicBoolean(true);
                        AtomicBoolean isDefaultCaseAbsent = new AtomicBoolean(true);
                        AtomicBoolean isUsingArrows = new AtomicBoolean(true);
                        AtomicBoolean isLastCaseEmpty = new AtomicBoolean(false);

                        List<Statement> updatedCases = ListUtils.map(originalSwitch.getCases().getStatements(), (index, s) -> {
                            if (!isQualified.get()) {
                                return null;
                            }

                            J.Case caseItem = (J.Case) s;

                            if (caseItem.getCaseLabels().get(0) instanceof J.Identifier &&
                                    ((J.Identifier) caseItem.getCaseLabels().get(0)).getSimpleName().equals("default")) {
                                isDefaultCaseAbsent.set(false);
                            }

                            if (caseItem.getBody() != null) { // arrow cases
                                if (caseItem.getBody() instanceof J.Block) {
                                    J.Block block = (J.Block) caseItem.getBody();
                                    if (block.getStatements().size() == 1 && block.getStatements().get(0) instanceof J.Assignment) {
                                        J.Assignment assignment = (J.Assignment) block.getStatements().get(0);
                                        if (assignment.getVariable() instanceof J.Identifier) {
                                            J.Identifier variable = (J.Identifier) assignment.getVariable();
                                            if (variable.getSimpleName().equals(variableName) && !containsIdentifier(variableName, assignment.getAssignment())) {
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
                                    if (index + 1 == originalSwitch.getCases().getStatements().size()) {
                                        isLastCaseEmpty.set(true);
                                    }
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
                                        if (variable.getSimpleName().equals(variableName) && !containsIdentifier(variableName, assignment.getAssignment())) {
                                            J.Yield yieldStatement = new J.Yield(
                                                    randomId(),
                                                    assignment.getPrefix().withWhitespace(" "),
                                                    Markers.EMPTY,
                                                    false,
                                                    assignment.getAssignment()
                                            );
                                            return caseItem.withStatements(singletonList(yieldStatement));
                                        }
                                    }
                                }
                            }

                            isQualified.set(false);
                            return null;
                        });

                        boolean shouldAddDefaultCase = isDefaultCaseAbsent.get() && !SwitchUtils.coversAllPossibleValues(originalSwitch);
                        Expression originalInitializer = originalVariable.getInitializer();

                        if (!isQualified.get() ||
                                (originalInitializer == null && shouldAddDefaultCase) ||
                                (isLastCaseEmpty.get() && !shouldAddDefaultCase)) {
                            return null;
                        }

                        if (shouldAddDefaultCase) {
                            updatedCases.add(
                                    createDefaultCase(isUsingArrows.get(), originalInitializer.withPrefix(Space.SINGLE_SPACE), originalSwitch)
                            );
                        }

                        return
                                new J.SwitchExpression(
                                        randomId(),
                                        Space.SINGLE_SPACE,
                                        Markers.EMPTY,
                                        originalSwitch.getSelector(),
                                        originalSwitch.getCases().withStatements(updatedCases),
                                        originalVariable.getType()
                                );
                    }

                    private J.Case createDefaultCase(boolean arrow, Expression returnedExpression, J.Switch originalSwitch) {
                        String template = "switch(1) {\n" + "default" + (arrow ? " ->" : ": yield") + " #{any()};\n}";
                        J.Switch switchStatement = JavaTemplate.apply(
                                template,
                                new Cursor(getCursor(), originalSwitch),
                                originalSwitch.getCoordinates().replace(),
                                returnedExpression
                        );
                        return (J.Case) switchStatement.getCases().getStatements().get(0);
                    }

                    private boolean containsIdentifier(String identifierName, Expression expression) {
                        return new JavaIsoVisitor<AtomicBoolean>() {
                            @Override
                            public J.Identifier visitIdentifier(J.Identifier id, AtomicBoolean found) {
                                if (id.getSimpleName().equals(identifierName)) {
                                    found.set(true);
                                    return id;
                                }
                                return super.visitIdentifier(id, found);
                            }
                        }.reduce(expression, new AtomicBoolean()).get();
                    }

                    // Is any code elsewhere executed due to the provided expression
                    private boolean canHaveSideEffects(@Nullable Expression expression) {
                        if (expression == null) {
                            return false;
                        }

                        return new JavaIsoVisitor<AtomicBoolean>() {
                            @Override
                            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, AtomicBoolean found) {
                                found.set(true);
                                return method;
                            }

                            @Override
                            public J.NewClass visitNewClass(J.NewClass newClass, AtomicBoolean found) {
                                found.set(true);
                                return newClass;
                            }

                            private boolean isToStringImplicitelyCalled(Expression a, Expression b) {
                                // Assuming an implicit `.toString()` call could have a side effect, but excluding
                                // the java.lang.* classes from that rule.
                                if (TypeUtils.isAssignableTo("java.lang.String", a.getType()) &&
                                        TypeUtils.isAssignableTo("java.lang.String", b.getType())) {
                                    return false;
                                }

                                return a.getType() == JavaType.Primitive.String &&
                                        (!(b.getType() instanceof JavaType.Primitive || requireNonNull(b.getType()).toString().startsWith("java.lang")) &&
                                                !TypeUtils.isAssignableTo("java.lang.String", b.getType()));
                            }

                            @Override
                            public J.Binary visitBinary(J.Binary binary, AtomicBoolean found) {
                                if (isToStringImplicitelyCalled(binary.getLeft(), binary.getRight()) ||
                                        isToStringImplicitelyCalled(binary.getRight(), binary.getLeft())) {
                                    found.set(true);
                                    return binary;
                                }
                                return super.visitBinary(binary, found);
                            }
                        }.reduce(expression, new AtomicBoolean()).get();
                    }
                }
        );
    }
}
