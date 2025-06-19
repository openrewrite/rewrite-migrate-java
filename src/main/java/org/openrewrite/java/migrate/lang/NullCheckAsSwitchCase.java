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
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.tree.*;
import org.openrewrite.staticanalysis.groovy.GroovyFileChecker;
import org.openrewrite.staticanalysis.kotlin.KotlinFileChecker;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

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
        TreeVisitor<?, ExecutionContext> preconditions = Preconditions.and(
                new UsesJavaVersion<>(21),
                Preconditions.not(new KotlinFileChecker<>()),
                Preconditions.not(new GroovyFileChecker<>())
        );

        return Preconditions.check(preconditions, new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitBlock(J.Block block, ExecutionContext ctx) {
                AtomicReference<@Nullable NullCheck> nullCheck = new AtomicReference<>();
                J.Block b = block.withStatements(ListUtils.map(block.getStatements(), (index, statement) -> {
                    // Maybe remove a null check preceding a switch statement
                    Optional<NullCheck> nullCheckOpt = nullCheck().get(statement, getCursor());
                    if (nullCheckOpt.isPresent()) {
                        NullCheck check = nullCheckOpt.get();
                        J nextStatement = index + 1 < block.getStatements().size() ? block.getStatements().get(index + 1) : null;
                        if (!(nextStatement instanceof J.Switch) || check.returns() || check.couldModifyNullCheckedValue()) {
                            return statement;
                        }
                        J.Switch nextSwitch = (J.Switch) nextStatement;
                        // Only if the switch does not have a null case and switches on the same value as the null check, we can remove the null check
                        // It must have all possible input values covered
                        if (hasNullCase(nextSwitch) ||
                                !SemanticallyEqual.areEqual(nextSwitch.getSelector().getTree(), check.getNullCheckedParameter()) ||
                                !coversAllPossibleValues(nextSwitch)) {
                            return statement;
                        }

                        nullCheck.set(check);
                        return null;
                    }

                    // Update the switch following a removed null check
                    NullCheck check = nullCheck.getAndSet(null);
                    if (check != null && statement instanceof J.Switch) {
                        J.Switch aSwitch = (J.Switch) statement;
                        J.Case nullCase = createNullCase(aSwitch, check.whenNull());
                        return aSwitch.withCases(aSwitch.getCases().withStatements(
                                ListUtils.insert(aSwitch.getCases().getStatements(), nullCase, 0)));
                    }
                    return statement;
                }));
                return super.visitBlock(b, ctx);
            }

            private boolean hasNullCase(J.Switch switch_) {
                for (Statement c : switch_.getCases().getStatements()) {
                    if (c instanceof J.Case) {
                        for (J j : ((J.Case) c).getCaseLabels()) {
                            if (j instanceof Expression && J.Literal.isLiteralValue((Expression) j, null)) {
                                return true;
                            }
                        }
                    }
                }
                return false;
            }

            private J.Case createNullCase(J.Switch aSwitch, Statement whenNull) {
                J.Case currentFirstCase = aSwitch.getCases().getStatements().isEmpty() || !(aSwitch.getCases().getStatements().get(0) instanceof J.Case) ? null : (J.Case) aSwitch.getCases().getStatements().get(0);
                if (currentFirstCase == null || J.Case.Type.Rule == currentFirstCase.getType()) {
                    if (whenNull instanceof J.Block && ((J.Block) whenNull).getStatements().size() == 1) {
                        whenNull = ((J.Block) whenNull).getStatements().get(0);
                    }
                    String semicolon = whenNull instanceof J.Block ? "" : ";";
                    J.Switch switchWithNullCase = JavaTemplate.apply(
                            "switch(#{any()}) { case null -> #{any()}" + semicolon + " }",
                            new Cursor(getCursor(), aSwitch),
                            aSwitch.getCoordinates().replace(),
                            aSwitch.getSelector().getTree(),
                            whenNull);
                    J.Case nullCase = (J.Case) switchWithNullCase.getCases().getStatements().get(0);
                    return nullCase.withBody(requireNonNull(nullCase.getBody()).withPrefix(Space.SINGLE_SPACE));
                } else {
                    List<J> statements = new ArrayList<>();
                    statements.add(aSwitch.getSelector().getTree());
                    if (whenNull instanceof J.Block) {
                        statements.addAll(((J.Block) whenNull).getStatements());
                    } else {
                        statements.add(whenNull);
                    }
                    StringBuilder template = new StringBuilder("switch(#{any()}) {\ncase null:");
                    for (int i = 1; i < statements.size(); i++) {
                        template.append("\n#{any()};");
                    }
                    template.append("\nbreak;\n}");
                    J.Switch switchWithNullCase = JavaTemplate.apply(
                            template.toString(),
                            new Cursor(getCursor(), aSwitch),
                            aSwitch.getCoordinates().replace(),
                            statements.toArray());
                    J.Case nullCase = (J.Case) switchWithNullCase.getCases().getStatements().get(0);
                    Space currentFirstCaseIndentation = currentFirstCase.getStatements().stream().map(J::getPrefix).findFirst().orElse(Space.SINGLE_SPACE);

                    return nullCase.withStatements(ListUtils.mapFirst(nullCase.getStatements(), s -> s == null ? null : s.withPrefix(currentFirstCaseIndentation)));
                }
            }

            private boolean coversAllPossibleValues(J.Switch switch_) {
                List<J> labels = switch_.getCases().getStatements().stream().map(J.Case.class::cast).map(J.Case::getCaseLabels).flatMap(Collection::stream).collect(Collectors.toList());
                if (labels.stream().anyMatch(label -> label instanceof J.Identifier && "default".equals(((J.Identifier) label).getSimpleName()))) {
                    return true;
                }
                JavaType javaType = switch_.getSelector().getTree().getType();
                if (javaType instanceof JavaType.Class && ((JavaType.Class) javaType).getKind() == JavaType.FullyQualified.Kind.Enum) {
                    return ((JavaType.Class) javaType).getMembers().stream().allMatch(variable ->
                            labels.stream().anyMatch(label -> {
                                if (!(label instanceof TypeTree)) {
                                    return false;
                                }
                                TypeTree labelJavaType = (TypeTree) label;
                                if (!TypeUtils.isOfType(labelJavaType.getType(), javaType)) {
                                    return false;
                                }
                                J.Identifier enumName = null;
                                if (label instanceof J.Identifier) {
                                    enumName = (J.Identifier) label;
                                } else if (label instanceof J.FieldAccess) {
                                    enumName = ((J.FieldAccess) label).getName();
                                }
                                return enumName != null && variable.getName().equals(enumName.getSimpleName());
                            }));
                }
                return false;
            }
        });
    }
}
