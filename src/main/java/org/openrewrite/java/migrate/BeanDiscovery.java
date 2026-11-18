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
import org.jspecify.annotations.Nullable;
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
    private static final String JAKARTA_NAMESPACE = "https://jakarta.ee/xml/ns/jakartaee";
    private static final Set<String> CDI_NAMESPACES = new HashSet<>(Arrays.asList(
            "http://java.sun.com/xml/ns/javaee",
            "http://xmlns.jcp.org/xml/ns/javaee",
            JAKARTA_NAMESPACE));

    String displayName = "Add `version` and `bean-discovery-mode` to `beans.xml` files";

    String description = "Alters `beans.xml` files with a missing `version` attribute to include this attribute. A file that declares no `bean-discovery-mode` and has no version or one below 4.0 also gets `bean-discovery-mode=\"all\"`, so it remains an explicit bean archive under the CDI 4.0 default of `annotated`; a declared mode is kept.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        XmlVisitor<ExecutionContext> xmlVisitor = new XmlVisitor<ExecutionContext>() {
            @Override
            public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = (Xml.Tag) super.visitTag(tag, ctx);
                if (!BEANS_MATCHER.matches(getCursor())) {
                    return t;
                }

                // Determine which tags are already present
                boolean hasBeanDiscoveryMode = false;
                boolean javaxNamespace = false;
                String version = null;
                String idealVersion = null;
                for (Xml.Attribute attribute : t.getAttributes()) {
                    if ("xmlns".equals(attribute.getKeyAsString())) {
                        if (!CDI_NAMESPACES.contains(attribute.getValueAsString())) {
                            return t;
                        }
                        javaxNamespace = !JAKARTA_NAMESPACE.equals(attribute.getValueAsString());
                    } else if ("bean-discovery-mode".equals(attribute.getKeyAsString())) {
                        hasBeanDiscoveryMode = true;
                    } else if ("version".equals(attribute.getKeyAsString())) {
                        version = attribute.getValueAsString();
                    } else if (attribute.getKeyAsString().endsWith("schemaLocation")) {
                        String schemaLocation = attribute.getValueAsString();
                        idealVersion = parseVersion(schemaLocation);
                    }
                }

                // Only a missing mode is set: CDI 4.0 changed the default from `all` to `annotated`, a declared mode means
                // the same in every version. The CDI 1.1 to 3.0 specs also call any beans.xml without a version an explicit
                // bean archive, but Weld and OpenWebBeans, the implementations behind Jakarta EE servers, honour a declared
                // mode whatever the version.
                if (!hasBeanDiscoveryMode && (version == null || isBeforeCdi4(version, idealVersion, javaxNamespace))) {
                    t = addAttribute(t, "bean-discovery-mode", "all", ctx);
                }
                return version != null ? t : addAttribute(t, "version", idealVersion != null ? idealVersion : "4.0", ctx);
            }

            private boolean isBeforeCdi4(String version, @Nullable String schemaVersion, boolean javaxNamespace) {
                try {
                    return Integer.parseInt(version.trim().split("\\.")[0]) < 4;
                } catch (NumberFormatException e) {
                    // A placeholder such as `${cdi.version}`: the `javax` namespaces or the schema's version tell
                    return javaxNamespace || schemaVersion != null && isBeforeCdi4(schemaVersion, null, false);
                }
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
