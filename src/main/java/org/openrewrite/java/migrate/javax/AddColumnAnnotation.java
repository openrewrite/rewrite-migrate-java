/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.java.migrate.javax;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.AddOrUpdateAnnotationAttribute;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.search.FindAnnotations;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;

import java.util.Comparator;

@Value
@EqualsAndHashCode(callSuper = false)
public class AddColumnAnnotation extends Recipe {

    @Override
    public String getDisplayName() {
        return "`@ElementCollection` annotations must be accompanied by a defined `@Column` annotation";
    }

    @Override
    public String getDescription() {
        return "When an attribute is annotated with `@ElementCollection`, a separate table is created for the attribute that includes the attribute \n" +
               "ID and value. In OpenJPA, the column for the annotated attribute is named element, whereas EclipseLink names the column based on \n" +
               "the name of the attribute. To remain compatible with tables that were created with OpenJPA, add a `@Column` annotation with the name \n" +
               "attribute set to element.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {

        return Preconditions.check(
                new UsesType<>("javax.persistence.ElementCollection", true),
                new JavaIsoVisitor<ExecutionContext>() {
                    boolean visitedTopLevelClass = false;

                    @Override
                    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                        // if top-level class has already been checked, continue running recipe
                        if (visitedTopLevelClass) {
                            return super.visitClassDeclaration(classDecl, ctx);
                        }
                        visitedTopLevelClass = true;
                        if (!FindAnnotations.find(classDecl, "@javax.persistence.Entity").isEmpty()) {
                            return super.visitClassDeclaration(classDecl, ctx);
                        }
                        // Exit if class is not @Entity
                        return classDecl;
                    }

                    @Override
                    public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                        // Exit if var does not have @ElementCollection or has @Transient
                        if (FindAnnotations.find(multiVariable, "@javax.persistence.ElementCollection").isEmpty() ||
                            !FindAnnotations.find(multiVariable, "@javax.persistence.Transient").isEmpty()) {
                            return multiVariable;
                        }

                        // Create and add @Column annotation
                        if (FindAnnotations.find(multiVariable, "@javax.persistence.Column").isEmpty()) {
                            maybeAddImport("javax.persistence.Column");
                            return JavaTemplate.builder("@Column(name = \"element\")")
                                    .javaParser(JavaParser.fromJavaVersion().classpathFromResources(ctx, "javax.persistence-api-2.2"))
                                    .imports("javax.persistence.Column")
                                    .build()
                                    .apply(getCursor(), multiVariable.getCoordinates().addAnnotation(Comparator.comparing(J.Annotation::getSimpleName)));
                        }

                        // Update existing @Column annotation
                        J.VariableDeclarations updatedVariable = (J.VariableDeclarations) new AddOrUpdateAnnotationAttribute(
                                "javax.persistence.Column", "name", "element", null, true, null)
                                .getVisitor().visit(multiVariable, ctx, getCursor().getParentTreeCursor());
                        return super.visitVariableDeclarations(updatedVariable, ctx);
                    }
                }
        );
    }
}
