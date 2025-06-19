/*
 * Copyright 2025 the original author or authors.
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

import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.search.FindAnnotations;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.XmlVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.*;

import static java.util.Collections.emptyList;

public class AddStaticVariableOnProducerSessionBean extends ScanningRecipe<Set<String>> {

    private static final XPathMatcher EJB_PATH = new XPathMatcher("ejb-jar/enterprise-beans/session");

    @Override
    public String getDisplayName() {
        return "Adds `static` modifier to `@Produces` fields that are in session beans";
    }

    @Override
    public String getDescription() {
        return "Ensures that the fields annotated with `@Produces` which is inside the session bean (`@Stateless`, `@Stateful`, or `@Singleton`) are declared `static`.";
    }

    @Override
    public Set<String> getInitialValue(ExecutionContext ctx) {
        return new HashSet<>();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Set<String> acc) {
        return Preconditions.check(
                new FindSourceFiles("**/ejb-jar.xml"),
                new XmlVisitor<ExecutionContext>() {

                    @Override
                    public Xml visitTag(Xml.Tag tag, ExecutionContext ctx) {
                        String className = null;
                        if (EJB_PATH.matches(getCursor())) {
                            for (Xml.Tag child : tag.getChildren()) {
                                if ("ejb-class".equals(child.getName())) {
                                    className = child.getValue().orElse(null);
                                    break;
                                }
                            }
                            if (className != null) {
                                acc.add(className);
                            }
                        }
                        return super.visitTag(tag, ctx);
                    }
                });
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Set<String> acc) {
        return Preconditions.check(
                new UsesType<>("jakarta.enterprise.inject.Produces", false),
                new JavaVisitor<ExecutionContext>() {
                    @Override
                    public J visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                        if (!multiVariable.hasModifier(J.Modifier.Type.Static) &&
                            hasAnnotation(multiVariable, "@jakarta.enterprise.inject.Produces") &&
                            (isInSessionBean() || isInXml())) {
                            return multiVariable.withModifiers(ListUtils.concat(multiVariable.getModifiers(),
                                    new J.Modifier(Tree.randomId(), Space.SINGLE_SPACE, Markers.EMPTY, null, J.Modifier.Type.Static, emptyList())));
                        }
                        return super.visitVariableDeclarations(multiVariable, ctx);
                    }

                    private boolean isInSessionBean() {
                        J.ClassDeclaration parentClass = getCursor().firstEnclosing(J.ClassDeclaration.class);
                        if (parentClass == null) {
                            return false;
                        }
                        return hasAnnotation(parentClass, "@jakarta.ejb.Singleton") ||
                               hasAnnotation(parentClass, "@jakarta.ejb.Stateful") ||
                               hasAnnotation(parentClass, "@jakarta.ejb.Stateless");
                    }

                    private boolean isInXml() {
                        J.ClassDeclaration parentClass = getCursor().firstEnclosing(J.ClassDeclaration.class);
                        if (parentClass != null && parentClass.getType() instanceof JavaType.FullyQualified) {
                            JavaType.FullyQualified fqType = parentClass.getType();
                            String fqName = fqType.getFullyQualifiedName();
                            return acc.contains(fqName);
                        }
                        return false;
                    }

                    private boolean hasAnnotation(J j, String annotationPattern) {
                        return !FindAnnotations.find(j, annotationPattern).isEmpty();
                    }
                }
        );
    }
}
