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
import java.util.List;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.marker.CompactConstructor;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.service.AnnotationService;

import static java.util.Comparator.comparing;

@EqualsAndHashCode(callSuper = false)
@Value
public class UseNoArgsConstructor extends Recipe {
    
    private static final AnnotationMatcher OVERRIDE_MATCHER =
        new AnnotationMatcher("java.lang.Override");

    String displayName = "Use `@NoArgsConstructor` where applicable";

    String description = "Prefer the Lombok `@NoArgsConstructor` annotation over explicitly written out constructors.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            @Override
            public J.@Nullable MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
				if (method.isConstructor() &&
						!method.getMarkers().findFirst(CompactConstructor.class).isPresent() &&
						method.getParameters().get(0) instanceof J.Empty &&
                        (method.getThrows() == null || method.getThrows().isEmpty()) &&
                        method.getBody() != null &&
                        method.getBody().getStatements().isEmpty() &&
                        method.getBody().getEnd().getComments().isEmpty()) {
                    J.ClassDeclaration enclosing = getCursor().firstEnclosing(J.ClassDeclaration.class);
                    AccessLevel accessLevel = LombokUtils.getAccessLevel(method);
                    List<J.Annotation> constructorAnnotations =
                    service(AnnotationService.class).getAllAnnotations(getCursor());
                    constructorAnnotations.removeIf(OVERRIDE_MATCHER::matches);

                    doAfterVisit(new JavaIsoVisitor<ExecutionContext>() {
                        @Override
                        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                            if (TypeUtils.isOfType(classDecl.getType(), enclosing.getType())) {
                                String template = UseRequiredArgsConstructor.buildAnnotationTemplate(
                                        "NoArgsConstructor", accessLevel, constructorAnnotations);
                                maybeAddImport("lombok.AccessLevel");
                                maybeAddImport("lombok.NoArgsConstructor");
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
                return super.visitMethodDeclaration(method, ctx);
            }
        };
    }
}
