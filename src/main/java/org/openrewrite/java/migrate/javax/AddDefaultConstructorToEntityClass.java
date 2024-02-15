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

import lombok.EqualsAndHashCode;
import org.openrewrite.*;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.search.FindAnnotations;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;

@EqualsAndHashCode(callSuper = false)
public class AddDefaultConstructorToEntityClass extends Recipe {
    @Override
    public String getDisplayName() {
        return "`@Entity` objects with constructors must also have a default constructor";
    }

    @Override
    public String getDescription() {
        return "When a Java Persistence API (JPA) entity class has a constructor with arguments, the class must also " +
               "have a default, no-argument constructor. The OpenJPA implementation automatically generates the " +
               "no-argument constructor, but the EclipseLink implementation does not.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                Preconditions.or(
                        new UsesType<>("javax.persistence.Entity", true),
                        new UsesType<>("javax.persistence.MappedSuperclass", true)),
                new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                        // Exit if class not annotated with either @Entity or @MappedSuperclass
                        if (FindAnnotations.find(classDecl, "javax.persistence.Entity").isEmpty()
                            && FindAnnotations.find(classDecl, "javax.persistence.MappedSuperclass").isEmpty()) {
                            return classDecl;
                        }

                        // Exit if class already has default no-arg constructor
                        if (classDecl.getBody().getStatements().stream()
                                .filter(statement -> statement instanceof J.MethodDeclaration)
                                .map(J.MethodDeclaration.class::cast)
                                .filter(J.MethodDeclaration::isConstructor)
                                .anyMatch(constructor -> constructor.getParameters().get(0) instanceof J.Empty)) {
                            return classDecl;
                        }

                        // Add default constructor with empty body
                        // For testing simplicity, adding as last statement in class body
                        return classDecl.withBody(JavaTemplate.builder("public #{}(){}")
                                .contextSensitive()
                                .build()
                                .apply(new Cursor(getCursor(), classDecl.getBody()),
                                        classDecl.getBody().getCoordinates().lastStatement(),
                                        classDecl.getSimpleName()
                                )
                        );
                    }
                }
        );
    }
}
