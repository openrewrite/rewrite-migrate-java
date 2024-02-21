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
import org.openrewrite.*;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.search.FindAnnotations;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Value
@EqualsAndHashCode(callSuper = false)
public class AddTransientAnnotationToEntity extends ScanningRecipe<AddTransientAnnotationToEntity.EntityAccumulator> {

    @Override
    public String getDisplayName() {
        return "Unannotated entity attributes require a Transient annotation";
    }

    @Override
    public String getDescription() {
        return "In OpenJPA, attributes that are themselves entity classes are not persisted by default. EclipseLink has " +
               "a different default behavior and tries to persist these attributes to the database. To keep the OpenJPA " +
               "behavior of ignoring unannotated entity attributes, add the `javax.persistence.Transient` annotation to " +
               "these attributes in EclipseLink.";
    }

    static class EntityAccumulator {
        private final Set<JavaType> entityClasses = new HashSet<>();

        public void addEntity(JavaType type) {
            entityClasses.add(type);
        }
        public boolean isEntity(JavaType type) {
            return entityClasses.contains(type);
        }
    }

    @Override
    public EntityAccumulator getInitialValue(ExecutionContext ctx) {
        return new EntityAccumulator();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(EntityAccumulator acc) {
        return Preconditions.check(
                new UsesType<>("javax.persistence.Entity", true),
                new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                        if (FindAnnotations.find(classDecl, "javax.persistence.Entity").isEmpty()) {
                            return classDecl;
                        }
                        // Collect @Entity classes
                        JavaType type = classDecl.getType();
                        if (type != null) {
                            acc.addEntity(type);
                        }
                        return classDecl;
                    }
                }
        );
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(EntityAccumulator acc) {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                // Exit if attribute is not an Entity class
                if (!acc.isEntity(multiVariable.getType())) {
                    return multiVariable;
                }
                // Exit if attribute is already JPA annotated
                List<J.Annotation> annos = multiVariable.getLeadingAnnotations();
                if (!annos.isEmpty()) {
                    for (J.Annotation anno : annos) {
                        if (anno.getType().toString().contains("javax.persistence")) {
                            return multiVariable;
                        }
                    }
                }
                // Add @Transient annotation
                maybeAddImport("javax.persistence.Transient");
                return JavaTemplate.builder("@Transient")
                        .contextSensitive()
                        .javaParser(JavaParser.fromJavaVersion().classpathFromResources(ctx, "javax.persistence-api-2.2"))
                        .imports("javax.persistence.Transient")
                        .build()
                        .apply(getCursor(), multiVariable.getCoordinates().addAnnotation(Comparator.comparing(J.Annotation::getSimpleName)));
            }
        };
    }
}