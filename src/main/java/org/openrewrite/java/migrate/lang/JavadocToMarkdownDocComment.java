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

import lombok.Getter;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaPrinter;
import org.openrewrite.java.marker.JavaVersion;
import org.openrewrite.java.marker.LineComment;
import org.openrewrite.java.marker.Varargs;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;
import org.openrewrite.marker.SearchResult;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;

import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

public class JavadocToMarkdownDocComment extends Recipe {

    @Getter
    final String displayName = "Convert Javadoc to Markdown documentation comments";

    @Getter
    final String description = "Convert traditional Javadoc comments (`/** ... */`) to Markdown documentation comments (`///`) " +
            "as supported by JEP 467 in Java 23+. Transforms HTML constructs like `<pre>`, `<code>`, `<em>`, `<p>`, and lists " +
            "to their Markdown equivalents, and converts inline tags like `{@code}` and `{@link}` to Markdown syntax.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        // Markdown documentation comments (JEP 467) are recognized by the JDK that compiles the
        // source (Java 23+), independent of the -source/--release bytecode level. So gate on the
        // compiling JDK (JavaVersion.createdBy) rather than UsesJavaVersion, which checks the
        // source compatibility / bytecode level.
        TreeVisitor<?, ExecutionContext> compiledByJava23OrLater = new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public @Nullable J visit(@Nullable Tree tree, ExecutionContext ctx) {
                if (tree instanceof JavaSourceFile) {
                    JavaSourceFile cu = (JavaSourceFile) tree;
                    if (cu.getMarkers().findFirst(JavaVersion.class)
                            .filter(v -> v.getMajorCreatedByVersion() >= 23)
                            .isPresent()) {
                        return SearchResult.found(cu);
                    }
                }
                return (J) tree;
            }
        };
        return Preconditions.check(compiledByJava23OrLater, new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public Space visitSpace(Space space, Space.Location loc, ExecutionContext ctx) {
                String spaceWhitespace = space.getWhitespace();
                List<Comment> comments = space.getComments();
                return space.withComments(ListUtils.map(comments, (i, comment) -> {
                    if (!(comment instanceof Javadoc.DocComment) ||
                        comment.getMarkers().findFirst(LineComment.class).isPresent()) {
                        return comment;
                    }
                    // A `///` comment runs to the end of the line, so code after it moves to the next line;
                    // a Javadoc with code on both sides has no line of its own to convert into
                    String precedingWhitespace = i == 0 ? spaceWhitespace : comments.get(i - 1).getSuffix();
                    if (!comment.getSuffix().contains("\n") && !precedingWhitespace.contains("\n")) {
                        return comment;
                    }
                    return convertDocComment((Javadoc.DocComment) comment, spaceWhitespace);
                }));
            }
        });
    }

    private static Comment convertDocComment(Javadoc.DocComment docComment, String spaceWhitespace) {
        // Derive the indentation from the space whitespace (e.g., "\n    " → "    ")
        int lastNewline = spaceWhitespace.lastIndexOf('\n');
        String indentation = lastNewline >= 0 ? spaceWhitespace.substring(lastNewline + 1) : spaceWhitespace;

        JavadocToMarkdownConverter converter = new JavadocToMarkdownConverter();
        converter.convert(docComment.getBody());

        // Strip one leading space (from the space after * in javadoc), right-trim, drop
        // leading/trailing blank lines, and collapse consecutive blank lines
        List<List<Object>> lines = normalizeLines(converter.getLines());

        String suffix = docComment.getSuffix().contains("\n") ? docComment.getSuffix() : "\n" + indentation;
        return new Javadoc.DocComment(
                Tree.randomId(),
                Markers.build(singletonList(new LineComment(Tree.randomId()))),
                toBody(lines, "\n" + indentation + "///"),
                suffix);
    }

    private static List<List<Object>> normalizeLines(List<List<Object>> raw) {
        // Strip one leading space and right-trim
        List<List<Object>> lines = new ArrayList<>(raw.size());
        for (List<Object> line : raw) {
            List<Object> stripped = ListUtils.mapFirst(line, part -> part instanceof String ?
                    emptyToNull(stripLeadingSpace((String) part)) : part);
            lines.add(ListUtils.mapLast(stripped, part -> part instanceof String ?
                    emptyToNull(((String) part).replaceAll("\\s+$", "")) : part));
        }

        // Trim leading and trailing blank lines
        int start = 0;
        while (start < lines.size() && lines.get(start).isEmpty()) {
            start++;
        }
        int end = lines.size();
        while (end > start && lines.get(end - 1).isEmpty()) {
            end--;
        }

        // Collapse consecutive blank lines
        List<List<Object>> result = new ArrayList<>();
        boolean prevBlank = false;
        for (int i = start; i < end; i++) {
            List<Object> line = lines.get(i);
            boolean blank = line.isEmpty();
            if (!blank || !prevBlank) {
                result.add(line);
            }
            prevBlank = blank;
        }
        return result;
    }

    private static @Nullable String emptyToNull(String s) {
        return s.isEmpty() ? null : s;
    }

    private static String stripLeadingSpace(String s) {
        return s.startsWith(" ") ? s.substring(1) : s;
    }

    /**
     * As the parser lays out a Markdown documentation comment, a block tag holds what follows it up to the next one.
     */
    private static List<Javadoc> toBody(List<List<Object>> lines, String margin) {
        List<Javadoc> body = new ArrayList<>();
        Tag blockTag = null;
        List<Javadoc> description = null;
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                (description == null ? body : description)
                        .add(new Javadoc.LineBreak(Tree.randomId(), margin, Markers.EMPTY));
            }
            boolean first = true;
            for (Object part : lines.get(i)) {
                String leadingSpace = first ? " " : "";
                first = false;
                if (part instanceof Tag && ((Tag) part).isBlock()) {
                    if (blockTag != null) {
                        body.add(blockTag.describe(description));
                    }
                    if (!leadingSpace.isEmpty()) {
                        body.add(text(leadingSpace));
                    }
                    blockTag = (Tag) part;
                    description = new ArrayList<>();
                    continue;
                }
                List<Javadoc> target = description == null ? body : description;
                if (part instanceof String) {
                    target.add(text(leadingSpace + part));
                } else {
                    if (!leadingSpace.isEmpty()) {
                        target.add(text(leadingSpace));
                    }
                    target.add(((Tag) part).getTree());
                }
            }
        }
        if (blockTag != null) {
            body.add(blockTag.describe(description));
        }
        return body;
    }

    private static Javadoc.Text text(String text) {
        return new Javadoc.Text(Tree.randomId(), Markers.EMPTY, text);
    }

    /**
     * A tag kept from the Javadoc, so that the references it holds stay attributed. {@code text} is its Markdown.
     */
    @Value
    static class Tag {
        Javadoc tree;
        String text;

        @Nullable
        Function<List<Javadoc>, Javadoc> withDescription;

        boolean isBlock() {
            return withDescription != null;
        }

        Javadoc describe(List<Javadoc> description) {
            return requireNonNull(withDescription).apply(description);
        }
    }

    static class JavadocToMarkdownConverter {
        private static final Pattern ESCAPED_HTML_TAG = Pattern.compile("&lt;[A-Za-z/!?]");

        private final List<List<Object>> lines = new ArrayList<>();
        private List<Object> currentParts = new ArrayList<>();
        private StringBuilder currentLine = new StringBuilder();
        private boolean inPre = false;
        private boolean inCode = false;
        private boolean suppressNextLineBreak = false;
        private final Deque<String> listStack = new ArrayDeque<>();
        private final Deque<Integer> listCounterStack = new ArrayDeque<>();

        List<List<Object>> getLines() {
            flushLine();
            return lines;
        }

        private void flushLine() {
            if (!currentLineIsEmpty() || lines.isEmpty()) {
                endLine();
            }
        }

        private boolean currentLineIsEmpty() {
            return currentParts.isEmpty() && currentLine.length() == 0;
        }

        private boolean currentLineIsBlank() {
            return currentParts.isEmpty() && StringUtils.isBlank(currentLine.toString());
        }

        private void endLine() {
            if (currentLine.length() > 0) {
                currentParts.add(currentLine.toString());
            }
            lines.add(currentParts);
            currentParts = new ArrayList<>();
            currentLine = new StringBuilder();
        }

        private void addLine(String text) {
            List<Object> line = new ArrayList<>(1);
            if (!text.isEmpty()) {
                line.add(text);
            }
            lines.add(line);
        }

        private int startTag() {
            if (currentLine.length() > 0) {
                currentParts.add(currentLine.toString());
                currentLine = new StringBuilder();
            }
            return lines.size();
        }

        /**
         * Replace the text written since {@link #startTag} with the tag, unless the tag spans lines, as it would
         * still print their Javadoc margins.
         */
        @SafeVarargs
        private final void keepTag(int start, Javadoc tag, @Nullable Function<List<Javadoc>, Javadoc> withDescription,
                                   List<Javadoc>... spaces) {
            if (lines.size() != start) {
                return;
            }
            for (List<Javadoc> space : spaces) {
                for (Javadoc j : space) {
                    if (j instanceof Javadoc.LineBreak) {
                        return;
                    }
                }
            }
            currentParts.add(new Tag(tag, currentLine.toString(), withDescription));
            currentLine = new StringBuilder();
        }

        void convert(List<Javadoc> body) {
            for (int i = 0; i < body.size(); i++) {
                Javadoc node = body.get(i);
                // Each entity is a text node of its own, so join adjacent text to decode entities in context
                while (node instanceof Javadoc.Text && i + 1 < body.size() && body.get(i + 1) instanceof Javadoc.Text) {
                    Javadoc.Text text = (Javadoc.Text) node;
                    node = text.withText(text.getText() + ((Javadoc.Text) body.get(++i)).getText());
                }
                convertNode(node);
            }
        }

        private void convertNode(Javadoc node) {
            // A `<br>` already ended the current line; an immediately following physical
            // line break is the same break, so don't let it add a spurious blank line.
            boolean afterBr = suppressNextLineBreak;
            suppressNextLineBreak = false;
            if (node instanceof Javadoc.Text) {
                currentLine.append(decodeHtmlEntities(((Javadoc.Text) node).getText()));
            } else if (node instanceof Javadoc.LineBreak) {
                if (!afterBr) {
                    endLine();
                }
            } else if (node instanceof Javadoc.Literal) {
                convertLiteral((Javadoc.Literal) node);
            } else if (node instanceof Javadoc.Link) {
                convertLink((Javadoc.Link) node);
            } else if (node instanceof Javadoc.StartElement) {
                convertStartElement((Javadoc.StartElement) node);
            } else if (node instanceof Javadoc.EndElement) {
                convertEndElement((Javadoc.EndElement) node);
            } else if (node instanceof Javadoc.Parameter) {
                convertParameter((Javadoc.Parameter) node);
            } else if (node instanceof Javadoc.Return) {
                convertReturn((Javadoc.Return) node);
            } else if (node instanceof Javadoc.Throws) {
                convertThrows((Javadoc.Throws) node);
            } else if (node instanceof Javadoc.See) {
                convertSee((Javadoc.See) node);
            } else if (node instanceof Javadoc.Since) {
                convertSince((Javadoc.Since) node);
            } else if (node instanceof Javadoc.Author) {
                convertAuthor((Javadoc.Author) node);
            } else if (node instanceof Javadoc.Deprecated) {
                convertDeprecated((Javadoc.Deprecated) node);
            } else if (node instanceof Javadoc.InheritDoc) {
                int start = startTag();
                currentLine.append("{@inheritDoc}");
                keepTag(start, node, null, ((Javadoc.InheritDoc) node).getEndBrace());
            } else if (node instanceof Javadoc.Snippet) {
                convertSnippet((Javadoc.Snippet) node);
            } else if (node instanceof Javadoc.DocRoot) {
                int start = startTag();
                currentLine.append("{@docRoot}");
                keepTag(start, node, null, ((Javadoc.DocRoot) node).getEndBrace());
            } else if (node instanceof Javadoc.InlinedValue) {
                convertInlinedValue((Javadoc.InlinedValue) node);
            } else if (node instanceof Javadoc.Version) {
                convertVersion((Javadoc.Version) node);
            } else if (node instanceof Javadoc.Hidden) {
                convertHidden((Javadoc.Hidden) node);
            } else if (node instanceof Javadoc.Index) {
                convertIndex((Javadoc.Index) node);
            } else if (node instanceof Javadoc.Summary) {
                convertSummary((Javadoc.Summary) node);
            } else if (node instanceof Javadoc.UnknownBlock) {
                convertUnknownBlock((Javadoc.UnknownBlock) node);
            } else if (node instanceof Javadoc.UnknownInline) {
                convertUnknownInline((Javadoc.UnknownInline) node);
            } else if (node instanceof Javadoc.Erroneous) {
                convert(((Javadoc.Erroneous) node).getText());
            } else if (node instanceof Javadoc.Reference) {
                currentLine.append(printReference((Javadoc.Reference) node));
            }
        }

        private void convertLiteral(Javadoc.Literal literal) {
            String content = stripLeadingSpace(renderInline(literal.getDescription(), true));
            // Both {@code} and {@literal} map to backticks in Markdown.
            // {@literal} prevents HTML interpretation; backticks achieve the same in Markdown.
            if (content.contains("\n")) {
                // Multi-line: use fenced code block
                currentLine.append("```");
                endLine();
                for (String line : content.split("\n", -1)) {
                    addLine(line);
                }
                currentLine.append("```");
            } else {
                currentLine.append('`').append(content).append('`');
            }
        }

        private void convertLink(Javadoc.Link link) {
            String ref = printReference(link.getTreeReference());
            String label = stripLeadingSpace(renderInline(link.getLabel())).trim();

            if (!label.isEmpty()) {
                currentLine.append('[').append(label).append("][").append(ref).append(']');
            } else {
                currentLine.append('[').append(ref).append(']');
            }
        }

        private void convertStartElement(Javadoc.StartElement element) {
            String name = element.getName().toLowerCase();
            if (inPre && !"pre".equals(name)) {
                if ("code".equals(name)) {
                    // Place the fence (and optional language) on its own line so the
                    // code content starts fresh; otherwise the first code line would be
                    // consumed as the fenced code block's info string.
                    currentLine.append(extractCodeLanguage(element));
                    endLine();
                    return;
                }
                renderHtmlStartElement(element);
                return;
            }
            switch (name) {
                case "pre":
                    inPre = true;
                    currentLine.append("```");
                    break;
                case "code":
                    if (!inPre) {
                        inCode = true;
                        currentLine.append('`');
                    }
                    break;
                case "p":
                    // Blank line for paragraph
                    endLine();
                    addLine("");
                    break;
                case "br":
                    // Line break: end the current line. Suppress the physical line break that
                    // usually follows so the two collapse into a single break rather than a
                    // blank line; a redundant break before a `<p>` is absorbed when blank lines
                    // are collapsed.
                    endLine();
                    suppressNextLineBreak = true;
                    break;
                case "em":
                case "i":
                    currentLine.append('_');
                    break;
                case "strong":
                case "b":
                    currentLine.append("**");
                    break;
                case "ul":
                    listStack.push("ul");
                    endLine();
                    break;
                case "ol":
                    listStack.push("ol");
                    listCounterStack.push(1);
                    endLine();
                    break;
                case "li":
                    if (!listStack.isEmpty()) {
                        String listType = listStack.peek();
                        if ("ol".equals(listType)) {
                            int count = listCounterStack.pop();
                            currentLine.append(count).append(". ");
                            listCounterStack.push(count + 1);
                        } else {
                            currentLine.append("- ");
                        }
                    }
                    break;
                default:
                    renderHtmlStartElement(element);
                    break;
            }
        }

        private String extractCodeLanguage(Javadoc.StartElement element) {
            return element.getAttributes().stream()
                    .filter(Javadoc.Attribute.class::isInstance)
                    .map(Javadoc.Attribute.class::cast)
                    .filter(a -> "class".equalsIgnoreCase(a.getName()))
                    .filter(a -> a.getValue() != null)
                    .map(a -> renderInline(a.getValue()).trim())
                    .map(JavadocToMarkdownConverter::stripAttributeQuotes)
                    .findFirst()
                    .orElse("");
        }

        private static String stripAttributeQuotes(String value) {
            if (value.length() < 2) {
                return value;
            }

            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '\'' && last == '\'') || (first == '"' && last == '"')) {
                return value.substring(1, value.length() - 1);
            }
            return value;
        }

        private void renderHtmlStartElement(Javadoc.StartElement element) {
            currentLine.append('<').append(element.getName());
            for (Javadoc attr : element.getAttributes()) {
                if (attr instanceof Javadoc.Attribute) {
                    Javadoc.Attribute a = (Javadoc.Attribute) attr;
                    currentLine.append(' ').append(a.getName());
                    List<Javadoc> value = a.getValue();
                    if (value != null && !value.isEmpty()) {
                        currentLine.append('=').append(renderInline(value));
                    }
                }
            }
            if (element.isSelfClosing()) {
                currentLine.append('/');
            }
            currentLine.append('>');
        }

        private void convertEndElement(Javadoc.EndElement element) {
            String name = element.getName().toLowerCase();
            if (inPre && !"pre".equals(name)) {
                if (!"code".equals(name)) {
                    currentLine.append("</").append(element.getName()).append('>');
                }
                return;
            }
            switch (name) {
                case "pre":
                    inPre = false;
                    // Flush any trailing inline code (e.g. `...);</code></pre>`) so the
                    // closing fence sits on its own line, as Markdown requires. Only flush
                    // real content; a whitespace-only line means the fence is already alone.
                    if (!currentLineIsBlank()) {
                        endLine();
                    }
                    currentLine.append("```");
                    break;
                case "code":
                    if (!inPre) {
                        inCode = false;
                        currentLine.append('`');
                    }
                    break;
                case "em":
                case "i":
                    currentLine.append('_');
                    break;
                case "strong":
                case "b":
                    currentLine.append("**");
                    break;
                case "ul":
                    if (!listStack.isEmpty()) {
                        listStack.pop();
                    }
                    break;
                case "ol":
                    if (!listStack.isEmpty()) {
                        listStack.pop();
                    }
                    if (!listCounterStack.isEmpty()) {
                        listCounterStack.pop();
                    }
                    break;
                case "li":
                    // End of list item handled naturally by line breaks
                    break;
                case "p":
                    // </p> is often implicit, ignore
                    break;
                default:
                    // Pass through unknown end elements
                    currentLine.append("</").append(element.getName()).append('>');
                    break;
            }
        }

        private void convertParameter(Javadoc.Parameter param) {
            int start = startTag();
            currentLine.append("@param");
            convert(param.getSpaceBeforeName());
            J name = param.getName();
            if (name == null) {
                Javadoc.Reference nameRef = param.getNameReference();
                if (nameRef != null) {
                    name = nameRef.getTree();
                }
            }
            if (name != null) {
                if (name instanceof J.TypeParameter) {
                    // Generic type parameters (e.g. `@param <T>`) are parsed as a J.TypeParameter
                    // with the surrounding angle brackets stripped, so re-add them here.
                    currentLine.append('<').append(printJ(name)).append('>');
                } else {
                    currentLine.append(printJ(name));
                }
            }
            keepTag(start, param, param::withDescription, param.getSpaceBeforeName());
            convert(param.getDescription());
        }

        private void convertReturn(Javadoc.Return ret) {
            int start = startTag();
            currentLine.append("@return");
            keepTag(start, ret, ret::withDescription);
            convert(ret.getDescription());
        }

        private void convertThrows(Javadoc.Throws thr) {
            int start = startTag();
            currentLine.append(thr.isThrowsKeyword() ? "@throws " : "@exception ");
            J exceptionName = thr.getExceptionName();
            if (exceptionName != null) {
                currentLine.append(printJ(exceptionName));
            }
            keepTag(start, thr, thr::withDescription, thr.getSpaceBeforeExceptionName());
            convert(thr.getDescription());
        }

        private void convertSee(Javadoc.See see) {
            int start = startTag();
            currentLine.append("@see");
            for (Javadoc node : see.getSpaceBeforeTree()) {
                convertNode(node);
            }
            Javadoc.Reference treeRef = see.getTreeReference();
            if (treeRef != null) {
                currentLine.append(printReference(treeRef));
            } else {
                J tree = see.getTree();
                if (tree != null) {
                    currentLine.append(printJRef(tree));
                }
            }
            keepTag(start, see, see::withReference, see.getSpaceBeforeTree());
            convert(see.getReference());
        }

        private void convertSince(Javadoc.Since since) {
            int start = startTag();
            currentLine.append("@since");
            keepTag(start, since, since::withDescription);
            convert(since.getDescription());
        }

        private void convertAuthor(Javadoc.Author author) {
            int start = startTag();
            currentLine.append("@author");
            keepTag(start, author, author::withName);
            convert(author.getName());
        }

        private void convertDeprecated(Javadoc.Deprecated deprecated) {
            int start = startTag();
            currentLine.append("@deprecated");
            keepTag(start, deprecated, deprecated::withDescription);
            convert(deprecated.getDescription());
        }

        private void convertSnippet(Javadoc.Snippet snippet) {
            currentLine.append("{@snippet");
            convert(snippet.getAttributes());
            convert(snippet.getContent());
            currentLine.append('}');
        }

        private void convertInlinedValue(Javadoc.InlinedValue value) {
            int start = startTag();
            currentLine.append("{@value");
            J tree = value.getTree();
            if (tree != null) {
                currentLine.append(' ').append(printJ(tree));
            }
            currentLine.append('}');
            keepTag(start, value, null, value.getSpaceBeforeTree(), value.getEndBrace());
        }

        private void convertVersion(Javadoc.Version version) {
            int start = startTag();
            currentLine.append("@version");
            keepTag(start, version, version::withBody);
            convert(version.getBody());
        }

        private void convertHidden(Javadoc.Hidden hidden) {
            int start = startTag();
            currentLine.append("@hidden");
            keepTag(start, hidden, hidden::withBody);
            convert(hidden.getBody());
        }

        private void convertIndex(Javadoc.Index index) {
            currentLine.append("{@index");
            convert(index.getSearchTerm());
            convert(index.getDescription());
            currentLine.append('}');
        }

        private void convertSummary(Javadoc.Summary summary) {
            currentLine.append("{@summary");
            convert(summary.getSummary());
            currentLine.append('}');
        }

        private void convertUnknownBlock(Javadoc.UnknownBlock block) {
            int start = startTag();
            currentLine.append('@').append(block.getName());
            keepTag(start, block, block::withContent);
            convert(block.getContent());
        }

        private void convertUnknownInline(Javadoc.UnknownInline inline) {
            currentLine.append("{@").append(inline.getName());
            convert(inline.getContent());
            currentLine.append('}');
        }

        private String renderInline(List<Javadoc> body) {
            return renderInline(body, inCode);
        }

        private String renderInline(List<Javadoc> body, boolean code) {
            JavadocToMarkdownConverter inlineConverter = new JavadocToMarkdownConverter();
            inlineConverter.inPre = this.inPre;
            inlineConverter.inCode = code;
            inlineConverter.convert(body);
            StringJoiner text = new StringJoiner("\n");
            for (List<Object> line : inlineConverter.getLines()) {
                StringBuilder lineText = new StringBuilder();
                for (Object part : line) {
                    lineText.append(part instanceof Tag ? ((Tag) part).getText() : part);
                }
                text.add(lineText);
            }
            return text.toString();
        }

        private static String printReference(Javadoc.@Nullable Reference ref) {
            if (ref == null) {
                return "";
            }
            J tree = ref.getTree();
            if (tree == null) {
                return "";
            }
            return printJRef(tree);
        }

        /**
         * Print a J tree as a Javadoc-style reference (using # for members instead of .)
         */
        private static String printJRef(J tree) {
            if (tree instanceof J.Identifier) {
                return ((J.Identifier) tree).getSimpleName();
            }
            if (tree instanceof J.FieldAccess) {
                J.FieldAccess fa = (J.FieldAccess) tree;
                String target = printJRef(fa.getTarget());
                String name = fa.getSimpleName();
                if (target.isEmpty()) {
                    return "#" + name;
                }
                return target + "." + name;
            }
            if (tree instanceof J.MemberReference) {
                J.MemberReference mr = (J.MemberReference) tree;
                return printJRef(mr.getContaining()) + "#" + printJRef(mr.getReference());
            }
            if (tree instanceof J.MethodInvocation) {
                J.MethodInvocation mi = (J.MethodInvocation) tree;
                StringBuilder sb = new StringBuilder();
                if (mi.getSelect() != null) {
                    sb.append(printJRef(mi.getSelect()));
                    sb.append('#');
                }
                sb.append(mi.getSimpleName());
                sb.append('(');
                List<Expression> args = mi.getArguments();
                for (int i = 0; i < args.size(); i++) {
                    if (i > 0) {
                        sb.append( ", " );
                    }
                    Expression arg = args.get(i);
                    if (!(arg instanceof J.Empty)) {
                        sb.append(printJRef(arg));
                    }
                }
                sb.append(')');
                return sb.toString();
            }
            return printJ(tree);
        }

        private static String printJ(J tree) {
            if (tree instanceof J.Identifier) {
                return ((J.Identifier) tree).getSimpleName();
            }
            if (tree instanceof J.FieldAccess) {
                J.FieldAccess fa = (J.FieldAccess) tree;
                return printJ(fa.getTarget()) + "." + fa.getSimpleName();
            }
            if (tree instanceof J.MemberReference) {
                J.MemberReference mr = (J.MemberReference) tree;
                return printJ(mr.getContaining()) + "#" + printJ(mr.getReference());
            }
            if (tree instanceof J.ParameterizedType) {
                J.ParameterizedType pt = (J.ParameterizedType) tree;
                StringBuilder sb = new StringBuilder(printJ(pt.getClazz()));
                if (pt.getTypeParameters() != null) {
                    sb.append('<');
                    for (int i = 0; i < pt.getTypeParameters().size(); i++) {
                        if (i > 0) {
                            sb.append( ", " );
                        }
                        sb.append(printJ(pt.getTypeParameters().get(i)));
                    }
                    sb.append('>');
                }
                return sb.toString();
            }
            if (tree instanceof J.ArrayType) {
                J.ArrayType arrayType = (J.ArrayType) tree;
                String suffix = arrayType.getMarkers().findFirst(Varargs.class).isPresent() ? "..." : "[]";
                return printJ(arrayType.getElementType()) + suffix;
            }
            if (tree instanceof J.Primitive) {
                return ((J.Primitive) tree).getType().getKeyword();
            }
            try {
                return tree.print(new JavaPrinter<>()).trim();
            } catch (Exception e) {
                return tree.toString();
            }
        }

        private String decodeHtmlEntities(String text) {
            if (!text.contains("&")) {
                return text;
            }
            // Outside code, Markdown would read a decoded `<T>` as an HTML tag and hide it
            if (inPre || inCode || !ESCAPED_HTML_TAG.matcher(text).find()) {
                text = text
                        .replace("&lt;", "<")
                        .replace("&gt;", ">");
            }
            return text
                    .replace("&amp;", "&")
                    .replace("&quot;", "\"")
                    .replace("&apos;", "'")
                    .replace("&nbsp;", " ")
                    .replace("&#64;", "@");
        }
    }
}
