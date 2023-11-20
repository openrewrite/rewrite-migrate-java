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
import org.openrewrite.xml.ChangeTagAttribute;
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
        return "The recipe updates incompatible namespaces with the right value of schemaLocation.";
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
                    String key = attribute.getKeyAsString();
                    String value = attribute.getValueAsString();
                    if ("xmlns".equals(key)) {
                        nameSpaceValue = value;
                    } else if (key.endsWith("schemaLocation")) {
                        schemaLocation = value;
                    }
                }
                if (!updateNameSpaceSchemaLocation(nameSpaceValue, schemaLocation).isEmpty()) {
                    t = updateAttribute(t, updateNameSpaceSchemaLocation(nameSpaceValue, schemaLocation), ctx);
                }
                return t;
            }

            private Xml.Tag updateAttribute(Xml.Tag t, String value, ExecutionContext ctx) {
                TreeVisitor<?, ExecutionContext> changeTagVisitor = new ChangeTagAttribute("beans", "xsi:schemaLocation", value, null).getVisitor();
                t = (Xml.Tag) changeTagVisitor.visit(t, ctx, getCursor());
                return t;
            }

            private String updateNameSpaceSchemaLocation(String nameSpaceValue, String schemaLocation) {
                if (NS_SUN.equalsIgnoreCase(nameSpaceValue) && !(SUN_SCHEMA_LOCATION.equalsIgnoreCase(schemaLocation))) {
                    return (SUN_SCHEMA_LOCATION);
                } else if (NS_JCP.equalsIgnoreCase(nameSpaceValue) && !(JCP_SCHEMA_LOCATION.equalsIgnoreCase(schemaLocation))) {
                    return (JCP_SCHEMA_LOCATION);
                }
                return "";
            }

        };
        return Preconditions.check(new HasSourcePath<>("**/beans.xml"), xmlVisitor);
    }
}
