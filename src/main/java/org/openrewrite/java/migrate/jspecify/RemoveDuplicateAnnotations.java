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
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.TypeMatcher;
import org.openrewrite.java.search.SemanticallyEqual;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = false)
@Value
public class RemoveDuplicateAnnotations extends Recipe {

    @Option(displayName = "Annotation type",
            description = "The type of annotation to deduplicate.",
            example = "org.jspecify.annotations.*")
    String annotationType;

    String displayName = "Remove duplicate annotations";

    String description = "Remove annotations that are repeated on the same element, keeping only the first occurrence. " +
                         "Duplicates typically arise when several distinct annotations are migrated to a single new annotation, " +
                         "such as when both `javax.annotation.Nullable` and `javax.annotation.CheckForNull` become " +
                         "`org.jspecify.annotations.Nullable`.";

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new UsesType<>(annotationType, null), new JavaIsoVisitor<ExecutionContext>() {
            final TypeMatcher typeMatcher = new TypeMatcher(annotationType);

            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, ctx);
                return cd.withLeadingAnnotations(removeDuplicates(cd.getLeadingAnnotations()));
            }

            @Override
            public J.MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                J.MethodDeclaration md = super.visitMethodDeclaration(method, ctx);
                return md.withLeadingAnnotations(removeDuplicates(md.getLeadingAnnotations()));
            }

            @Override
            public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext ctx) {
                J.VariableDeclarations mv = super.visitVariableDeclarations(multiVariable, ctx);
                return mv.withLeadingAnnotations(removeDuplicates(mv.getLeadingAnnotations()));
            }

            @Override
            public J.Modifier visitModifier(J.Modifier modifier, ExecutionContext ctx) {
                J.Modifier m = super.visitModifier(modifier, ctx);
                return m.withAnnotations(removeDuplicates(m.getAnnotations()));
            }

            @Override
            public J.AnnotatedType visitAnnotatedType(J.AnnotatedType annotatedType, ExecutionContext ctx) {
                J.AnnotatedType at = super.visitAnnotatedType(annotatedType, ctx);
                return at.withAnnotations(removeDuplicates(at.getAnnotations()));
            }

            @Override
            public J.ArrayType visitArrayType(J.ArrayType arrayType, ExecutionContext ctx) {
                J.ArrayType at = super.visitArrayType(arrayType, ctx);
                return at.withAnnotations(removeDuplicates(at.getAnnotations()));
            }

            @Override
            public J.Identifier visitIdentifier(J.Identifier identifier, ExecutionContext ctx) {
                J.Identifier id = super.visitIdentifier(identifier, ctx);
                return id.withAnnotations(removeDuplicates(id.getAnnotations()));
            }

            private @Nullable List<J.Annotation> removeDuplicates(@Nullable List<J.Annotation> annotations) {
                if (annotations == null || annotations.size() < 2) {
                    return annotations;
                }
                List<J.Annotation> kept = new ArrayList<>(annotations.size());
                return ListUtils.filter(annotations, annotation -> {
                    if (matchesType(annotation) &&
                            kept.stream().anyMatch(earlier -> SemanticallyEqual.areEqual(earlier, annotation))) {
                        return false;
                    }
                    kept.add(annotation);
                    return true;
                });
            }

            private boolean matchesType(J.Annotation annotation) {
                JavaType.FullyQualified fq = TypeUtils.asFullyQualified(annotation.getType());
                return fq != null && typeMatcher.matches(fq);
            }
        });
    }
}
