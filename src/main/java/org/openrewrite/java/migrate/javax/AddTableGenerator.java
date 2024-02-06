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
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.search.FindAnnotations;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Value
@EqualsAndHashCode(callSuper = false)
public class AddTableGenerator extends Recipe {
    @Override
    public String getDisplayName() {
        return "Attributes with automatically generated values require configuration";
    }

    @Override
    public String getDescription() {
        return "Adds missing `@TableGenerator` annotation and updates the `@GeneratedValue` annotation values when it uses automatically generated values.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(
                new UsesType<>("javax.persistence.GeneratedValue", false),
                new JavaIsoVisitor<ExecutionContext>() {
                    private final AnnotationMatcher GENERATED_VALUE = new AnnotationMatcher("@javax.persistence.GeneratedValue");
                    private final AnnotationMatcher GENERATED_VALUE_AUTO = new AnnotationMatcher("@javax.persistence.GeneratedValue(strategy=javax.persistence.GenerationType.AUTO)");

                    @Override
                    public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                        Set<J.Annotation> generatedValueAnnotations = FindAnnotations.find(multiVariable, "@javax.persistence.GeneratedValue");
                        if (generatedValueAnnotations.isEmpty()) {
                            return multiVariable;
                        }

                        J.Annotation generatedValueAnnotation = generatedValueAnnotations.iterator().next();
                        List<Expression> args = generatedValueAnnotation.getArguments();
                        if (!(args == null || args.isEmpty() || GENERATED_VALUE_AUTO.matches(generatedValueAnnotation))) {
                            return multiVariable;
                        }

                        J.VariableDeclarations updatedVariable = JavaTemplate.apply(
                                "@javax.persistence.TableGenerator(name = \"OPENJPA_SEQUENCE_TABLE\", table = \"OPENJPA_SEQUENCE_TABLE\", pkColumnName = \"ID\", valueColumnName = \"SEQUENCE_VALUE\", pkColumnValue = \"0\")",
                                getCursor(),
                                multiVariable.getCoordinates().addAnnotation(Comparator.comparing(J.Annotation::getSimpleName)));
                        return super.visitVariableDeclarations(updatedVariable, ctx);
                    }

                    @Override
                    public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
                        if (!GENERATED_VALUE.matches(annotation) && !GENERATED_VALUE_AUTO.matches(annotation)) {
                            return annotation;
                        }
                        return JavaTemplate.builder("strategy = javax.persistence.GenerationType.TABLE, generator = \"OPENJPA_SEQUENCE_TABLE\"")
                                .contextSensitive()
                                .build()
                                .apply(getCursor(), annotation.getCoordinates().replaceArguments());
                    }
                }
        );
    }
}
