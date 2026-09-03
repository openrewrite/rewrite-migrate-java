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

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.Space;
import org.openrewrite.java.tree.Statement;
import org.openrewrite.java.tree.TypeUtils;

import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Comparator.comparing;
import static org.openrewrite.java.tree.J.Modifier.Type.Final;
import static org.openrewrite.java.tree.J.Modifier.Type.Private;
import static org.openrewrite.java.tree.J.Modifier.Type.Public;

@EqualsAndHashCode(callSuper = false)
@Value
public class UseLombokValue extends Recipe {

    private static final Set<String> CONFLICTING_LOMBOK_ANNOTATIONS = new HashSet<>(Arrays.asList(
            "Accessors", "AllArgsConstructor", "Builder", "Data", "EqualsAndHashCode", "FieldDefaults",
            "Getter", "NoArgsConstructor", "RequiredArgsConstructor", "Setter", "SuperBuilder",
            "ToString", "Value"
    ));

    String displayName = "Use `@Value` where applicable";

    String description = "Prefer Lombok's `@Value` annotation over boilerplate in immutable value classes.";

    Set<String> tags = singleton("lombok");

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaIsoVisitor<ExecutionContext>() {
            private final Map<UUID, ValueClass> valueClasses = new HashMap<>();

            @Override
            public J.ClassDeclaration visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext ctx) {
                ValueClass valueClass = findValueClass(classDecl);
                if (valueClass == null) {
                    return super.visitClassDeclaration(classDecl, ctx);
                }

                valueClasses.put(classDecl.getId(), valueClass);
                maybeAddImport("lombok.Value");
                J.ClassDeclaration cd = JavaTemplate.builder("@Value")
                        .imports("lombok.Value")
                        .javaParser(JavaParser.fromJavaVersion().classpathFromResources(ctx, "lombok"))
                        .build()
                        .apply(getCursor(), classDecl.getCoordinates().addAnnotation(comparing(J.Annotation::getSimpleName)));
                cd = super.visitClassDeclaration(cd, ctx);
                return cd.withModifiers(ListUtils.map(cd.getModifiers(), modifier ->
                        modifier.getType() == Final ? null : modifier));
            }

