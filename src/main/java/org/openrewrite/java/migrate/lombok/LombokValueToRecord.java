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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.RemoveAnnotationVisitor;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.tree.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

@Value
@EqualsAndHashCode(callSuper = false)
public class LombokValueToRecord extends Recipe {

    @Option(displayName = "useExactToString",
            description = "When set the `toString` format from Lombok is used in the migrated record.")
    boolean useExactToString;

    @JsonCreator
    public LombokValueToRecord(final @JsonProperty("useExactToString") boolean useExactToString) {
        this.useExactToString = useExactToString;
    }

    @Override
    public String getDisplayName() {
        return "Convert `@lombok.Value` class to Record";
    }

    @Override
    public String getDescription() {
        return "Convert Lombok `@Value` annotated classes to standard Java Records.";
    }

    @Override
    public Set<String> getTags() {
        return Collections.singleton("lombok");
    }

    @Override
    public boolean causesAnotherCycle() {
        return true;
    }

    @Override
    public int maxCycles() {
        return 2;
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        final TreeVisitor<?, ExecutionContext> check = Preconditions.and(
                new UsesJavaVersion<>(17)
        );

        return Preconditions.check(check, new LombokValueToRecord.LombokValueToRecordVisitor(useExactToString));
    }

    private static class LombokValueToRecordVisitor extends JavaIsoVisitor<ExecutionContext> {

        private static final Pattern LOMBOK_ANNOTATION_PATTERN = Pattern.compile("^lombok.*");

        private static final JavaTemplate TO_STRING_TEMPLATE = JavaTemplate
                .builder("@Override public String toString() { return \"#{}(\" +\n#{}\n\")\"; }")
                .contextSensitive()
                .build();

        private static final String TO_STRING_MEMBER_LINE_PATTERN = "\"%s=\" + %s +";

        private static final String TO_STRING_MEMBER_DELIMITER = "\", \" +\n";

        private static final Map<String, Set<String>> RECORD_TYPE_TO_MEMBERS = new ConcurrentHashMap<>();

        private static final String STANDARD_GETTER_PREFIX = "get";

        private final boolean useExactToString;

