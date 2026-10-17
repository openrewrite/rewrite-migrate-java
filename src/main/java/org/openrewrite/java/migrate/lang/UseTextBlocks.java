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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.style.IntelliJ;
import org.openrewrite.java.style.TabsAndIndentsStyle;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JContainer;
import org.openrewrite.java.tree.JRightPadded;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.marker.Markers;
import org.openrewrite.staticanalysis.kotlin.KotlinFileChecker;
import org.openrewrite.style.Style;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static org.openrewrite.Tree.randomId;

@EqualsAndHashCode(callSuper = false)
@Value
public class UseTextBlocks extends Recipe {
    @Option(displayName = "Whether to convert strings without newlines (the default value is true).",
            description = "Whether or not strings without newlines should be converted to text block when processing code. " +
                          "The default value is true.",
            example = "true",
            required = false)
    boolean convertStringsWithoutNewlines;

    @Option(displayName = "Whether to avoid line continuation escape sequences.",
            description = "When enabled, the recipe avoids using `\\` line continuation escapes in text blocks " +
                          "where the content contains newlines. Non-newline-joined strings are placed on the same " +
                          "text block line instead. The default value is false.",
            example = "true",
            required = false)
    boolean avoidLineContinuations;

    /**
     * Optional enhancement to migrate additive string concatenations containing variables or expressions
     * into a text block combined with {@code .formatted(...)} (e.g. {@code """\n%s""".formatted(var)}).
     * <p>
     * Defaults to {@code false} to preserve strict backward compatibility with existing recipes and migrations.
     */
    @Option(displayName = "Whether to convert concatenations with variables using .formatted(...).",
            description = "Whether or not string concatenations with variables should be converted to text blocks with .formatted(...). " +
                          "The default value is false.",
            example = "true",
            required = false)
    boolean formatVariables;

    public UseTextBlocks() {
        convertStringsWithoutNewlines = true;
        avoidLineContinuations = false;
        formatVariables = false;
    }

    public UseTextBlocks(final boolean convertStringsWithoutNewlines) {
        this.convertStringsWithoutNewlines = convertStringsWithoutNewlines;
        this.avoidLineContinuations = false;
        this.formatVariables = false;
    }

    public UseTextBlocks(final boolean convertStringsWithoutNewlines, final boolean avoidLineContinuations) {
        this.convertStringsWithoutNewlines = convertStringsWithoutNewlines;
        this.avoidLineContinuations = avoidLineContinuations;
        this.formatVariables = false;
    }

    public UseTextBlocks(final boolean convertStringsWithoutNewlines, final boolean avoidLineContinuations, final boolean formatVariables) {
        this.convertStringsWithoutNewlines = convertStringsWithoutNewlines;
        this.avoidLineContinuations = avoidLineContinuations;
        this.formatVariables = formatVariables;
    }

    String displayName = "Use text blocks";

    String description = "Text blocks are easier to read than concatenated strings.";

