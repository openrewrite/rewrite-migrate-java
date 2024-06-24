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
import org.openrewrite.java.style.IntelliJ;
import org.openrewrite.java.style.TabsAndIndentsStyle;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.Markers;
import org.openrewrite.staticanalysis.kotlin.KotlinFileChecker;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.openrewrite.Tree.randomId;

@Value
@EqualsAndHashCode(callSuper = false)
public class UseTextBlocks extends Recipe {
    @Option(displayName = "Whether to convert strings without newlines (the default value is true).",
            description = "Whether or not strings without newlines should be converted to text block when processing code. " +
                          "The default value is true.",
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
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        TreeVisitor<?, ExecutionContext> preconditions = Preconditions.and(
                Preconditions.not(new KotlinFileChecker<>()),
                new HasJavaVersion("17", true).getVisitor()
        );
        return Preconditions.check(preconditions, new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitBinary(J.Binary binary, ExecutionContext ctx) {
                List<J.Literal> stringLiterals = new ArrayList<>();

                StringBuilder contentSb = new StringBuilder();
                StringBuilder concatenationSb = new StringBuilder();

                boolean allLiterals = allLiterals(binary);
                if (!allLiterals) {
                    return binary; // Not super.visitBinary(binary, ctx) because we don't want to visit the children
                }

                boolean flattenable = flatAdditiveStringLiterals(binary, stringLiterals, contentSb, concatenationSb);
                if (!flattenable) {
                    return super.visitBinary(binary, ctx);
                }

                boolean hasNewLineInConcatenation = containsNewLineInContent(concatenationSb.toString());
                if (!hasNewLineInConcatenation) {
                    return super.visitBinary(binary, ctx);
                }

                String content = contentSb.toString();

                if (!convertStringsWithoutNewlines && !containsNewLineInContent(content)) {
                    return super.visitBinary(binary, ctx);
                }

                return toTextBlock(binary, content, stringLiterals, concatenationSb.toString());
            }


            private J.Literal toTextBlock(J.Binary binary, String content, List<J.Literal> stringLiterals, String concatenation) {
                final String passPhrase;
                try {
                    passPhrase = generatePassword(content);
                } catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                }

                StringBuilder sb = new StringBuilder();
                StringBuilder originalContent = new StringBuilder();
                stringLiterals = stringLiterals.stream().filter(s -> !s.getValue().toString().isEmpty()).collect(Collectors.toList());
                for (int i = 0; i < stringLiterals.size(); i++) {
                    String s = stringLiterals.get(i).getValue().toString();
                    sb.append(s);
                    originalContent.append(s);
                    if (i != stringLiterals.size() - 1) {
                        String nextLine = stringLiterals.get(i + 1).getValue().toString();
                        char nextChar = nextLine.charAt(0);
                        if (!s.endsWith("\n") && nextChar != '\n') {
                            sb.append(passPhrase);
                        }
                    }
                }

                content = sb.toString();

                TabsAndIndentsStyle tabsAndIndentsStyle = Optional.ofNullable(getCursor().firstEnclosingOrThrow(SourceFile.class)
                        .getStyle(TabsAndIndentsStyle.class)).orElse(IntelliJ.tabsAndIndents());
                boolean useTab = tabsAndIndentsStyle.getUseTabCharacter();
                int tabSize = tabsAndIndentsStyle.getTabSize();

                String indentation = getIndents(concatenation, useTab, tabSize);

                boolean isEndsWithNewLine = content.endsWith("\n");

                // references:
                //  - https://docs.oracle.com/en/java/javase/14/docs/specs/text-blocks-jls.html
                //  - https://javaalmanac.io/features/textblocks/

                // escape backslashes
                content = content.replace("\\", "\\\\");
                // escape triple quotes
                content = content.replace("\"\"\"", "\"\"\\\"");
                // preserve trailing spaces
                content = content.replace(" \n", "\\s\n");
                // handle preceding indentation
                content = content.replace("\n", "\n" + indentation);
                // handle line continuations
                content = content.replace(passPhrase, "\\\n" + indentation);

                // add first line
                content = "\n" + indentation + content;

                // add last line to ensure the closing delimiter is in a new line to manage indentation & remove the
                // need to escape ending quote in the content
                if (!isEndsWithNewLine) {
                    content = content + "\\\n" + indentation;
                }

                return new J.Literal(randomId(), binary.getPrefix(), Markers.EMPTY, originalContent.toString(),
                        String.format("\"\"\"%s\"\"\"", content), null, JavaType.Primitive.String);
            }
        });
    }

    private static boolean allLiterals(Expression exp) {
        return isRegularStringLiteral(exp) || exp instanceof J.Binary
                                              && ((J.Binary) exp).getOperator() == J.Binary.Type.Addition
                                              && allLiterals(((J.Binary) exp).getLeft()) && allLiterals(((J.Binary) exp).getRight());
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
        if (expr instanceof J.Literal) {
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

    private static String getIndents(String concatenation, boolean useTabCharacter, int tabSize) {
        int[] tabAndSpaceCounts = shortestPrefixAfterNewline(concatenation, tabSize);
        int tabCount = tabAndSpaceCounts[0];
        int spaceCount = tabAndSpaceCounts[1];
        if (useTabCharacter) {
            return StringUtils.repeat("\t", tabCount) +
                   StringUtils.repeat(" ", spaceCount);
        } else {
            // replace tab with spaces if the style is using spaces
            return StringUtils.repeat(" ", tabCount * tabSize + spaceCount);
        }
    }

    /**
     * @param concatenation a string to present concatenation context
     * @param tabSize       from autoDetect
     * @return an int array of size 2, 1st value is tab count, 2nd value is space count
     */
    private static int[] shortestPrefixAfterNewline(String concatenation, int tabSize) {
        int shortest = Integer.MAX_VALUE;
        int[] shortestPair = new int[]{0, 0};
        int tabCount = 0;
        int spaceCount = 0;

        boolean afterNewline = false;
        for (int i = 0; i < concatenation.length(); i++) {
            char c = concatenation.charAt(i);
            if (c != ' ' && c != '\t' && afterNewline) {
                if ((spaceCount + tabCount * tabSize) < shortest) {
                    shortest = spaceCount + tabCount;
                    shortestPair[0] = tabCount;
                    shortestPair[1] = spaceCount;
                }
                afterNewline = false;
            }

            if (c == '\n') {
                afterNewline = true;
                spaceCount = 0;
                tabCount = 0;
            } else if (c == ' ') {
                if (afterNewline) {
                    spaceCount++;
                }
            } else if (c == '\t') {
                if (afterNewline) {
                    tabCount++;
                }
            } else {
                afterNewline = false;
                spaceCount = 0;
                tabCount = 0;
            }
        }

        if ((spaceCount + tabCount > 0) && ((spaceCount + tabCount) < shortest)) {
            shortestPair[0] = tabCount;
            shortestPair[1] = spaceCount;
        }

        return shortestPair;
    }

    private static String generatePassword(String originalStr) throws NoSuchAlgorithmException {
        final String SALT = "kun";
        String password = "";
        String saltedStr = originalStr + SALT;

        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = md.digest(saltedStr.getBytes());

        password = Base64.getEncoder().encodeToString(hashBytes);

        while (originalStr.contains(password)) {
            hashBytes = md.digest(password.getBytes());
            password = Base64.getEncoder().encodeToString(hashBytes);
        }

        return password;
    }
}
