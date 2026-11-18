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
package org.openrewrite.java.migrate;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.marker.Markers;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.XmlVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@EqualsAndHashCode(callSuper = false)
@Value
public class BeanDiscovery extends Recipe {

    private static final XPathMatcher BEANS_MATCHER = new XPathMatcher("/beans");
    private static final Pattern VERSION_PATTERN = Pattern.compile("_([^\\/\\.]+)\\.xsd");
    private static final Set<String> CDI_NAMESPACES = new HashSet<>(Arrays.asList(
            "http://java.sun.com/xml/ns/javaee",
            "http://xmlns.jcp.org/xml/ns/javaee",
            "https://jakarta.ee/xml/ns/jakartaee"));

    String displayName = "Add `version` and `bean-discovery-mode` to `beans.xml` files without a version";

    String description = "Alters `beans.xml` files with a missing `version` attribute to include this attribute. A file that declares no `bean-discovery-mode` also gets `bean-discovery-mode=\"all\"`, to remain an explicit bean archive; a declared mode is kept.";

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
                    if ("xmlns".equals(attribute.getKeyAsString()) && !CDI_NAMESPACES.contains(attribute.getValueAsString())) {
                        return t;
                    } else if ("bean-discovery-mode".equals(attribute.getKeyAsString())) {
                        hasBeanDiscoveryMode = true;
                    } else if (attribute.getKeyAsString().endsWith("schemaLocation")) {
                        String schemaLocation = attribute.getValueAsString();
                        idealVersion = parseVersion(schemaLocation);
                    }
                }

                // Only a missing mode is set: CDI 4.0 changed the default from `all` to `annotated`, a declared mode means
                // the same in every version. The CDI 1.1 to 3.0 specs also call any beans.xml without a version an explicit
                // bean archive, but Weld and OpenWebBeans, the implementations behind Jakarta EE servers, honour a declared
                // mode whatever the version.
                if (!hasBeanDiscoveryMode) {
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
