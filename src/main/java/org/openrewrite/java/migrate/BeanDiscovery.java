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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.XmlVisitor;
import org.openrewrite.xml.tree.Xml;
import org.openrewrite.marker.Markers;
import org.openrewrite.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Value
@EqualsAndHashCode(callSuper = true)
public class BeanDiscovery extends Recipe {

    @Override
    public String getDisplayName() {
        return "Behavior change to bean discovery in modules with beans.xml file with no version specified";
    }

    @Override
    public String getDescription() {
        return "Alters beans with missing version attribute to include this attribute as well as the bean-discovery-mode=\"all\" attribute to maintain an explicit bean archive.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new XmlVisitor<ExecutionContext>() {

            final Pattern versionPattern = Pattern.compile("_([^\\/\\.]+)\\.xsd");

            boolean hasVersion = false;
            boolean hasBeanDiscoveryMode = false;
            String idealVersion = null;


            @Override
            public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = (Xml.Tag) super.visitTag(tag, ctx);
                this.hasVersion = false;
                if (new XPathMatcher("beans").matches(getCursor())) {
                    // find versions
                    t = t.withAttributes(ListUtils.map(t.getAttributes(), this::visitAttributes));

                    if(!this.hasVersion) {
                        if(this.hasBeanDiscoveryMode) {
                            t = t.withAttributes(ListUtils.map(t.getAttributes(), this::visitBeanDiscoveryModeAttribute));
                        } else {
                            t = t.withAttributes(ListUtils.concat(t.getAttributes(), autoFormat(new Xml.Attribute(Tree.randomId(), "", Markers.EMPTY,
                                    new Xml.Ident(Tree.randomId(), "", Markers.EMPTY, "bean-discovery-mode"),
                                    "",
                                    autoFormat(new Xml.Attribute.Value(Tree.randomId(), "", Markers.EMPTY,
                                            Xml.Attribute.Value.Quote.Double,
                                            "all"), ctx)), ctx)));
                        }

                        String version = this.idealVersion != null ? this.idealVersion : "4.0";

                        t = t.withAttributes(ListUtils.concat(t.getAttributes(), autoFormat(new Xml.Attribute(Tree.randomId(), "", Markers.EMPTY,
                                new Xml.Ident(Tree.randomId(), "", Markers.EMPTY, "version"),
                                "",
                                autoFormat(new Xml.Attribute.Value(Tree.randomId(), "", Markers.EMPTY,
                                        Xml.Attribute.Value.Quote.Double,
                                        version), ctx)), ctx)));
                    }

                }
                return t;
            }

            public String parseVersion(String schemaLocation) {
                String version = null;
                Matcher m = versionPattern.matcher(schemaLocation);
                if (m.find()) {
                    version = m.group(1).replace("_", ".");
                }
                return version;
            }

            public Xml.Attribute visitBeanDiscoveryModeAttribute(Xml.Attribute attribute) {
                if (attribute.getKeyAsString().equals("bean-discovery-mode")) {
                    return attribute.withValue(
                            new Xml.Attribute.Value(attribute.getId(),
                                    "",
                                    attribute.getMarkers(),
                                    attribute.getValue().getQuote(),
                                    "all"));
                }
                return attribute;
            }

            public Xml.Attribute visitAttributes(Xml.Attribute attribute) {
                if (attribute.getKeyAsString().equals("version")) {
                    hasVersion = true;
                } else if (attribute.getKeyAsString().equals("bean-discovery-mode")) {
                    this.hasBeanDiscoveryMode = true;
                } else if (attribute.getKeyAsString().contains("schemaLocation")) {
                    String schemaLocation = attribute.getValueAsString();
                    this.idealVersion = parseVersion(schemaLocation);
                }
                return attribute;
            }
        };
    }
    
}
