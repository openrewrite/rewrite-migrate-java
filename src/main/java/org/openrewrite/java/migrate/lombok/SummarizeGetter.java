/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.java.migrate.lombok;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.service.AnnotationService;
import org.openrewrite.java.tree.J;

import java.util.List;

import static java.util.Comparator.comparing;

@Value
@EqualsAndHashCode(callSuper = false)
public class SummarizeGetter extends Recipe {

    private static final AnnotationMatcher ANNOTATION_MATCHER = new AnnotationMatcher("lombok.Getter");

    @Override
    public String getDisplayName() {
        return "Summarize `@Getter` on fields to class level annotation";
    }

    @Override
    public String getDescription() {
        //language=markdown
        return "Substitutes a class level `@Getter` annotation for annotations on every field.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        JavaIsoVisitor<ExecutionContext> visitor = new JavaIsoVisitor<ExecutionContext>() {

            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                if (allFieldsHaveGetter(classDecl)) {
                    return addGetterToClass(removeGetterFromFields(classDecl));
                }
                return super.visitClassDeclaration(classDecl, ctx);
            }

            private boolean allFieldsHaveGetter(J.ClassDeclaration classDecl) {
                AnnotationService annotationService = service(AnnotationService.class);
                return classDecl.getBody().getStatements().stream()
                        .filter(J.VariableDeclarations.class::isInstance)
                        .map(J.VariableDeclarations.class::cast)
                        .allMatch(vd -> {
                            List<J.Annotation> allAnnotations = annotationService.getAllAnnotations(new Cursor(getCursor(), vd));
                            return !allAnnotations.isEmpty() &&
                                    allAnnotations.stream().anyMatch(annotation -> ANNOTATION_MATCHER.matches(annotation) &&
                                            (annotation.getArguments() == null || annotation.getArguments().isEmpty() || annotation.getArguments().get(0) instanceof J.Empty));
                        });
            }

            private J.ClassDeclaration removeGetterFromFields(J.ClassDeclaration classDecl) {
                return classDecl.withBody(classDecl.getBody().withStatements(ListUtils.map(classDecl.getBody().getStatements(), s -> {
                    if (s instanceof J.VariableDeclarations) {
                        J.VariableDeclarations vd = (J.VariableDeclarations) s;
                        return vd.withLeadingAnnotations(ListUtils.map(vd.getLeadingAnnotations(),
                                a -> ANNOTATION_MATCHER.matches(a) ? null : a));
                    }
                    return s;
                })));
            }

            private J.ClassDeclaration addGetterToClass(J.ClassDeclaration classDecl) {
                return JavaTemplate.builder("@Getter")
                        .imports("lombok.Getter")
                        .javaParser(JavaParser.fromJavaVersion().classpath("lombok"))
                        .build().apply(updateCursor(classDecl), classDecl.getCoordinates().addAnnotation(comparing(J.Annotation::getSimpleName)));
            }
        };
        return Preconditions.check(new UsesType<>("lombok.Getter", false), visitor);
    }
}
