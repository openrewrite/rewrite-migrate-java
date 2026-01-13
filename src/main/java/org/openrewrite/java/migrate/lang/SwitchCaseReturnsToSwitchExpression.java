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
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;
import org.openrewrite.staticanalysis.groovy.GroovyFileChecker;
import org.openrewrite.staticanalysis.kotlin.KotlinFileChecker;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.openrewrite.Tree.randomId;

@Value
@EqualsAndHashCode(callSuper = false)
public class SwitchCaseReturnsToSwitchExpression extends Recipe {
    String displayName = "Convert switch cases where every case returns into a returned switch expression";

    String description = "Switch statements where each case returns a value can be converted to a switch expression that returns the value directly. " +
               "This recipe is only applicable for Java 21 and later.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        TreeVisitor<?, ExecutionContext> preconditions = Preconditions.and(
                new UsesJavaVersion<>(21),
                Preconditions.not(new KotlinFileChecker<>()),
                Preconditions.not(new GroovyFileChecker<>())
        );
        return Preconditions.check(preconditions, new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.Block visitBlock(J.Block block, ExecutionContext ctx) {
                J.Block b = super.visitBlock(block, ctx);
                AtomicReference<Boolean> newReturn = new AtomicReference<>(false);
                return b.withStatements(ListUtils.map(b.getStatements(), statement -> {
                    if (newReturn.get()) {
                        return null; // Drop statements after the first converted switch expression
                    }
                    if (statement instanceof J.Switch) {
                        J.Switch sw = (J.Switch) statement;
                        if (canConvertToSwitchExpression(sw)) {
                            newReturn.set(true);
                            J.SwitchExpression switchExpression = convertToSwitchExpression(sw);
                            return new J.Return(randomId(), sw.getPrefix(), Markers.EMPTY, switchExpression);
                        }
                    }
                    return statement;
                }));
            }

            private boolean canConvertToSwitchExpression(J.Switch switchStatement) {
                for (Statement statement : switchStatement.getCases().getStatements()) {
                    if (!(statement instanceof J.Case)) {
                        return false;
                    }

                    J.Case caseStatement = (J.Case) statement;
                    if (caseStatement.getBody() != null) {
                        // Arrow case
                        J body = caseStatement.getBody();
                        if (body instanceof J.Block) {
                            if (!isReturnCase(((J.Block) body).getStatements())) {
                                return false;
                            }
                        } else if (!(body instanceof J.Return)) {
                            return false;
                        }
                    } else {
                        // Colon case
                        if (!isReturnCase(caseStatement.getStatements())) {
                            return false;
                        }
                    }
                }

                // We need either a default case or the switch to cover all possible values
                return SwitchUtils.coversAllPossibleValues(switchStatement);
            }

            private boolean isReturnCase(List<Statement> statements) {
                if (statements.size() != 1) {
                    return false;
                }
                // Handle block containing a single return
                if (statements.get(0) instanceof J.Block) {
                    return isReturnCase(((J.Block) statements.get(0)).getStatements());
                }
                // Direct return statement
                return statements.get(0) instanceof J.Return;
            }

            private J.SwitchExpression convertToSwitchExpression(J.Switch switchStatement) {
                JavaType returnType = extractReturnType(switchStatement);

                List<Statement> convertedCases = ListUtils.map(switchStatement.getCases().getStatements(), statement -> {
                    J.Case caseStatement = (J.Case) statement;
                    if (caseStatement.getBody() != null) {
                        // Arrow case
                        J body = caseStatement.getBody();
                        if (body instanceof J.Block && ((J.Block) body).getStatements().size() == 1) {
                            body = ((J.Block) body).getStatements().get(0);
                        }
                        if (body instanceof J.Return) {
                            J.Return ret = (J.Return) body;
                            if (ret.getExpression() != null) {
                                return caseStatement.withBody(ret.getExpression());
                            }
                        }
                    } else {
                        // Colon case - convert to arrow case
                        Expression returnExpression = extractReturnExpression(caseStatement.getStatements());
                        if (returnExpression != null) {
                            // When converting from colon to arrow syntax, we need to ensure proper spacing
                            JContainer<J> caseLabels = caseStatement.getPadding().getCaseLabels();
                            JContainer<J> updatedLabels = caseLabels.getPadding().withElements(
                                    ListUtils.mapLast(caseLabels.getPadding().getElements(),
                                            elem -> elem.withAfter(Space.SINGLE_SPACE)));
                            return caseStatement
                                    .withStatements(null)
                                    .withBody(returnExpression.withPrefix(Space.SINGLE_SPACE))
                                    .withType(J.Case.Type.Rule)
                                    .getPadding()
                                    .withCaseLabels(updatedLabels);
                        }
                    }
                    return caseStatement;
                });
                return new J.SwitchExpression(
                        randomId(),
                        Space.SINGLE_SPACE,
                        Markers.EMPTY,
                        switchStatement.getSelector(),
                        switchStatement.getCases().withStatements(convertedCases),
                        returnType
                );
            }

            private @Nullable JavaType extractReturnType(J.Switch switchStatement) {
                for (Statement statement : switchStatement.getCases().getStatements()) {
                    J.Case caseStatement = (J.Case) statement;
                    if (caseStatement.getBody() != null) {
                        J body = caseStatement.getBody();
                        if (body instanceof J.Block && ((J.Block) body).getStatements().size() == 1) {
                            body = ((J.Block) body).getStatements().get(0);
                        }
                        if (body instanceof J.Return) {
                            J.Return ret = (J.Return) body;
                            if (ret.getExpression() != null && ret.getExpression().getType() != null) {
                                return ret.getExpression().getType();
                            }
                        }
                    } else {
                        Expression returnExpression = extractReturnExpression(caseStatement.getStatements());
                        if (returnExpression != null && returnExpression.getType() != null) {
                            return returnExpression.getType();
                        }
                    }
                }
                return null;
            }


            private @Nullable Expression extractReturnExpression(List<Statement> statements) {
                if (statements.size() != 1) {
                    return null;
                }
                // Handle block containing a single return
                if (statements.get(0) instanceof J.Block) {
                    J.Block block = (J.Block) statements.get(0);
                    if (block.getStatements().size() == 1 && block.getStatements().get(0) instanceof J.Return) {
                        return ((J.Return) block.getStatements().get(0)).getExpression();
                    }
                }
                // Direct return statement
                if (statements.get(0) instanceof J.Return) {
                    return ((J.Return) statements.get(0)).getExpression();
                }
                return null;
            }
        });
    }
}
