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
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.RemoveAnnotationVisitor;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.service.AnnotationService;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JContainer;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

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
            final AnnotationMatcher annotationMatcher = new AnnotationMatcher("@" + annotationType);

            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                J.MethodDeclaration md = super.visitMethodDeclaration(method, ctx);
                if (md.getReturnTypeExpression() == null ||
                    md.getReturnTypeExpression().getType() != JavaType.Primitive.Void ||
                    !service(AnnotationService.class).matches(getCursor(), annotationMatcher)) {
                    return md;
                }

                for (J.Annotation annotation : md.getAllAnnotations()) {
                    if (annotationMatcher.matches(annotation)) {
                        maybeRemoveImport(TypeUtils.asFullyQualified(annotation.getType()));
                    }
                }

                // Leave parameter and body annotations alone by only visiting the method header
                J.MethodDeclaration header = md.getPadding().withParameters(JContainer.empty()).withBody(null);
                header = (J.MethodDeclaration) new RemoveAnnotationVisitor(annotationMatcher)
                        .visitNonNull(header, ctx, getCursor().getParentOrThrow());
                return header.getPadding().withParameters(md.getPadding().getParameters()).withBody(md.getBody());
            }
        });
    }
}
