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
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.openrewrite.java.migrate.lang.NullCheck.Matcher.nullCheck;
import static org.openrewrite.java.tree.J.Block.createEmptyBlock;

@EqualsAndHashCode(callSuper = false)
@Value
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
                new UsesJavaVersion<>(21),
                Preconditions.not(new KotlinFileChecker<>()),
                Preconditions.not(new GroovyFileChecker<>())
        );
        return Preconditions.check(preconditions, new JavaVisitor<ExecutionContext>() {

            @Override
            public J visitIf(J.If if_, ExecutionContext ctx) {
                J.Switch switch_ = new SwitchCandidate(if_, getCursor()).buildSwitchTemplate();
                if (switch_ != null) {
                    switch_ = new JavaIsoVisitor<ExecutionContext>() {
                        @Override
                        public J.Case visitCase(J.Case case_, ExecutionContext ctx) {
                            if (case_.getBody() == null) {
                                return case_;
                            }
                            if (case_.getBody() instanceof J.Block &&
                                    ((J.Block) case_.getBody()).getStatements().isEmpty() &&
                                    !((J.Block) case_.getBody()).getEnd().isEmpty()) {
                                return case_.withBody(((J.Block) case_.getBody())
                                        .withPrefix(Space.SINGLE_SPACE)
                                        .withEnd(Space.EMPTY));
                            }
                            return case_.withBody(case_.getBody().withPrefix(Space.SINGLE_SPACE));
                        }
                    }.visitSwitch(switch_, ctx);
                    return super.visitSwitch(switch_, ctx);
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
        private final Cursor cursor;
        private final J.If if_;

        private boolean potentialCandidate = true;

        private SwitchCandidate(J.If if_, Cursor cursor) {
            this.if_ = if_;
            this.cursor = cursor;
            Cursor parent = cursor.getParent(2);
            if (parent == null || parent.getValue() instanceof J.If.Else) {
                potentialCandidate = false;
                return;
            }
            J.If ifPart = if_;
            while (potentialCandidate && ifPart != null) {
                if (ifPart.getIfCondition().getTree() instanceof J.Binary) {
                    ifPart = handleNullCheck(ifPart, cursor);
                } else if (ifPart.getIfCondition().getTree() instanceof J.InstanceOf) {
                    ifPart = handleInstanceOfCheck(ifPart);
                } else {
                    potentialCandidate = false;
                    return;
                }
            }
            potentialCandidate = validatePotentialCandidate();
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

        private boolean validatePotentialCandidate() {
            Optional<Expression> switchOn = switchOn();
            // all ifs in the chain must be on the same variable in order to be a candidate for switch pattern matching
            if (!switchOn.isPresent() || !patternMatchers.keySet().stream()
                    .map(J.InstanceOf::getExpression)
                    .allMatch(it -> SemanticallyEqual.areEqual(switchOn.get(), it))) {
                return false;
            }
            // All InstanceOf checks must have a pattern, otherwise we can't use switch pattern matching
            // (consider calling org.openrewrite.staticanalysis.InstanceOfPatternMatch - or java 17 upgrade - first)
            if (patternMatchers.keySet().stream().anyMatch(instanceOf -> instanceOf.getPattern() == null)) {
                return false;
            }
            // The blocks cannot do a return as that would lead to all blocks having to do a return,
            // the block/expression difference in return for switch statements / expressions being different...
            if (returns(nullCheckedStatement) || patternMatchers.values().stream().anyMatch(this::returns) || returns(else_)) {
                return false;
            }
            // Do no harm -> If we do not know how to replace(yet), do not replace
            if (patternMatchers.keySet().stream().anyMatch(instanceOf -> {
                J clazz = instanceOf.getClazz();
                return !(clazz instanceof J.Identifier || clazz instanceof J.FieldAccess || clazz instanceof J.ArrayType || clazz instanceof J.ParameterizedType);
            })) {
                return false;
            }
            boolean nullCaseInSwitch = nullCheckedParameter != null && SemanticallyEqual.areEqual(nullCheckedParameter, switchOn.get());
            boolean hasLastElseBlock = else_ != null;

            // we need at least 3 cases to use a switch
            return 3 <= patternMatchers.size() +
                    (nullCaseInSwitch ? 1 : 0) +
                    (hasLastElseBlock ? 1 : 0);
        }

        private boolean returns(@Nullable Statement statement) {
            return statement != null && new JavaIsoVisitor<AtomicBoolean>() {
                @Override
                public J.Return visitReturn(J.Return return_, AtomicBoolean atomicBoolean) {
                    atomicBoolean.set(true);
                    return return_;
                }
            }.reduce(statement, new AtomicBoolean(false)).get();
        }

        public J.@Nullable Switch buildSwitchTemplate() {
            Optional<Expression> switchOn = switchOn();
            if (!this.potentialCandidate || !switchOn.isPresent()) {
                return null;
            }
            Object[] arguments = new Object[2 + (nullCheckedParameter != null ? 1 : 0) + (patternMatchers.size() * 3)];
            arguments[0] = switchOn.get();
            StringBuilder switchBody = new StringBuilder("switch (#{any()}) {\n");
            int i = 1;
            if (nullCheckedParameter != null) {
                switchBody.append("case null -> #{any()};\n");
                arguments[i++] = getStatement(Objects.requireNonNull(nullCheckedStatement));
            }
            for (Map.Entry<J.InstanceOf, Statement> entry : patternMatchers.entrySet()) {
                J.InstanceOf instanceOf = entry.getKey();
                switchBody.append("case #{}#{} -> #{any()};\n");
                arguments[i++] = getClassName(instanceOf);
                arguments[i++] = getPattern(instanceOf);
                arguments[i++] = getStatement(entry.getValue());
            }
            switchBody.append("default -> #{any()};\n");
            if (else_ != null) {
                arguments[i] = getStatement(else_);
            } else {
                arguments[i] = createEmptyBlock();
            }
            switchBody.append("}\n");

            return JavaTemplate.apply(switchBody.toString(), cursor, if_.getCoordinates().replace(), arguments).withPrefix(if_.getPrefix());
        }

        private Optional<Expression> switchOn() {
            return patternMatchers.keySet().stream()
                    .map(J.InstanceOf::getExpression)
                    .filter(e -> e instanceof J.FieldAccess || e instanceof J.Identifier)
                    .findAny();
        }

        private String getClassName(J.InstanceOf statement) {
            if (statement.getClazz() instanceof J.Identifier) {
                return ((J.Identifier) statement.getClazz()).getSimpleName();
            }

            return statement.getClazz().toString();
        }

        private String getPattern(J.InstanceOf statement) {
            if (statement.getPattern() instanceof J.Identifier) {
                return " " + ((J.Identifier) statement.getPattern()).getSimpleName();
            }
            return "";
        }

        private Statement getStatement(Statement statement) {
            if (statement instanceof J.Block && ((J.Block) statement).getStatements().size() == 1) {
                Statement firstStatement = ((J.Block) statement).getStatements().get(0);
                if (firstStatement instanceof Expression || firstStatement instanceof J.Throw) {
                    return firstStatement;
                }
            }
            return statement;
        }
    }
}
