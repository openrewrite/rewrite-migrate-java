/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.java.migrate.javax;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.RemoveAnnotation;
import org.openrewrite.java.search.FindAnnotations;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.HashSet;
import java.util.Set;

public class RemoveEmbeddableId extends ScanningRecipe<RemoveEmbeddableId.Accumulator> {

    @Override
    public String getDisplayName() {
        return "`@Embeddable` classes cannot have an `@Id` annotation when referenced by an `@EmbeddedId` annotation";
    }

    @Override
    public String getDescription() {
        return "According to the Java Persistence API (JPA) specification, if an entity defines an attribute with an " +
               "`@EmbeddedId` annotation, the embeddable class cannot contain an attribute with an `@Id` annotation. " +
               "If both the `@EmbeddedId` annotation and the `@Id` annotation are defined, " +
               "OpenJPA ignores the `@Id` annotation, whereas EclipseLink throws an exception.";
    }

    @Override
    public Accumulator getInitialValue(ExecutionContext ctx) {
        return new Accumulator();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Accumulator acc) {
        return Preconditions.check(
                new UsesType<>("javax.persistence.EmbeddedId", true),
                new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration cd, ExecutionContext ctx) {
                        if (!FindAnnotations.find(cd, "@javax.persistence.Entity").isEmpty()) {
                            return super.visitClassDeclaration(cd, ctx);
                        }
                        // Exit if class is not Entity
                        return cd;
                    }

                    @Override
                    public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                        // Exit if var does not have @EmbeddedId
                        if (FindAnnotations.find(multiVariable, "@javax.persistence.EmbeddedId").isEmpty()) {
                            return multiVariable;
                        }

                        // Collect the classes of objects tagged with @EmbeddedId
                        JavaType type = multiVariable.getType();
                        if (type != null) {
                            acc.addClass(type);
                        }
                        return multiVariable;
                    }
                }
        );
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Accumulator acc) {
        return Preconditions.check(
                Preconditions.and(
                        new UsesType<>("javax.persistence.Embeddable", true),
                        new UsesType<>("javax.persistence.Id", true)
                ),
                new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                        // Ensure class has @Embeddable annotation, and was tagged with @EmbeddedId from another class
                        if (!FindAnnotations.find(classDecl, "@javax.persistence.Embeddable").isEmpty() &&
                            acc.isEmbeddableClass(classDecl.getType())) {
                            // Remove @Id annotation from anything in the class (only applies to VariableDeclarations)
                            doAfterVisit(new RemoveAnnotation("javax.persistence.Id").getVisitor());
                        }
                        return classDecl;
                    }
                }
        );
    }

    public static class Accumulator {
        private final Set<JavaType> definedEmbeddableClasses = new HashSet<>();

        public void addClass(JavaType type) {
            definedEmbeddableClasses.add(type);
        }

        public boolean isEmbeddableClass(@Nullable JavaType type) {
            return definedEmbeddableClasses.contains(type);
        }
    }
}