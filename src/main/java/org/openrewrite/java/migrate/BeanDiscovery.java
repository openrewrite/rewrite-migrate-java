/*
 * Copyright 2023 the original author or authors.
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
package org.openrewrite.java.migrate;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.marker.Markers;
import org.openrewrite.xml.ChangeTagAttribute;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.XmlVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Value
@EqualsAndHashCode(callSuper = false)
public class BeanDiscovery extends Recipe {

    private static final XPathMatcher BEANS_MATCHER = new XPathMatcher("/beans");
    private static final Pattern VERSION_PATTERN = Pattern.compile("_([^\\/\\.]+)\\.xsd");

    @Override
    public String getDisplayName() {
        return "Behavior change to bean discovery in modules with `beans.xml` file with no version specified";
    }

    @Override
    public String getDescription() {
        return "Alters beans with missing version attribute to include this attribute as well as the bean-discovery-mode=\"all\" attribute to maintain an explicit bean archive.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        XmlVisitor<ExecutionContext> xmlVisitor = new XmlVisitor<ExecutionContext>() {
            @Override
            public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = (Xml.Tag) super.visitTag(tag, ctx);
                if (!BEANS_MATCHER.matches(getCursor()) || t.getAttributes().stream()
                        .map(Xml.Attribute::getKeyAsString)
                        .anyMatch("version"::equals)) {
                    return t;
                }

                // Determine which tags are already present
                boolean hasBeanDiscoveryMode = false;
                String idealVersion = null;
                for (Xml.Attribute attribute : t.getAttributes()) {
                    if (attribute.getKeyAsString().equals("bean-discovery-mode")) {
                        hasBeanDiscoveryMode = true;
                    } else if (attribute.getKeyAsString().endsWith("schemaLocation")) {
                        String schemaLocation = attribute.getValueAsString();
                        idealVersion = parseVersion(schemaLocation);
                    }
                }

                // Update or apply bean-discovery-mode=all
                if (hasBeanDiscoveryMode) {
                    TreeVisitor<?, ExecutionContext> changeTagVisitor = new ChangeTagAttribute("beans", "bean-discovery-mode", "all", null, null).getVisitor();
                    t = (Xml.Tag) changeTagVisitor.visit(t, ctx, getCursor());
                } else {
                    t = addAttribute(t, "bean-discovery-mode", "all", ctx);
                }

                // Add version attribute
                return addAttribute(t, "version", idealVersion != null ? idealVersion : "4.0", ctx);
            }

            private String parseVersion(String schemaLocation) {
                String version = null;
                Matcher m = VERSION_PATTERN.matcher(schemaLocation);
                if (m.find()) {
                    version = m.group(1).replace("_", ".");
                }
                return version;
            }

            private Xml.Tag addAttribute(Xml.Tag t, String name, String all, ExecutionContext ctx) {
                Xml.Attribute attribute = new Xml.Attribute(Tree.randomId(), "", Markers.EMPTY,
                        new Xml.Ident(Tree.randomId(), "", Markers.EMPTY, name),
                        "",
                        new Xml.Attribute.Value(Tree.randomId(), "", Markers.EMPTY, Xml.Attribute.Value.Quote.Double, all));
                return t.withAttributes(ListUtils.concat(t.getAttributes(), autoFormat(attribute, ctx)));
            }

        };
        return Preconditions.check(new FindSourceFiles("**/beans.xml"), xmlVisitor);
    }
}
