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
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.search.HasJavaVersion;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import static org.openrewrite.Tree.randomId;

@Value
@EqualsAndHashCode(callSuper = false)
public class UseTextBlocks extends Recipe {
    @Option(displayName = "Whether convert strings without newlines",
        description = "whether or not strings without newlines should be converted to text block when processing code.",
        example = "true",
        required = false)
    @Nullable
    boolean convertStringsWithoutNewlines;

    public UseTextBlocks() {
        convertStringsWithoutNewlines = true;
    }

    public UseTextBlocks(boolean convertStringsWithoutNewlines) {
        this.convertStringsWithoutNewlines = convertStringsWithoutNewlines;
    }

    @Override
    public String getDisplayName() {
        return "Use text blocks";
    }

    @Override
    public String getDescription() {
        return "Text blocks are easier to read than concatenated strings.";
    }

    @Override
    public @Nullable Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(3);
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new HasJavaVersion("17", true).getVisitor();
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitBinary(J.Binary binary, ExecutionContext ctx) {
                List<J.Literal> stringLiterals = new ArrayList<>();

                StringBuilder contentSb = new StringBuilder();
                StringBuilder concatenationSb = new StringBuilder();

                boolean flattenable = flatAdditiveStringLiterals(binary, stringLiterals, contentSb, concatenationSb);
                if (!flattenable) {
                    return super.visitBinary(binary, ctx);
                }

                String content = contentSb.toString();
                boolean hasNewLineInConcatenation = containsNewLineInContent(concatenationSb.toString());
                if (!hasNewLineInConcatenation) {
                    return super.visitBinary(binary, ctx);
                }

                if (!convertStringsWithoutNewlines && !containsNewLineInContent(content)) {
                    return super.visitBinary(binary, ctx);
                }

                String indentation = getIndents(concatenationSb.toString());
                StringBuilder finalContentSb = new StringBuilder().append("\n");

                for (J.Literal stingLiteral : stringLiterals) {
                    String line = indentation + stingLiteral.getValue().toString();

                    if (line.endsWith(" \n")) {
                        // Change line ending from " \n" -> "\s" to preserve trailing spaces,
                        // since `\s` can act as fence to prevent the stripping of trailing white space.
                        // see https://docs.oracle.com/en/java/javase/20/text-blocks/index.html
                        line = line.substring(0, line.length() - 2) + "\\s\n";
                    } else if (!line.endsWith("\n")) {
                        // Adds the `\<line-terminator>` escape sequence
                        line = line + "\\\n";
                    }

                    finalContentSb.append(line);
                }

                // Adds last line
                if (!finalContentSb.toString().endsWith("\n")) {
                    finalContentSb.append("\\\n");
                }
                finalContentSb.append(indentation);

                return new J.Literal(randomId(), binary.getPrefix(), Markers.EMPTY, finalContentSb,
                    String.format("\"\"\"%s\"\"\"", finalContentSb), null, JavaType.Primitive.String);
            }
        };
    }

    private static boolean flatAdditiveStringLiterals(Expression expression,
                                                      List<J.Literal> stringLiterals,
                                                      StringBuilder contentSb,
                                                      StringBuilder concatenationSb) {
        if (expression instanceof J.Binary) {
            J.Binary b = (J.Binary) expression;
            if (b.getOperator() != J.Binary.Type.Addition) {
                return false;
            }
            concatenationSb.append(b.getPrefix().getWhitespace()).append("-");
            concatenationSb.append(b.getPadding().getOperator().getBefore().getWhitespace()).append("-");
            return flatAdditiveStringLiterals(b.getLeft(), stringLiterals, contentSb, concatenationSb)
                   && flatAdditiveStringLiterals(b.getRight(), stringLiterals, contentSb, concatenationSb);
        } else if (isRegularStringLiteral(expression)) {
            J.Literal l = (J.Literal) expression;
            stringLiterals.add(l);
            contentSb.append(l.getValue().toString());
            concatenationSb.append(l.getPrefix().getWhitespace()).append("-");
            return true;
        }

        return false;
    }

    private static boolean isRegularStringLiteral(Expression expr) {
        if ( expr instanceof J.Literal) {
            J.Literal l = (J.Literal) expr;

            return TypeUtils.isString(l.getType()) &&
                   l.getValueSource() != null &&
                    !l.getValueSource().startsWith("\"\"\"");
        }
        return false;
    }

    private static boolean containsNewLineInContent(String content) {
        // ignore the new line is the last character
        for (int i = 0; i < content.length() - 1; i++) {
            char c = content.charAt(i);
            if (c == '\n') {
                return true;
            }
        }
        return false;
    }

    private static String getIndents(String concatenation) {
        return StringUtils.repeat(" ", shortestSpaceAfterNewline(concatenation));
    }

    public static int shortestSpaceAfterNewline(String str) {
        int minSpace = Integer.MAX_VALUE;
        int spaceCount = 0;
        boolean afterNewline = false;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c != ' ' && afterNewline) {
                minSpace = Math.min(minSpace, spaceCount);
                afterNewline = false;
            }

            if (c == '\n') {
                afterNewline = true;
                spaceCount = 0;
            } else if (c == ' ') {
                if (afterNewline) {
                    spaceCount++;
                }
            } else {
                afterNewline = false;
                spaceCount = 0;
            }
        }
        if (spaceCount > 0) {
            minSpace = Math.min(minSpace, spaceCount);
        }
        return minSpace == Integer.MAX_VALUE ? 0 : minSpace;
    }
}
