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
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaPrinter;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.tree.*;
import org.openrewrite.marker.Markers;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static java.util.Collections.singletonList;

public class JavadocToMarkdownDocComment extends Recipe {

    @Getter
    final String displayName = "Convert Javadoc to Markdown documentation comments";

    @Getter
    final String description = "Convert traditional Javadoc comments (`/** ... */`) to Markdown documentation comments (`///`) " +
            "as supported by JEP 467 in Java 23+. Transforms HTML constructs like `<pre>`, `<code>`, `<em>`, `<p>`, and lists " +
            "to their Markdown equivalents, and converts inline tags like `{@code}` and `{@link}` to Markdown syntax.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesJavaVersion<>(23), new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public Space visitSpace(Space space, Space.Location loc, ExecutionContext ctx) {
                String spaceWhitespace = space.getWhitespace();
                return space.withComments(ListUtils.flatMap(space.getComments(), comment ->
                        comment instanceof Javadoc.DocComment ?
                                convertDocComment((Javadoc.DocComment) comment, spaceWhitespace) :
                                comment));
            }
        });
    }

    private static List<Comment> convertDocComment(Javadoc.DocComment docComment, String spaceWhitespace) {
        // Derive the indentation from the space whitespace (e.g., "\n    " → "    ")
        int lastNewline = spaceWhitespace.lastIndexOf('\n');
        String indentation = lastNewline >= 0 ? spaceWhitespace.substring(lastNewline + 1) : spaceWhitespace;

        JavadocToMarkdownConverter converter = new JavadocToMarkdownConverter();
        converter.convert(docComment.getBody());

        // Strip one leading space (from the space after * in javadoc), right-trim, drop
        // leading/trailing blank lines, and collapse consecutive blank lines
        List<String> lines = normalizeLines(converter.getLines());

        if (lines.isEmpty()) {
            lines = singletonList("");
        }

        String interLineSuffix = "\n" + indentation;
        return ListUtils.mapLast(toTextComments(lines, interLineSuffix),
                comment -> comment.withSuffix(docComment.getSuffix()));
    }

    private static List<String> normalizeLines(List<String> raw) {
        // Strip one leading space and right-trim
        List<String> lines = new ArrayList<>(raw.size());
        for (String line : raw) {
            String stripped = line.startsWith(" ") ? line.substring(1) : line;
            lines.add(stripped.replaceAll("\\s+$", ""));
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
        List<String> result = new ArrayList<>();
        boolean prevBlank = false;
        for (int i = start; i < end; i++) {
            String line = lines.get(i);
            boolean blank = line.isEmpty();
            if (!blank || !prevBlank) {
                result.add(line);
            }
            prevBlank = blank;
        }
        return result;
    }

    private static List<Comment> toTextComments(List<String> lines, String suffix) {
        List<Comment> result = new ArrayList<>(lines.size());
        for (String lineContent : lines) {
            // TextComment(false, text, suffix, markers) prints as "// + text"
            // So text="/ content" produces "/// content"
            String text = lineContent.isEmpty() ? "/" : "/ " + lineContent;
            result.add(new TextComment(false, text, suffix, Markers.EMPTY));
        }
        return result;
    }

    static class JavadocToMarkdownConverter {
        private final List<String> lines = new ArrayList<>();
        private StringBuilder currentLine = new StringBuilder();
        private boolean inPre = false;
        private final Deque<String> listStack = new ArrayDeque<>();
        private final Deque<Integer> listCounterStack = new ArrayDeque<>();

        List<String> getLines() {
            flushLine();
            return lines;
        }

        private void flushLine() {
            if (currentLine.length() > 0) {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder();
            } else if (lines.isEmpty()) {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder();
            }
        }

        void convert(List<Javadoc> body) {
            for (Javadoc node : body) {
                convertNode(node);
            }
        }

        private void convertNode(Javadoc node) {
            if (node instanceof Javadoc.Text) {
                currentLine.append(decodeHtmlEntities(((Javadoc.Text) node).getText()));
            } else if (node instanceof Javadoc.LineBreak) {
                lines.add(currentLine.toString());
                currentLine = new StringBuilder();
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
                currentLine.append("{@inheritDoc}");
            } else if (node instanceof Javadoc.Snippet) {
                convertSnippet((Javadoc.Snippet) node);
            } else if (node instanceof Javadoc.DocRoot) {
                currentLine.append("{@docRoot}");
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
            String content = stripLeadingSpace(renderInline(literal.getDescription()));
            // Both {@code} and {@literal} map to backticks in Markdown.
            // {@literal} prevents HTML interpretation; backticks achieve the same in Markdown.
            if (content.contains("\n")) {
                // Multi-line: use fenced code block
                currentLine.append("```");
                lines.add(currentLine.toString());
                for (String line : content.split("\n", -1)) {
                    lines.add(line);
                }
                currentLine = new StringBuilder("```");
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
                        currentLine.append('`');
                    }
                    break;
                case "p":
                    // Blank line for paragraph
                    lines.add(currentLine.toString());
                    lines.add("");
                    currentLine = new StringBuilder();
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
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder();
                    break;
                case "ol":
                    listStack.push("ol");
                    listCounterStack.push(1);
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder();
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
                currentLine.append("</").append(element.getName()).append('>');
                return;
            }
            switch (name) {
                case "pre":
                    inPre = false;
                    currentLine.append("```");
                    break;
                case "code":
                    if (!inPre) {
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
            currentLine.append("@param");
            convert(param.getSpaceBeforeName());
            J name = param.getName();
            if (name != null) {
                currentLine.append(printJ(name));
            }
            Javadoc.Reference nameRef = param.getNameReference();
            if (nameRef != null && nameRef.getTree() != null && name == null) {
                currentLine.append(printJ(nameRef.getTree()));
            }
            convert(param.getDescription());
        }

        private void convertReturn(Javadoc.Return ret) {
            currentLine.append("@return");
            convert(ret.getDescription());
        }

        private void convertThrows(Javadoc.Throws thr) {
            currentLine.append(thr.isThrowsKeyword() ? "@throws " : "@exception ");
            J exceptionName = thr.getExceptionName();
            if (exceptionName != null) {
                currentLine.append(printJ(exceptionName));
            }
            convert(thr.getDescription());
        }

        private void convertSee(Javadoc.See see) {
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
            convert(see.getReference());
        }

        private void convertSince(Javadoc.Since since) {
            currentLine.append("@since");
            convert(since.getDescription());
        }

        private void convertAuthor(Javadoc.Author author) {
            currentLine.append("@author");
            convert(author.getName());
        }

        private void convertDeprecated(Javadoc.Deprecated deprecated) {
            currentLine.append("@deprecated");
            convert(deprecated.getDescription());
        }

        private void convertSnippet(Javadoc.Snippet snippet) {
            currentLine.append("{@snippet");
            convert(snippet.getAttributes());
            convert(snippet.getContent());
            currentLine.append('}');
        }

        private void convertInlinedValue(Javadoc.InlinedValue value) {
            currentLine.append("{@value");
            J tree = value.getTree();
            if (tree != null) {
                currentLine.append(' ').append(printJ(tree));
            }
            currentLine.append('}');
        }

        private void convertVersion(Javadoc.Version version) {
            currentLine.append("@version");
            convert(version.getBody());
        }

        private void convertHidden(Javadoc.Hidden hidden) {
            currentLine.append("@hidden");
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
            currentLine.append('@').append(block.getName());
            convert(block.getContent());
        }

        private void convertUnknownInline(Javadoc.UnknownInline inline) {
            currentLine.append("{@").append(inline.getName());
            convert(inline.getContent());
            currentLine.append('}');
        }

        private String renderInline(List<Javadoc> body) {
            JavadocToMarkdownConverter inlineConverter = new JavadocToMarkdownConverter();
            inlineConverter.inPre = this.inPre;
            inlineConverter.convert(body);
            List<String> inlineLines = inlineConverter.getLines();
            return String.join("\n", inlineLines);
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
                return target + "#" + name;
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
                        sb.append(printJ((J) pt.getTypeParameters().get(i)));
                    }
                    sb.append('>');
                }
                return sb.toString();
            }
            if (tree instanceof J.ArrayType) {
                return printJ(((J.ArrayType) tree).getElementType()) + "[]";
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

        private static String stripLeadingSpace(String s) {
            return s.startsWith(" ") ? s.substring(1) : s;
        }

        private static String decodeHtmlEntities(String text) {
            if (!text.contains("&")) {
                return text;
            }
            return text
                    .replace("&lt;", "<")
                    .replace("&gt;", ">")
                    .replace("&amp;", "&")
                    .replace("&quot;", "\"")
                    .replace("&apos;", "'")
                    .replace("&nbsp;", " ")
                    .replace("&#64;", "@");
        }
    }
}
