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
import org.openrewrite.xml.trait.Namespaced;
import org.openrewrite.xml.tree.Xml;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;

@Value
@EqualsAndHashCode(callSuper = false)
public class JakartaFacesCompositeLibraryNamespaces extends Recipe {

    private static final Pattern LEGACY_COMPOSITE_LIBRARY = Pattern.compile("^http://(?:xmlns\\.jcp\\.org|java\\.sun\\.com)/jsf/composite/(.+)$");

    String displayName = "Migrate Faces composite component library namespaces";

    String description = "Replace `http://xmlns.jcp.org/jsf/composite/<library>` and `http://java.sun.com/jsf/composite/<library>` " +
            "namespaces in XHTML files with `jakarta.faces.composite/<library>`.";

    Set<String> tags = new LinkedHashSet<>(asList("jakarta", "faces", "jsf"));

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new FindSourceFiles("**/*.xhtml"), new XmlIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Attribute visitAttribute(Xml.Attribute attribute, ExecutionContext ctx) {
                Xml.Attribute a = super.visitAttribute(attribute, ctx);
                if (!Namespaced.isNamespaceDefinitionAttribute(a.getKeyAsString())) {
                    return a;
                }
                Matcher library = LEGACY_COMPOSITE_LIBRARY.matcher(a.getValueAsString());
                if (!library.matches()) {
                    return a;
                }
                return a.withValue(a.getValue().withValue("jakarta.faces.composite/" + library.group(1)));
            }
        });
    }
}