    Duration estimatedEffortPerOccurrence = Duration.ofMinutes( 3 );

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        TreeVisitor<?, ExecutionContext> preconditions = Preconditions.and(
                Preconditions.not(new KotlinFileChecker<>()),
                new UsesJavaVersion<>(17)
        );
        return Preconditions.check(preconditions, new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitBinary(final J.Binary binary, final ExecutionContext ctx) {
                if (containsComments(binary)) {
                    return binary;
                }

                if (!formatVariables) {
                    final boolean allLiterals = allLiterals(binary);
                    if (!allLiterals) {
                        return binary; // Not super.visitBinary(binary, ctx) because we don't want to visit the children
                    }

                    final List<J.Literal> stringLiterals = new ArrayList<>();
                    final StringBuilder contentSb = new StringBuilder();
                    final StringBuilder concatenationSb = new StringBuilder();

                    final boolean flattenable = flatAdditiveStringLiterals(binary, stringLiterals, contentSb, concatenationSb);
                    if (!flattenable) {
                        return super.visitBinary(binary, ctx);
                    }

                    final boolean hasNewLineInConcatenation = containsNewLineInContent(concatenationSb.toString());
                    if (!hasNewLineInConcatenation) {
                        return super.visitBinary(binary, ctx);
                    }

                    final String content = contentSb.toString();
                    if (content.contains("\r")) {
                        // Carriage returns aren't yet carried over into text blocks, which always end with a newline \n
                        return super.visitBinary(binary, ctx);
                    }

                    if (!convertStringsWithoutNewlines && !containsNewLineInContent(content)) {
                        return super.visitBinary(binary, ctx);
                    }

                    return toTextBlock(binary, content, stringLiterals, concatenationSb.toString());
                }

                final List<Operand> operands = new ArrayList<>();
                final StringBuilder concatenationSb = new StringBuilder();

                final boolean collectSuccess = collectOperands(
                        binary,
                        true,
                        operands,
                        concatenationSb,
                        false
                );
                if (!collectSuccess) {
                    return super.visitBinary(binary, ctx);
                }

                boolean hasStringLiteral = false;
                for (final Operand op : operands) {
                    if (!op.isVariable()) {
                        hasStringLiteral = true;
                        break;
                    }
                }
                if (!hasStringLiteral) {
                    return super.visitBinary(binary, ctx);
                }

                final boolean hasNewLineInConcatenation = containsNewLineInContent(concatenationSb.toString());
                if (!hasNewLineInConcatenation) {
                    return super.visitBinary(binary, ctx);
                }

                final StringBuilder contentSb = new StringBuilder();
                for (final Operand op : operands) {
                    contentSb.append(op.getContent());
                }
                final String content = contentSb.toString();
                if (content.contains("\r")) {
                    // Carriage returns aren't yet carried over into text blocks, which always end with a newline \n
                    return super.visitBinary(binary, ctx);
                }

                if (!convertStringsWithoutNewlines && !containsNewLineInContent(content)) {
                    return super.visitBinary(binary, ctx);
                }

                boolean hasVariables = false;
                for (final Operand op : operands) {
                    if (op.isVariable()) {
                        hasVariables = true;
                        break;
                    }
                }
                if (!hasVariables) {
                    final List<J.Literal> stringLiterals = new ArrayList<>(operands.size());
                    for (final Operand op : operands) {
                        stringLiterals.add((J.Literal) op.getExpression());
                    }
                    return toTextBlock(binary, content, stringLiterals, concatenationSb.toString());
                }

                return toFormattedTextBlock(binary, operands, concatenationSb.toString());
            }

            private J.Literal toTextBlock(final J.Binary binary, String content, final List<J.Literal> stringLiterals, final String concatenation) {
                final String passPhrase;
                try {
                    passPhrase = generatePassword(content);
                } catch (final NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                }

                final StringBuilder sb = new StringBuilder();
                final StringBuilder originalContent = new StringBuilder();
                final List<J.Literal> filteredLiterals = new ArrayList<>();
                for (final J.Literal s : stringLiterals) {
                    if (s.getValue() != null && !s.getValue().toString().isEmpty()) {
                        filteredLiterals.add(s);
                    }
                }
                final boolean skipLineContinuations = avoidLineContinuations && containsNewLineInContent(content);
                for (int i = 0; i < filteredLiterals.size(); i++) {
                    final String s = requireNonNull(filteredLiterals.get(i).getValue()).toString();
                    sb.append(s);
                    originalContent.append(s);
                    if (i != filteredLiterals.size() - 1) {
                        final String nextLine = requireNonNull(filteredLiterals.get(i + 1).getValue()).toString();
                        final char nextChar = nextLine.charAt(0);
                        if (!s.endsWith("\n") && nextChar != '\n' && !skipLineContinuations) {
                            sb.append(passPhrase);
                        }
                    }
                }

                final TabsAndIndentsStyle tabsAndIndentsStyle = Style.from(
                        TabsAndIndentsStyle.class,
                        getCursor().firstEnclosingOrThrow(SourceFile.class),
                        IntelliJ::tabsAndIndents
                );
                final boolean useTab = tabsAndIndentsStyle.getUseTabCharacter();
                final int tabSize = tabsAndIndentsStyle.getTabSize();

                final String indentation = getIndents(concatenation, useTab, tabSize);

                // references:
                //  - https://docs.oracle.com/en/java/javase/14/docs/specs/text-blocks-jls.html
                //  - https://javaalmanac.io/features/textblocks/

                content = sb.toString();
                // escape backslashes
                content = content.replace("\\", "\\\\");
                // re-escape tab and form feed characters as escape sequences. A literal tab or form feed is
                // indistinguishable from incidental leading whitespace, so javac may strip it during text block
                // whitespace processing (JLS 3.10.6), silently changing the string's value.
                content = content.replace("\t", "\\t");
                content = content.replace("\f", "\\f");
                // escape triple quotes
                content = content.replace("\"\"\"", "\"\"\\\"");
                // preserve trailing spaces before a newline
                content = content.replace(" \n", "\\s\n");
                // preserve a trailing space on the final content line, which a text block would otherwise strip,
                // silently changing the string's value
                final boolean endsWithTrailingSpace = content.endsWith(" ");
                if (endsWithTrailingSpace) {
                    content = content.substring(0, content.length() - 1) + "\\s";
                }
                // handle preceding indentation
                content = content.replace("\n", "\n" + indentation);
                // handle line continuations
                content = content.replace(passPhrase, "\\\n" + indentation);

                // add first line
                content = "\n" + indentation + content;

                // add last line to ensure the closing delimiter is in a new line to manage indentation & remove the
                // need to escape ending quote in the content. A trailing space additionally requires a line
                // continuation so the closing delimiter does not introduce a spurious newline.
                if (isEndsWithSpecialCharacters(sb.toString()) || endsWithTrailingSpace) {
                    content = content + "\\\n" + indentation;
                }

                return new J.Literal(randomId(), binary.getPrefix(), Markers.EMPTY, originalContent.toString(),
                        String.format("\"\"\"%s\"\"\"", content), null, JavaType.Primitive.String);
            }

            /**
             * Converts an additive concatenation containing variable expressions into a text block chained with
             * {@code .formatted(args)}.
             * <p>
             * Architecture details:
             * <ul>
             *   <li>Literal {@code %} characters in string segments are safely escaped to {@code %%}.</li>
             *   <li>Line continuations {@code \} and indentation are aligned identically to standard {@code toTextBlock}.</li>
             *   <li>Attaches fully resolved {@link JavaType.Method} for {@code String.formatted(Object... args)} to both
             *       the {@link J.Identifier} and {@link J.MethodInvocation} to ensure strict AST type attribution and pass
             *       {@code FindMissingTypes} checks.</li>
             * </ul>
             */
            private J.MethodInvocation toFormattedTextBlock(
                    final J.Binary binary,
                    final List<Operand> operands,
                    final String concatenation
            ) {
                final List<Operand> filteredOperands = new ArrayList<>();
                for (final Operand op : operands) {
                    if (op.isVariable() || !op.getContent().isEmpty()) {
                        filteredOperands.add(op);
                    }
                }

                final StringBuilder templateContent = new StringBuilder();
                for (int i = 0; i < filteredOperands.size(); i++) {
                    final Operand op = filteredOperands.get(i);
                    if (op.isVariable()) {
                        templateContent.append(op.getContent());
                    } else {
                        templateContent.append(op.getContent().replace("%", "%%"));
                    }
                }
                final String contentForPassword = templateContent.toString();
                final String passPhrase;
                try {
                    passPhrase = generatePassword(contentForPassword);
                } catch (final NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                }

                final StringBuilder sb = new StringBuilder();
                final StringBuilder originalContent = new StringBuilder();
                final boolean skipLineContinuations = avoidLineContinuations && containsNewLineInContent(contentForPassword);

                for (int i = 0; i < filteredOperands.size(); i++) {
                    final Operand op = filteredOperands.get(i);
                    final String piece = op.isVariable() ? op.getContent() : op.getContent().replace("%", "%%");
                    sb.append(piece);
                    originalContent.append(piece);
                    if (i != filteredOperands.size() - 1) {
                        final Operand nextOp = filteredOperands.get(i + 1);
                        final String nextPiece = nextOp.isVariable() ? nextOp.getContent() : nextOp.getContent().replace("%", "%%");
                        final char nextChar = nextPiece.isEmpty() ? ' ' : nextPiece.charAt(0);
                        if (!piece.endsWith("\n") && nextChar != '\n' && !skipLineContinuations && nextOp.isLineBreakBefore()) {
                            sb.append(passPhrase);
                        }
                    }
                }

                final TabsAndIndentsStyle tabsAndIndentsStyle = Style.from(
                        TabsAndIndentsStyle.class,
                        getCursor().firstEnclosingOrThrow(SourceFile.class),
                        IntelliJ::tabsAndIndents
                );
                final boolean useTab = tabsAndIndentsStyle.getUseTabCharacter();
                final int tabSize = tabsAndIndentsStyle.getTabSize();

                final String indentation = getIndents(concatenation, useTab, tabSize);

                String content = sb.toString();
                // escape backslashes
                content = content.replace("\\", "\\\\");
                // re-escape tab and form feed characters
                content = content.replace("\t", "\\t");
                content = content.replace("\f", "\\f");
                // escape triple quotes
                content = content.replace("\"\"\"", "\"\"\\\"");
                // preserve trailing spaces before a newline
                content = content.replace(" \n", "\\s\n");
                // preserve a trailing space on the final content line
                final boolean endsWithTrailingSpace = content.endsWith(" ");
                if (endsWithTrailingSpace) {
                    content = content.substring(0, content.length() - 1) + "\\s";
                }
                // handle preceding indentation
                content = content.replace("\n", "\n" + indentation);
                // handle line continuations
                content = content.replace(passPhrase, "\\\n" + indentation);

                // add first line
                content = "\n" + indentation + content;

                // add last line to ensure the closing delimiter is in a new line to manage indentation & remove the
                // need to escape ending quote in the content. A trailing space additionally requires a line
                // continuation so the closing delimiter does not introduce a spurious newline.
                if (isEndsWithSpecialCharacters(sb.toString()) || endsWithTrailingSpace) {
                    content = content + "\\\n" + indentation;
                }

                final J.Literal textBlock = new J.Literal(
                        randomId(),
                        Space.EMPTY,
                        Markers.EMPTY,
                        originalContent.toString(),
                        String.format("\"\"\"%s\"\"\"", content),
                        null,
                        JavaType.Primitive.String
                );

                final List<Expression> variableExpressions = new ArrayList<>();
                for (final Operand op : filteredOperands) {
                    if (op.isVariable()) {
                        variableExpressions.add(op.getExpression());
                    }
                }

                final List<JRightPadded<Expression>> paddedArgs = new ArrayList<>(variableExpressions.size());
                for (int i = 0; i < variableExpressions.size(); i++) {
                    final Expression expr = variableExpressions.get(i);
                    final Space prefix = (i == 0) ? Space.EMPTY : Space.format(" ");
                    paddedArgs.add(JRightPadded.build(expr.withPrefix(prefix)));
                }

                final JContainer<Expression> argsContainer = JContainer.build(
                        Space.EMPTY,
                        paddedArgs,
                        Markers.EMPTY
                );

                JavaType.FullyQualified stringType = TypeUtils.asFullyQualified(binary.getType());
                if (stringType == null) {
                    stringType = JavaType.ShallowClass.build("java.lang.String");
                }
                JavaType.Method formattedMethod = null;
                for (final JavaType.Method m : stringType.getMethods()) {
                    if ("formatted".equals(m.getName())) {
                        formattedMethod = m;
                        break;
                    }
                }
                if (formattedMethod == null) {
                    final JavaType objectArrayType = new JavaType.Array(
                            null,
                            JavaType.ShallowClass.build("java.lang.Object"),
                            null
                    );
                    formattedMethod = new JavaType.Method(
                            null,
                            1L,
                            stringType,
                            "formatted",
                            stringType,
                            singletonList("args"),
                            singletonList(objectArrayType),
                            emptyList(),
                            emptyList(),
                            emptyList(),
                            emptyList()
                    );
                }

                final J.Identifier methodName = new J.Identifier(
                        randomId(),
                        Space.EMPTY,
                        Markers.EMPTY,
                        emptyList(),
                        "formatted",
                        formattedMethod,
                        null
                );

                return new J.MethodInvocation(
                        randomId(),
                        binary.getPrefix(),
                        Markers.EMPTY,
                        JRightPadded.build(textBlock),
                        null,
                        methodName,
                        argsContainer,
                        formattedMethod
                );
            }

            private boolean isEndsWithSpecialCharacters(final String content) {
                return content.endsWith("\"") ||
                       content.endsWith("=") ||
                       content.endsWith("}") ||
                       content.endsWith(";");
            }
        });
    }

    private static boolean containsComments(final Expression expression) {
        if (expression instanceof J.Binary) {
            final J.Binary b = (J.Binary) expression;
            if (!b.getPrefix().getComments().isEmpty() ||
                !b.getPadding().getOperator().getBefore().getComments().isEmpty()) {
                return true;
            }
            return containsComments(b.getLeft()) || containsComments(b.getRight());
        }
        return !expression.getPrefix().getComments().isEmpty();
    }

    private static final class Operand {
        private final Expression expression;
        private final boolean variable;
        private final String content;
        private final boolean lineBreakBefore;

        Operand(
                final Expression expression,
                final boolean variable,
                final String content,
                final boolean lineBreakBefore
        ) {
            this.expression = expression;
            this.variable = variable;
            this.content = content;
            this.lineBreakBefore = lineBreakBefore;
        }

        public Expression getExpression() {
            return expression;
        }

        public boolean isVariable() {
            return variable;
        }

        public String getContent() {
            return content;
        }

        public boolean isLineBreakBefore() {
            return lineBreakBefore;
        }
    }

    /**
     * Maps an operand's resolved {@link JavaType} to a printf format specifier.
     * <p>
     * Note: Floats and doubles intentionally map to {@code %s} (rather than {@code %f}) to preserve exact decimal
     * formatting and avoid locale-dependent comma vs dot separators when transforming string concatenations.
     */
    private static String getFormatSpecifier(final @Nullable JavaType type) {
        final JavaType.Primitive primitive = TypeUtils.asPrimitive(type);
        if (primitive != null) {
            switch (primitive) {
                case Int:
                case Long:
                case Short:
                case Byte:
                    return "%d";
                case Boolean:
                    return "%b";
                case Char:
                    return "%c";
                default:
                    return "%s";
            }
        }
        final JavaType.FullyQualified fq = TypeUtils.asFullyQualified(type);
        if (fq != null) {
            final String fqn = fq.getFullyQualifiedName();
            if ("java.lang.Integer".equals(fqn) || "java.lang.Long".equals(fqn) || "java.lang.Short".equals(fqn) || "java.lang.Byte".equals(fqn)) {
                return "%d";
            }
            if ("java.lang.Boolean".equals(fqn)) {
                return "%b";
            }
            if ("java.lang.Character".equals(fqn)) {
                return "%c";
            }
            return "%s";
        }
        return "%s";
    }

    private static boolean collectOperands(
            final Expression expression,
            final boolean formatVariables,
            final List<Operand> operands,
            final StringBuilder concatenationSb,
            final boolean lineBreakBefore
    ) {
        if (expression instanceof J.Binary) {
            final J.Binary b = (J.Binary) expression;
            if (b.getOperator() == J.Binary.Type.Addition && TypeUtils.isString(b.getType())) {
                concatenationSb.append(b.getPrefix().getWhitespace())
                        .append("-");
                concatenationSb.append(b.getPadding().getOperator().getBefore().getWhitespace())
                        .append("-");

                final boolean leftSuccess = collectOperands(
                        b.getLeft(),
                        formatVariables,
                        operands,
                        concatenationSb,
                        lineBreakBefore
                );
                if (!leftSuccess) {
                    return false;
                }

                final boolean hasLineBreakBetween = containsNewLineInContent(b.getPadding().getOperator().getBefore().getWhitespace()) ||
                        containsNewLineInContent(b.getRight().getPrefix().getWhitespace());

                return collectOperands(
                        b.getRight(),
                        formatVariables,
                        operands,
                        concatenationSb,
                        hasLineBreakBetween
                );
            }
        }
        if (isRegularStringLiteral(expression)) {
            final J.Literal l = (J.Literal) expression;
            final String val = requireNonNull(l.getValue()).toString();
            operands.add(new Operand(l, false, val, lineBreakBefore));
            concatenationSb.append(l.getPrefix().getWhitespace())
                    .append("-");
            return true;
        }
        if (formatVariables) {
            if (expression instanceof J.Literal && TypeUtils.isString(expression.getType())) {
                return false;
            }
            final String specifier = getFormatSpecifier(expression.getType());
            operands.add(new Operand(expression, true, specifier, lineBreakBefore));
            concatenationSb.append(expression.getPrefix().getWhitespace())
                    .append("-");
            return true;
        }
        return false;
    }

    private static boolean allLiterals(Expression exp) {
        return isRegularStringLiteral(exp) || exp instanceof J.Binary &&
                                              ((J.Binary) exp).getOperator() == J.Binary.Type.Addition &&
                                              allLiterals(((J.Binary) exp).getLeft()) && allLiterals(((J.Binary) exp).getRight());
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
            return flatAdditiveStringLiterals(b.getLeft(), stringLiterals, contentSb, concatenationSb) &&
                    flatAdditiveStringLiterals(b.getRight(), stringLiterals, contentSb, concatenationSb);
        }
        if (isRegularStringLiteral(expression)) {
            J.Literal l = (J.Literal) expression;
            stringLiterals.add(l);
            contentSb.append(requireNonNull(l.getValue()));
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
                   !l.getValueSource().startsWith("\"\"\"") &&
                   // Defend against literals whose value still looks like the source (e.g. parser failed to
                   // decode supplementary characters encoded as surrogate pairs); skipping prevents emitting
                   // a broken text block that drops or re-escapes content.
                   !l.getValueSource().equals(l.getValue());
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
        }
        // replace tab with spaces if the style is using spaces
        return StringUtils.repeat(" ", tabCount * tabSize + spaceCount);
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
        String saltedStr = originalStr + SALT;

        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = md.digest(saltedStr.getBytes());

        String password = Base64.getEncoder().encodeToString(hashBytes);

        while (originalStr.contains(password)) {
            hashBytes = md.digest(password.getBytes());
            password = Base64.getEncoder().encodeToString(hashBytes);
        }

        return password;
    }
}
