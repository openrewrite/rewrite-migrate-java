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
import lombok.Getter;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.staticanalysis.kotlin.KotlinFileChecker;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.openrewrite.java.migrate.lang.NullCheck.Matcher.nullCheck;

@Value
@EqualsAndHashCode(callSuper = false)
public class IfElseIfConstructToSwitch extends Recipe {
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
            public J visitIf(J.If iff, ExecutionContext ctx) {
                SwitchCandidate switchCandidate = new SwitchCandidate(iff, getCursor());

                if (switchCandidate.isValidCandidate()) {
                    Object[] arguments = switchCandidate.buildTemplateArguments(getCursor());
                    String switchBody = switchCandidate.buildTemplate();
                    JavaTemplate switchTemplate = JavaTemplate.builder(switchBody).javaParser(JavaParser.fromJavaVersion().classpathFromResources(ctx)).contextSensitive().build();
                    return super.visitSwitch(switchTemplate.apply(getCursor(), iff.getCoordinates().replace(), arguments).withPrefix(iff.getPrefix()), ctx);
                } else {
                    return super.visitIf(iff, ctx);
                }
            }
        });
    }

    private static class SwitchCandidate {
        private final Map<J.InstanceOf, Statement> patternMatchers = new LinkedHashMap<>();
        private J.@Nullable Identifier nullCheckedParameter = null;
        private @Nullable Statement nullCheckedStatement = null;
        private @Nullable Statement elze = null;
        @Getter
        private boolean potentialCandidate = true;

        SwitchCandidate(J.If iff, Cursor cursor) {
            J.If ifPart = iff;
            while (potentialCandidate && ifPart != null) {
                if (ifPart.getIfCondition().getTree() instanceof J.Binary) {
                    ifPart = handleNullCheck(ifPart, cursor);
                } else if (ifPart.getIfCondition().getTree() instanceof J.InstanceOf) {
                    ifPart = handleInstanceOfCheck(ifPart);
                } else {
                    noPotentialCandidate();
                }
            }
        }

        J.@Nullable If handleNullCheck(J.If ifPart, Cursor cursor) {
            if (ifPart.getIfCondition().getTree() instanceof J.Binary) {
                Optional<NullCheck> nullCheck = nullCheck().get(ifPart, cursor);
                if (nullCheck.isPresent()) {
                    nullCheckedParameter = nullCheck.get().getNullCheckedParameter();
                    nullCheckedStatement = nullCheck.get().whenNull();
                    Statement elsePart = nullCheck.get().whenNotNull();
                    if (elsePart instanceof J.If) {
                        ifPart = (J.If) elsePart;
                    } else {
                        elze = elsePart;
                        ifPart = null;
                    }
                } else {
                    noPotentialCandidate();
                }
                return ifPart;
            }
            throw new IllegalArgumentException("Unsupported if type: " + ifPart.getIfCondition().getTree().getClass().getSimpleName());
        }

        J.@Nullable If handleInstanceOfCheck(J.If ifPart) {
            if (ifPart.getIfCondition().getTree() instanceof J.InstanceOf) {
                patternMatchers.put((J.InstanceOf) ifPart.getIfCondition().getTree(), ifPart.getThenPart());
                J.If.Else elsePart = ifPart.getElsePart();
                if (elsePart != null && elsePart.getBody() instanceof J.If) {
                    ifPart = (J.If) elsePart.getBody();
                } else {
                    elze = elsePart != null ? elsePart.getBody() : null;
                    ifPart = null;
                }
                return ifPart;
            }
            throw new IllegalArgumentException("Unsupported if type: " + ifPart.getIfCondition().getTree().getClass().getSimpleName());
        }

        void noPotentialCandidate() {
            this.potentialCandidate = false;
        }

        boolean isValidCandidate() {
            // all ifs in the chain must be on the same variable
            if (potentialCandidate && patternMatchers.keySet().stream().map(J.InstanceOf::getExpression).map(expression -> ((J.Identifier) expression).getSimpleName()).distinct().count() != 1) {
                // pattern matching in a switch can only happen if all if cases are on the same variable.
                this.potentialCandidate = false;
                return false;
            }
            boolean nullCaseInSwitch = nullCheckedParameter != null && nullCheckedParameter.getSimpleName().equals(switchOn());
            boolean hasLastElseBlock = elze != null;

            // we need at least 3 cases to use a switch
            if (potentialCandidate && patternMatchers.keySet().size() + (nullCaseInSwitch ? 1 : 0) + (hasLastElseBlock ? 1 : 0) <= 2) {
                this.potentialCandidate = false;
            }
            return potentialCandidate;
        }

        String switchOn() {
            return ((J.Identifier) patternMatchers.keySet().stream().map(J.InstanceOf::getExpression).findAny().get()).getSimpleName();
        }

        Object[] buildTemplateArguments(Cursor cursor) {
            Object[] arguments = new Object[1 + (nullCheckedParameter != null ? 1 : 0) + (patternMatchers.size() * 3) + (elze != null ? 1 : 0)];
            arguments[0] = switchOn();
            int i = 1;
            if (nullCheckedParameter != null) {
                // case null -> nullCheckedStatement
                arguments[i++] = getStatementArgument(nullCheckedStatement, cursor);
            }
            for (Map.Entry<J.InstanceOf, Statement> entry : patternMatchers.entrySet()) {
                J.InstanceOf instanceOf = entry.getKey();
                // case class (pattern) -> statement
                arguments[i++] = ((J.Identifier) instanceOf.getClazz()).getSimpleName();
                arguments[i++] = instanceOf.getPattern() == null ? "" : instanceOf.getPattern().withPrefix(Space.SINGLE_SPACE).print(cursor);
                arguments[i++] = getStatementArgument(entry.getValue(), cursor);
            }
            if (elze != null) {
                // default -> statement
                arguments[i++] = getStatementArgument(elze, cursor);
            }

            return arguments;
        }

        String buildTemplate() {
            StringBuilder switchBody = new StringBuilder("switch (#{}) {\n");
            if (nullCheckedParameter != null) {
                switchBody.append("    case null -> #{}\n");
            }
            for (int i = 0; i < patternMatchers.size(); i++) {
                switchBody.append("    case #{}#{} -> #{}\n");
            }
            if (elze != null) {
                switchBody.append("    default -> #{}\n");
            }
            switchBody.append("}\n");

            return switchBody.toString();
        }

        private String getStatementArgument(Statement statement, Cursor cursor) {
            Statement toAdd = statement;
            String semicolon = "";
            if (statement instanceof J.Block && ((J.Block) statement).getStatements().size() == 1) {
                toAdd = ((J.Block) statement).getStatements().get(0);
            }
            if (!(toAdd instanceof J.Block)) {
                semicolon = ";";
            }
            return toAdd.withPrefix(Space.EMPTY).print(cursor) + semicolon;
        }
    }
}
