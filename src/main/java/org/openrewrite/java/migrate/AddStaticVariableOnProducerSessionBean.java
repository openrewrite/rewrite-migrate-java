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

import lombok.Getter;
import lombok.Setter;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.search.FindAnnotations;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;
import org.openrewrite.xml.XmlVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.emptyList;

public class AddStaticVariableOnProducerSessionBean extends ScanningRecipe<AddStaticVariableOnProducerSessionBean.Accumulator> {
    @Override
    public String getDisplayName() {
        return "Adds `static` modifier to `@Produces` field that are on session bean";
    }

    @Override
    public String getDescription() {
        return "Ensures that the fields annotated with `@Produces` which is inside the session bean (`@Stateless`, `@Stateful`, or `@Singleton`) are declared `static`.";
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return new XmlVisitor<ExecutionContext>() {
            @Override
            public Xml visitTag(Xml.Tag tag, ExecutionContext executionContext) {
                String className = null;
                String sessionType = null;
                for (Xml.Tag child : tag.getChildren()) {
                    if ("ejb-class".equals(child.getName())) {
                        className = child.getValue().orElse(null);
                    } else if ("session-type".equals(child.getName())) {
                        sessionType = child.getValue().orElse(null);
                    }
                }
                if (className != null && sessionType != null) {
                    acc.getSessionBeanClasses().put(className, sessionType);
                }
                return super.visitTag(tag, executionContext);
            }
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return
                Preconditions.check(
                        Preconditions.or(
                                new UsesType<>("jakarta.enterprise.inject.Produces", false),
                                Preconditions.or(
                                        new UsesType<>("jakarta.ejb.Singleton", false),
                                        new UsesType<>("jakarta.ejb.Stateful", false),
                                        new UsesType<>("jakarta.ejb.Stateless", false)
                                )
                        ),
                        new JavaVisitor<ExecutionContext>() {
                            private final Accumulator accumulator = acc;

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
                                    return acc.getSessionBeanClasses().containsKey(fqName);
                                }
                                return false;
                            }

                            private boolean hasAnnotation(J j, String annotationPattern) {
                                return !FindAnnotations.find(j, annotationPattern).isEmpty();
                            }
                        }
                );
    }

    // @Data
    @Setter
    @Getter
    public static class Accumulator {
        private Map<String, String> sessionBeanClasses = new HashMap<>();
    }
}
