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

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.AnnotationMatcher;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.RemoveAnnotationVisitor;
import org.openrewrite.java.search.UsesJavaVersion;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Statement;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

@Value
@EqualsAndHashCode(callSuper = false)
public class LombokValueToRecord extends ScanningRecipe<Map<String, Set<String>>> {

    private static final AnnotationMatcher LOMBOK_VALUE_MATCHER = new AnnotationMatcher("@lombok.Value");

    @Option(displayName = "Add a `toString()` implementation matching Lombok",
            description = "When set the `toString` format from Lombok is used in the migrated record.",
            required = false)
    @Nullable
    Boolean useExactToString;

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
    public Map<String, Set<String>> getInitialValue(ExecutionContext ctx) {
        return new ConcurrentHashMap<>();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(Map<String, Set<String>> acc) {
        TreeVisitor<?, ExecutionContext> check = Preconditions.and(
                new UsesJavaVersion<>(17),
                new UsesType<>("lombok.Value", false)
        );
        return Preconditions.check(check, new ScannerVisitor(acc));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(Map<String, Set<String>> recordTypesToMembers) {
        return new LombokValueToRecord.LombokValueToRecordVisitor(useExactToString, recordTypesToMembers);
    }


    @RequiredArgsConstructor
    private static class ScannerVisitor extends JavaIsoVisitor<ExecutionContext> {
        private final Map<String, Set<String>> acc;

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
            J.ClassDeclaration cd = super.visitClassDeclaration(classDecl, ctx);
            if (!isRelevantClass(cd)) {
                return cd;
            }

            List<J.VariableDeclarations> memberVariables = findAllClassFields(cd).collect(toList());
            if (hasMemberVariableAssignments(memberVariables)) {
                return cd;
            }

            assert cd.getType() != null : "Class type must not be null";
            acc.putIfAbsent(
                    cd.getType().getFullyQualifiedName(),
                    getMemberVariableNames(memberVariables));

            return cd;
        }

        private boolean isRelevantClass(J.ClassDeclaration classDeclaration) {
            return classDeclaration.getType() != null
                    && !J.ClassDeclaration.Kind.Type.Record.equals(classDeclaration.getKind())
                    && classDeclaration.getAllAnnotations().stream().allMatch(LOMBOK_VALUE_MATCHER::matches)
                    && !hasGenericTypeParameter(classDeclaration)
                    && classDeclaration.getBody().getStatements().stream().allMatch(this::isRecordCompatibleField);
        }

        private boolean hasGenericTypeParameter(J.ClassDeclaration classDeclaration) {
            List<J.TypeParameter> typeParameters = classDeclaration.getTypeParameters();
            return typeParameters != null && !typeParameters.isEmpty();
        }

        private boolean isRecordCompatibleField(Statement statement) {
            if (!(statement instanceof J.VariableDeclarations)) {
                return false;
            }
            J.VariableDeclarations variableDeclarations = (J.VariableDeclarations) statement;
            if (variableDeclarations.getModifiers().stream().anyMatch(modifier -> modifier.getType() == J.Modifier.Type.Static)) {
                return false;
            }
            if (!variableDeclarations.getAllAnnotations().isEmpty()) {
                return false;
            }
            return true;
        }

        private boolean hasMemberVariableAssignments(List<J.VariableDeclarations> memberVariables) {
            return memberVariables
                    .stream()
                    .map(J.VariableDeclarations::getVariables)
                    .flatMap(List::stream)
                    .map(J.VariableDeclarations.NamedVariable::getInitializer)
                    .anyMatch(Objects::nonNull);
        }

    }

    private static class LombokValueToRecordVisitor extends JavaIsoVisitor<ExecutionContext> {
        private static final JavaTemplate TO_STRING_TEMPLATE = JavaTemplate
                .builder("@Override public String toString() { return \"#{}(\" +\n#{}\n\")\"; }")
                .contextSensitive()
                .build();

        private static final String TO_STRING_MEMBER_LINE_PATTERN = "\"%s=\" + %s +";
        private static final String TO_STRING_MEMBER_DELIMITER = "\", \" +\n";
        private static final String STANDARD_GETTER_PREFIX = "get";

        private final Boolean useExactToString;
        private final Map<String, Set<String>> recordTypeToMembers;

        public LombokValueToRecordVisitor(Boolean useExactToString, Map<String, Set<String>> recordTypeToMembers) {
            this.useExactToString = useExactToString;
            this.recordTypeToMembers = recordTypeToMembers;
        }

        @Override
        public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {
            J.MethodInvocation methodInvocation = super.visitMethodInvocation(method, ctx);

            if (!isMethodInvocationOnRecordTypeClassMember(methodInvocation)) {
                return methodInvocation;
            }

            J.Identifier methodName = methodInvocation.getName();
            return methodInvocation
                    .withName(methodName
                            .withSimpleName(getterMethodNameToFluentMethodName(methodName.getSimpleName()))
                    );
        }

        private boolean isMethodInvocationOnRecordTypeClassMember(J.MethodInvocation methodInvocation) {
            Expression expression = methodInvocation.getSelect();
            if (!isClassExpression(expression)) {
                return false;
            }

            JavaType.Class classType = (JavaType.Class) expression.getType();
            if (classType == null) {
                return false;
            }

            String methodName = methodInvocation.getName().getSimpleName();
            String classFqn = classType.getFullyQualifiedName();

            return recordTypeToMembers.containsKey(classFqn)
                    && methodName.startsWith(STANDARD_GETTER_PREFIX)
                    && recordTypeToMembers.get(classFqn).contains(getterMethodNameToFluentMethodName(methodName));
        }

        private static boolean isClassExpression(@Nullable Expression expression) {
            return expression != null && (expression.getType() instanceof JavaType.Class);
        }

        private static String getterMethodNameToFluentMethodName(String methodName) {
            StringBuilder fluentMethodName = new StringBuilder(
                    methodName.replace(STANDARD_GETTER_PREFIX, ""));

            if (fluentMethodName.length() == 0) {
                return "";
            }

            char firstMemberChar = fluentMethodName.charAt(0);
            fluentMethodName.setCharAt(0, Character.toLowerCase(firstMemberChar));

            return fluentMethodName.toString();
        }

        private static List<Statement> mapToConstructorArguments(List<J.VariableDeclarations> memberVariables) {
            return memberVariables
                    .stream()
                    .map(it -> it
                            .withModifiers(Collections.emptyList())
                            .withVariables(it.getVariables())
                    )
                    .map(Statement.class::cast)
                    .collect(toList());
        }

        private J.ClassDeclaration addExactToStringMethod(J.ClassDeclaration classDeclaration,
                                                          List<J.VariableDeclarations> memberVariables) {
            return classDeclaration.withBody(TO_STRING_TEMPLATE
                    .apply(new Cursor(getCursor(), classDeclaration.getBody()),
                            classDeclaration.getBody().getCoordinates().lastStatement(),
                            classDeclaration.getSimpleName(),
                            memberVariablesToString(getMemberVariableNames(memberVariables))));
        }

        private static String memberVariablesToString(Set<String> memberVariables) {
            return memberVariables
                    .stream()
                    .map(member -> String.format(TO_STRING_MEMBER_LINE_PATTERN, member, member))
                    .collect(Collectors.joining(TO_STRING_MEMBER_DELIMITER));
        }

        private static JavaType.Class buildRecordType(J.ClassDeclaration classDeclaration) {
            assert classDeclaration.getType() != null : "Class type must not be null";
            String className = classDeclaration.getType().getFullyQualifiedName();

            return JavaType.ShallowClass.build(className)
                    .withKind(JavaType.FullyQualified.Kind.Record);
        }

        @Override
        public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration cd, ExecutionContext ctx) {
            J.ClassDeclaration classDeclaration = super.visitClassDeclaration(cd, ctx);
            JavaType.FullyQualified classType = classDeclaration.getType();

            if (classType == null || !recordTypeToMembers.containsKey(classType.getFullyQualifiedName())) {
                return classDeclaration;
            }

            List<J.VariableDeclarations> memberVariables = findAllClassFields(classDeclaration)
                    .collect(toList());

            List<Statement> bodyStatements = new ArrayList<>(classDeclaration.getBody().getStatements());
            bodyStatements.removeAll(memberVariables);

            doAfterVisit(new RemoveAnnotationVisitor(LOMBOK_VALUE_MATCHER));
            classDeclaration = classDeclaration
                    .withKind(J.ClassDeclaration.Kind.Type.Record)
                    .withType(buildRecordType(classDeclaration))
                    .withBody(classDeclaration.getBody()
                            .withStatements(bodyStatements)
                    )
                    .withPrimaryConstructor(mapToConstructorArguments(memberVariables));

            if (useExactToString != null && useExactToString) {
                classDeclaration = addExactToStringMethod(classDeclaration, memberVariables);
            }

            return maybeAutoFormat(cd, classDeclaration, ctx);
        }
    }

    private static Stream<J.VariableDeclarations> findAllClassFields(J.ClassDeclaration cd) {
        return cd.getBody().getStatements()
                .stream()
                .filter(J.VariableDeclarations.class::isInstance)
                .map(J.VariableDeclarations.class::cast);
    }

    private static Set<String> getMemberVariableNames(List<J.VariableDeclarations> memberVariables) {
        return memberVariables
                .stream()
                .map(J.VariableDeclarations::getVariables)
                .flatMap(List::stream)
                .map(J.VariableDeclarations.NamedVariable::getSimpleName)
                .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);
    }
}

