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
import lombok.Value;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.search.FindAnnotations;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;

// TODO: possibly rename to EntityNoArgConstructor?
@Value
@EqualsAndHashCode(callSuper = false)
public class AddDefaultConstructorToEntityClass extends Recipe {
    @Override
    public String getDisplayName() {
        return "@Entity objects with constructors must also have a default constructor";
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
                        new UsesType<>("javax.persistence.MappedSuperclass", true)
                ),
                new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration cd, ExecutionContext ctx) {
                        // Exit if class not annotated with either @Entity or @MappedSuperclass
                        if (FindAnnotations.find(cd, "javax.persistence.Entity").isEmpty()
                            && FindAnnotations.find(cd, "javax.persistence.MappedSuperclass").isEmpty()) {
                            return cd;
                        }

                        // Check if class has a default no-arg constructor
                        boolean hasDefaultConstructor = cd.getBody().getStatements().stream()
                                .filter(statement -> statement instanceof J.MethodDeclaration)
                                .map(J.MethodDeclaration.class::cast)
                                .filter(methodDeclaration -> methodDeclaration.getMethodType().getName().equals("<constructor>"))
                                .anyMatch(constructor -> constructor.getParameters().get(0).getClass().equals(J.Empty.class));

                        // Exit if class already has default no-arg constructor
                        if (hasDefaultConstructor) {
                            return cd;
                        }

                        // Add default constructor with empty body
                        // For testing simplicity, adding as last statement in class body
                        cd = cd.withBody(
                                JavaTemplate.builder("public #{}(){}")
                                        .contextSensitive()
                                        .build()
                                        .apply(
                                                new Cursor(getCursor(), cd.getBody()),
                                                cd.getBody().getCoordinates().lastStatement(),
                                                cd.getSimpleName()
                                        )
                        );
                        cd = autoFormat(cd, ctx);
                        return cd;
                    }
                }
        );
    }
}