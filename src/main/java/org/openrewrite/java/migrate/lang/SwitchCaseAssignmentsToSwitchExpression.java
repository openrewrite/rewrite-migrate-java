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
import org.openrewrite.java.search.SemanticallyEqual;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;
import org.openrewrite.staticanalysis.InlineVariable;
import org.openrewrite.staticanalysis.groovy.GroovyFileChecker;
import org.openrewrite.staticanalysis.kotlin.KotlinFileChecker;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Collections.singletonList;
import static org.openrewrite.Tree.randomId;

@Value
@EqualsAndHashCode(callSuper = false)
public class SwitchCaseAssignmentsToSwitchExpression extends Recipe {
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
        return Preconditions.check(preconditions, new JavaIsoVisitor<ExecutionContext>() {
            boolean supportsMultiCaseLabelsWithDefaultCase = false;

            @Override
            public J.CompilationUnit visitCompilationUnit(J.CompilationUnit cu, ExecutionContext ctx) {
                supportsMultiCaseLabelsWithDefaultCase = SwitchUtils.supportsMultiCaseLabelsWithDefaultCase(cu);
                return super.visitCompilationUnit(cu, ctx);
            }

            @Override
            public J.Block visitBlock(J.Block originalBlock, ExecutionContext ctx) {
                J.Block block = super.visitBlock(originalBlock, ctx);

                AtomicReference<J.@Nullable Switch> originalSwitch = new AtomicReference<>();

                int lastIndex = block.getStatements().size() - 1;
                return block.withStatements(ListUtils.map(block.getStatements(), (index, statement) -> {
                    if (statement == originalSwitch.getAndSet(null)) {
                        doAfterVisit(new InlineVariable().getVisitor());
                        doAfterVisit(new SwitchExpressionYieldToArrow().getVisitor());
                        // We've already converted the switch/assignments to an assignment with a switch expression.
                        return null;
                    }

                    if (index < lastIndex &&
                            statement instanceof J.VariableDeclarations &&
                            ((J.VariableDeclarations) statement).getVariables().size() == 1 &&
                            !canHaveSideEffects(((J.VariableDeclarations) statement).getVariables().get(0).getInitializer()) &&
                            block.getStatements().get(index + 1) instanceof J.Switch) {
                        J.VariableDeclarations vd = (J.VariableDeclarations) statement;
                        J.Switch nextStatementSwitch = (J.Switch) block.getStatements().get(index + 1);

                        if (supportsMultiCaseLabelsWithDefaultCase || !SwitchUtils.hasMultiCaseLabelsWithDefault(nextStatementSwitch.getCases().getStatements())) {
                            J.VariableDeclarations.NamedVariable originalVariable = vd.getVariables().get(0);
                            J.SwitchExpression newSwitchExpression = buildNewSwitchExpression(nextStatementSwitch, originalVariable);
                            if (newSwitchExpression != null) {
                                originalSwitch.set(nextStatementSwitch);
                                return vd
                                        .withVariables(singletonList(originalVariable.getPadding().withInitializer(
                                                JLeftPadded.<Expression>build(newSwitchExpression).withBefore(Space.SINGLE_SPACE))))
                                        .withComments(ListUtils.concatAll(vd.getComments(), nextStatementSwitch.getComments()));
                            }
                        }
                    }
                    return statement;
                }));
            }

            private J.@Nullable SwitchExpression buildNewSwitchExpression(J.Switch originalSwitch, J.VariableDeclarations.NamedVariable originalVariable) {
                        J.Identifier originalVariableId = originalVariable.getName();
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
                                    "default".equals(((J.Identifier) caseItem.getCaseLabels().get(0)).getSimpleName())) {
                                isDefaultCaseAbsent.set(false);
                            }

                            if (caseItem.getBody() != null) { // arrow cases
                                J caseBody = caseItem.getBody();
                                if (caseBody instanceof J.Block && ((J.Block) caseBody).getStatements().size() == 1) {
                                    caseBody = ((J.Block) caseBody).getStatements().get(0);
                                }
                                J.Assignment assignment = extractAssignmentOfVariable(caseBody, originalVariableId);
                                if (assignment != null) {
                                    return caseItem.withBody(assignment.getAssignment());
                                }
                            } else {  // colon cases
                                isUsingArrows.set(false);
                                boolean isLastCase = index + 1 == originalSwitch.getCases().getStatements().size();

                                List<Statement> caseStatements = caseItem.getStatements();
                                if (caseStatements.isEmpty()) {
                                    if (isLastCase) {
                                        isLastCaseEmpty.set(true);
                                    }
                                    return caseItem;
                                }

                                J.Assignment assignment = extractAssignmentFromColonCase(caseStatements, isLastCase, originalVariableId);
                                if (assignment != null) {
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

                            isQualified.set(false);
                            return null;
                        });
                        if (!isQualified.get()) {
                            return null;
                        }

                        boolean shouldAddDefaultCase = isDefaultCaseAbsent.get() && !SwitchUtils.coversAllPossibleValues(originalSwitch);
                        Expression originalInitializer = originalVariable.getInitializer();
                        if ((originalInitializer == null && shouldAddDefaultCase) ||
                                (isLastCaseEmpty.get() && !shouldAddDefaultCase)) {
                            return null;
                        }

                        if (shouldAddDefaultCase) {
                            updatedCases.add(createDefaultCase(originalSwitch, originalInitializer.withPrefix(Space.SINGLE_SPACE), isUsingArrows.get()));
                        }

                        return new J.SwitchExpression(
                                randomId(),
                                Space.SINGLE_SPACE,
                                Markers.EMPTY,
                                originalSwitch.getSelector(),
                                originalSwitch.getCases().withStatements(updatedCases),
                                originalVariable.getType());
                    }

                    private J.@Nullable Assignment extractAssignmentFromColonCase(List<Statement> caseStatements, boolean isLastCase, J.Identifier variableId) {
                        if (caseStatements.size() == 1 && caseStatements.get(0) instanceof J.Block) {
                            caseStatements = ((J.Block) caseStatements.get(0)).getStatements();
                        }
                        if ((caseStatements.size() == 2 && caseStatements.get(1) instanceof J.Break) || (caseStatements.size() == 1 && isLastCase)) {
                            return extractAssignmentOfVariable(caseStatements.get(0), variableId);
                        }
                        return null;
                    }

                    private J.@Nullable Assignment extractAssignmentOfVariable(J maybeAssignment, org.openrewrite.java.tree.J.Identifier variableId) {
                        if (maybeAssignment instanceof J.Assignment) {
                            J.Assignment assignment = (J.Assignment) maybeAssignment;
                            if (assignment.getVariable() instanceof J.Identifier) {
                                J.Identifier variable = (J.Identifier) assignment.getVariable();
                                if (SemanticallyEqual.areEqual(variable, variableId) &&
                                        !containsIdentifier(variableId, assignment.getAssignment())) {
                                    return assignment;
                                }
                            }
                        }
                        return null;
                    }

                    private J.Case createDefaultCase(J.Switch originalSwitch, Expression returnedExpression, boolean arrow) {
                        J.Switch switchStatement = JavaTemplate.apply(
                                "switch(1) { default" + (arrow ? " ->" : ": yield") + " #{any()}; }",
                                new Cursor(getCursor(), originalSwitch),
                                originalSwitch.getCoordinates().replace(),
                                returnedExpression
                        );
                        return (J.Case) switchStatement.getCases().getStatements().get(0);
                    }

                    private boolean containsIdentifier(J.Identifier identifier, Expression expression) {
                        return new JavaIsoVisitor<AtomicBoolean>() {
                            @Override
                            public J.Identifier visitIdentifier(J.Identifier id, AtomicBoolean found) {
                                if (SemanticallyEqual.areEqual(id, identifier)) {
                                    found.set(true);
                                    return id;
                                }
                                return super.visitIdentifier(id, found);
                            }
                        }.reduce(expression, new AtomicBoolean()).get();
                    }

                    // Might the initializer affect the input or output of the switch expression?
                    private boolean canHaveSideEffects(@Nullable Expression expression) {
                        if (expression == null) {
                            return false;
                        }

                        return new JavaIsoVisitor<AtomicBoolean>() {
                            @Override
                            public J.Assignment visitAssignment(J.Assignment assignment, AtomicBoolean found) {
                                found.set(true);
                                return super.visitAssignment(assignment, found);
                            }

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

                            @Override
                            public J.Unary visitUnary(J.Unary unary, AtomicBoolean found) {
                                found.set(true);
                                return super.visitUnary(unary, found);
                            }

                            private boolean isToStringImplicitlyCalled(Expression a, Expression b) {
                                // Assuming an implicit `.toString()` call could have a side effect, but excluding
                                // the java.lang.* classes from that rule.
                                if (TypeUtils.isAssignableTo("java.lang.String", a.getType()) &&
                                        TypeUtils.isAssignableTo("java.lang.String", b.getType())) {
                                    return false;
                                }

                                if (b.getType() == null) {
                                    return true; // Don't make any changes if the type is unknown.
                                }

                                return a.getType() == JavaType.Primitive.String &&
                                        (!(b.getType() instanceof JavaType.Primitive || b.getType().toString().startsWith("java.lang")) &&
                                                !TypeUtils.isAssignableTo("java.lang.String", b.getType()));
                            }

                            @Override
                            public J.Binary visitBinary(J.Binary binary, AtomicBoolean found) {
                                if (isToStringImplicitlyCalled(binary.getLeft(), binary.getRight()) ||
                                        isToStringImplicitlyCalled(binary.getRight(), binary.getLeft())) {
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
