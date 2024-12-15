/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.java.migrate.lombok;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;

import static java.util.Comparator.comparing;

@Value
@EqualsAndHashCode(callSuper = false)
public class SummarizeSetter extends Recipe {

    @Override
    public String getDisplayName() {
        //language=markdown
        return "Summarize @Setter on fields to class level annotation";
    }

    @Override
    public String getDescription() {
        //language=markdown
        return "Substitutes a class level `@Setter` annotation for annotations on every field.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new Summarizer();
    }


    @Value
    @EqualsAndHashCode(callSuper = false)
    private static class Summarizer extends JavaIsoVisitor<ExecutionContext> {
        private static final String ALL_FIELDS_DECORATED_ACC = "ALL_FIELDS_DECORATED_ACC";

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {

            //initialize variable to store if all encountered fields have setters
            getCursor().putMessage(ALL_FIELDS_DECORATED_ACC, true);

            //delete methods, note down corresponding fields
            J.ClassDeclaration classDeclAfterVisit = super.visitClassDeclaration(classDecl, ctx);

            boolean allFieldsAnnotated = getCursor().pollNearestMessage(ALL_FIELDS_DECORATED_ACC);

            //only thing that can have changed is removal of setter methods
            //and something needs to have changed before we add an annotation at class level
            if (classDeclAfterVisit != classDecl && allFieldsAnnotated) {
                //Add annotation
                JavaTemplate template = JavaTemplate.builder("@Setter\n")
                            .imports("lombok.Setter")
                            .javaParser(JavaParser.fromJavaVersion().classpath("lombok"))
                        .build();

                return template.apply(
                        updateCursor(classDeclAfterVisit),
                        classDeclAfterVisit.getCoordinates().addAnnotation(comparing(J.Annotation::getSimpleName)));
            }
            return classDecl;
        }

        @Override
        public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations variableDecls, ExecutionContext ctx){

            boolean allFieldsAnnotatedSoFar = getCursor().getNearestMessage(ALL_FIELDS_DECORATED_ACC);
            if (!allFieldsAnnotatedSoFar) {
                return variableDecls;
            }
            J.VariableDeclarations visited = super.visitVariableDeclarations(variableDecls, ctx);

            boolean hasSetterAnnotation = variableDecls != visited;
            if (hasSetterAnnotation) {
                return fixFormat(variableDecls, visited, ctx);
            } else if (variableDecls.hasModifier(J.Modifier.Type.Final)
                    || variableDecls.hasModifier(J.Modifier.Type.Static)) {
                //final fields and static field don't need to have an annotation
                return visited;
            } else {
                getCursor().putMessageOnFirstEnclosing(J.ClassDeclaration.class, ALL_FIELDS_DECORATED_ACC, false);
            }
            return variableDecls;
        }

        private J.VariableDeclarations fixFormat(J.VariableDeclarations initial, J.VariableDeclarations visited, ExecutionContext ctx) {

            boolean isAnnotationOnLineAbove = initial.toString().contains("@Setter\n");

            boolean isTopAnnotationRemoved = !initial.getLeadingAnnotations().isEmpty()
                    && initial.getLeadingAnnotations()
                    .get(0)
                    .getSimpleName().equals("Setter");

            if (isAnnotationOnLineAbove && isTopAnnotationRemoved) {
                String minus1NewLine = visited.getPrefix().getWhitespace().replaceFirst("\n", "");
                visited = visited.withPrefix(visited.getPrefix().withWhitespace(minus1NewLine));
            }
            return autoFormat(visited, ctx);
        }

        @Override
        public J.Annotation visitAnnotation(J.Annotation annotation, ExecutionContext ctx) {
            boolean isSetterAnnotated = annotation.getSimpleName().equals("Setter")
                    && annotation.getArguments() == null; //no Access level, or other arguments

            return isSetterAnnotated
                    //should only trigger on field annotation, not class annotation
                    && getCursor().getParent().getValue() instanceof J.VariableDeclarations
                    ? null
                    : annotation;
        }
    }
}