        public LombokValueToRecordVisitor(final boolean useExactToString) {
            this.useExactToString = useExactToString;
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(
                final J.MethodInvocation method,
                final ExecutionContext executionContext
        ) {
            final J.MethodInvocation methodInvocation = super.visitMethodInvocation(method, executionContext);

            if (executionContext.getCycle() <= 1 || !isMethodInvocationOnRecordTypeClassMember(methodInvocation)) {
                return methodInvocation;
            }

            final J.Identifier methodName = methodInvocation.getName();
            return methodInvocation
                    .withName(methodName
                            .withSimpleName(getterMethodNameToFluentMethodName(methodName.getSimpleName()))
                    );
        }

        private static boolean isMethodInvocationOnRecordTypeClassMember(final J.MethodInvocation methodInvocation) {
            final Expression expression = methodInvocation.getSelect();
            if (!isClassExpression(expression)) {
                return false;
            }

            final JavaType.Class classType = (JavaType.Class) expression.getType();
            if (classType == null) {
                return false;
            }

            final String methodName = methodInvocation.getName().getSimpleName();

            return RECORD_TYPE_TO_MEMBERS.containsKey(classType.getFullyQualifiedName())
                    && methodName.startsWith(STANDARD_GETTER_PREFIX)
                    && RECORD_TYPE_TO_MEMBERS
                    .get(classType.getFullyQualifiedName())
                    .contains(getterMethodNameToFluentMethodName(methodName));
        }

        private static boolean isClassExpression(final @Nullable Expression expression) {
            return expression != null && (expression.getType() instanceof JavaType.Class);
        }

        private static String getterMethodNameToFluentMethodName(final String methodName) {
            final StringBuilder fluentMethodName = new StringBuilder(
                    methodName.replace(STANDARD_GETTER_PREFIX, ""));

            if (fluentMethodName.length() == 0) {
                return "";
            }

            final char firstMemberChar = fluentMethodName.charAt(0);
            fluentMethodName.setCharAt(0, Character.toLowerCase(firstMemberChar));

            return fluentMethodName.toString();
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(final J.ClassDeclaration cd, final ExecutionContext ctx) {
            J.ClassDeclaration classDeclaration = super.visitClassDeclaration(cd, ctx);

            if (!isRelevantClass(classDeclaration)) {
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

            if (useExactToString) {
                classDeclaration = addExactToStringMethod(classDeclaration, memberVariables);
            }

            addToRecordTypeState(classDeclaration, memberVariables);

            return maybeAutoFormat(cd, classDeclaration, ctx);
        }

        private J.ClassDeclaration addExactToStringMethod(final J.ClassDeclaration classDeclaration,
                                                          final List<J.VariableDeclarations> memberVariables) {
            return classDeclaration.withBody(TO_STRING_TEMPLATE
                    .apply(new Cursor(getCursor(), classDeclaration.getBody()),
                            classDeclaration.getBody().getCoordinates().lastStatement(),
                            classDeclaration.getSimpleName(),
                            memberVariablesToString(getMemberVariableNames(memberVariables))));
        }

        private static boolean isRelevantClass(final J.ClassDeclaration classDeclaration) {
            return classDeclaration.getType() != null
                    && !isRecord(classDeclaration)
                    && hasOnlyLombokValueAnnotation(classDeclaration)
                    && !hasGenericTypeParameter(classDeclaration)
                    && !hasExplicitMethods(classDeclaration)
                    && !hasExplicitConstructor(classDeclaration);
        }

        private static String memberVariablesToString(final Set<String> memberVariables) {
            return memberVariables
                    .stream()
                    .map(member -> String.format(TO_STRING_MEMBER_LINE_PATTERN, member, member))
                    .collect(Collectors.joining(TO_STRING_MEMBER_DELIMITER));
        }

        private static void addToRecordTypeState(final J.ClassDeclaration classDeclaration,
                                                 final List<J.VariableDeclarations> memberVariables
        ) {
            final JavaType.FullyQualified classType = requireNonNull(classDeclaration.getType(),
                    "Class type must not be null");

            RECORD_TYPE_TO_MEMBERS.putIfAbsent(
                    classType.getFullyQualifiedName(),
                    getMemberVariableNames(memberVariables));
        }

        private static boolean hasExplicitMethods(final J.ClassDeclaration classDeclaration) {
            return classDeclaration
                    .getBody()
                    .getStatements()
                    .stream()
                    .anyMatch(J.MethodDeclaration.class::isInstance);
        }

        private static Set<String> getMemberVariableNames(final List<J.VariableDeclarations> memberVariables) {
            return memberVariables
                    .stream()
                    .map(J.VariableDeclarations::getVariables)
                    .flatMap(List::stream)
                    .map(J.VariableDeclarations.NamedVariable::getSimpleName)
                    .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);
        }

        private static boolean hasGenericTypeParameter(final J.ClassDeclaration classDeclaration) {
            final List<J.TypeParameter> typeParameters = classDeclaration.getTypeParameters();

            return typeParameters != null && !typeParameters.isEmpty();
        }

        private static JavaType.Class buildRecordType(final J.ClassDeclaration classDeclaration) {
            requireNonNull(classDeclaration.getType(), "Class type must not be null");
            final String className = requireNonNull(classDeclaration.getType().getFullyQualifiedName(),
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

        private static boolean hasOnlyLombokValueAnnotation(final J.ClassDeclaration cd) {
            return cd.getAllAnnotations()
                    .stream()
                    .filter(annotation -> TypeUtils.isAssignableTo(LOMBOK_ANNOTATION_PATTERN, annotation.getType()))
                    .map(J.Annotation::getSimpleName)
                    .allMatch("Value"::equals);
        }
    }
}
