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
import org.openrewrite.ExecutionContext;
import org.openrewrite.FindSourceFiles;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.xml.XmlIsoVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.LinkedHashSet;
import java.util.Set;

import static java.util.Arrays.asList;

@Value
@EqualsAndHashCode(callSuper = false)
public class JakartaFacesXhtmlEE9 extends Recipe {

    String displayName = "Faces XHTML migration for Jakarta EE 9";

    String description = "Find and replace javax references to jakarta in XHTML files.";

    Set<String> tags = new LinkedHashSet<>(asList("jakarta", "faces", "jsf"));

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        // Unlike `org.openrewrite.text.FindAndReplace`, this keeps pages XML, so XML recipes running later still apply
        return Preconditions.check(new FindSourceFiles("**/*.xhtml"), new XmlIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Attribute visitAttribute(Xml.Attribute attribute, ExecutionContext ctx) {
                Xml.Attribute a = super.visitAttribute(attribute, ctx);
                Xml.Attribute.Value value = a.getValue();
                return a.withValue(value.withValue(toJakarta(value.getValue())));
            }

            @Override
            public Xml.CharData visitCharData(Xml.CharData charData, ExecutionContext ctx) {
                Xml.CharData c = super.visitCharData(charData, ctx);
                return c.withText(toJakarta(c.getText()));
            }

            @Override
            public Xml.Comment visitComment(Xml.Comment comment, ExecutionContext ctx) {
                Xml.Comment c = super.visitComment(comment, ctx);
                return c.withText(toJakarta(c.getText()));
            }

            private String toJakarta(String text) {
                return text.replace("javax.", "jakarta.");
            }
        });
    }
}
