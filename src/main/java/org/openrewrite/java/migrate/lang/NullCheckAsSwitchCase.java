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
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.TreeVisitingPrinter;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.staticanalysis.kotlin.KotlinFileChecker;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.openrewrite.java.migrate.lang.NullCheck.Matcher.nullCheck;

@Value
@EqualsAndHashCode(callSuper = false)
public class NullCheckAsSwitchCase extends Recipe {
    @Override
    public String getDisplayName() {
        return "Use pattern matching in switch cases";
    }

    @Override
    public String getDescription() {
        return "Enhance the Java programming language with pattern matching for switch expressions and statements. " +
                "Extending pattern matching to switch allows an expression to be tested against a number of patterns, each with a specific action, so that complex data-oriented queries can be expressed concisely and safely.";
    }

    @Override
    public @Nullable Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(3);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(Preconditions.not(new KotlinFileChecker<>()), new JavaVisitor<ExecutionContext>() {

            @Override
            public J visitCompilationUnit(J.CompilationUnit cu, ExecutionContext executionContext) {
                System.out.println(TreeVisitingPrinter.printTree(getCursor()));
                return super.visitCompilationUnit(cu, executionContext);
            }

            @Override
            public J visitBlock(J.Block block, ExecutionContext executionContext) {
                List<Statement> newStatements = new ArrayList<>();
                for (int i = 0; i < block.getStatements().size(); i++) {
                    J statement = block.getStatements().get(i);
                    Optional<NullCheck> nullCheckOpt = nullCheck().get(statement, getCursor());
                    if (nullCheckOpt.isPresent()) {
                        NullCheck nullCheck = nullCheckOpt.get();
                        J.Identifier nullCheckedVariable = nullCheck.getNullCheckedParameter();
                        J nextStatement = i < block.getStatements().size() - 1 ? block.getStatements().get(i + 1) : null;
                        // If there is no Return in the if-body, it's not reassigning the switched variable, and it's checking null on the same variable as the switch is switching on.
                        if (!nullCheck.returns()
                                && !nullCheck.assigns(nullCheckedVariable)
                                && nextStatement instanceof J.Switch
                                && ((J.Switch) nextStatement).getSelector().getTree() instanceof J.Identifier && ((J.Identifier)((J.Switch) nextStatement).getSelector().getTree()).getSimpleName().equals(nullCheckedVariable.getSimpleName())) {
                            Statement nullBlock = nullCheck.whenNull();
                            String semicolon = "";
                            if (nullBlock instanceof J.Block && ((J.Block) nullBlock).getStatements().size() == 1) {
                                nullBlock = ((J.Block) nullBlock).getStatements().get(0);
                            }
                            if (!(nullBlock instanceof J.Block)) {
                                semicolon = ";";
                            }

                            J.Switch aSwitch = (J.Switch) nextStatement;
                            J.Block cases = aSwitch.getCases();
                            JavaTemplate switchTemplate = JavaTemplate.builder("case null -> #{}").contextSensitive().build();
                            aSwitch = switchTemplate.apply(new Cursor(getCursor(), aSwitch), cases.getCoordinates().firstStatement(), nullBlock.withPrefix(Space.EMPTY).print(getCursor()) + semicolon);

                            newStatements.add(aSwitch);
                            i++;
                        } else {
                            newStatements.add((Statement) statement);
                        }
                    } else {
                        newStatements.add((Statement) statement);
                    }
                }
                return super.visitBlock(block.withStatements(newStatements), executionContext);
            }
        });
    }
}
