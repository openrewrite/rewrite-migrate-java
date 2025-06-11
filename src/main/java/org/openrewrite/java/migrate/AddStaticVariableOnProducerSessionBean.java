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
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

import java.util.Collections;

public class AddStaticVariableOnProducerSessionBean extends Recipe {
    @Override
    public String getDisplayName() {
        return "Adds static variable to @Produces field that are on session bean";
    }

    @Override
    public String getDescription() {
        return "Ensures that the fields annotated with @Produces which is inside the session bean (@Stateless, @Stateful, or @Singleton) are declared static.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(Preconditions.and(new UsesType<>("jakarta.enterprise.inject.Produces", false),
                        Preconditions.or(
                                new UsesType<>("jakarta.ejb.Singleton", false),
                                new UsesType<>("jakarta.ejb.Stateful", false),
                                new UsesType<>("jakarta.ejb.Stateless", false)
                        )),
                new JavaVisitor<ExecutionContext>() {
                    @Override
                    public J visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                        if (!multiVariable.hasModifier(J.Modifier.Type.Static) &&
                            hasAnnotation(multiVariable, "@jakarta.enterprise.inject.Produces") &&
                            isSessionBean()) {
                            return multiVariable.withModifiers(ListUtils.concat(multiVariable.getModifiers(),
                                    new J.Modifier(Tree.randomId(), Space.SINGLE_SPACE, Markers.EMPTY, null, J.Modifier.Type.Static, Collections.emptyList())));
                        }
                        return super.visitVariableDeclarations(multiVariable, ctx);
                    }

                    private boolean isSessionBean() {
                        J.ClassDeclaration j = getCursor().firstEnclosing(J.ClassDeclaration.class);
                        if (j == null) {
                            return false;
                        }
                        return hasAnnotation(j, "@jakarta.ejb.Singleton") ||
                               hasAnnotation(j, "@jakarta.ejb.Stateful") ||
                               hasAnnotation(j, "@jakarta.ejb.Stateless");
                    }

                    private boolean hasAnnotation(J j, String annotationPattern) {
                        return !FindAnnotations.find(j, annotationPattern).isEmpty();
                    }
                });
    }
}
