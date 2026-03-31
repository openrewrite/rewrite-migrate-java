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
import org.openrewrite.java.tree.*;

import org.jspecify.annotations.Nullable;

import static java.util.Collections.singletonList;

@EqualsAndHashCode(callSuper = false)
@Value
public class MoveAnnotationToArrayType extends Recipe {

    @Option(displayName = "Annotation type",
            description = "The type of annotation to move to the array type. " +
                          "Should target the pre-migration annotation type to avoid changing the semantics " +
                          "of pre-existing type-use annotations on object arrays.",
            example = "javax.annotation.*ull*")
    String annotationType;

    String displayName = "Move annotation to array type";

    String description = "When an annotation like `@Nullable` is applied to an array type in declaration position, " +
                         "this recipe moves it to the array brackets. " +
                         "For example, `@Nullable byte[]` becomes `byte @Nullable[]`. " +
                         "Best used before `ChangeType` in a migration pipeline, targeting the pre-migration annotation type.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>(annotationType, null), new JavaIsoVisitor<ExecutionContext>() {
            final TypeMatcher typeMatcher = new TypeMatcher(annotationType);

            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                J.MethodDeclaration md = super.visitMethodDeclaration(method, ctx);

                if (!(md.getReturnTypeExpression() instanceof J.ArrayType)) {
                    return md;
                }

                J.@Nullable Annotation match = md.getLeadingAnnotations().stream()
                        .filter(this::matchesType)
                        .findFirst()
                        .orElse(null);
                if (match == null) {
                    return md;
                }

                J.Annotation toRemove = match;
                md = md.withLeadingAnnotations(ListUtils.map(md.getLeadingAnnotations(), a -> a == toRemove ? null : a));

                J.ArrayType arrayType = (J.ArrayType) md.getReturnTypeExpression();
                arrayType = arrayType.withAnnotations(
                        singletonList(match.withPrefix(Space.SINGLE_SPACE)));
                md = md.withReturnTypeExpression(arrayType);
                if (md.getLeadingAnnotations().isEmpty()) {
                    md = md.withReturnTypeExpression(arrayType.withPrefix(
                            arrayType.getPrefix().withWhitespace("")));
                }
                return autoFormat(md, arrayType, ctx, getCursor().getParentOrThrow());
            }

            @Override
            public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                J.VariableDeclarations mv = super.visitVariableDeclarations(multiVariable, ctx);

                if (!(mv.getTypeExpression() instanceof J.ArrayType)) {
                    return mv;
                }

                J.@Nullable Annotation match = mv.getLeadingAnnotations().stream()
                        .filter(this::matchesType)
                        .findFirst()
                        .orElse(null);
                if (match == null) {
                    return mv;
                }

                J.Annotation toRemove = match;
                mv = mv.withLeadingAnnotations(ListUtils.map(mv.getLeadingAnnotations(), a -> a == toRemove ? null : a));

                J.ArrayType arrayType = (J.ArrayType) mv.getTypeExpression();
                arrayType = arrayType.withAnnotations(
                        singletonList(match.withPrefix(Space.SINGLE_SPACE)));
                if (mv.getLeadingAnnotations().isEmpty()) {
                    arrayType = arrayType.withPrefix(arrayType.getPrefix().withWhitespace(""));
                }
                mv = mv.withTypeExpression(arrayType);
                return autoFormat(mv, arrayType, ctx, getCursor().getParentOrThrow());
            }

            private boolean matchesType(J.Annotation ann) {
                JavaType.FullyQualified fq = TypeUtils.asFullyQualified(ann.getType());
                return fq != null && typeMatcher.matches(fq);
            }
        });
    }
}
