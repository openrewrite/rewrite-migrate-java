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
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.staticanalysis.kotlin.KotlinFileChecker;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.openrewrite.java.migrate.lang.NullCheck.Matcher.nullCheck;

@Value
@EqualsAndHashCode(callSuper = false)
public class NullCheckAsSwitchCase extends Recipe {
    @Override
    public String getDisplayName() {
        return "Add null check to existing switch cases";
    }

    @Override
    public String getDescription() {
        return "In later java versions, null checks are valid switch cases. " +
                "This recipe will only add null checks to existing switch cases if there are no other statements in between them " +
                "or if the block in the if statement is not impacting the flow of the switch.";
    }

    @Override
    public @Nullable Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(3);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(Preconditions.not(new KotlinFileChecker<>()), new JavaVisitor<ExecutionContext>() {

            @Override
            public J visitBlock(J.Block block, ExecutionContext ctx) {
                List<Statement> statements = block.getStatements();
                AtomicReference<NullCheck> nullCheck = new AtomicReference<>();

                return super.visitBlock(block.withStatements(ListUtils.map(statements, (index, statement) -> {
                    Optional<NullCheck> nullCheckOpt = nullCheck().get(statement, getCursor());
                    if (nullCheckOpt.isPresent()) {
                        NullCheck check = nullCheckOpt.get();
                        J.Identifier nullCheckedVariable = check.getNullCheckedParameter();
                        if (check.returns() || check.assigns(nullCheckedVariable)) {
                            return statement;
                        }
                        J nextStatement = index < block.getStatements().size() - 1 ? block.getStatements().get(index + 1) : null;
                        if (nextStatement == null || !(nextStatement instanceof J.Switch && ((J.Switch) nextStatement).getSelector().getTree() instanceof J.Identifier && ((J.Identifier)((J.Switch) nextStatement).getSelector().getTree()).getSimpleName().equals(nullCheckedVariable.getSimpleName()))) {
                            return statement;
                        }
                        //We only set it if next statement is a J.Switch
                        nullCheck.set(check);
                        return null;
                    }
                    NullCheck check = nullCheck.get();
                    if (check != null && statement instanceof J.Switch) {
                        Statement nullBlock = check.whenNull();
                        String semicolon = "";
                        if (nullBlock instanceof J.Block && ((J.Block) nullBlock).getStatements().size() == 1) {
                            nullBlock = ((J.Block) nullBlock).getStatements().get(0);
                        }
                        if (!(nullBlock instanceof J.Block)) {
                            semicolon = ";";
                        }

                        J.Switch aSwitch = (J.Switch) statement;
                        J.Block cases = aSwitch.getCases();
                        J.Switch switchWithNullCase = JavaTemplate.builder("switch(#{}) {" +
                                        "    case null -> #{}" +
                                        "}")
                                .contextSensitive()
                                .build()
                                .apply(new Cursor(getCursor(), aSwitch), aSwitch.getCoordinates().replace(), check.getNullCheckedParameter().getSimpleName(), nullBlock.withPrefix(Space.EMPTY).print(getCursor()) + semicolon);

                        Statement nullCase = switchWithNullCase.getCases().getStatements().get(0);
                        nullCheck.set(null);
                        return aSwitch.withCases(
                                cases.withStatements(ListUtils.insert(cases.getStatements(), nullCase, 0))
                        );
                    }
                    nullCheck.set(null);
                    return statement;
                })), ctx);
            }
        });
    }
}
