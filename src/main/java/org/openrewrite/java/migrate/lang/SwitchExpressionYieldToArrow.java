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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JContainer;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.staticanalysis.groovy.GroovyFileChecker;
import org.openrewrite.staticanalysis.kotlin.KotlinFileChecker;

import static java.util.Objects.requireNonNull;

@Value
@EqualsAndHashCode(callSuper = false)
public class SwitchExpressionYieldToArrow extends Recipe {
    @Override
    public String getDisplayName() {
        return "Convert switch expression yield to arrow";
    }

    @Override
    public String getDescription() {
        return "Convert switch expressions with colon cases and yield statements to arrow syntax. " +
               "This recipe is only applicable for Java 21 and later.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        TreeVisitor<?, ExecutionContext> preconditions = Preconditions.and(
                new UsesJavaVersion<>(21),
                Preconditions.not(new KotlinFileChecker<>()),
                Preconditions.not(new GroovyFileChecker<>())
        );
        return Preconditions.check(preconditions, new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.SwitchExpression visitSwitchExpression(J.SwitchExpression switchExpression, ExecutionContext ctx) {
                J.SwitchExpression se = super.visitSwitchExpression(switchExpression, ctx);

                // Check if this is a complex case (more than just a yield)
                if (anythingOtherThanYield(se)) {
                    return se;
                }

                return se.withCases(se.getCases().withStatements(ListUtils.map(se.getCases().getStatements(), statement -> {
                    J.Case caseStatement = (J.Case) requireNonNull(statement);
                    J.Yield yieldStatement = (J.Yield) caseStatement.getStatements().get(0);

                    // Add space after the last case label to create space before arrow
                    JContainer<J> caseLabels = caseStatement.getPadding().getCaseLabels();
                    JContainer<J> updatedLabels = caseLabels.getPadding().withElements(
                            ListUtils.mapLast(caseLabels.getPadding().getElements(),
                                    elem -> elem.withAfter(Space.SINGLE_SPACE))
                    );

                    return caseStatement
                            .withStatements(null)
                            .withBody(yieldStatement.getValue().withPrefix(Space.SINGLE_SPACE))
                            .withType(J.Case.Type.Rule)
                            .getPadding()
                            .withCaseLabels(updatedLabels);
                })));
            }

            // For now, we only convert switch expressions that consist solely of yield statements
            private boolean anythingOtherThanYield(J.SwitchExpression se) {
                for (Statement statement : se.getCases().getStatements()) {
                    if (!(statement instanceof J.Case)) {
                        return true;
                    }

                    J.Case caseStatement = (J.Case) statement;
                    if (caseStatement.getType() != J.Case.Type.Statement ||
                            caseStatement.getStatements().size() != 1 ||
                            !(caseStatement.getStatements().get(0) instanceof J.Yield)) {
                        return true;
                    }
                }
                return false;
            }
        });
    }
}
