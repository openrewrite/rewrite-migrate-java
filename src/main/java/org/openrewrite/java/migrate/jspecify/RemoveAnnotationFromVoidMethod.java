/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.java.migrate.jspecify;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.TypeMatcher;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.TypeUtils;

import java.util.List;

@EqualsAndHashCode(callSuper = false)
@Value
public class RemoveAnnotationFromVoidMethod extends Recipe {

    @Option(displayName = "Annotation type",
            description = "The type of annotation to remove from methods returning `void`.",
            example = "org.jetbrains.annotations.*ull*")
    String annotationType;

    String displayName = "Remove annotation from `void` method";

    String description = "Nullability annotations are meaningless on methods returning `void`, " +
                         "and type-use annotations such as JSpecify's `@Nullable` are not permitted there. " +
                         "This recipe removes such annotations so they are not carried over by a later `ChangeType`.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>(annotationType, null), new JavaIsoVisitor<ExecutionContext>() {
            final TypeMatcher typeMatcher = new TypeMatcher(annotationType);

            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                J.MethodDeclaration md = super.visitMethodDeclaration(method, ctx);

                if (md.getReturnTypeExpression() == null ||
                    md.getReturnTypeExpression().getType() != JavaType.Primitive.Void) {
                    return md;
                }

                // Annotations following a modifier, as in `public @Nullable static void m()`
                md = md.withModifiers(ListUtils.map(md.getModifiers(),
                        m -> m.withAnnotations(removeMatching(m.getAnnotations()))));

                // Annotations preceding type parameters, as in `public @Nullable <T> void m()`
                J.TypeParameters typeParameters = md.getAnnotations().getTypeParameters();
                if (typeParameters != null) {
                    md = md.getAnnotations().withTypeParameters(
                            typeParameters.withAnnotations(removeMatching(typeParameters.getAnnotations())));
                }

                // Annotations directly preceding the return type, as in `public @Nullable void m()`
                if (md.getReturnTypeExpression() instanceof J.AnnotatedType) {
                    J.AnnotatedType annotatedType = (J.AnnotatedType) md.getReturnTypeExpression();
                    List<J.Annotation> annotations = annotatedType.getAnnotations();
                    List<J.Annotation> remaining = removeMatching(annotations);
                    if (remaining.isEmpty()) {
                        md = md.withReturnTypeExpression(annotatedType.getTypeExpression().withPrefix(
                                annotatedType.getPrefix().withWhitespace(annotations.get(0).getPrefix().getWhitespace())));
                    } else if (remaining != annotations) {
                        Space firstAnnotationPrefix = annotations.get(0).getPrefix();
                        md = md.withReturnTypeExpression(annotatedType.withAnnotations(
                                ListUtils.mapFirst(remaining, a -> a.withPrefix(firstAnnotationPrefix))));
                    }
                }

                List<J.Annotation> original = md.getLeadingAnnotations();
                List<J.Annotation> leading = removeMatching(original);
                if (leading == original) {
                    return md;
                }

                Space firstPrefix = original.get(0).getPrefix();
                if (!leading.isEmpty()) {
                    leading = ListUtils.mapFirst(leading, a -> a.withPrefix(firstPrefix));
                    return md.withLeadingAnnotations(leading);
                }

                md = md.withLeadingAnnotations(leading);
                if (!md.getModifiers().isEmpty()) {
                    md = md.withModifiers(Space.formatFirstPrefix(md.getModifiers(),
                            Space.firstPrefix(md.getModifiers()).withWhitespace(firstPrefix.getWhitespace())));
                } else if (md.getPadding().getTypeParameters() != null) {
                    md = md.getPadding().withTypeParameters(md.getPadding().getTypeParameters().withPrefix(
                            md.getPadding().getTypeParameters().getPrefix().withWhitespace(firstPrefix.getWhitespace())));
                } else {
                    md = md.withReturnTypeExpression(md.getReturnTypeExpression().withPrefix(
                            md.getReturnTypeExpression().getPrefix().withWhitespace(firstPrefix.getWhitespace())));
                }
                return md;
            }

            private List<J.Annotation> removeMatching(List<J.Annotation> annotations) {
                return ListUtils.map(annotations, a -> {
                    if (matchesType(a)) {
                        maybeRemoveImport(TypeUtils.asFullyQualified(a.getType()));
                        return null;
                    }
                    return a;
                });
            }

            private boolean matchesType(J.Annotation ann) {
                JavaType.FullyQualified fq = TypeUtils.asFullyQualified(ann.getType());
                return fq != null && typeMatcher.matches(fq);
            }
        });
    }
}
