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
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.search.SemanticallyEqual;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.staticanalysis.kotlin.KotlinFileChecker;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.requireNonNull;
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
        return "In later Java 21+, null checks are valid in switch cases. " +
                "This recipe will only add null checks to existing switch cases if there are no other statements in between them " +
                "or if the block in the if statement is not impacting the flow of the switch.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(3);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(Preconditions.not(new KotlinFileChecker<>()), new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitBlock(J.Block block, ExecutionContext ctx) {
                AtomicReference<@Nullable NullCheck> nullCheck = new AtomicReference<>();
                J.Block b = block.withStatements(ListUtils.map(block.getStatements(), (index, statement) -> {
                    Optional<NullCheck> nullCheckOpt = nullCheck().get(statement, getCursor());
                    if (nullCheckOpt.isPresent()) {
                        NullCheck check = nullCheckOpt.get();
                        J nextStatement = index < block.getStatements().size() - 1 ? block.getStatements().get(index + 1) : null;
                        if (!(nextStatement instanceof J.Switch) ||
                                !SemanticallyEqual.areEqual(((J.Switch) nextStatement).getSelector().getTree(), check.getNullCheckedParameter()) ||
                                check.returns() ||
                                check.couldModifyNullCheckedValue()) {
                            return statement;
                        }
                        nullCheck.set(check);
                        return null;
                    }
                    NullCheck check = nullCheck.get();
                    nullCheck.set(null);
                    if (check != null && statement instanceof J.Switch) {
                        Statement nullBlock = check.whenNull();
                        if (nullBlock instanceof J.Block && ((J.Block) nullBlock).getStatements().size() == 1) {
                            nullBlock = ((J.Block) nullBlock).getStatements().get(0);
                        }
                        J.Switch aSwitch = (J.Switch) statement;
                        J.Case nullCase = getNullCase(aSwitch, check.getNullCheckedParameter(), nullBlock);
                        return aSwitch.withCases(aSwitch.getCases().withStatements(
                                ListUtils.insert(aSwitch.getCases().getStatements(), nullCase, 0)));
                    }
                    return statement;
                }));
                return super.visitBlock(b, ctx);
            }

            private J.Case getNullCase(J.Switch aSwitch, Expression expression, Statement nullBlock) {
                String semicolon = nullBlock instanceof J.Block ? "" : ";";
                J.Switch switchWithNullCase = JavaTemplate.apply(
                        "switch(#{any()}) { case null -> #{any()}" + semicolon + " }",
                        new Cursor(getCursor(), aSwitch),
                        aSwitch.getCoordinates().replace(),
                        expression,
                        nullBlock);
                J.Case nullCase = (J.Case) switchWithNullCase.getCases().getStatements().get(0);
                return nullCase.withBody(requireNonNull(nullCase.getBody()).withPrefix(Space.SINGLE_SPACE));
            }
        });
    }
}
