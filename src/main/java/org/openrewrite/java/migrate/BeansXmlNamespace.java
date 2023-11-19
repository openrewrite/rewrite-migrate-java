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
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.XmlVisitor;
import org.openrewrite.xml.tree.Xml;

@Value
@EqualsAndHashCode(callSuper = true)
public class BeansXmlNamespace extends Recipe {

    private static final XPathMatcher BEANS_MATCHER = new XPathMatcher("/beans");
    private static final String NS_SUN = "http://java.sun.com/xml/ns/javaee";
    private static final String NS_JCP = "http://xmlns.jcp.org/xml/ns/javaee";
    private static final String SUN_SCHEMA_LOCATION = "http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/beans_1_0.xsd";
    private static final String JCP_SCHEMA_LOCATION = "http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/beans_1_1.xsd";
    private static final String DISABLE_VALIDATING = "org.jboss.weld.xml.disableValidating";

    @Override
    public String getDisplayName() {
        return "Check valid namespace and schema location in the`beans.xml` file ";
    }

    @Override
    public String getDescription() {
        return " Adds JVM property `org.jboss.weld.xml.disableValidating=true` when valid namespace and schema location is not found in file.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        XmlVisitor<ExecutionContext> xmlVisitor = new XmlVisitor<ExecutionContext>() {
            @Override
            public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = (Xml.Tag) super.visitTag(tag, ctx);
                if (!BEANS_MATCHER.matches(getCursor()) || t.getAttributes().stream().map(Xml.Attribute::getKeyAsString).noneMatch("xmlns"::equals)) {
                    return t;
                }
                String nameSpaceValue = null, schemaLocation = null;
                for (Xml.Attribute attribute : t.getAttributes()) {
                    if (attribute.getKeyAsString().equals("xmlns")) {
                        nameSpaceValue = attribute.getValueAsString();
                    } else if (attribute.getKeyAsString().endsWith("schemaLocation")) {
                        schemaLocation = attribute.getValueAsString();
                    } else if (DISABLE_VALIDATING.equalsIgnoreCase(attribute.getKeyAsString())) {
                        return t;
                    }

                }
                if (!checkNameSpaceSchemaLocation(nameSpaceValue, schemaLocation)) {
                    return addAttribute(t, DISABLE_VALIDATING, String.valueOf(true), ctx);
                }
                return t;
            }

            private Xml.Tag addAttribute(Xml.Tag t, String name, String all, ExecutionContext ctx) {
                Xml.Attribute attribute = new Xml.Attribute(Tree.randomId(), "", Markers.EMPTY, new Xml.Ident(Tree.randomId(), "", Markers.EMPTY, name), "", new Xml.Attribute.Value(Tree.randomId(), "", Markers.EMPTY, Xml.Attribute.Value.Quote.Double, all));
                return t.withAttributes(ListUtils.concat(t.getAttributes(), autoFormat(attribute, ctx)));
            }

            private boolean checkNameSpaceSchemaLocation(String nameSpaceValue, String schemaLocation) {
                if (NS_SUN.equalsIgnoreCase(nameSpaceValue) && SUN_SCHEMA_LOCATION.equalsIgnoreCase(schemaLocation)) {
                    return true;
                }
                return nameSpaceValue.equalsIgnoreCase(NS_JCP) && schemaLocation.equalsIgnoreCase(JCP_SCHEMA_LOCATION);
            }

        };
        return Preconditions.check(new HasSourcePath<>("**/beans.xml"), xmlVisitor);
    }
}
