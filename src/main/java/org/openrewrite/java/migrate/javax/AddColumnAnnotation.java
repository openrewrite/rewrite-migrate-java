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
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.search.FindAnnotations;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Value
@EqualsAndHashCode(callSuper = false)
public class AddColumnAnnotation extends Recipe {

    @Override
    public String getDisplayName() {
        return "ElementCollection annotations must be accompanied by a defined Column annotation";
    }

    @Override
    public String getDescription() {
        return "When an attribute is annotated with @ElementCollection, a separate table is created for the attribute that includes the attribute \n" +
               "  ID and value. In OpenJPA, the column for the annotated attribute is named element, whereas EclipseLink names the column based on \n" +
               "  the name of the attribute. To remain compatible with tables that were created with OpenJPA, add a @Column annotation with the name \n" +
               "  attribute set to element.";
    }

    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                //
                new UsesType<>("javax.persistence.ElementCollection", true),
                new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                        // Exit if var does not have @ElementCollection
                        Set<J.Annotation> elementCollectionAnnotations = FindAnnotations.find(multiVariable, "@javax.persistence.ElementCollection");
                        if (elementCollectionAnnotations.isEmpty()) {
                            return multiVariable;
                        }

                        // Check if var has @Column with `name` attribute
                        Set<J.Annotation> columnAnnotations = FindAnnotations.find(multiVariable, "@javax.persistence.Column");
                        if (!columnAnnotations.isEmpty()) {
                            J.Annotation columnAnnotation = columnAnnotations.iterator().next();
                            List<Expression> args = columnAnnotation.getArguments();
                            // Exit if @Column has "name" attribute already
                            if (!(args == null || args.isEmpty())) {
                                // @Column had attributes
                                for (Expression arg : args) {
                                    J.Assignment attribute = (J.Assignment) arg;
                                    if (attribute.getVariable().toString().equals("name")) {
                                        // @Column had "name" attribute
                                        return multiVariable;
                                    }
                                }
                            }

                            // Update @Column annotation with `name = "element"`
                            return JavaTemplate.builder("#{} = \"#{}\"")
                                    .contextSensitive()
                                    .build()
                                    .apply(
                                            getCursor(),
                                            columnAnnotation.getCoordinates().replaceArguments(),
                                            "name", "element"
                                    );
                        }

                        J.VariableDeclarations updatedVariable = JavaTemplate.apply(
                                "@javax.persistence.Column(name = \"element\"",
                                getCursor(),
                                multiVariable.getCoordinates().addAnnotation(Comparator.comparing(J.Annotation::getSimpleName))
                        );
                        return updatedVariable;
                    }
                }
        );
    }
}
