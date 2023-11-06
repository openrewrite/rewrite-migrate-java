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
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.J.Modifier;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.staticanalysis.ModifierOrder.sortModifiers;

@Value
@EqualsAndHashCode(callSuper = false)
public class MXBeanNonPublic extends Recipe {

    @Override
    public String getDisplayName() {
        return "MBean and MXBean interfaces must be public.";
    }

    @Override
    public String getDescription() {
        return "Sets visibility of MBean and MXBean interfaces to public.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new ClassImplementationVisitor();
    }

    public class ClassImplementationVisitor extends JavaIsoVisitor<ExecutionContext> {
        public boolean shouldUpdate(J.ClassDeclaration classDecl) {
            if (classDecl.hasModifier(J.Modifier.Type.Public) || !(classDecl.hasModifier(Modifier.Type.Abstract) || classDecl.getKind().name().equals("Interface"))) {
                return false;
            }

            List<J.Annotation> leadingAnnotations = classDecl.getLeadingAnnotations();
            for (J.Annotation leadingAnnotation : leadingAnnotations) {
                JavaType.Class type = (JavaType.Class) leadingAnnotation.getType();
                if (type.getFullyQualifiedName().equals("javax.management.MXBean")) {
                    List<Expression> args = leadingAnnotation.getArguments();
                    if (args == null || args.isEmpty()) {
                        return true;
                    }

                    for (Expression arg : args) {
                        if (arg instanceof J.Assignment) {
                            J.Assignment assignment = (J.Assignment) arg;
                            if (assignment.getVariable().toString().equals("value")) {
                                Expression assignmentExp = assignment.getAssignment();
                                if (assignmentExp instanceof J.Assignment) {
                                    J.Literal literal = (J.Literal) assignmentExp;
                                    return literal.getValue() == null || !literal.getValue().toString().equals("false");
                                }
                            }
                        }
                    }
                }
            }

            String className = classDecl.getName().getSimpleName();
            return className.endsWith("MXBean") || className.endsWith("MBean");
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration cd, ExecutionContext executionContext) {
            // need to visit sub-classes
            J.ClassDeclaration classDecl = super.visitClassDeclaration(cd, executionContext);

            boolean shouldUpdate = shouldUpdate(classDecl);
            // Don't make changes to classes that don't match the fully qualified name
            if (classDecl.getType() == null || !shouldUpdate) {
                return classDecl;
            }

            List<Modifier> oldModifiers = classDecl.getModifiers();
            List<Modifier> newModifiers = new ArrayList<Modifier>();
            for (Modifier modifier : oldModifiers) {
                if (modifier.getType() != Modifier.Type.Private && modifier.getType() != Modifier.Type.Protected) {
                    newModifiers.add(modifier);
                }
            }
            newModifiers.add(new J.Modifier(randomId(), Space.EMPTY, Markers.EMPTY, Modifier.Type.Public, emptyList()));
            newModifiers = sortModifiers(newModifiers);
            classDecl = maybeAutoFormat(classDecl, classDecl.withModifiers(newModifiers), executionContext);

            return classDecl;
        }
    }
}
