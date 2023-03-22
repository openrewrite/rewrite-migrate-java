/*
 * Copyright 2021 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
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
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.search.HasJavaVersion;
import org.openrewrite.java.style.IntelliJ;
import org.openrewrite.java.style.TabsAndIndentsStyle;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.Optional;
import java.util.StringJoiner;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

@Value
@EqualsAndHashCode(callSuper = false)
public class UseTextBlocks extends Recipe {

    @Option(displayName = "End line delimiter",
            description = "A regex of the end line delimiter that should be present on a " +
                          "string for replacement to happen. IntelliJ only suggests replacing " +
                          "concatenated strings with text blocks when each string ends with a newline, " +
                          "but sometimes it is preferable to widen this to any whitespace character, for example.",
            example = "\\s+",
            required = false)
    @Nullable
    String endLineDelimiter;

    @Override
    public String getDisplayName() {
        return "Use text blocks";
    }

    @Override
    public String getDescription() {
        return "Text blocks are easier to read than concatenated strings.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new HasJavaVersion("17", true).getVisitor();
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        Pattern endLinePattern = Pattern.compile((endLineDelimiter == null ? "\\s+" : endLineDelimiter) + "$");
        return new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitBinary(J.Binary binary, ExecutionContext ctx) {
                if (isMultilineRegularString(binary, true)) {
                    TabsAndIndentsStyle tabsAndIndentsStyle = Optional.ofNullable(getCursor().firstEnclosingOrThrow(SourceFile.class)
                            .getStyle(TabsAndIndentsStyle.class)).orElse(IntelliJ.tabsAndIndents());

                    J.Literal last = (J.Literal) binary.getRight();

                    StringJoiner joiner = new StringJoiner("\n");
                    new JavaIsoVisitor<Integer>() {
                        int joined = 0;

                        @Override
                        public J.Literal visitLiteral(J.Literal literal, Integer p) {
                            String s = requireNonNull((String) literal.getValue());
                            if (s.isEmpty() && joined == 0) {
                                return literal;
                            }
                            s = last.getPrefix().getIndent() + s.replaceAll("\\s+$", "");
                            if (binary.getPrefix().getWhitespace().contains("\n")) {
                                for (int i = 0; i < tabsAndIndentsStyle.getContinuationIndent(); i++) {
                                    s = (tabsAndIndentsStyle.getUseTabCharacter() ? "\t" : " ") + s;
                                }
                            }
                            joiner.add(s);
                            joined++;
                            return literal;
                        }
                    }.visit(binary, 0);

                    String value = "\n" + joiner + "\n" + last.getPrefix().getIndent();
                    if (binary.getPrefix().getWhitespace().contains("\n")) {
                        for (int i = 0; i < tabsAndIndentsStyle.getContinuationIndent(); i++) {
                            value += (tabsAndIndentsStyle.getUseTabCharacter() ? "\t" : " ");
                        }
                    }

                    return last.withValue(value)
                            .withValueSource(String.format("\"\"\"%s\"\"\"", value))
                            .withPrefix(binary.getPrefix());
                }
                return super.visitBinary(binary, ctx);
            }

            private J.Literal firstLiteral(J.Binary binary) {
                if (binary.getLeft() instanceof J.Binary) {
                    return firstLiteral((J.Binary) binary.getLeft());
                }
                return (J.Literal) binary.getLeft();
            }

            private boolean isMultilineRegularString(J.Binary binary, boolean outermost) {
                return isRegularString(binary.getRight(), outermost) &&
                       (
                               binary.getRight().getPrefix().getWhitespace().contains("\n") ||
                               binary.getPadding().getOperator().getBefore().getWhitespace().contains("\n")
                       ) &&
                       (
                               isRegularString(binary.getLeft(), false) ||
                               (binary.getLeft() instanceof J.Binary && isMultilineRegularString((J.Binary) binary.getLeft(), false))
                       );
            }

            private boolean isRegularString(Expression expr, boolean last) {
                if (!(expr instanceof J.Literal) || ((J.Literal) expr).getType() != JavaType.Primitive.String) {
                    return false;
                }
                if (((J.Literal) expr).getValueSource() == null ||
                    ((J.Literal) expr).getValueSource().startsWith("\"\"\"")) {
                    return false;
                }
                String s = (String) requireNonNull(((J.Literal) expr).getValue());
                if (!last && !endLinePattern.matcher(s).find()) {
                    return s.isEmpty();
                }
                return true;
            }
        };
    }
}
