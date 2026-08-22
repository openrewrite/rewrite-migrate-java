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
package org.openrewrite.java.migrate.lombok;

import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static java.util.Comparator.comparing;

abstract class LombokAccessorOnType extends Recipe {

    protected abstract Class<? extends Annotation> accessorAnnotation();

    protected abstract boolean isEligibleForTypeLevelAccessor(J.VariableDeclarations field,
                                                              J.VariableDeclarations.NamedVariable variable);

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        Class<? extends Annotation> accessorAnnotation = accessorAnnotation();
        AnnotationMatcher accessorMatcher = new AnnotationMatcher("@" + accessorAnnotation.getName());
        return new JavaIsoVisitor<ExecutionContext>() {
            private final Map<UUID, Set<UUID>> fieldsToHoistByClass = new HashMap<>();

            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                Set<UUID> fieldsToHoist = fieldsToHoist(classDecl, accessorMatcher);
                if (fieldsToHoist == null) {
                    return super.visitClassDeclaration(classDecl, ctx);
                }

                fieldsToHoistByClass.put(classDecl.getId(), fieldsToHoist);
                maybeAddImport(accessorAnnotation.getName());
                J.ClassDeclaration cd = JavaTemplate.builder("@" + accessorAnnotation.getSimpleName())
                        .imports(accessorAnnotation.getName())
                        .javaParser(JavaParser.fromJavaVersion().classpathFromResources(ctx, "lombok"))
                        .build()
                        .apply(getCursor(), classDecl.getCoordinates().addAnnotation(comparing(J.Annotation::getSimpleName)));
                return super.visitClassDeclaration(cd, ctx);
            }

            @Override
            public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations variableDeclarations,
                                                                     ExecutionContext ctx) {
                J.VariableDeclarations vd = super.visitVariableDeclarations(variableDeclarations, ctx);
                J.ClassDeclaration enclosing = getCursor().firstEnclosing(J.ClassDeclaration.class);
                if (enclosing == null) {
                    return vd;
                }

                Set<UUID> fieldsToHoist = fieldsToHoistByClass.get(enclosing.getId());
                if (fieldsToHoist == null || !fieldsToHoist.contains(vd.getId())) {
                    return vd;
                }

                List<J.Annotation> annotations = new ArrayList<>(vd.getLeadingAnnotations());
                annotations.removeIf(accessorMatcher::matches);
                return maybeAutoFormat(vd, vd.withLeadingAnnotations(annotations), ctx);
            }
        };
    }

    private @Nullable Set<UUID> fieldsToHoist(J.ClassDeclaration classDecl, AnnotationMatcher accessorMatcher) {
        if (classDecl.getKind() != J.ClassDeclaration.Kind.Type.Class ||
                classDecl.getLeadingAnnotations().stream().anyMatch(accessorMatcher::matches)) {
            return null;
        }

        Set<UUID> fieldsToHoist = new HashSet<>();
        for (Statement statement : classDecl.getBody().getStatements()) {
            if (!(statement instanceof J.VariableDeclarations)) {
                continue;
            }

            J.VariableDeclarations field = (J.VariableDeclarations) statement;
            boolean hasEligibleField = false;
            boolean hasIneligibleField = false;
            for (J.VariableDeclarations.NamedVariable variable : field.getVariables()) {
                if (isEligibleForTypeLevelAccessor(field, variable)) {
                    hasEligibleField = true;
                } else {
                    hasIneligibleField = true;
                }
            }

            // A single declaration can annotate multiple variables; do not remove an annotation partially.
            if (hasEligibleField && hasIneligibleField) {
                return null;
            }
            if (!hasEligibleField) {
                continue;
            }

            J.Annotation annotation = findAccessorAnnotation(field, accessorMatcher);
            if (annotation == null ||
                    (annotation.getArguments() != null && !annotation.getArguments().isEmpty())) {
                return null;
            }
            fieldsToHoist.add(field.getId());
        }
        return fieldsToHoist.isEmpty() ? null : fieldsToHoist;
    }

    private static J.@Nullable Annotation findAccessorAnnotation(J.VariableDeclarations field,
                                                                  AnnotationMatcher accessorMatcher) {
        J.Annotation result = null;
        for (J.Annotation annotation : field.getLeadingAnnotations()) {
            if (accessorMatcher.matches(annotation)) {
                if (result != null) {
                    return null;
                }
                result = annotation;
            }
        }
        return result;
    }

    protected static boolean isStaticField(J.VariableDeclarations field,
                                           J.VariableDeclarations.NamedVariable variable) {
        JavaType.Variable fieldType = variable.getName().getFieldType();
        return field.hasModifier(J.Modifier.Type.Static) ||
                (fieldType != null && fieldType.hasFlags(Flag.Static));
    }

    protected static boolean isFinalField(J.VariableDeclarations field,
                                          J.VariableDeclarations.NamedVariable variable) {
        JavaType.Variable fieldType = variable.getName().getFieldType();
        return field.hasModifier(J.Modifier.Type.Final) ||
                (fieldType != null && fieldType.hasFlags(Flag.Final));
    }

    protected static boolean hasSyntheticFieldName(J.VariableDeclarations.NamedVariable variable) {
        return variable.getSimpleName().startsWith("$");
    }
}
