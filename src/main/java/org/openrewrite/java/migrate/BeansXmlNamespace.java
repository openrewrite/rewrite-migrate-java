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

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

@Value
@EqualsAndHashCode(callSuper = false)
public class BeansXmlNamespace extends Recipe {

    private static final XPathMatcher BEANS_MATCHER = new XPathMatcher("/beans");
    private static final String NS_SUN = "http://java.sun.com/xml/ns/javaee";
    private static final String NS_JCP = "http://xmlns.jcp.org/xml/ns/javaee";
    private static final String SUN_SCHEMA_LOCATION = "http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/beans_1_0.xsd";
    private static final String JCP_SCHEMA_LOCATION = "http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/beans_1_1.xsd";

    @Override
    public String getDisplayName() {
        return "Change `beans.xml` `schemaLocation` to match XML namespace";
    }

    @Override
    public String getDescription() {
        return "Set the `schemaLocation` that corresponds to the `xmlns` set in `beans.xml` files.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new FindSourceFiles("**/beans.xml"), new XmlVisitor<ExecutionContext>() {
            @Override
            public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
                Xml.Tag t = (Xml.Tag) super.visitTag(tag, ctx);
                if (BEANS_MATCHER.matches(getCursor())) {
                    Map<String, String> attributes = t.getAttributes().stream().collect(toMap(Xml.Attribute::getKeyAsString, Xml.Attribute::getValueAsString));
                    String xmlns = attributes.get("xmlns");
                    String schemaLocation = attributes.get("xsi:schemaLocation");
                    if (NS_SUN.equalsIgnoreCase(xmlns) && !SUN_SCHEMA_LOCATION.equalsIgnoreCase(schemaLocation)) {
                        doAfterVisit(new ChangeTagAttribute("beans", "xsi:schemaLocation", SUN_SCHEMA_LOCATION, null, null).getVisitor());
                    } else if (NS_JCP.equalsIgnoreCase(xmlns) && !JCP_SCHEMA_LOCATION.equalsIgnoreCase(schemaLocation)) {
                        doAfterVisit(new ChangeTagAttribute("beans", "xsi:schemaLocation", JCP_SCHEMA_LOCATION, null, null).getVisitor());
                    }
                }
                return t;
            }
        });
    }
}