            @Override
            public J.@Nullable MethodDeclaration visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext ctx) {
                ValueClass valueClass = enclosingValueClass();
                if (valueClass != null && valueClass.getMethodIds().contains(method.getId())) {
                    return null;
                }
                return super.visitMethodDeclaration(method, ctx);
            }

            @Override
            public J.VariableDeclarations visitVariableDeclarations(J.VariableDeclarations variableDeclarations, ExecutionContext ctx) {
                J.VariableDeclarations vd = super.visitVariableDeclarations(variableDeclarations, ctx);
                ValueClass valueClass = enclosingValueClass();
                if (valueClass != null && valueClass.getFieldIds().contains(vd.getId())) {
                    J.VariableDeclarations updated = vd.withModifiers(emptyList());
                    if (updated.getTypeExpression() != null) {
                        updated = updated.withTypeExpression(updated.getTypeExpression().withPrefix(Space.EMPTY));
                    }
                    return updated;
                }
                return vd;
            }

            private @Nullable ValueClass enclosingValueClass() {
                J.ClassDeclaration enclosing = getCursor().firstEnclosing(J.ClassDeclaration.class);
                return enclosing == null ? null : valueClasses.get(enclosing.getId());
            }
        };
    }

    private static @Nullable ValueClass findValueClass(J.ClassDeclaration classDecl) {
        if (classDecl.getType() == null ||
                !classDecl.hasModifier(Final) ||
                hasConflictingLombokAnnotation(classDecl)) {
            return null;
        }

        List<J.VariableDeclarations> fields = new ArrayList<>();
        List<J.MethodDeclaration> methods = new ArrayList<>();
        for (Statement statement : classDecl.getBody().getStatements()) {
            if (statement instanceof J.VariableDeclarations) {
                fields.add((J.VariableDeclarations) statement);
            } else if (statement instanceof J.MethodDeclaration) {
                methods.add((J.MethodDeclaration) statement);
            }
        }

        if (fields.isEmpty() || fields.stream().anyMatch(field -> !isValueField(field))) {
            return null;
        }

        List<J.MethodDeclaration> constructors = new ArrayList<>();
        for (J.MethodDeclaration method : methods) {
            if (method.isConstructor()) {
                constructors.add(method);
            }
        }
        if (constructors.size() != 1) {
            return null;
        }

        J.MethodDeclaration constructor = constructors.get(0);
        if (!isValueConstructor(constructor, classDecl)) {
            return null;
        }

        Set<UUID> methodIds = new HashSet<>();
        methodIds.add(constructor.getId());
        for (J.VariableDeclarations field : fields) {
            for (J.VariableDeclarations.NamedVariable variable : field.getVariables()) {
                Optional<J.MethodDeclaration> getter = findGetter(methods, variable);
                if (!getter.isPresent()) {
                    return null;
                }
                methodIds.add(getter.get().getId());
            }
        }

        if (!hasExplicitObjectMethods(methods)) {
            return null;
        }

        Set<UUID> fieldIds = new HashSet<>();
        for (J.VariableDeclarations field : fields) {
            fieldIds.add(field.getId());
        }
        return new ValueClass(fieldIds, methodIds);
    }

    private static boolean hasConflictingLombokAnnotation(J.ClassDeclaration classDecl) {
        return classDecl.getLeadingAnnotations().stream()
                .map(J.Annotation::getSimpleName)
                .anyMatch(CONFLICTING_LOMBOK_ANNOTATIONS::contains);
    }

    private static boolean isValueField(J.VariableDeclarations field) {
        if (!field.getAllAnnotations().isEmpty() ||
                !hasOnlyModifiers(field.getModifiers(), Private, Final) ||
                field.getVariables().isEmpty()) {
            return false;
        }
        for (J.VariableDeclarations.NamedVariable variable : field.getVariables()) {
            if (variable.getType() == null) {
                return false;
            }
        }
        return true;
    }

    private static boolean isValueConstructor(J.MethodDeclaration constructor, J.ClassDeclaration classDecl) {
        if (!hasOnlyModifiers(constructor.getModifiers(), Public) ||
                !constructor.getAllAnnotations().isEmpty() ||
                !isEmpty(constructor.getThrows()) ||
                !isEmpty(constructor.getTypeParameters()) ||
                hasAnnotatedOrVarargsParameters(constructor)) {
            return false;
        }
        return LombokUtils.isConstructorAssigningExactFields(constructor, LombokUtils.getRequiredFields(classDecl));
    }

    private static boolean hasAnnotatedOrVarargsParameters(J.MethodDeclaration method) {
        for (Statement parameter : method.getParameters()) {
            if (!(parameter instanceof J.VariableDeclarations)) {
                return true;
            }
            J.VariableDeclarations variableDeclarations = (J.VariableDeclarations) parameter;
            if (!variableDeclarations.getAllAnnotations().isEmpty() || variableDeclarations.getVarargs() != null) {
                return true;
            }
        }
        return false;
    }

    private static Optional<J.MethodDeclaration> findGetter(List<J.MethodDeclaration> methods,
                                                             J.VariableDeclarations.NamedVariable field) {
        String getterName = LombokUtils.deriveGetterMethodName(field.getType(), field.getSimpleName());
        List<J.MethodDeclaration> getters = new ArrayList<>();
        for (J.MethodDeclaration method : methods) {
            if (method.getSimpleName().equals(getterName) &&
                    LombokUtils.isGetter(method) &&
                    hasOnlyModifiers(method.getModifiers(), Public) &&
                    method.getAllAnnotations().isEmpty() &&
                    isEmpty(method.getThrows()) &&
                    isEmpty(method.getTypeParameters())) {
                getters.add(method);
            }
        }
        return getters.size() == 1 ? Optional.of(getters.get(0)) : Optional.empty();
    }

    private static boolean hasExplicitObjectMethods(List<J.MethodDeclaration> methods) {
        boolean hasEquals = false;
        boolean hasHashCode = false;
        boolean hasToString = false;
        for (J.MethodDeclaration method : methods) {
            hasEquals |= isEquals(method);
            hasHashCode |= isHashCode(method);
            hasToString |= isToString(method);
        }
        return hasEquals && hasHashCode && hasToString;
    }

    private static boolean isEquals(J.MethodDeclaration method) {
        if (!"equals".equals(method.getSimpleName()) ||
                method.getType() != JavaType.Primitive.Boolean ||
                method.getParameters().size() != 1 ||
                !(method.getParameters().get(0) instanceof J.VariableDeclarations)) {
            return false;
        }
        J.VariableDeclarations parameter = (J.VariableDeclarations) method.getParameters().get(0);
        return parameter.getVariables().size() == 1 &&
                isType(parameter.getVariables().get(0).getType(), "java.lang.Object");
    }

    private static boolean isHashCode(J.MethodDeclaration method) {
        return "hashCode".equals(method.getSimpleName()) &&
                method.getType() == JavaType.Primitive.Int &&
                hasNoParameters(method);
    }

    private static boolean isToString(J.MethodDeclaration method) {
        return "toString".equals(method.getSimpleName()) &&
                isType(method.getType(), "java.lang.String") &&
                hasNoParameters(method);
    }

    private static boolean hasNoParameters(J.MethodDeclaration method) {
        return method.getParameters().isEmpty() ||
                (method.getParameters().size() == 1 && method.getParameters().get(0) instanceof J.Empty);
    }

    private static boolean isEmpty(@Nullable List<?> values) {
        return values == null || values.isEmpty();
    }

    private static boolean isType(@Nullable JavaType type, String fullyQualifiedName) {
        JavaType.FullyQualified fullyQualified = TypeUtils.asFullyQualified(type);
        return fullyQualified != null && fullyQualifiedName.equals(fullyQualified.getFullyQualifiedName());
    }

    private static boolean hasOnlyModifiers(List<J.Modifier> modifiers, J.Modifier.Type... expectedModifiers) {
        if (modifiers.size() != expectedModifiers.length) {
            return false;
        }
        Set<J.Modifier.Type> expected = new HashSet<>(Arrays.asList(expectedModifiers));
        return modifiers.stream().map(J.Modifier::getType).allMatch(expected::contains);
    }

    @Value
    private static class ValueClass {
        Set<UUID> fieldIds;
        Set<UUID> methodIds;
    }
}
