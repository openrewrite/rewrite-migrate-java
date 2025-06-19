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
import lombok.Getter;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.search.SemanticallyEqual;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.staticanalysis.groovy.GroovyFileChecker;
import org.openrewrite.staticanalysis.kotlin.KotlinFileChecker;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.openrewrite.java.migrate.lang.NullCheck.Matcher.nullCheck;
import static org.openrewrite.java.tree.J.Block.createEmptyBlock;

@Value
@EqualsAndHashCode(callSuper = false)
public class IfElseIfConstructToSwitch extends Recipe {
    @Override
    public String getDisplayName() {
        return "If-else-if-else to switch";
    }

    @Override
    public String getDescription() {
        return "Replace if-else-if-else with switch statements. In order to be replaced with a switch, " +
                "all conditions must be on the same variable and there must be at least three cases.";
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
            public J visitIf(J.If if_, ExecutionContext ctx) {
                SwitchCandidate switchCandidate = new SwitchCandidate(if_, getCursor());

                if (switchCandidate.isPotentialCandidate()) {
                    Object[] arguments = switchCandidate.buildTemplateArguments(getCursor());
                    if (arguments.length == 0) {
                        super.visitIf(if_, ctx);
                    }
                    String switchBody = switchCandidate.buildTemplate();
                    J.Switch switch_ = JavaTemplate.builder(switchBody)
                            .contextSensitive()
                            .build()
                            .apply(getCursor(), if_.getCoordinates().replace(), arguments)
                            .withPrefix(if_.getPrefix());
                    return super.visitSwitch(
                            new JavaIsoVisitor<ExecutionContext>() {
                                @Override
                                public J.Case visitCase(J.Case case_, ExecutionContext ctx) {
                                    if (!(case_.getBody() instanceof J.Block) || !((J.Block) case_.getBody()).getStatements().isEmpty() || ((J.Block) case_.getBody()).getEnd().isEmpty()) {
                                        return case_;
                                    }
                                    return case_.withBody(createEmptyBlock().withPrefix(Space.SINGLE_SPACE));
                                }
                            }.visitSwitch(switch_, ctx), ctx);
                }
                return super.visitIf(if_, ctx);
            }
        });
    }

    private static class SwitchCandidate {
        private final Map<J.InstanceOf, Statement> patternMatchers = new LinkedHashMap<>();
        private @Nullable Expression nullCheckedParameter = null;
        private @Nullable Statement nullCheckedStatement = null;
        private @Nullable Statement else_ = null;

        @Getter
        private boolean potentialCandidate = true;

        private SwitchCandidate(J.If if_, Cursor cursor) {
            J.If ifPart = if_;
            while (potentialCandidate && ifPart != null) {
                if (ifPart.getIfCondition().getTree() instanceof J.Binary) {
                    ifPart = handleNullCheck(ifPart, cursor);
                } else if (ifPart.getIfCondition().getTree() instanceof J.InstanceOf) {
                    ifPart = handleInstanceOfCheck(ifPart);
                } else {
                    potentialCandidate = false;
                }
            }
            validatePotentialCandidate();
        }

        private J.@Nullable If handleNullCheck(J.If ifPart, Cursor cursor) {
            Optional<NullCheck> nullCheck = nullCheck().get(ifPart, cursor);
            if (nullCheck.isPresent()) {
                nullCheckedParameter = nullCheck.get().getNullCheckedParameter();
                nullCheckedStatement = nullCheck.get().whenNull();
                Statement elsePart = nullCheck.get().whenNotNull();
                if (elsePart instanceof J.If) {
                    ifPart = (J.If) elsePart;
                } else {
                    else_ = elsePart;
                    ifPart = null;
                }
            } else {
                potentialCandidate = false;
            }
            return ifPart;
        }

        private J.@Nullable If handleInstanceOfCheck(J.If ifPart) {
            patternMatchers.put((J.InstanceOf) ifPart.getIfCondition().getTree(), ifPart.getThenPart());
            J.If.Else elsePart = ifPart.getElsePart();
            if (elsePart != null && elsePart.getBody() instanceof J.If) {
                ifPart = (J.If) elsePart.getBody();
            } else {
                else_ = elsePart != null ? elsePart.getBody() : null;
                ifPart = null;
            }
            return ifPart;
        }

        private void validatePotentialCandidate() {
            Optional<Expression> switchOn = switchOn();
            // all ifs in the chain must be on the same variable in order to be a candidate for switch pattern matching
            if (!switchOn.isPresent() || potentialCandidate && !patternMatchers.keySet().stream()
                    .map(J.InstanceOf::getExpression)
                    .allMatch(it -> SemanticallyEqual.areEqual(switchOn.get(), it))) {
                potentialCandidate = false;
                return;
            }
            // All InstanceOf checks must have a pattern, otherwise we can't use switch pattern matching (consider calling org.openrewrite.staticanalysis.InstanceOfPatternMatch - or java 17 upgrade - first)
            if (patternMatchers.keySet().stream().anyMatch(instanceOf -> instanceOf.getPattern() == null)) {
                potentialCandidate = false;
                return;
            }
            boolean nullCaseInSwitch = nullCheckedParameter != null && SemanticallyEqual.areEqual(nullCheckedParameter, switchOn.get());
            boolean hasLastElseBlock = else_ != null;

            // we need at least 3 cases to use a switch
            if (potentialCandidate && patternMatchers.keySet().size() + (nullCaseInSwitch ? 1 : 0) + (hasLastElseBlock ? 1 : 0) <= 2) {
                potentialCandidate = false;
            }
        }

        Optional<Expression> switchOn() {
            return patternMatchers.keySet().stream()
                    .map(J.InstanceOf::getExpression)
                    .filter(e -> e instanceof J.FieldAccess || e instanceof J.Identifier)
                    .findAny();
        }

        Object[] buildTemplateArguments(Cursor cursor) {
            Optional<Expression> switchOn = switchOn();
            if (!switchOn.isPresent()) {
                return new Object[0];
            }
            Object[] arguments = new Object[1 + (nullCheckedParameter != null ? 1 : 0) + (patternMatchers.size() * 3) + (else_ != null ? 1 : 0)];
            arguments[0] = switchOn.get();
            int i = 1;
            if (nullCheckedParameter != null) {
                // case null -> nullCheckedStatement
                arguments[i++] = getStatementArgument(nullCheckedStatement, cursor);
            }
            for (Map.Entry<J.InstanceOf, Statement> entry : patternMatchers.entrySet()) {
                J.InstanceOf instanceOf = entry.getKey();
                // case class (pattern) -> statement
                if (instanceOf.getClazz() instanceof J.Identifier) {
                    arguments[i++] = ((J.Identifier) instanceOf.getClazz()).getSimpleName();
                } else if (instanceOf.getClazz() instanceof J.FieldAccess) {
                    arguments[i++] = ((J.FieldAccess) instanceOf.getClazz()).toString();
                }
                arguments[i++] = instanceOf.getPattern() == null ? "" : instanceOf.getPattern().withPrefix(Space.SINGLE_SPACE).print(cursor);
                arguments[i++] = getStatementArgument(entry.getValue(), cursor);
            }
            if (else_ != null) {
                // default -> statement
                arguments[i] = getStatementArgument(else_, cursor);
            }

            return arguments;
        }

        String buildTemplate() {
            StringBuilder switchBody = new StringBuilder("switch (#{any()}) {\n");
            if (nullCheckedParameter != null) {
                switchBody.append("case null -> #{}\n");
            }
            for (int i = 0; i < patternMatchers.size(); i++) {
                switchBody.append("case #{}#{} -> #{}\n");
            }
            if (else_ != null) {
                switchBody.append("default -> #{}\n");
            } else {
                switchBody.append("default -> {}\n");
            }
            switchBody.append("}\n");

            return switchBody.toString();
        }

        private String getStatementArgument(Statement statement, Cursor cursor) {
            Statement toAdd = statement;
            if (statement instanceof J.Block && ((J.Block) statement).getStatements().size() == 1) {
                toAdd = ((J.Block) statement).getStatements().get(0);
            }
            String suffix = toAdd instanceof J.Block ? "" : ";";
            return toAdd.withPrefix(Space.EMPTY).print(cursor) + suffix;
        }
    }
}
