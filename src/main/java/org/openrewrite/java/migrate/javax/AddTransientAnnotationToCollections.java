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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;

import java.util.Comparator;
import java.util.regex.Pattern;

@Value
@EqualsAndHashCode(callSuper = false)
public class AddTransientAnnotationToCollections extends Recipe {

    @Override
    public String getDisplayName() {
        return "Unannotated collection attributes require a Transient annotation";
    }

    @Override
    public String getDescription() {
        return "In OpenJPA, attributes that inherit from the `java.util.Collection<E>` interface are not a default " +
               "persistent type, so these attributes are not persisted unless they are annotated. EclipseLink has a " +
               "different default behavior and attempts to persist these attributes to the database. To keep the OpenJPA " +
               "behavior of ignoring unannotated collection attributes, add the `javax.persistence.Transient` annotation " +
               "to these attributes in EclipseLink.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        Pattern collection = Pattern.compile("java.util.Collection");
        return Preconditions.check(
                // Only apply to JPA classes
                Preconditions.or(
                        new UsesType<>("javax.persistence.Entity", true),
                        new UsesType<>("javax.persistence.MappedSuperclass", true),
                        new UsesType<>("javax.persistence.Embeddable", true)
                ),
                new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                        // Exit if not Collection
                        if (!multiVariable.getType().isAssignableFrom(collection)) {
                            return multiVariable;
                        }
                        // Exit if already has JPA annotation
                        if (multiVariable.getLeadingAnnotations().stream()
                                .anyMatch(anno -> anno.getType().toString().contains("javax.persistence"))) {
                            return multiVariable;
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
                }
        );
    }
}