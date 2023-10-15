/*
 * Copyright 2023 the original author or authors.
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

import org.openrewrite.ExecutionContext;
import org.openrewrite.Preconditions;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.RemoveAnnotationVisitor;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public class LombokValueToRecord extends Recipe {
    @Override
    public String getDisplayName() {
        return "Convert Value class to Record";
    }

    @Override
    public String getDescription() {
        return "Convert Lombok Value annotated classes to standard java record classes in java 11 or higher.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("lombok");
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(5);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        final TreeVisitor<?, ExecutionContext> check = Preconditions.and(
                new UsesJavaVersion<>(17)
        );

        return Preconditions.check(check, new LombokValueToRecord.LombokValueToRecordVisitor());
    }

    private static class LombokValueToRecordVisitor extends JavaIsoVisitor<ExecutionContext> {

        @Override
        public J.ClassDeclaration visitClassDeclaration(final J.ClassDeclaration cd, final ExecutionContext ctx) {
            J.ClassDeclaration classDeclaration = super.visitClassDeclaration(cd, ctx);

            if (isRecord(classDeclaration)
                    || hasExplicitConstructor(classDeclaration)
                    || !hasOnlyLombokValueAnnotation(classDeclaration)
            ) {
                return classDeclaration;
            }

            final List<J.VariableDeclarations> memberVariables = findAllClassFields(classDeclaration);
            if (hasMemberVariableAssignments(memberVariables)) {
                return classDeclaration;
            }

            final List<Statement> bodyStatements = new ArrayList<>(classDeclaration.getBody().getStatements());
            bodyStatements.removeAll(memberVariables);

            classDeclaration = removeValueAnnotation(classDeclaration, ctx);
            classDeclaration = classDeclaration
                    .withKind(J.ClassDeclaration.Kind.Type.Record)
                    .withType(buildRecordType(classDeclaration))
                    .withBody(classDeclaration.getBody()
                            .withStatements(bodyStatements)
                    )
                    .withPrimaryConstructor(mapToConstructorArguments(memberVariables));

            return maybeAutoFormat(cd, classDeclaration, ctx);
        }

        private static JavaType.Class buildRecordType(final J.ClassDeclaration cd) {
            requireNonNull(cd.getType(), "Class type must not be null");
            final String className = requireNonNull(cd.getType().getFullyQualifiedName(),
                    "Fully qualified name of class must not be null");

            return JavaType.ShallowClass.build(className)
                    .withKind(JavaType.FullyQualified.Kind.Record);
        }

        private static boolean isRecord(final J.ClassDeclaration classDeclaration) {
            return J.ClassDeclaration.Kind.Type.Record.equals(classDeclaration.getKind());
        }

        private static boolean hasExplicitConstructor(final J.ClassDeclaration classDeclaration) {
            return classDeclaration.getPrimaryConstructor() != null || classDeclaration
                    .getBody()
                    .getStatements()
                    .stream()
                    .filter(J.MethodDeclaration.class::isInstance)
                    .map(J.MethodDeclaration.class::cast)
                    .map(J.MethodDeclaration::getMethodType)
                    .filter(Objects::nonNull)
                    .anyMatch(JavaType.Method::isConstructor);
        }

        private static List<Statement> mapToConstructorArguments(
                final List<J.VariableDeclarations> memberVariables
        ) {
            return memberVariables
                    .stream()
                    .map(it -> it
                            .withModifiers(Collections.emptyList())
                            .withVariables(it.getVariables())
                    )
                    .map(Statement.class::cast)
                    .collect(Collectors.toList());
        }

        private J.ClassDeclaration removeValueAnnotation(final J.ClassDeclaration cd, final ExecutionContext ctx) {
            maybeRemoveImport("lombok.Value");

            return new RemoveAnnotationVisitor(
                    new AnnotationMatcher("@lombok.Value")
            ).visitClassDeclaration(cd, ctx);
        }

        private static List<J.VariableDeclarations> findAllClassFields(final J.ClassDeclaration cd) {
            return new ArrayList<>(cd.getBody().getStatements())
                    .stream()
                    .filter(J.VariableDeclarations.class::isInstance)
                    .map(J.VariableDeclarations.class::cast)
                    .collect(Collectors.toList());
        }

        private static boolean hasMemberVariableAssignments(final List<J.VariableDeclarations> memberVariables) {
            return memberVariables
                    .stream()
                    .map(J.VariableDeclarations::getVariables)
                    .flatMap(List::stream)
                    .map(J.VariableDeclarations.NamedVariable::getInitializer)
                    .anyMatch(J.Literal.class::isInstance);
        }

        private static final Pattern LOMBOK_ANNOTATION_PATTERN = Pattern.compile("^lombok.*");

        private static boolean hasOnlyLombokValueAnnotation(final J.ClassDeclaration cd) {
            return cd.getAllAnnotations()
                    .stream()
                    .filter(annotation -> TypeUtils.isAssignableTo(LOMBOK_ANNOTATION_PATTERN, annotation.getType()))
                    .map(J.Annotation::getSimpleName)
                    .allMatch("Value"::equals);
        }
    }
}
