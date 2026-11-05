/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.java.migrate.jakarta;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.FindSourceFiles;
import org.openrewrite.Option;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.Validated;
import org.openrewrite.marker.AlreadyReplaced;
import org.openrewrite.xml.XmlIsoVisitor;
import org.openrewrite.xml.tree.Xml;

import static org.openrewrite.Tree.randomId;

@Value
@EqualsAndHashCode(callSuper = false)
public class FindAndReplaceInXml extends Recipe {

    @Option(displayName = "Find",
            description = "The literal text to find.",
            example = "javax.")
    String find;

    @Option(displayName = "Replace",
            description = "The text to replace it with.",
            example = "jakarta.")
    String replace;

    @Option(displayName = "File pattern",
            description = "A glob expression limiting the files searched. All XML files are searched by default.",
            required = false,
            example = "**/*.xhtml")
    @Nullable
    String filePattern;

    String displayName = "Find and replace text in XML";

    String description = "Replace literal text in the attribute values, text, CDATA sections and comments of XML files. " +
            "Unlike `org.openrewrite.text.FindAndReplace`, files stay XML, so XML recipes running later in the same " +
            "recipe run still apply to them.";

    @Override
    public Validated<Object> validate() {
        return super.validate().and(Validated.notBlank("find", find));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        TreeVisitor<?, ExecutionContext> visitor = new XmlIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {
                boolean alreadyReplaced = document.getMarkers().findAll(AlreadyReplaced.class).stream()
                        .anyMatch(m -> find.equals(m.getFind()) && replace.equals(m.getReplace()));
                if (alreadyReplaced) {
                    return document;
                }
                Xml.Document d = super.visitDocument(document, ctx);
                return d == document ? d : d.withMarkers(d.getMarkers().add(new AlreadyReplaced(randomId(), find, replace)));
            }

            @Override
            public Xml.Attribute visitAttribute(Xml.Attribute attribute, ExecutionContext ctx) {
                Xml.Attribute a = super.visitAttribute(attribute, ctx);
                Xml.Attribute.Value value = a.getValue();
                return a.withValue(value.withValue(value.getValue().replace(find, replace)));
            }

            @Override
            public Xml.CharData visitCharData(Xml.CharData charData, ExecutionContext ctx) {
                Xml.CharData c = super.visitCharData(charData, ctx);
                return c.withText(c.getText().replace(find, replace));
            }

            @Override
            public Xml.Comment visitComment(Xml.Comment comment, ExecutionContext ctx) {
                Xml.Comment c = super.visitComment(comment, ctx);
                return c.withText(c.getText().replace(find, replace));
            }
        };
        return filePattern == null ? visitor : Preconditions.check(new FindSourceFiles(filePattern), visitor);
    }
}
