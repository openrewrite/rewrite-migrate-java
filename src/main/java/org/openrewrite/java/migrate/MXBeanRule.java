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
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.J.Modifier;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;
import org.openrewrite.marker.SearchResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static org.openrewrite.Tree.randomId;
import static org.openrewrite.java.tree.J.ClassDeclaration.Kind.Type.Interface;
import static org.openrewrite.staticanalysis.ModifierOrder.sortModifiers;

@Value
@EqualsAndHashCode(callSuper = false)
public class MXBeanRule extends Recipe {

    @Override
    public String getDisplayName() {
        return "MBean and MXBean interfaces must be public";
    }

    @Override
    public String getDescription() {
        return "Sets visibility of MBean and MXBean interfaces to public.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                Preconditions.and(
                        new JavaVisitor<ExecutionContext>() {
                            @Override
                            public J visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                                if (!classDecl.hasModifier(Modifier.Type.Public) && classDecl.getKind() == Interface) {
                                    return SearchResult.found(classDecl, "Not yet public interface");
                                }
                                return super.visitClassDeclaration(classDecl, ctx);
                            }
                        },
                        Preconditions.or(
                                new UsesType("javax.management.MXBean", true),
                                new JavaVisitor<ExecutionContext>() {
                                    @Override
                                    public J visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                                        String className = classDecl.getName().getSimpleName();
                                        if (className.endsWith("MXBean") || className.endsWith("MBean")) {
                                            return SearchResult.found(classDecl, "Matching class name");
                                        }
                                        return super.visitClassDeclaration(classDecl, ctx);
                                    }
                                })
                ), new ClassImplementationVisitor());
    }

    private static class ClassImplementationVisitor extends JavaIsoVisitor<ExecutionContext> {
        private static final AnnotationMatcher MX_BEAN = new AnnotationMatcher("@javax.management.MXBean");
        private static final AnnotationMatcher MX_BEAN_VALUE_TRUE = new AnnotationMatcher("@javax.management.MXBean(value=true)");

        private boolean shouldUpdate(J.ClassDeclaration classDecl) {
            // Annotation with no argument, or explicit true argument
            List<J.Annotation> leadingAnnotations = classDecl.getLeadingAnnotations();
            Optional<J.Annotation> firstAnnotation = leadingAnnotations.stream().filter(MX_BEAN::matches).findFirst();
            if (firstAnnotation.isPresent()) {
                List<Expression> arguments = firstAnnotation.get().getArguments();
                return arguments == null || arguments.isEmpty() || MX_BEAN_VALUE_TRUE.matches(firstAnnotation.get());
            }
            // Suffix naming convention
            String className = classDecl.getName().getSimpleName();
            return className.endsWith("MXBean") || className.endsWith("MBean");
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDeclaration, ExecutionContext ctx) {
            J.ClassDeclaration cd = super.visitClassDeclaration(classDeclaration, ctx);
            if (!shouldUpdate(cd)) {
                return cd;
            }

            List<Modifier> modifiers = new ArrayList<>(cd.getModifiers());
            modifiers.removeIf(modifier -> modifier.getType() == Modifier.Type.Private ||
                    modifier.getType() == Modifier.Type.Protected ||
                    modifier.getType() == Modifier.Type.Abstract);
            modifiers.add(new J.Modifier(randomId(), Space.EMPTY, Markers.EMPTY, Modifier.Type.Public, emptyList()));
            return maybeAutoFormat(cd, cd.withModifiers(sortModifiers(modifiers)), ctx);
        }
    }
}
