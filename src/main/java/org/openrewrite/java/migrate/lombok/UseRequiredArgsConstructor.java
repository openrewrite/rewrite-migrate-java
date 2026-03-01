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

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.marker.CompactConstructor;
import org.openrewrite.java.service.AnnotationService;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;

import java.util.List;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.joining;

@EqualsAndHashCode(callSuper = false)
@Value
public class UseRequiredArgsConstructor extends Recipe {

    private static final AnnotationMatcher REQUIRED_ARGS_MATCHER = new AnnotationMatcher("@lombok.RequiredArgsConstructor");
    private static final AnnotationMatcher OVERRIDE_MATCHER = new AnnotationMatcher("java.lang.Override");

    String displayName = "Use `@RequiredArgsConstructor` where applicable";

    String description = "Prefer the Lombok `@RequiredArgsConstructor` annotation over explicitly written out constructors " +
            "that only assign final fields.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.@Nullable MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                if (!method.isConstructor() ||
                        method.getMarkers().findFirst(CompactConstructor.class).isPresent()) {
                    return super.visitMethodDeclaration(method, ctx);
                }

                J.ClassDeclaration enclosing = getCursor().firstEnclosing(J.ClassDeclaration.class);
                if (enclosing == null) {
                    return super.visitMethodDeclaration(method, ctx);
                }

                // Skip if class already has @RequiredArgsConstructor
                if (enclosing.getLeadingAnnotations().stream().anyMatch(REQUIRED_ARGS_MATCHER::matches)) {
                    return super.visitMethodDeclaration(method, ctx);
                }

                List<J.VariableDeclarations.NamedVariable> requiredFields = LombokUtils.getRequiredFields(enclosing);
                if (requiredFields.isEmpty()) {
                    return super.visitMethodDeclaration(method, ctx);
                }

                if (!LombokUtils.isConstructorAssigningExactFields(method, requiredFields)) {
                    return super.visitMethodDeclaration(method, ctx);
                }

                AccessLevel accessLevel = LombokUtils.getAccessLevel(method);
                List<J.Annotation> constructorAnnotations = service(AnnotationService.class).getAllAnnotations(getCursor());
                constructorAnnotations.removeIf(OVERRIDE_MATCHER::matches);

                doAfterVisit(new JavaIsoVisitor<ExecutionContext>() {
                    @Override
                    public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                        if (TypeUtils.isOfType(classDecl.getType(), enclosing.getType())) {
                            String template = buildAnnotationTemplate("RequiredArgsConstructor", accessLevel, constructorAnnotations);
                            maybeAddImport("lombok.RequiredArgsConstructor");
                            if (accessLevel != AccessLevel.PUBLIC) {
                                maybeAddImport("lombok.AccessLevel");
                            }
                            for (J.Annotation ann : constructorAnnotations) {
                                String annType = ann.getAnnotationType().toString();
                                maybeAddImport(annType.contains(".") ? annType : guessImport(ann));
                            }
                            return JavaTemplate.builder(template)
                                    .imports("lombok.*")
                                    .javaParser(JavaParser.fromJavaVersion().classpathFromResources(ctx, "lombok"))
                                    .build()
                                    .apply(getCursor(), classDecl.getCoordinates().addAnnotation(comparing(J.Annotation::getSimpleName)));
                        }
                        return super.visitClassDeclaration(classDecl, ctx);
                    }
                });
                return null;
            }
        };
    }

    static String buildAnnotationTemplate(String annotationName, AccessLevel accessLevel, List<J.Annotation> constructorAnnotations) {
        String accessPart = accessLevel == AccessLevel.PUBLIC ? "" : "AccessLevel." + accessLevel.name();
        String onConstructorPart = "";
        if (!constructorAnnotations.isEmpty()) {
            onConstructorPart = "onConstructor_ = {" +
                    constructorAnnotations.stream()
                            .map(J.Annotation::toString)
                            .map(String::trim)
                            .collect(joining(", ")) +
                    "}";
        }

        String args = Stream.of(accessPart.isEmpty() ? null : "access = " + accessPart, onConstructorPart.isEmpty() ? null : onConstructorPart)
                .filter(s -> s != null)
                .collect(joining(", "));

        return "@" + annotationName + (args.isEmpty() ? "" : "(" + args + ")");
    }

    private static String guessImport(J.Annotation ann) {
        if (ann.getAnnotationType().getType() != null) {
            return TypeUtils.asFullyQualified(ann.getAnnotationType().getType()) != null
                    ? TypeUtils.asFullyQualified(ann.getAnnotationType().getType()).getFullyQualifiedName()
                    : ann.getAnnotationType().toString();
        }
        return ann.getAnnotationType().toString();
    }
}
