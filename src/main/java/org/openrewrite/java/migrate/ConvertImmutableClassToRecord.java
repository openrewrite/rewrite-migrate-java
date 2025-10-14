/*
 * Copyright 2024 the original author or authors.
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
package org.openrewrite.java.migrate;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.java.ChangeMethodName;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.tree.J;

import java.util.*;
import java.util.stream.Collectors;

@Value
@EqualsAndHashCode(callSuper = false)
public class ConvertImmutableClassToRecord extends Recipe {

    @Option(displayName = "Package pattern",
            description = "A glob pattern to match packages where classes should be converted to records. " +
                         "If not specified, the recipe will be applied to all packages.",
            example = "com.example.**",
            required = false)
    @Nullable
    String packagePattern;

    @Override
    public String getDisplayName() {
        return "Convert immutable class to record";
    }

    @Override
    public String getDescription() {
        //language=Markdown
        return "Converts immutable classes to Java records. This is a composite recipe that:\n" +
                "  1. Identifies classes that meet record conversion criteria\n" +
                "  2. Converts eligible classes to records\n" +
                "  3. Updates all getter method calls to use record accessor syntax\n" +
                "\n" +
                "  A class is eligible for conversion if it:\n" +
                "  - Has only private fields\n" +
                "  - Has corresponding getter methods for all fields\n" +
                "  - Has no setter methods\n" +
                "  - Does not extend another class\n" +
                "  - Is effectively immutable";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new ConvertImmutableClassToRecordVisitor();
    }

    private class ConvertImmutableClassToRecordVisitor extends JavaVisitor<ExecutionContext> {

        @Override
        public J visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
            if (!isEligibleForRecordConversion(classDecl) || !matchesPackagePattern(classDecl)) {
                return classDecl;
            }

            // Get all getter methods that will need to be updated
            List<J.MethodDeclaration> getterMethods = getGetterMethods(classDecl);
            String classTypeFqn = getFullyQualifiedName(classDecl);

            // Schedule method name changes for all getters after this transformation
            for (J.MethodDeclaration getter : getterMethods) {
                String fieldName = getFieldNameFromGetter(getter);
                if (fieldName != null) {
                    String oldMethodPattern = classTypeFqn + " " + getter.getSimpleName() + "()";
                    doAfterVisit(new ChangeMethodName(oldMethodPattern, fieldName, true, null).getVisitor());
                }
            }

            // Transform the class to a record
            return transformToRecord(classDecl);
        }

        private J.ClassDeclaration transformToRecord(J.ClassDeclaration classDecl) {
            List<J.VariableDeclarations> fields = getPrivateFields(classDecl);
            List<J.MethodDeclaration> utilityMethods = extractUtilityMethods(classDecl);

            // Create record components string
            StringBuilder recordComponents = new StringBuilder();
            for (int i = 0; i < fields.size(); i++) {
                J.VariableDeclarations field = fields.get(i);
                for (J.VariableDeclarations.NamedVariable var : field.getVariables()) {
                    if (recordComponents.length() > 0) {
                        recordComponents.append(", ");
                    }

                    // Include field annotations
                    StringBuilder fieldComponent = new StringBuilder();
                    if (!field.getLeadingAnnotations().isEmpty()) {
                        for (J.Annotation annotation : field.getLeadingAnnotations()) {
                            fieldComponent.append("@").append(annotation.getAnnotationType().toString()).append(" ");
                        }
                    }
                    fieldComponent.append(field.getTypeExpression()).append(" ").append(var.getSimpleName());
                    recordComponents.append(fieldComponent);
                }
            }

            // Build record template
            StringBuilder recordTemplate = new StringBuilder();

            // Add class-level annotations and modifiers
            if (!classDecl.getLeadingAnnotations().isEmpty() || !classDecl.getModifiers().isEmpty()) {
                for (J.Annotation annotation : classDecl.getLeadingAnnotations()) {
                    recordTemplate.append("@").append(annotation.getAnnotationType().toString()).append(" ");
                }
                for (J.Modifier modifier : classDecl.getModifiers()) {
                    if (modifier.getType() != J.Modifier.Type.Final) { // Records are implicitly final
                        recordTemplate.append(modifier.getType().toString().toLowerCase()).append(" ");
                    }
                }
            }

            recordTemplate.append("record ").append(classDecl.getSimpleName()).append("(")
                    .append(recordComponents).append(")");

            if (!classDecl.getImplements().isEmpty()) {
                recordTemplate.append(" implements ");
                for (int i = 0; i < classDecl.getImplements().size(); i++) {
                    if (i > 0) recordTemplate.append(", ");
                    recordTemplate.append("#{any()}");
                }
            }

            recordTemplate.append(" { ");
            for (int i = 0; i < utilityMethods.size(); i++) {
                recordTemplate.append("#{any()} ");
            }
            recordTemplate.append("}");

            // Prepare template parameters
            List<Object> templateParams = new ArrayList<>();
            if (!classDecl.getImplements().isEmpty()) {
                for (J j : classDecl.getImplements()) {
                    templateParams.add(j);
                }
            }
            for (J.MethodDeclaration method : utilityMethods) {
                templateParams.add(method);
            }

            JavaTemplate template = JavaTemplate.builder(recordTemplate.toString()).build();
            return template.apply(updateCursor(classDecl), classDecl.getCoordinates().replace(), templateParams.toArray());
        }

        private boolean isEligibleForRecordConversion(J.ClassDeclaration classDecl) {
            // Skip if class extends another class (records can't extend classes)
            if (classDecl.getExtends() != null) {
                return false;
            }

            // Skip if class is not a regular class
            if (classDecl.getKind() != J.ClassDeclaration.Kind.Type.Class) {
                return false;
            }

            List<J.VariableDeclarations> fields = getPrivateFields(classDecl);
            if (fields.isEmpty()) {
                return false;
            }

            // Check if all fields have corresponding getters
            Set<String> fieldNames = fields.stream()
                    .flatMap(field -> field.getVariables().stream())
                    .map(J.VariableDeclarations.NamedVariable::getSimpleName)
                    .collect(Collectors.toSet());

            Set<String> getterFields = getGetterMethods(classDecl).stream()
                    .map(this::getFieldNameFromGetter)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            return fieldNames.equals(getterFields) && !hasSetterMethods(classDecl);
        }

        private List<J.MethodDeclaration> extractUtilityMethods(J.ClassDeclaration classDecl) {
            return classDecl.getBody().getStatements().stream()
                    .filter(J.MethodDeclaration.class::isInstance)
                    .map(J.MethodDeclaration.class::cast)
                    .filter(method -> !isConstructor(method) && !isGetterMethod(method) && !isSetterMethod(method))
                    .collect(Collectors.toList());
        }

        private List<J.VariableDeclarations> getPrivateFields(J.ClassDeclaration classDecl) {
            return classDecl.getBody().getStatements().stream()
                    .filter(J.VariableDeclarations.class::isInstance)
                    .map(J.VariableDeclarations.class::cast)
                    .filter(field -> field.hasModifier(J.Modifier.Type.Private))
                    .collect(Collectors.toList());
        }

        private List<J.MethodDeclaration> getGetterMethods(J.ClassDeclaration classDecl) {
            return classDecl.getBody().getStatements().stream()
                    .filter(J.MethodDeclaration.class::isInstance)
                    .map(J.MethodDeclaration.class::cast)
                    .filter(this::isGetterMethod)
                    .collect(Collectors.toList());
        }

        private boolean hasSetterMethods(J.ClassDeclaration classDecl) {
            return classDecl.getBody().getStatements().stream()
                    .filter(J.MethodDeclaration.class::isInstance)
                    .map(J.MethodDeclaration.class::cast)
                    .anyMatch(this::isSetterMethod);
        }

        private boolean isConstructor(J.MethodDeclaration method) {
            return method.isConstructor();
        }

        private boolean isGetterMethod(J.MethodDeclaration method) {
            String methodName = method.getSimpleName();
            return (methodName.startsWith("get") || methodName.startsWith("is"))
                    && method.getParameters().isEmpty()
                    && method.getReturnTypeExpression() != null;
        }

        private boolean isSetterMethod(J.MethodDeclaration method) {
            String methodName = method.getSimpleName();
            return methodName.startsWith("set")
                    && method.getParameters().size() == 1;
        }

        @Nullable
        private String getFieldNameFromGetter(J.MethodDeclaration getter) {
            String methodName = getter.getSimpleName();
            if (methodName.startsWith("get") && methodName.length() > 3) {
                return Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
            } else if (methodName.startsWith("is") && methodName.length() > 2) {
                return Character.toLowerCase(methodName.charAt(2)) + methodName.substring(3);
            }
            return null;
        }

        private boolean matchesPackagePattern(J.ClassDeclaration classDecl) {
            if (packagePattern == null || packagePattern.trim().isEmpty()) {
                return true; // No pattern specified, match all
            }

            String packageName = getPackageName(classDecl);
            if (packageName == null) {
                return packagePattern.equals("*"); // Default package
            }

            // Simple glob pattern matching
            String pattern = packagePattern.replace("**", ".*").replace("*", "[^.]*");
            return packageName.matches(pattern);
        }

        private String getPackageName(J.ClassDeclaration classDecl) {
            J.CompilationUnit cu = getCursor().firstEnclosing(J.CompilationUnit.class);
            if (cu != null && cu.getPackageDeclaration() != null) {
                return cu.getPackageDeclaration().getExpression().toString();
            }
            return null;
        }

        private String getFullyQualifiedName(J.ClassDeclaration classDecl) {
            String packageName = getPackageName(classDecl);
            if (packageName != null) {
                return packageName + "." + classDecl.getSimpleName();
            }
            return classDecl.getSimpleName();
        }
    }
}
