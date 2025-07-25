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
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;
import org.openrewrite.staticanalysis.groovy.GroovyFileChecker;
import org.openrewrite.staticanalysis.kotlin.KotlinFileChecker;

import java.util.List;

import static org.openrewrite.Tree.randomId;

@Value
@EqualsAndHashCode(callSuper = false)
public class SwitchExpressionYieldToArrow extends Recipe {
    @Override
    public String getDisplayName() {
        return "Convert switch expression colon case to arrow";
    }

    @Override
    public String getDescription() {
        return "Convert switch expressions with colon cases and yield statements to arrow syntax. " +
                "This is only applicable for Java 14 and later.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        TreeVisitor<?, ExecutionContext> preconditions = Preconditions.and(
                new UsesJavaVersion<>(14),
                Preconditions.not(new KotlinFileChecker<>()),
                Preconditions.not(new GroovyFileChecker<>())
        );
        return Preconditions.check(preconditions, new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.SwitchExpression visitSwitchExpression(J.SwitchExpression switchExpression, ExecutionContext ctx) {
                J.SwitchExpression se = super.visitSwitchExpression(switchExpression, ctx);
                
                // First check if all cases are either arrow cases or simple yield cases
                boolean hasColonCases = false;
                boolean hasArrowCases = false;
                boolean hasComplexCases = false;
                
                for (Statement statement : se.getCases().getStatements()) {
                    if (statement instanceof J.Case) {
                        J.Case caseStatement = (J.Case) statement;
                        if (caseStatement.getType() == J.Case.Type.Rule) {
                            hasArrowCases = true;
                        } else if (caseStatement.getType() == J.Case.Type.Statement && caseStatement.getBody() == null) {
                            hasColonCases = true;
                            List<Statement> statements = caseStatement.getStatements();
                            // Check if this is a complex case (more than just a yield)
                            if (statements.size() != 1 || !(statements.get(0) instanceof J.Yield)) {
                                hasComplexCases = true;
                            }
                        }
                    }
                }
                
                // Don't convert if there are no colon cases, has complex cases, or has a mix of arrow and colon
                if (!hasColonCases || hasComplexCases || (hasArrowCases && hasColonCases)) {
                    return se;
                }
                
                List<Statement> convertedCases = ListUtils.map(se.getCases().getStatements(), statement -> {
                    if (!(statement instanceof J.Case)) {
                        return statement;
                    }
                    
                    J.Case caseStatement = (J.Case) statement;
                    
                    // Only convert colon cases with yield statements
                    if (caseStatement.getType() == J.Case.Type.Statement && caseStatement.getBody() == null) {
                        List<Statement> statements = caseStatement.getStatements();
                        
                        // Check if this case has a single yield statement
                        if (statements.size() == 1 && statements.get(0) instanceof J.Yield) {
                            J.Yield yieldStatement = (J.Yield) statements.get(0);
                            Expression value = yieldStatement.getValue();
                            
                            if (value != null) {
                                // Add space after the last case label to create space before arrow
                                J.Case.Padding padding = caseStatement.getPadding();
                                JContainer<J> caseLabels = padding.getCaseLabels();
                                JContainer<J> updatedLabels = caseLabels.getPadding().withElements(
                                    ListUtils.mapLast(caseLabels.getPadding().getElements(), 
                                        elem -> elem.withAfter(Space.SINGLE_SPACE))
                                );
                                
                                return caseStatement
                                        .withStatements(null)
                                        .withBody(value.withPrefix(Space.SINGLE_SPACE))
                                        .withType(J.Case.Type.Rule)
                                        .getPadding()
                                        .withCaseLabels(updatedLabels);
                            }
                        }
                    }
                    
                    return caseStatement;
                });
                
                return se.withCases(se.getCases().withStatements(convertedCases));
            }
        });
    }
}